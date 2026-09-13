package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.HandlerThread
import android.provider.Telephony
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.room.CtrlDatabase
import com.example.data.repository.LogRepositoryImpl
import com.example.data.repository.MacroRepositoryImpl
import com.example.data.repository.VariableRepositoryImpl
import com.example.domain.model.Macro
import com.example.domain.model.Trigger
import com.example.domain.usecase.EvaluerConditionsUseCase
import com.example.domain.usecase.ExecuterMacroUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.math.sqrt

/**
 * Service d'arrière-plan permanent pour l'orchestration des macros Ctrl.
 * Écoute continue des capteurs physiques, récepteurs système dynamiques et bouton d'arrêt d'urgence.
 * Les déclencheurs temporels (HeureFixe / Intervalle) NE sont plus gérés ici : ils sont
 * planifiés de façon fiable par [com.example.data.triggers.scheduler.TriggerScheduler]
 * (AlarmManager / WorkManager), qui survit à Doze et au process kill contrairement à cette
 * boucle. Ce service reste responsable de tout ce qui exige un process vivant en continu :
 * capteurs, callbacks système temps réel, presse-papier.
 * Conforme aux sections 8.1, 10.1, 13 et 14 du cahier des charges Ctrl.
 */
class CtrlForegroundService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var macroRepo: MacroRepositoryImpl
    private lateinit var varRepo: VariableRepositoryImpl
    private lateinit var logRepo: LogRepositoryImpl
    private lateinit var evaluerUseCase: EvaluerConditionsUseCase
    private lateinit var executerUseCase: ExecuterMacroUseCase

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lightSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var lastShakeTime = 0L

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private var isAccelerometerRegistered = false
    private var isLightRegistered = false
    private var isProxRegistered = false
    private var dernierCheckLumiereMs = 0L

    // Cache mémoire ultra-rapide des macros actives pour éviter tout blocage du Main Thread
    @Volatile
    private var macrosActivesCache: List<Macro> = emptyList()

    // États "dernière valeur connue" pour déclenchement sur front (evite le spam de macros
    // sur des événements système qui peuvent se répéter en continu à état inchangé).
    private var lastProximityProche: Boolean? = null
    private var lastOrientationPortrait: Boolean? = null
    private var lastVpnActif: Boolean? = null
    private var lastHotspotActif: Boolean? = null
    private val etatLuminositeDepasse = mutableMapOf<String, Boolean>()
    private val etatTemperatureDepasse = mutableMapOf<String, Boolean>()
    private var dernierSmsSortantId: Long = -1L

    private var cameraManager: CameraManager? = null
    private var torchCallback: CameraManager.TorchCallback? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var clipboardManager: ClipboardManager? = null
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var smsSentObserver: ContentObserver? = null

    private val dynamicSystemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            serviceScope.launch {
                declencherMacrosPourAction(action, intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val dao = CtrlDatabase.getInstance(this).ctrlDao()
        macroRepo = MacroRepositoryImpl(dao)
        varRepo = VariableRepositoryImpl(dao)
        logRepo = LogRepositoryImpl(dao)
        evaluerUseCase = EvaluerConditionsUseCase(this, varRepo)
        executerUseCase = ExecuterMacroUseCase(this, evaluerUseCase, macroRepo, varRepo, logRepo)

        creerCanalNotification()
        val notification = creerNotification()
        startForeground(NOTIFICATION_ID, notification)

        initialiserCapteurs()

        // Abonnement réactif pour synchroniser le cache mémoire et adapter l'écoute des capteurs
        serviceScope.launch {
            try {
                macroRepo.getMacros().collect { liste ->
                    val actives = liste.filter { it.active }
                    macrosActivesCache = actives
                    actualiserEcouteursCapteurs(actives)
                }
            } catch (_: Exception) {}
        }

        enregistrerReceiversSysteme()
        initialiserCallbackTorche()
        initialiserCallbackVpn()
        initialiserEcouteurPressePapier()
        initialiserObservateurSmsEnvoyes()
        lancerBoucleEtatsPeriodiques()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_EMERGENCY_STOP) {
            declencherArretUrgence()
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val portrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
        if (lastOrientationPortrait != portrait) {
            lastOrientationPortrait = portrait
            serviceScope.launch {
                val macros = macrosActivesCache
                macros.filter { it.trigger is Trigger.OrientationEcran && (it.trigger as Trigger.OrientationEcran).portrait == portrait }
                    .forEach { executerUseCase.executer(it) }
            }
        }
    }

    private fun declencherArretUrgence() {
        // Arrêt immédiat de tous les jobs en cours
        serviceScope.coroutineContext.cancelChildren()
        serviceScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, "Arrêt d'urgence Ctrl : Toutes les macros ont été interrompues.", Toast.LENGTH_LONG).show()
        }
    }

    private fun enregistrerReceiversSysteme() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED")
            addAction("android.hardware.usb.action.USB_DEVICE_DETACHED")
        }
        registerReceiver(dynamicSystemReceiver, filter)
    }

    private fun initialiserCapteurs() {
        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            // CRITIQUE : Déporter la réception des capteurs sur un thread d'arrière-plan dédié
            sensorThread = HandlerThread("CtrlSensorThread").apply { start() }
            sensorHandler = Handler(sensorThread!!.looper)
        } catch (_: Exception) {}
    }

    private fun actualiserEcouteursCapteurs(actives: List<Macro>) {
        val sm = sensorManager ?: return
        val handler = sensorHandler ?: return

        val needsShake = actives.any { it.trigger is Trigger.Secousse }
        val needsLight = actives.any { it.trigger is Trigger.CapteurLuminosite }
        val needsProx = actives.any { it.trigger is Trigger.CapteurProximite }

        // Accéléromètre : actif uniquement si une macro 'Secousse' est active
        if (needsShake && !isAccelerometerRegistered) {
            accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, handler)
                isAccelerometerRegistered = true
            }
        } else if (!needsShake && isAccelerometerRegistered) {
            accelerometer?.let { sm.unregisterListener(this, it) }
            isAccelerometerRegistered = false
        }

        // Capteur de luminosité : actif uniquement si nécessaire
        if (needsLight && !isLightRegistered) {
            lightSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
            lightSensor?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, handler)
                isLightRegistered = true
            }
        } else if (!needsLight && isLightRegistered) {
            lightSensor?.let { sm.unregisterListener(this, it) }
            isLightRegistered = false
        }

        // Capteur de proximité : actif uniquement si nécessaire
        if (needsProx && !isProxRegistered) {
            proximitySensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            proximitySensor?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, handler)
                isProxRegistered = true
            }
        } else if (!needsProx && isProxRegistered) {
            proximitySensor?.let { sm.unregisterListener(this, it) }
            isProxRegistered = false
        }
    }

    /** CameraManager.TorchCallback est une API publique (API 23+), aucune permission requise. */
    private fun initialiserCallbackTorche() {
        try {
            cameraManager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val callback = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    serviceScope.launch {
                        val macros = macroRepo.getMacros().first().filter { it.active }
                        macros.filter { it.trigger is Trigger.TorcheState && (it.trigger as Trigger.TorcheState).allumee == enabled }
                            .forEach { executerUseCase.executer(it) }
                    }
                }
            }
            torchCallback = callback
            cameraManager?.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
        } catch (_: Exception) {}
    }

    /** Détecte le VPN via les capacités réseau (API publique), plutôt qu'un broadcast qui n'existe pas. */
    private fun initialiserCallbackVpn() {
        try {
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    val vpnActif = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    if (lastVpnActif != vpnActif) {
                        lastVpnActif = vpnActif
                        serviceScope.launch {
                            val macros = macroRepo.getMacros().first().filter { it.active }
                            macros.filter { it.trigger is Trigger.VpnState && (it.trigger as Trigger.VpnState).actif == vpnActif }
                                .forEach { executerUseCase.executer(it) }
                        }
                    }
                }
            }
            networkCallback = callback
            connectivityManager?.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
        } catch (_: Exception) {}
    }

    /**
     * Écoute les changements du presse-papier. Depuis Android 10, seule l'app au premier plan
     * peut LIRE le contenu ; l'événement de changement reste néanmoins reçu, ce qui suffit
     * au déclencheur "Modification du Presse-Papier" (sans lecture du contenu copié).
     */
    private fun initialiserEcouteurPressePapier() {
        try {
            clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val listener = ClipboardManager.OnPrimaryClipChangedListener {
                serviceScope.launch {
                    val macros = macroRepo.getMacros().first().filter { it.active }
                    macros.filter { it.trigger is Trigger.PressePapierModifie }
                        .forEach { executerUseCase.executer(it) }
                }
            }
            clipboardListener = listener
            clipboardManager?.addPrimaryClipChangedListener(listener)
        } catch (_: Exception) {}
    }

    /** Observe la table des SMS envoyés (nécessite READ_SMS) pour le déclencheur "SMS Envoyé". */
    private fun initialiserObservateurSmsEnvoyes() {
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    serviceScope.launch { verifierDernierSmsEnvoye() }
                }
            }
            smsSentObserver = observer
            contentResolver.registerContentObserver(Telephony.Sms.Sent.CONTENT_URI, true, observer)
        } catch (_: Exception) {}
    }

    private suspend fun verifierDernierSmsEnvoye() {
        try {
            val cursor = contentResolver.query(
                Telephony.Sms.Sent.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS),
                null, null,
                "${Telephony.Sms.DATE} DESC LIMIT 1"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                    val destinataire = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                    if (id != dernierSmsSortantId) {
                        dernierSmsSortantId = id
                        val macros = macroRepo.getMacros().first().filter { m -> m.active }
                        macros.filter { m ->
                            val t = m.trigger
                            t is Trigger.SmsEnvoye && (t.destinataireFiltre.isNullOrBlank() ||
                                    destinataire.contains(t.destinataireFiltre, ignoreCase = true))
                        }.forEach { executerUseCase.executer(it) }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Boucle de secours à basse fréquence pour les états sans API d'écoute fiable
     * (point d'accès Wi-Fi : pas de broadcast public documenté). Les déclencheurs
     * temporels (HeureFixe/Intervalle) NE sont plus vérifiés ici, voir TriggerScheduler.
     */
    private fun lancerBoucleEtatsPeriodiques() {
        serviceScope.launch {
            while (isActive) {
                verifierEtatHotspot()
                delay(20_000L)
            }
        }
    }

    private suspend fun verifierEtatHotspot() {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager ?: return
            val methode = wifiManager.javaClass.getMethod("isWifiApEnabled")
            methode.isAccessible = true
            val actif = methode.invoke(wifiManager) as? Boolean ?: return
            if (lastHotspotActif != actif) {
                lastHotspotActif = actif
                val macros = macrosActivesCache
                macros.filter { it.trigger is Trigger.HotspotState && (it.trigger as Trigger.HotspotState).actif == actif }
                    .forEach { executerUseCase.executer(it) }
            }
        } catch (_: Exception) {
            // Reflection sur API cachée : peut échouer selon l'OEM/version, dégradation silencieuse.
        }
    }

    private suspend fun declencherMacrosPourAction(action: String, intent: Intent) {
        val macros = macrosActivesCache

        when (action) {
            Intent.ACTION_SCREEN_ON -> {
                macros.filter { it.trigger is Trigger.EcranState && (it.trigger as Trigger.EcranState).allume }
                    .forEach { executerUseCase.executer(it) }
            }
            Intent.ACTION_SCREEN_OFF -> {
                macros.filter { it.trigger is Trigger.EcranState && !(it.trigger as Trigger.EcranState).allume }
                    .forEach { executerUseCase.executer(it) }
            }
            Intent.ACTION_USER_PRESENT -> {
                macros.filter { it.trigger is Trigger.EcranDeverrouille }
                    .forEach { executerUseCase.executer(it) }
            }
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                if (state == 1) {
                    macros.filter { it.trigger is Trigger.CasqueAudio && (it.trigger as Trigger.CasqueAudio).branche }
                        .forEach { executerUseCase.executer(it) }
                } else if (state == 0) {
                    macros.filter { it.trigger is Trigger.CasqueAudio && !(it.trigger as Trigger.CasqueAudio).branche }
                        .forEach { executerUseCase.executer(it) }
                }
            }
            Intent.ACTION_POWER_CONNECTED -> {
                macros.filter { it.trigger is Trigger.Alimentation && (it.trigger as Trigger.Alimentation).connectee }
                    .forEach { executerUseCase.executer(it) }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                macros.filter { it.trigger is Trigger.Alimentation && !(it.trigger as Trigger.Alimentation).connectee }
                    .forEach { executerUseCase.executer(it) }
            }
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                val actif = pm?.isPowerSaveMode == true
                macros.filter { it.trigger is Trigger.EconomiseurBatterie && (it.trigger as Trigger.EconomiseurBatterie).actif == actif }
                    .forEach { executerUseCase.executer(it) }
            }
            AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val silencieux = am?.ringerMode == AudioManager.RINGER_MODE_SILENT
                macros.filter { it.trigger is Trigger.ModeSilencieux && (it.trigger as Trigger.ModeSilencieux).actif == silencieux }
                    .forEach { executerUseCase.executer(it) }
            }
            "android.hardware.usb.action.USB_DEVICE_ATTACHED" -> {
                macros.filter { it.trigger is Trigger.UsbConnexion && (it.trigger as Trigger.UsbConnexion).connecte }
                    .forEach { executerUseCase.executer(it) }
            }
            "android.hardware.usb.action.USB_DEVICE_DETACHED" -> {
                macros.filter { it.trigger is Trigger.UsbConnexion && !(it.trigger as Trigger.UsbConnexion).connecte }
                    .forEach { executerUseCase.executer(it) }
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                val tempDixiemes = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                if (tempDixiemes >= 0) {
                    val tempCelsius = tempDixiemes / 10.0
                    for (m in macros) {
                        val t = m.trigger
                        if (t is Trigger.TemperatureBatterie) {
                            val depasse = if (t.superieur) tempCelsius > t.seuilCelsius else tempCelsius < t.seuilCelsius
                            val etaitDepasse = etatTemperatureDepasse[m.id] ?: false
                            if (depasse && !etaitDepasse) {
                                executerUseCase.executer(m)
                            }
                            etatTemperatureDepasse[m.id] = depasse
                        }
                    }
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val evt = event ?: return
        when (evt.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = evt.values[0]
                val y = evt.values[1]
                val z = evt.values[2]
                val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

                if (gForce > 2.2f) {
                    val now = System.currentTimeMillis()
                    if (now - lastShakeTime > 1500L) { // Anti-rebond
                        lastShakeTime = now
                        serviceScope.launch {
                            val macros = macrosActivesCache.filter { it.trigger is Trigger.Secousse }
                            macros.forEach { executerUseCase.executer(it) }
                        }
                    }
                }
            }

            Sensor.TYPE_LIGHT -> {
                val now = System.currentTimeMillis()
                // Throttling à 1s pour le capteur de lumière
                if (now - dernierCheckLumiereMs >= 1000L) {
                    dernierCheckLumiereMs = now
                    val lux = evt.values[0]
                    serviceScope.launch {
                        val macros = macrosActivesCache.filter { it.trigger is Trigger.CapteurLuminosite }
                        for (m in macros) {
                            val t = m.trigger as Trigger.CapteurLuminosite
                            val depasse = if (t.inferieur) lux < t.seuilLux else lux > t.seuilLux
                            val etaitDepasse = etatLuminositeDepasse[m.id] ?: false
                            if (depasse && !etaitDepasse) {
                                executerUseCase.executer(m)
                            }
                            etatLuminositeDepasse[m.id] = depasse
                        }
                    }
                }
            }

            Sensor.TYPE_PROXIMITY -> {
                val proche = evt.values[0] < (evt.sensor?.maximumRange ?: 5f)
                if (lastProximityProche != proche) {
                    lastProximityProche = proche
                    serviceScope.launch {
                        val macros = macrosActivesCache.filter { it.trigger is Trigger.CapteurProximite && (it.trigger as Trigger.CapteurProximite).proche == proche }
                        macros.forEach { executerUseCase.executer(it) }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(dynamicSystemReceiver) } catch (_: Exception) {}
        sensorManager?.unregisterListener(this)
        sensorThread?.quitSafely()
        try { torchCallback?.let { cameraManager?.unregisterTorchCallback(it) } } catch (_: Exception) {}
        try { networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) } } catch (_: Exception) {}
        try { clipboardListener?.let { clipboardManager?.removePrimaryClipChangedListener(it) } } catch (_: Exception) {}
        try { smsSentObserver?.let { contentResolver.unregisterContentObserver(it) } } catch (_: Exception) {}
        serviceScope.cancel()
    }

    private fun creerCanalNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Service d'Automatisation Ctrl",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintient l'écoute active des déclencheurs et l'arrêt d'urgence"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun creerNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Bouton Arrêt d'Urgence dans la notification persistante (Section 13 & 14)
        val stopIntent = Intent(this, CtrlForegroundService::class.java).apply {
            action = ACTION_EMERGENCY_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ctrl - Moteur d'automatisation actif")
            .setContentText("Surveillance en cours des déclencheurs et capteurs")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_delete,
                "Arrêt d'urgence",
                stopPendingIntent
            )
            .build()
    }

    companion object {
        const val ACTION_EMERGENCY_STOP = "com.example.ctrl.ACTION_EMERGENCY_STOP"
        private const val CHANNEL_ID = "ctrl_foreground_channel"
        private const val NOTIFICATION_ID = 1001

        fun demarrer(context: Context) {
            val intent = Intent(context, CtrlForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun arreter(context: Context) {
            val intent = Intent(context, CtrlForegroundService::class.java)
            context.stopService(intent)
        }

        fun arretUrgence(context: Context) {
            val intent = Intent(context, CtrlForegroundService::class.java).apply {
                action = ACTION_EMERGENCY_STOP
            }
            context.startService(intent)
        }
    }
}
