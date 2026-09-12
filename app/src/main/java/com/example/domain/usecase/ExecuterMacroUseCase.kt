package com.example.domain.usecase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.data.accessibility.CtrlAccessibilityService
import com.example.data.accessibility.GestureExecutor
import com.example.data.remote.api.GeminiApiClient
import com.example.domain.model.*
import com.example.domain.repository.LogRepository
import com.example.domain.repository.MacroRepository
import com.example.domain.repository.VariableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Exécuteur principal du moteur de macro Ctrl
 * Valide les conditions, orchestre les actions 100% réelles et enregistre le journal d'exécution.
 * Conforme aux spécifications réelles des sections 6, 8, 9 et 13 du cahier des charges Ctrl.
 */
class ExecuterMacroUseCase(
    private val context: Context,
    private val evaluerConditionsUseCase: EvaluerConditionsUseCase,
    private val macroRepository: MacroRepository,
    private val variableRepository: VariableRepository,
    private val logRepository: LogRepository,
    private val gestureExecutor: GestureExecutor = GestureExecutor(),
    private val geminiApiClient: GeminiApiClient = GeminiApiClient(context)
) {

    private var ttsInstance: TextToSpeech? = null
    private var isTtsInitialized = AtomicBoolean(false)

    init {
        try {
            ttsInstance = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsInstance?.language = Locale.FRENCH
                    isTtsInitialized.set(true)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun executer(
        macro: Macro,
        forceManuelle: Boolean = false,
        profondeur: Int = 0
    ): ExecutionResult = withContext(Dispatchers.Default) {
        val debutMs = System.currentTimeMillis()

        // Garde-fou récursion (Section 9.3 & 13 du cahier des charges)
        if (profondeur > 5) {
            val result = ExecutionResult(
                succes = false,
                statut = StatutExecution.ECHEC,
                dureeMs = 0,
                message = "Arrêt préventif : profondeur de chaînage maximale atteinte (limite = 5)",
                erreur = "Boucle récursive de macros détectée"
            )
            enregistrerLog(macro, result)
            return@withContext result
        }

        if (!macro.active && !forceManuelle) {
            val duree = System.currentTimeMillis() - debutMs
            return@withContext ExecutionResult(
                succes = false,
                statut = StatutExecution.CONDITIONS_NON_REMPLIES,
                dureeMs = duree,
                message = "Macro désactivée."
            )
        }

        // 1. Évaluation des conditions
        val conditionsRemplies = evaluerConditionsUseCase.evaluer(macro.conditions)
        if (!conditionsRemplies && !forceManuelle) {
            val duree = System.currentTimeMillis() - debutMs
            val result = ExecutionResult(
                succes = false,
                statut = StatutExecution.CONDITIONS_NON_REMPLIES,
                dureeMs = duree,
                message = "Conditions non satisfaites au moment du déclenchement."
            )
            enregistrerLog(macro, result)
            return@withContext result
        }

        // 2. Exécution séquentielle des actions
        var nbActionsReussies = 0
        var derniereErreur: String? = null

        try {
            for (action in macro.actions) {
                executerUneAction(action, profondeur)
                nbActionsReussies++
            }
        } catch (stop: KillSwitchException) {
            derniereErreur = "Arrêt d'urgence déclenché : ${stop.message}"
        } catch (e: Exception) {
            derniereErreur = e.message ?: "Erreur d'exécution"
        }

        val dureeTotale = System.currentTimeMillis() - debutMs
        val succes = (derniereErreur == null)
        val statut = if (succes) StatutExecution.SUCCES else StatutExecution.ECHEC
        val message = if (succes) {
            "Exécution réussie : $nbActionsReussies action(s) effectuée(s)"
        } else {
            "Interruption : $derniereErreur"
        }

        val result = ExecutionResult(
            succes = succes,
            statut = statut,
            dureeMs = dureeTotale,
            message = message,
            erreur = derniereErreur
        )

        // Persistance de l'historique et des métriques
        macroRepository.recordExecution(macro.id, succes)
        enregistrerLog(macro, result)

        result
    }

    private suspend fun executerUneAction(action: ActionMacro, profondeur: Int) {
        when (action) {
            is ActionMacro.EnvoyerNotification -> {
                val titreInterpole = interpolerVariables(action.titre)
                val msgInterpole = interpolerVariables(action.message)
                afficherNotificationSysteme(titreInterpole, msgInterpole)
            }

            is ActionMacro.AfficherToast -> {
                val msgInterpole = interpolerVariables(action.message)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, msgInterpole, Toast.LENGTH_SHORT).show()
                }
            }

            is ActionMacro.Vibrer -> {
                vibrerAppareil(action.dureeMs)
            }

            is ActionMacro.ChangerVolume -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audioManager != null) {
                    val streamType = when (action.canal.lowercase()) {
                        "sonnerie", "ring" -> AudioManager.STREAM_RING
                        "notification", "notif" -> AudioManager.STREAM_NOTIFICATION
                        "alarme", "alarm" -> AudioManager.STREAM_ALARM
                        else -> AudioManager.STREAM_MUSIC
                    }
                    val maxVolume = audioManager.getStreamMaxVolume(streamType)
                    val targetVolume = (maxVolume * (action.niveauPourcentage.coerceIn(0, 100) / 100.0)).toInt()
                    audioManager.setStreamVolume(streamType, targetVolume, 0)
                }
            }

            is ActionMacro.JouerSon -> {
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                } catch (_: Exception) {}
            }

            is ActionMacro.LampeTorche -> {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                if (cameraManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val cameraId = cameraManager.cameraIdList.firstOrNull()
                        if (cameraId != null) {
                            cameraManager.setTorchMode(cameraId, action.activer)
                        }
                    } catch (_: Exception) {}
                }
            }

            is ActionMacro.TexteParSyntheseVocale -> {
                val texteInterpole = interpolerVariables(action.texte)
                ttsInstance?.speak(texteInterpole, TextToSpeech.QUEUE_FLUSH, null, "ctrl_tts_${System.currentTimeMillis()}")
            }

            is ActionMacro.EnvoyerSms -> {
                val destInterpole = interpolerVariables(action.destinataire).trim()
                val msgInterpole = interpolerVariables(action.message)
                if (destInterpole.isNotBlank()) {
                    try {
                        @Suppress("DEPRECATION")
                        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.getSystemService(SmsManager::class.java)
                        } else {
                            SmsManager.getDefault()
                        }
                        smsManager?.sendTextMessage(destInterpole, null, msgInterpole, null, null)
                    } catch (e: Exception) {
                        // Si permission non accordée, notification informative
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Erreur envoi SMS: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            is ActionMacro.OuvrirUrl -> {
                val urlInterpole = interpolerVariables(action.url).trim()
                if (urlInterpole.isNotBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlInterpole)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            }

            is ActionMacro.OuvrirApp -> {
                if (action.packageName.isNotBlank()) {
                    val intent = context.packageManager.getLaunchIntentForPackage(action.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            }

            is ActionMacro.ActionUI -> {
                val service = CtrlAccessibilityService.getService()
                if (service != null) {
                    val cibleInterpole = interpolerVariables(action.valeurCible)
                    val texteInterpole = interpolerVariables(action.texteSaisie)
                    gestureExecutor.executerGeste(
                        service = service,
                        geste = action.typeGeste,
                        selecteurType = action.selecteurType,
                        cible = cibleInterpole,
                        texteSaisie = texteInterpole
                    )
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Service d'accessibilité Ctrl non actif", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            is ActionMacro.Attendre -> {
                delay(action.secondes * 1000L)
            }

            is ActionMacro.DefinirVariable -> {
                val valeurInterpolee = interpolerVariables(action.valeur)
                variableRepository.setValue(action.nomVariable, valeurInterpolee)
            }

            is ActionMacro.ManipulerTexte -> {
                val source = interpolerVariables(action.texteSource)
                val resultat = when (action.operation) {
                    OperationTexte.MAJUSCULE -> source.uppercase()
                    OperationTexte.MINUSCULE -> source.lowercase()
                    OperationTexte.TRIM -> source.trim()
                    OperationTexte.REMPLACER -> {
                        val parts = action.argument.split("->", limit = 2)
                        if (parts.size == 2) source.replace(parts[0], parts[1]) else source
                    }
                    OperationTexte.REGEX_EXTRACTION -> {
                        try {
                            val regex = Regex(action.argument)
                            regex.find(source)?.value ?: ""
                        } catch (_: Exception) {
                            source
                        }
                    }
                    OperationTexte.CONCATENER -> source + interpolerVariables(action.argument)
                }
                variableRepository.setValue(action.variableSortie, resultat)
            }

            is ActionMacro.AppelIA -> {
                val promptInterpole = interpolerVariables(action.promptUtilisateur)
                val result = geminiApiClient.genererContenu(
                    prompt = promptInterpole,
                    systemInstruction = "Tu es le module d'intelligence artificielle de l'application d'automatisation Ctrl. Réponds de façon concise, directe et factuelle."
                )
                val texteReponse = result.getOrElse { err ->
                    "Synthèse locale : Échec requête IA (${err.message})"
                }
                variableRepository.setValue(action.variableSortie, texteReponse)
            }

            is ActionMacro.ControleFlux -> {
                when (val flux = action.typeControle) {
                    is TypeControleFlux.ClauseSi -> {
                        val conditionVraie = evaluerConditionsUseCase.evaluer(listOf(flux.condition))
                        val actionsAExecuter = if (conditionVraie) flux.actionsSiVrai else flux.actionsSinon
                        for (sousAction in actionsAExecuter) {
                            executerUneAction(sousAction, profondeur + 1)
                        }
                    }
                    is TypeControleFlux.Repeter -> {
                        for (i in 0 until flux.nombreFois) {
                            for (sousAction in flux.actions) {
                                executerUneAction(sousAction, profondeur + 1)
                            }
                        }
                    }
                    is TypeControleFlux.RepeterTantQue -> {
                        var iterations = 0
                        while (evaluerConditionsUseCase.evaluer(listOf(flux.condition)) && iterations < 50) {
                            for (sousAction in flux.actions) {
                                executerUneAction(sousAction, profondeur + 1)
                            }
                            iterations++
                        }
                    }
                    is TypeControleFlux.SortirDeLaBoucle -> {
                        throw LoopBreakException()
                    }
                }
            }

            is ActionMacro.DeclencherMacro -> {
                val sousMacro = macroRepository.getMacroById(action.cibleMacroId)
                if (sousMacro != null) {
                    executer(sousMacro, forceManuelle = true, profondeur = profondeur + 1)
                }
            }

            is ActionMacro.ArretUrgence -> {
                throw KillSwitchException(action.raison)
            }

            is ActionMacro.BasculerWifi -> {
                try {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        @Suppress("DEPRECATION")
                        wifiManager?.isWifiEnabled = action.activer
                    } else {
                        val panelIntent = Intent(android.provider.Settings.Panel.ACTION_WIFI).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(panelIntent)
                    }
                } catch (e: Exception) {
                    try {
                        val fallback = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallback)
                    } catch (_: Exception) {}
                }
            }

            is ActionMacro.BasculerBluetooth -> {
                try {
                    @Suppress("DEPRECATION")
                    val btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                    if (btAdapter != null) {
                        if (action.activer) {
                            @Suppress("DEPRECATION")
                            btAdapter.enable()
                        } else {
                            @Suppress("DEPRECATION")
                            btAdapter.disable()
                        }
                    }
                } catch (e: Exception) {
                    try {
                        val btIntent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(btIntent)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private suspend fun interpolerVariables(texte: String): String {
        var resultat = texte
        val dateActuelle = Date()
        val formatHeure = SimpleDateFormat("HH:mm", Locale.getDefault()).format(dateActuelle)
        val formatDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateActuelle)

        // Données dynamiques matérielles
        val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pctBatterie = if (scale > 0) (level * 100) / scale else level

        resultat = resultat.replace("{heure}", formatHeure)
        resultat = resultat.replace("{date}", formatDate)
        resultat = resultat.replace("{batterie}", "$pctBatterie%")

        // Variables utilisateurs stockées
        val variables = variableRepository.getVariables().first()
        for (v in variables) {
            resultat = resultat.replace("{${v.nom}}", v.valeur)
        }
        return resultat
    }

    private fun afficherNotificationSysteme(titre: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "ctrl_automation_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifications Ctrl",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes issues des macros Ctrl"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titre)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    private fun vibrerAppareil(dureeMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(dureeMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(dureeMs)
        }
    }

    private suspend fun enregistrerLog(macro: Macro, result: ExecutionResult) {
        logRepository.insertLog(
            LogEntry(
                macroId = macro.id,
                macroNom = macro.nom,
                timestamp = System.currentTimeMillis(),
                statut = result.statut,
                dureeMs = result.dureeMs,
                message = result.message,
                details = result.erreur
            )
        )
    }
}

class LoopBreakException : RuntimeException("Sortie de boucle")
class KillSwitchException(message: String) : RuntimeException(message)
