package com.example.data.triggers.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.room.CtrlDatabase
import com.example.data.repository.LogRepositoryImpl
import com.example.data.repository.MacroRepositoryImpl
import com.example.data.repository.VariableRepositoryImpl
import com.example.data.triggers.scheduler.TriggerScheduler
import com.example.domain.usecase.EvaluerConditionsUseCase
import com.example.domain.usecase.ExecuterMacroUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reçoit les alarmes exactes planifiées par [TriggerScheduler] pour les déclencheurs
 * "Heure Fixe" et "Intervalle" (< 15 minutes). Exécute la macro puis se replanifie
 * elle-même pour l'occurrence suivante (le calcul de la prochaine occurrence est
 * entièrement délégué à TriggerScheduler.schedule, qui relit le trigger à jour).
 */
class AlarmTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val macroId = intent.getStringExtra(TriggerScheduler.EXTRA_MACRO_ID) ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val dao = CtrlDatabase.getInstance(context).ctrlDao()
                val macroRepo = MacroRepositoryImpl(dao)
                val varRepo = VariableRepositoryImpl(dao)
                val logRepo = LogRepositoryImpl(dao)
                val evaluerUseCase = EvaluerConditionsUseCase(context, varRepo)
                val executerUseCase = ExecuterMacroUseCase(context, evaluerUseCase, macroRepo, varRepo, logRepo)

                val macro = macroRepo.getMacroById(macroId)
                if (macro != null && macro.active) {
                    executerUseCase.executer(macro)
                    // Replanifie la prochaine occurrence (jour suivant pour HeureFixe,
                    // prochain tick pour un Intervalle < 15 min).
                    TriggerScheduler.schedule(context, macro)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
