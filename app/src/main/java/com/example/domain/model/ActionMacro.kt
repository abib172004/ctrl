package com.example.domain.model

/**
 * Catalogue complet des actions exécutables par le moteur Ctrl
 */
sealed class ActionMacro {
    abstract val label: String
    abstract val categorie: CategorieAction

    data class EnvoyerNotification(
        val titre: String = "Ctrl - Automatisation",
        val message: String = "Action exécutée avec succès"
    ) : ActionMacro() {
        override val label = "Notification : \"$titre\""
        override val categorie = CategorieAction.NOTIFICATION
    }

    data class AfficherToast(
        val message: String = "Macro exécutée !"
    ) : ActionMacro() {
        override val label = "Afficher message : \"$message\""
        override val categorie = CategorieAction.NOTIFICATION
    }

    data class Vibrer(
        val dureeMs: Long = 300
    ) : ActionMacro() {
        override val label = "Vibration (${dureeMs}ms)"
        override val categorie = CategorieAction.APPAREIL
    }

    data class ChangerVolume(
        val niveauPourcentage: Int = 50,
        val canal: String = "Média"
    ) : ActionMacro() {
        override val label = "Régler volume $canal à $niveauPourcentage%"
        override val categorie = CategorieAction.APPAREIL
    }

    data class JouerSon(
        val typeSon: String = "Bip confirmation"
    ) : ActionMacro() {
        override val label = "Jouer son : $typeSon"
        override val categorie = CategorieAction.APPAREIL
    }

    data class BasculerWifi(
        val activer: Boolean = true
    ) : ActionMacro() {
        override val label = if (activer) "Activer le Wi-Fi" else "Désactiver le Wi-Fi"
        override val categorie = CategorieAction.CONNECTIVITE
    }

    data class BasculerBluetooth(
        val activer: Boolean = true
    ) : ActionMacro() {
        override val label = if (activer) "Activer le Bluetooth" else "Désactiver le Bluetooth"
        override val categorie = CategorieAction.CONNECTIVITE
    }

    data class OuvrirApp(
        val packageName: String = "",
        val nomApp: String = "Application"
    ) : ActionMacro() {
        override val label = "Lancer $nomApp"
        override val categorie = CategorieAction.APPLICATIONS
    }

    data class ActionUI(
        val typeGeste: TypeGesteUI = TypeGesteUI.CLIC,
        val selecteurType: TypeSelecteur = TypeSelecteur.TEXTE,
        val valeurCible: String = "Valider",
        val texteSaisie: String = ""
    ) : ActionMacro() {
        override val label = when (typeGeste) {
            TypeGesteUI.CLIC -> "Clic UI sur [${selecteurType.name}: \"$valeurCible\"]"
            TypeGesteUI.SAISIE -> "Saisie \"$texteSaisie\" dans [\"$valeurCible\"]"
            TypeGesteUI.RETOUR -> "Appuyer sur le bouton Retour"
            TypeGesteUI.ACCUEIL -> "Retourner à l'écran d'Accueil"
        }
        override val categorie = CategorieAction.INTERFACE_UI
    }

    data class Attendre(
        val secondes: Int = 2
    ) : ActionMacro() {
        override val label = "Attendre $secondes seconde(s)"
        override val categorie = CategorieAction.FLUX
    }

    data class LampeTorche(
        val activer: Boolean = true
    ) : ActionMacro() {
        override val label = if (activer) "Allumer la Lampe Torche" else "Éteindre la Lampe Torche"
        override val categorie = CategorieAction.APPAREIL
    }

    data class TexteParSyntheseVocale(
        val texte: String = "Notification reçue",
        val langue: String = "fr"
    ) : ActionMacro() {
        override val label = "Synthèse vocale (TTS) : \"$texte\""
        override val categorie = CategorieAction.APPAREIL
    }

    data class EnvoyerSms(
        val destinataire: String = "",
        val message: String = "Alerte Ctrl"
    ) : ActionMacro() {
        override val label = "Envoyer SMS à $destinataire"
        override val categorie = CategorieAction.NOTIFICATION
    }

