package com.example.data.triggers.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.triggers.receivers.AlarmTriggerReceiver
import com.example.data.triggers.workers.MacroIntervalWorker
import com.example.domain.model.Macro
import com.example.domain.model.Trigger
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Point central de planification des déclencheurs temporels de Ctrl.
 *
 * Remplace l'ancienne boucle de polling toutes les 30s (bug de double-exécution sur
 * "Heure Fixe" et absence totale de prise en charge du trigger "Intervalle").
 *
 * Stratégie (section 4.4 du cahier des charges) :
 * - HeureFixe (heure précise) -> AlarmManager.setExactAndAllowWhileIdle, survit à Doze,
 *   se replanifie lui-même au jour suivant après chaque déclenchement.
 * - Intervalle >= 15 min -> WorkManager PeriodicWorkRequest (limite système = 15 min),
 *   se répète nativement, survit au redémarrage sans code additionnel.
 * - Intervalle < 15 min -> chaîne d'alarmes exactes auto-replanifiées (MacroDroid autorise
 *   des intervalles plus fins que la limite WorkManager).
 */
object TriggerScheduler {

    const val ACTION_ALARM_TRIGGER = "com.example.ctrl.ACTION_ALARM_TRIGGER"
    const val EXTRA_MACRO_ID = "extra_macro_id"
    private const val WORKMANAGER_MIN_MINUTES = 15L

    /** (Re)planifie une macro en fonction de son trigger. Annule d'abord toute planification existante. */
    fun schedule(context: Context, macro: Macro) {
        cancel(context, macro.id)
        if (!macro.active) return

        when (val t = macro.trigger) {
            is Trigger.HeureFixe -> planifierAlarmeExacte(context, macro.id, calculerProchaineHeureFixe(t))
            is Trigger.Intervalle -> {
                if (t.minutes >= WORKMANAGER_MIN_MINUTES) {
                    planifierWorkManagerPeriodique(context, macro.id, t.minutes)
                } else {
                    val prochaine = System.currentTimeMillis() + (t.minutes.coerceAtLeast(1) * 60_000L)
                    planifierAlarmeExacte(context, macro.id, prochaine)
                }
            }
            else -> Unit // Autres triggers gérés par le service d'accessibilité / receivers dynamiques
        }
    }

    /** Annule toute planification (alarme + WorkManager) associée à une macro. */
    fun cancel(context: Context, macroId: String) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(nomTravailWorkManager(macroId))
        } catch (_: Exception) {}

        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.cancel(alarmPendingIntent(context, macroId))
        } catch (_: Exception) {}
    }

    /** À appeler au démarrage de l'app / après redémarrage (BOOT_COMPLETED) pour tout replanifier. */
    fun reschedulerTout(context: Context, macros: List<Macro>) {
        for (macro in macros) {
            if (macro.active && (macro.trigger is Trigger.HeureFixe || macro.trigger is Trigger.Intervalle)) {
                schedule(context, macro)
            }
        }
    }

    private fun planifierAlarmeExacte(context: Context, macroId: String, declenchementMs: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = alarmPendingIntent(context, macroId)
        try {
            val peutExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (peutExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, declenchementMs, pi)
            } else {
                // Permission "Alarmes et rappels" non accordée par l'utilisateur (Android 12+) :
                // dégradation gracieuse vers une alarme non-exacte plutôt que planter.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, declenchementMs, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, declenchementMs, pi)
        }
    }

    private fun planifierWorkManagerPeriodique(context: Context, macroId: String, minutes: Int) {
        val request = PeriodicWorkRequestBuilder<MacroIntervalWorker>(minutes.toLong(), TimeUnit.MINUTES)
            .setInputData(workDataOf(MacroIntervalWorker.CLE_MACRO_ID to macroId))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            nomTravailWorkManager(macroId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun alarmPendingIntent(context: Context, macroId: String): PendingIntent {
        val intent = Intent(context, AlarmTriggerReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_MACRO_ID, macroId)
        }
        return PendingIntent.getBroadcast(
            context,
            macroId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nomTravailWorkManager(macroId: String) = "ctrl_intervalle_$macroId"

    /**
     * Calcule le prochain instant (epoch ms) correspondant à l'heure/minute configurées,
     * en respectant la liste des jours actifs (format Ctrl : Lundi=1 .. Dimanche=7).
     */
    private fun calculerProchaineHeureFixe(t: Trigger.HeureFixe): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, t.heure)
        cal.set(Calendar.MINUTE, t.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (t.jours.isNotEmpty()) {
            var garde = 0
            while (garde < 8) {
                val calDay = cal.get(Calendar.DAY_OF_WEEK)
                val ctrlDay = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
                if (t.jours.contains(ctrlDay)) break
                cal.add(Calendar.DAY_OF_YEAR, 1)
                garde++
            }
        }
        return cal.timeInMillis
    }
}
