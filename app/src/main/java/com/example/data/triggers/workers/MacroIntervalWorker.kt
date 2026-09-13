package com.example.data.triggers.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.room.CtrlDatabase
import com.example.data.repository.LogRepositoryImpl
import com.example.data.repository.MacroRepositoryImpl
import com.example.data.repository.VariableRepositoryImpl
import com.example.domain.model.Trigger
import com.example.domain.usecase.EvaluerConditionsUseCase
import com.example.domain.usecase.ExecuterMacroUseCase

/**
 * Worker WorkManager exécutant une macro dont le déclencheur est "À intervalles réguliers"
 * (>= 15 minutes, limite système de PeriodicWorkRequest). Survit à Doze et au redémarrage
 * sans code de replanification manuel : WorkManager se répète nativement.
 */
class MacroIntervalWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val macroId = inputData.getString(CLE_MACRO_ID) ?: return Result.failure()

        return try {
            val dao = CtrlDatabase.getInstance(applicationContext).ctrlDao()
            val macroRepo = MacroRepositoryImpl(dao)
            val varRepo = VariableRepositoryImpl(dao)
            val logRepo = LogRepositoryImpl(dao)
            val evaluerUseCase = EvaluerConditionsUseCase(applicationContext, varRepo)
            val executerUseCase = ExecuterMacroUseCase(applicationContext, evaluerUseCase, macroRepo, varRepo, logRepo)

            val macro = macroRepo.getMacroById(macroId)
            if (macro != null && macro.active && macro.trigger is Trigger.Intervalle) {
                executerUseCase.executer(macro)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val CLE_MACRO_ID = "macro_id"
    }
}
