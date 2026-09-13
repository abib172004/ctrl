package com.example.domain.model

/**
 * Catalogue complet des déclencheurs Ctrl (inspiré de MacroDroid)
 * Sealed class pure Kotlin garantissant l'exhaustivité de traitement.
 */
sealed class Trigger {
    abstract val id: String
    abstract val label: String
    abstract val categorie: CategorieTrigger

    // 1. Appels & SMS
    data class SmsRecu(
        override val id: String = "trigger_sms",
        val expediteurFiltre: String? = null,
        val motCleFiltre: String? = null
    ) : Trigger() {
        override val label = if (expediteurFiltre.isNullOrBlank()) "SMS Reçu" else "SMS de $expediteurFiltre"
        override val categorie = CategorieTrigger.APPEL_SMS
    }

    data class AppelEntrant(
        override val id: String = "trigger_call_in",
        val numeroFiltre: String? = null
    ) : Trigger() {
        override val label = if (numeroFiltre.isNullOrBlank()) "Appel Entrant" else "Appel de $numeroFiltre"
        override val categorie = CategorieTrigger.APPEL_SMS
    }

    data class AppelSortant(
        override val id: String = "trigger_call_out",
        val numeroFiltre: String? = null
    ) : Trigger() {
        override val label = if (numeroFiltre.isNullOrBlank()) "Appel Sortant" else "Appel sortant vers $numeroFiltre"
        override val categorie = CategorieTrigger.APPEL_SMS
    }

    data class AppelManque(
        override val id: String = "trigger_call_missed",
        val numeroFiltre: String? = null
    ) : Trigger() {
        override val label = if (numeroFiltre.isNullOrBlank()) "Appel Manqué" else "Appel manqué de $numeroFiltre"
        override val categorie = CategorieTrigger.APPEL_SMS
    }

    data class AppelTermine(
        override val id: String = "trigger_call_ended"
    ) : Trigger() {
        override val label = "Appel Terminé"
        override val categorie = CategorieTrigger.APPEL_SMS
    }

    data class SmsEnvoye(
        override val id: String = "trigger_sms_sent",
        val destinataireFiltre: String? = null
    ) : Trigger() {
        override val label = if (destinataireFiltre.isNullOrBlank()) "SMS Envoyé" else "SMS envoyé à $destinataireFiltre"
        override val categorie = CategorieTrigger.APPEL_SMS
    }

    // 2. Batterie & Alimentation
    data class NiveauBatterie(
        override val id: String = "trigger_battery_lvl",
        val seuil: Int = 20,
        val inferieur: Boolean = true
    ) : Trigger() {
        override val label = "Batterie ${if (inferieur) "<" else ">"} $seuil%"
        override val categorie = CategorieTrigger.BATTERIE
    }

    data class Alimentation(
        override val id: String = "trigger_power",
        val connectee: Boolean = true
    ) : Trigger() {
        override val label = if (connectee) "Chargeur Connecté" else "Chargeur Débranché"
        override val categorie = CategorieTrigger.BATTERIE
    }

    data class TemperatureBatterie(
        override val id: String = "trigger_battery_temp",
        val seuilCelsius: Int = 40,
        val superieur: Boolean = true
    ) : Trigger() {
        override val label = "Température batterie ${if (superieur) ">" else "<"} $seuilCelsius°C"
        override val categorie = CategorieTrigger.BATTERIE
    }

    data class EconomiseurBatterie(
        override val id: String = "trigger_battery_saver",
        val actif: Boolean = true
    ) : Trigger() {
        override val label = if (actif) "Économiseur de batterie activé" else "Économiseur de batterie désactivé"
        override val categorie = CategorieTrigger.BATTERIE
    }

    // 3. Connectivité
    data class WifiState(
        override val id: String = "trigger_wifi",
        val connecte: Boolean = true,
        val ssid: String? = null
    ) : Trigger() {
        override val label = if (ssid.isNullOrBlank()) {
            if (connecte) "Wi-Fi Connecté" else "Wi-Fi Déconnecté"
        } else {
            "Wi-Fi $ssid"
        }
        override val categorie = CategorieTrigger.CONNECTIVITE
    }

    data class BluetoothState(
        override val id: String = "trigger_bluetooth",
        val connecte: Boolean = true,
        val deviceName: String? = null
    ) : Trigger() {
        override val label = if (deviceName.isNullOrBlank()) {
            if (connecte) "Bluetooth Connecté" else "Bluetooth Déconnecté"
        } else {
            "Bluetooth $deviceName"
        }
        override val categorie = CategorieTrigger.CONNECTIVITE
    }

    data class VpnState(
        override val id: String = "trigger_vpn",
        val actif: Boolean = true
    ) : Trigger() {
        override val label = if (actif) "VPN Activé" else "VPN Désactivé"
        override val categorie = CategorieTrigger.CONNECTIVITE
    }

    data class UsbConnexion(
        override val id: String = "trigger_usb",
        val connecte: Boolean = true
    ) : Trigger() {
        override val label = if (connecte) "USB Connecté" else "USB Déconnecté"
        override val categorie = CategorieTrigger.CONNECTIVITE
    }

    data class HotspotState(
        override val id: String = "trigger_hotspot",
        val actif: Boolean = true
    ) : Trigger() {
        override val label = if (actif) "Point d'Accès Activé" else "Point d'Accès Désactivé"
        override val categorie = CategorieTrigger.CONNECTIVITE
    }

