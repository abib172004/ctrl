package com.example.domain.usecase

import android.app.KeyguardManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.example.data.accessibility.CtrlAccessibilityService
import com.example.domain.model.Condition
import com.example.domain.model.OperateurComparaison
import com.example.domain.model.OperateurLogique
import com.example.domain.repository.VariableRepository
import java.util.Calendar

/**
 * Évalue l'ensemble des conditions d'une macro avec de vraies requêtes matérielles et système
 * Conforme aux spécifications réelles de la section 8.2 du cahier des charges Ctrl.
 */
class EvaluerConditionsUseCase(
    private val context: Context,
    private val variableRepository: VariableRepository
) {

    suspend fun evaluer(conditions: List<Condition>): Boolean {
        if (conditions.isEmpty()) return true

        for (condition in conditions) {
            val satisfait = evaluerUneCondition(condition)
            if (!satisfait) return false
        }
        return true
    }

    private suspend fun evaluerUneCondition(condition: Condition): Boolean {
        val cal = Calendar.getInstance()
        return when (condition) {
            is Condition.PlageHoraire -> {
                val currentHour = cal.get(Calendar.HOUR_OF_DAY)
                val currentMin = cal.get(Calendar.MINUTE)
                val currentMinutes = currentHour * 60 + currentMin

                val startMinutes = condition.heureDebut * 60 + condition.minuteDebut
                val endMinutes = condition.heureFin * 60 + condition.minuteFin

                if (startMinutes <= endMinutes) {
                    currentMinutes in startMinutes..endMinutes
                } else {
                    // Plage traversant minuit (ex: 22h00 -> 07h00)
                    currentMinutes >= startMinutes || currentMinutes <= endMinutes
                }
            }

            is Condition.JoursSemaine -> {
                // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2..Saturday=7
                // Format Ctrl: Lundi=1 .. Dimanche=7
                val calDay = cal.get(Calendar.DAY_OF_WEEK)
                val ctrlDay = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
                condition.jours.contains(ctrlDay)
            }

            is Condition.NiveauBatterie -> {
                val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val pct = if (scale > 0) (level * 100) / scale else level
                if (condition.estSuperieur) pct >= condition.seuil else pct <= condition.seuil
            }

            is Condition.EnCharge -> {
                val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                if (condition.doitEtreEnCharge) isCharging else !isCharging
            }

            is Condition.WifiConnecte -> {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val net = cm?.activeNetwork
                val caps = cm?.getNetworkCapabilities(net)
                val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                if (condition.doitEtreConnecte) hasWifi else !hasWifi
            }

            is Condition.BluetoothActif -> {
                @Suppress("DEPRECATION")
                val btAdapter = BluetoothAdapter.getDefaultAdapter()
                val isBtOn = btAdapter?.isEnabled == true
                if (condition.doitEtreActif) isBtOn else !isBtOn
            }

            is Condition.EcranAllume -> {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val isInteractive = pm?.isInteractive == true
                if (condition.doitEtreAllume) isInteractive else !isInteractive
            }

            is Condition.CasqueBranche -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val isPlugged = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
                    devices.any { dev ->
                        dev.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        dev.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        dev.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        dev.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        dev.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
                    }
                } else {
                    @Suppress("DEPRECATION")
                    audioManager?.isWiredHeadsetOn == true
                }
                if (condition.doitEtreBranche) isPlugged else !isPlugged
            }

            is Condition.AppAuPremierPlan -> {
                // Vérifié via accessibilité si actif
                val service = CtrlAccessibilityService.getService()
                val currentPkg = service?.rootInActiveWindow?.packageName?.toString() ?: ""
                if (condition.packageName.isNotBlank()) {
                    currentPkg.equals(condition.packageName, ignoreCase = true)
                } else {
                    true
                }
            }

            is Condition.ServiceAccessibiliteActif -> {
                val actif = CtrlAccessibilityService.isEnabled(context)
                if (condition.requis) actif else !actif
            }

            is Condition.VariableValeur -> {
                val valeurReelle = variableRepository.getValue(condition.nomVariable)
                valeurReelle.equals(condition.valeurAttendue, ignoreCase = true)
            }

            is Condition.VariableComparaison -> {
                val brute = variableRepository.getValue(condition.nomVariable) ?: ""
                val brutNum = brute.toDoubleOrNull()
                val attenduNum = condition.valeur.toDoubleOrNull()
                when (condition.operateur) {
                    OperateurComparaison.EGAL -> brute.equals(condition.valeur, ignoreCase = true)
                    OperateurComparaison.DIFFERENT -> !brute.equals(condition.valeur, ignoreCase = true)
                    OperateurComparaison.CONTIENT -> brute.contains(condition.valeur, ignoreCase = true)
                    OperateurComparaison.SUPERIEUR -> {
                        if (brutNum != null && attenduNum != null) brutNum > attenduNum
                        else brute > condition.valeur
                    }
                    OperateurComparaison.INFERIEUR -> {
                        if (brutNum != null && attenduNum != null) brutNum < attenduNum
                        else brute < condition.valeur
                    }
                }
            }

            is Condition.VpnActif -> {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
                val vpnActif = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                if (condition.doitEtreActif) vpnActif else !vpnActif
            }

            is Condition.EconomiseurBatterieActif -> {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val actif = pm?.isPowerSaveMode == true
                if (condition.doitEtreActif) actif else !actif
            }

            is Condition.AppareilVerrouille -> {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val verrouille = km?.isKeyguardLocked == true
                if (condition.doitEtreVerrouille) verrouille else !verrouille
            }

            is Condition.JourDuMois -> {
                val jourActuel = cal.get(Calendar.DAY_OF_MONTH)
                condition.jours.contains(jourActuel)
            }

            is Condition.MacroEnCoursExecution -> {
                val enCours = ExecuterMacroUseCase.estEnCoursDExecution(condition.macroId)
                if (condition.doitEtreEnCours) enCours else !enCours
            }

            is Condition.Compose -> {
                if (condition.sousConditions.isEmpty()) return true
                when (condition.operateur) {
                    OperateurLogique.ET -> condition.sousConditions.all { evaluerUneCondition(it) }
                    OperateurLogique.OU -> condition.sousConditions.any { evaluerUneCondition(it) }
                    OperateurLogique.NON -> !condition.sousConditions.all { evaluerUneCondition(it) }
                    OperateurLogique.OU_EXCLUSIF -> condition.sousConditions.count { evaluerUneCondition(it) } == 1
                }
            }
        }
    }
}