    data class OuvrirUrl(
        val url: String = "https://google.com"
    ) : ActionMacro() {
        override val label = "Ouvrir URL : $url"
        override val categorie = CategorieAction.APPLICATIONS
    }

    data class ManipulerTexte(
        val texteSource: String = "",
        val operation: OperationTexte = OperationTexte.MAJUSCULE,
        val argument: String = "",
        val variableSortie: String = "texte_modifie"
    ) : ActionMacro() {
        override val label = "Texte ($operation) -> {$variableSortie}"
        override val categorie = CategorieAction.VARIABLES
    }

    data class ControleFlux(
        val typeControle: TypeControleFlux = TypeControleFlux.SortirDeLaBoucle
    ) : ActionMacro() {
        override val label = when (typeControle) {
            is TypeControleFlux.ClauseSi -> "Si [Condition] Alors ${typeControle.actionsSiVrai.size} action(s)"
            is TypeControleFlux.Repeter -> "Répéter ${typeControle.nombreFois} fois (${typeControle.actions.size} actions)"
            is TypeControleFlux.RepeterTantQue -> "Répéter tant que condition (${typeControle.actions.size} actions)"
            is TypeControleFlux.SortirDeLaBoucle -> "Sortir de la boucle (Break)"
        }
        override val categorie = CategorieAction.FLUX
    }

    data class ArretUrgence(
        val raison: String = "Arrêt demandé"
    ) : ActionMacro() {
        override val label = "Arrêt d'urgence (Kill Switch)"
        override val categorie = CategorieAction.FLUX
    }

    data class DefinirVariable(
        val nomVariable: String,
        val valeur: String
    ) : ActionMacro() {
        override val label = "Définir {$nomVariable} = \"$valeur\""
        override val categorie = CategorieAction.VARIABLES
    }

    data class AppelIA(
        val promptUtilisateur: String = "Résumer le texte",
        val variableSortie: String = "resultat_ia"
    ) : ActionMacro() {
        override val label = "Requête IA : \"$promptUtilisateur\" -> {$variableSortie}"
        override val categorie = CategorieAction.INTELLIGENCE_ARTIFICIELLE
    }

    data class DeclencherMacro(
        val cibleMacroId: String,
        val nomCible: String = "Sous-macro"
    ) : ActionMacro() {
        override val label = "Exécuter macro : \"$nomCible\""
        override val categorie = CategorieAction.FLUX
    }
}

enum class OperationTexte {
    MAJUSCULE,
    MINUSCULE,
    TRIM,
    REMPLACER,
    REGEX_EXTRACTION,
    CONCATENER
}

sealed class TypeControleFlux {
    data class ClauseSi(
        val condition: Condition,
        val actionsSiVrai: List<ActionMacro> = emptyList(),
        val actionsSinon: List<ActionMacro> = emptyList()
    ) : TypeControleFlux()

    data class Repeter(
        val nombreFois: Int = 3,
        val actions: List<ActionMacro> = emptyList()
    ) : TypeControleFlux()

    data class RepeterTantQue(
        val condition: Condition,
        val actions: List<ActionMacro> = emptyList()
    ) : TypeControleFlux()

    object SortirDeLaBoucle : TypeControleFlux()
}

enum class CategorieAction(val titre: String) {
    NOTIFICATION("Notifications & Alertes"),
    APPAREIL("Paramètres Appareil"),
    CONNECTIVITE("Connectivité"),
    APPLICATIONS("Applications"),
    INTERFACE_UI("Interface & Accessibilité"),
    FLUX("Contrôle de Flux"),
    VARIABLES("Variables"),
    INTELLIGENCE_ARTIFICIELLE("Intelligence Artificielle")
}

enum class TypeGesteUI {
    CLIC,
    SAISIE,
    RETOUR,
    ACCUEIL
}

enum class TypeSelecteur {
    ID,
    DESCRIPTION,
    TEXTE,
    COORDONNEES
}