    // 4. Date & Heure
    data class HeureFixe(
        override val id: String = "trigger_time",
        val heure: Int = 22,
        val minute: Int = 0,
        val jours: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7) // 1=Lundi..7=Dimanche
    ) : Trigger() {
        override val label = String.format("%02d:%02d", heure, minute)
        override val categorie = CategorieTrigger.DATE_HEURE
    }

    data class Intervalle(
        override val id: String = "trigger_interval",
        val minutes: Int = 30
    ) : Trigger() {
        override val label = "Toutes les $minutes min"
        override val categorie = CategorieTrigger.DATE_HEURE
    }

    // 5. Applications & Écran
    data class AppState(
        override val id: String = "trigger_app",
        val packageName: String = "",
        val appName: String = "Application",
        val estOuverte: Boolean = true
    ) : Trigger() {
        override val label = "$appName ${if (estOuverte) "ouverte" else "fermée"}"
        override val categorie = CategorieTrigger.APPLICATIONS
    }

    data class ContenuEcran(
        override val id: String = "trigger_screen_content",
        val motifRegex: String = "",
        val appSource: String? = null
    ) : Trigger() {
        override val label = if (appSource.isNullOrBlank()) {
            "Contenu d'écran correspond à \"$motifRegex\""
        } else {
            "Contenu de $appSource correspond à \"$motifRegex\""
        }
        override val categorie = CategorieTrigger.APPLICATIONS
    }

    data class ClicUI(
        override val id: String = "trigger_ui_click",
        val texteCible: String = ""
    ) : Trigger() {
        override val label = "Clic sur l'élément \"$texteCible\""
        override val categorie = CategorieTrigger.APPLICATIONS
    }

    data class NotificationRecue(
        override val id: String = "trigger_notification",
        val appSource: String? = null,
        val motCle: String? = null
    ) : Trigger() {
        override val label = if (motCle.isNullOrBlank()) "Notification Reçue" else "Notification '$motCle'"
        override val categorie = CategorieTrigger.APPLICATIONS
    }

    data class EcranState(
        override val id: String = "trigger_screen",
        val allume: Boolean = true
    ) : Trigger() {
        override val label = if (allume) "Écran Allumé" else "Écran Verrouillé"
        override val categorie = CategorieTrigger.EVENEMENT_APPAREIL
    }

    data class CasqueAudio(
        override val id: String = "trigger_headset",
        val branche: Boolean = true
    ) : Trigger() {
        override val label = if (branche) "Casque Branché" else "Casque Débranché"
        override val categorie = CategorieTrigger.CONNECTIVITE
    }

    data class Secousse(
        override val id: String = "trigger_shake",
        val sensibilite: Int = 12
    ) : Trigger() {
        override val label = "Appareil Secoué"
        override val categorie = CategorieTrigger.EVENEMENT_APPAREIL
    }

    data class CapteurLuminosite(
        override val id: String = "trigger_light_sensor",
        val seuilLux: Int = 10,
        val inferieur: Boolean = true
    ) : Trigger() {
        override val label = "Luminosité ${if (inferieur) "<" else ">"} $seuilLux lux"
        override val categorie = CategorieTrigger.EVENEMENT_APPAREIL
    }

    data class CapteurProximite(
        override val id: String = "trigger_proximity_sensor",
        val proche: Boolean = true
    ) : Trigger() {
        override val label = if (proche) "Objet à proximité" else "Plus d'objet à proximité"
        override val categorie = CategorieTrigger.EVENEMENT_APPAREIL
    }

    data class OrientationEcran(
        override val id: String = "trigger_orientation",
        val portrait: Boolean = true
    ) : Trigger() {
        override val label = if (portrait) "Orientation Portrait" else "Orientation Paysage"
        override val categorie = CategorieTrigger.EVENEMENT_APPAREIL
    }

    data class DemarrageAppareil(
        override val id: String = "trigger_boot"
    ) : Trigger() {
        override val label = "Démarrage de l'appareil"
        override val categorie = CategorieTrigger.EVENEMENT_APPAREIL
    }

    data class EcranDeverrouille(
        override val id: String = "trigger_screen_unlocked"
    ) : Trigger() {
        override val label = "Écran Déverrouillé"
        override val categorie = CategorieTrigger.EVENEMENT_APPAREIL
    }

    data class TorcheState(
        override val id: String = "trigger_torch",
        val allumee: Boolean = true
    ) : Trigger() {
        override val label = if (allumee) "Torche Allumée" else "Torche Éteinte"
        override val categorie = CategorieTrigger.EVENEMENT_APPAREIL
    }

    data class ModeSilencieux(
        override val id: String = "trigger_silent_mode",
        val actif: Boolean = true
    ) : Trigger() {
        override val label = if (actif) "Mode Silencieux Activé" else "Mode Silencieux Désactivé"
        override val categorie = CategorieTrigger.EVENEMENT_APPAREIL
    }

    data class PressePapierModifie(
        override val id: String = "trigger_clipboard"
    ) : Trigger() {
        override val label = "Modification du Presse-Papier"
        override val categorie = CategorieTrigger.EVENEMENT_APPAREIL
    }

    data class ChangementVariable(
        override val id: String = "trigger_var_change",
        val nomVariable: String = ""
    ) : Trigger() {
        override val label = "Variable '$nomVariable' modifiée"
        override val categorie = CategorieTrigger.EVENEMENT_APPAREIL
    }

    // 6. Déclenchement manuel ou test
    data class Manuel(
        override val id: String = "trigger_manual"
    ) : Trigger() {
        override val label = "Déclenchement Manuel"
        override val categorie = CategorieTrigger.MANUEL
    }
}

enum class CategorieTrigger(val titre: String) {
    APPEL_SMS("Appels & SMS"),
    BATTERIE("Batterie & Énergie"),
    CONNECTIVITE("Connectivité"),
    DATE_HEURE("Date & Heure"),
    APPLICATIONS("Applications"),
    EVENEMENT_APPAREIL("Appareil"),
    MANUEL("Manuel")
}
