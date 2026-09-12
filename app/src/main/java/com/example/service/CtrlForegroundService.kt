package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.room.CtrlDatabase
import com.example.data.repository.LogRepositoryImpl
import com.example.data.repository.MacroRepositoryImpl
import com.example.data.repository.VariableRepositoryImpl
import com.example.domain.model.Trigger
import com.example.domain.usecase.EvaluerConditionsUseCase
import com.example.domain.usecase.ExecuterMacroUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar
import kotlin.math.sqrt

/**
 * Service d'arrière-plan permanent pour l'orchestration des macros Ctrl
 * Écoute continue des capteurs physiques, récepteurs système et bouton d'arrêt d'urgence.
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
    private var lastShakeTime = 0L

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

        enregistrerReceiversSysteme()
        initialiserCapteurSecousse()
        lancerBoucleTemporelle()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_EMERGENCY_STOP) {
            declencherArretUrgence()
        }
        return START_STICKY
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
        }
        registerReceiver(dynamicSystemReceiver, filter)
    }

    private fun initialiserCapteurSecousse() {
        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (_: Exception) {}
    }

    private fun lancerBoucleTemporelle() {
        serviceScope.launch {
            while (isActive) {
                verifierMacrosTemporelles()
                delay(30_000L) // Vérification toutes les 30 secondes
            }
        }
    }

    private suspend fun verifierMacrosTemporelles() {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val calDay = cal.get(Calendar.DAY_OF_WEEK)
        val ctrlDay = if (calDay == Calendar.SUNDAY) 7 else calDay - 1

        val macros = macroRepo.getMacros().first().filter { it.active }
        for (m in macros) {
            when (val t = m.trigger) {
                is Trigger.HeureFixe -> {
                    if (t.heure == currentHour && t.minute == currentMinute && t.jours.contains(ctrlDay)) {
                        executerUseCase.executer(m)
                    }
                }
                else -> Unit
            }
        }
    }

    private suspend fun declencherMacrosPourAction(action: String, intent: Intent) {
        val macros = macroRepo.getMacros().first().filter { it.active }

        when (action) {
            Intent.ACTION_SCREEN_ON -> {
                macros.filter { it.trigger is Trigger.EcranState && (it.trigger as Trigger.EcranState).allume }
                    .forEach { executerUseCase.executer(it) }
            }
            Intent.ACTION_SCREEN_OFF -> {
                macros.filter { it.trigger is Trigger.EcranState && !(it.trigger as Trigger.EcranState).allume }
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
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

            if (gForce > 2.2f) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > 1500L) { // Anti-rebond
                    lastShakeTime = now
                    serviceScope.launch {
                        val macros = macroRepo.getMacros().first().filter { it.active }
                        macros.filterIsInstance<Trigger.Secousse>().forEach { _ -> }
                        macros.filter { it.trigger is Trigger.Secousse }.forEach {
                            executerUseCase.executer(it)
                        }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dynamicSystemReceiver)
        sensorManager?.unregisterListener(this)
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
