package com.example.domain.model

/**
 * Conditions d'exécution de macro dans Ctrl
 */
sealed class Condition {
    abstract val label: String

    data class PlageHoraire(
        val heureDebut: Int = 22,
        val minuteDebut: Int = 0,
        val heureFin: Int = 7,
        val minuteFin: Int = 0
    ) : Condition() {
        override val label = String.format("Entre %02d:%02d et %02d:%02d", heureDebut, minuteDebut, heureFin, minuteFin)
    }

    data class JoursSemaine(
        val jours: List<Int> = listOf(1, 2, 3, 4, 5) // Lundi à Vendredi
    ) : Condition() {
        override val label = if (jours.size == 5 && !jours.contains(6) && !jours.contains(7)) {
            "En semaine (Lun-Ven)"
        } else if (jours.size == 2 && jours.contains(6) && jours.contains(7)) {
            "Le week-end (Sam-Dim)"
        } else {
            "${jours.size} jours sélectionnés"
        }
    }

    data class NiveauBatterie(
        val seuil: Int = 20,
        val estSuperieur: Boolean = true
    ) : Condition() {
        override val label = "Batterie ${if (estSuperieur) ">=" else "<="} $seuil%"
    }

    data class EnCharge(
        val doitEtreEnCharge: Boolean = true
    ) : Condition() {
        override val label = if (doitEtreEnCharge) "Appareil en charge" else "Appareil sur batterie"
    }

    data class WifiConnecte(
        val doitEtreConnecte: Boolean = true,
        val nomReseau: String? = null
    ) : Condition() {
        override val label = if (nomReseau.isNullOrBlank()) {
            if (doitEtreConnecte) "Wi-Fi connecté" else "Wi-Fi déconnecté"
        } else {
            "Sur le Wi-Fi '$nomReseau'"
        }
    }

    data class BluetoothActif(
        val doitEtreActif: Boolean = true
    ) : Condition() {
        override val label = if (doitEtreActif) "Bluetooth actif" else "Bluetooth inactif"
    }

    data class EcranAllume(
        val doitEtreAllume: Boolean = true
    ) : Condition() {
        override val label = if (doitEtreAllume) "Écran allumé" else "Écran éteint"
    }

    data class VariableValeur(
        val nomVariable: String,
        val valeurAttendue: String
    ) : Condition() {
        override val label = "Variable '$nomVariable' == '$valeurAttendue'"
    }

    data class ServiceAccessibiliteActif(
        val requis: Boolean = true
    ) : Condition() {
        override val label = "Service Accessibilité actif"
    }

    data class CasqueBranche(
        val doitEtreBranche: Boolean = true
    ) : Condition() {
        override val label = if (doitEtreBranche) "Casque branché" else "Casque débranché"
    }

    data class AppAuPremierPlan(
        val packageName: String = "",
        val nomApp: String = "App"
    ) : Condition() {
        override val label = "$nomApp au premier plan"
    }

    data class Compose(
        val operateur: OperateurLogique = OperateurLogique.ET,
        val sousConditions: List<Condition> = emptyList()
    ) : Condition() {
        override val label = "${operateur.name} (${sousConditions.size} conditions)"
    }
}

enum class OperateurLogique {
    ET,
    OU,
    NON,
    OU_EXCLUSIF
}
