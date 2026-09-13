package com.example.data.triggers.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.TelephonyManager
import com.example.data.local.room.CtrlDatabase
import com.example.data.repository.LogRepositoryImpl
import com.example.data.repository.MacroRepositoryImpl
import com.example.data.repository.VariableRepositoryImpl
import com.example.data.triggers.scheduler.TriggerScheduler
import com.example.domain.model.Trigger
import com.example.domain.usecase.EvaluerConditionsUseCase
import com.example.domain.usecase.ExecuterMacroUseCase
import com.example.service.CtrlForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Récepteur des événements système Android réels (SMS, Appels, Batterie, Alimentation, Démarrage)
 * et déclenchement automatique des macros associées.
 * Conforme aux sections 8.1 et 10.1 du cahier des charges Ctrl.
 */
class SystemEventReceiver : BroadcastReceiver() {

    companion object {
        // État du dernier appel connu, conservé au niveau process (les instances de
        // BroadcastReceiver sont recréées à chaque réception) pour distinguer
        // appel sortant / manqué / terminé à partir des transitions IDLE/RINGING/OFFHOOK.
        @Volatile
        private var dernierEtatAppel: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val database = CtrlDatabase.getInstance(context)
        val dao = database.ctrlDao()
        val macroRepo = MacroRepositoryImpl(dao)
        val varRepo = VariableRepositoryImpl(dao)
        val logRepo = LogRepositoryImpl(dao)
        val evaluerUseCase = EvaluerConditionsUseCase(context, varRepo)
        val executerUseCase = ExecuterMacroUseCase(context, evaluerUseCase, macroRepo, varRepo, logRepo)

        CoroutineScope(Dispatchers.Default).launch {
            val macros = macroRepo.getMacros().first().filter { it.active }

            when (action) {
                Intent.ACTION_BOOT_COMPLETED -> {
                    CtrlForegroundService.demarrer(context)
                    // Replanifie tous les triggers temporels (HeureFixe / Intervalle) via
                    // AlarmManager/WorkManager, qui ne survivent pas nativement au redémarrage.
                    TriggerScheduler.reschedulerTout(context, macros)
                    macros.filter { it.trigger is Trigger.DemarrageAppareil }
                        .forEach { executerUseCase.executer(it) }
                }

                Intent.ACTION_POWER_CONNECTED -> {
                    macros.filter { it.trigger is Trigger.Alimentation && (it.trigger as Trigger.Alimentation).connectee }
                        .forEach { executerUseCase.executer(it) }
                }

                Intent.ACTION_POWER_DISCONNECTED -> {
                    macros.filter { it.trigger is Trigger.Alimentation && !(it.trigger as Trigger.Alimentation).connectee }
                        .forEach { executerUseCase.executer(it) }
                }

                Intent.ACTION_BATTERY_LOW -> {
                    macros.filter { it.trigger is Trigger.NiveauBatterie && (it.trigger as Trigger.NiveauBatterie).inferieur }
                        .forEach { executerUseCase.executer(it) }
                }

                Intent.ACTION_BATTERY_OKAY -> {
                    macros.filter { it.trigger is Trigger.NiveauBatterie && !(it.trigger as Trigger.NiveauBatterie).inferieur }
                        .forEach { executerUseCase.executer(it) }
                }

                Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                    try {
                        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                        for (sms in messages) {
                            val expediteur = sms.displayOriginatingAddress ?: ""
                            val corps = sms.displayMessageBody ?: ""

                            // Sauvegarder dans variables système pour interpolation
                            varRepo.setValue("dernier_sms_expediteur", expediteur)
                            varRepo.setValue("dernier_sms_texte", corps)

                            for (m in macros) {
                                val t = m.trigger
                                if (t is Trigger.SmsRecu) {
                                    val matchExpediteur = t.expediteurFiltre.isNullOrBlank() ||
                                            expediteur.contains(t.expediteurFiltre, ignoreCase = true)
                                    val matchMotCle = t.motCleFiltre.isNullOrBlank() ||
                                            corps.contains(t.motCleFiltre, ignoreCase = true)

                                    if (matchExpediteur && matchMotCle) {
                                        executerUseCase.executer(m)
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    val etatPrecedent = dernierEtatAppel
                    dernierEtatAppel = state

                    if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                        @Suppress("DEPRECATION")
                        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
                        if (incomingNumber.isNotBlank()) {
                            varRepo.setValue("dernier_appel_numero", incomingNumber)
                        }
                        for (m in macros) {
                            val t = m.trigger
                            if (t is Trigger.AppelEntrant) {
                                val matchNumero = t.numeroFiltre.isNullOrBlank() ||
                                        incomingNumber.contains(t.numeroFiltre, ignoreCase = true)
                                if (matchNumero) {
                                    executerUseCase.executer(m)
                                }
                            }
                        }
                    } else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                        if (etatPrecedent == null || etatPrecedent == TelephonyManager.EXTRA_STATE_IDLE) {
                            // IDLE -> OFFHOOK direct (sans sonnerie préalable) = appel sortant composé par l'utilisateur
                            macros.filter { it.trigger is Trigger.AppelSortant }
                                .forEach { executerUseCase.executer(it) }
                        }
                        // RINGING -> OFFHOOK = appel entrant décroché, déjà couvert par le trigger AppelEntrant ci-dessus
                    } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                        if (etatPrecedent == TelephonyManager.EXTRA_STATE_RINGING) {
                            // RINGING -> IDLE sans passer par OFFHOOK = appel manqué
                            macros.filter { it.trigger is Trigger.AppelManque }
                                .forEach { executerUseCase.executer(it) }
                        } else if (etatPrecedent == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                            // OFFHOOK -> IDLE = fin d'appel (entrant ou sortant)
                            macros.filter { it.trigger is Trigger.AppelTermine }
                                .forEach { executerUseCase.executer(it) }
                        }
                    }
                }
            }
        }
    }
}
