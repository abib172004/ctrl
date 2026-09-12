package com.example.domain.usecase

import com.example.domain.model.*
import java.util.UUID

/**
 * Moteur de synthèse IA pour la création de macro en langage naturel
 * Analyse la requête utilisateur (français ou anglais), identifie le déclencheur,
 * les conditions et les actions appropriées, et produit une Macro validée.
 */
class GenererMacroParIAUseCase {

    suspend fun genererMacro(prompt: String): Result<Macro> {
        val texte = prompt.trim().lowercase()

        if (texte.length < 3) {
            return Result.failure(IllegalArgumentException("Veuillez décrire plus précisément votre automatisation."))
        }

        // 1. Détection du Trigger
        val timeRegex = Regex("""\b(\d{1,2})[h:](\d{0,2})\b""")
        val trigger: Trigger = when {
            texte.contains("secou") || texte.contains("shake") -> {
                Trigger.Secousse(sensibilite = 12)
            }
            texte.contains("démarr") || texte.contains("boot") || texte.contains("allumage du tél") -> {
                Trigger.DemarrageAppareil()
            }
            texte.contains("appel") -> {
                Trigger.AppelEntrant()
            }
            texte.contains("casque") && (texte.contains("branch") || texte.contains("débranch")) -> {
                Trigger.CasqueAudio(branche = !texte.contains("débranch"))
            }
            timeRegex.containsMatchIn(texte) && (texte.contains("à") || texte.contains("heure") || texte.contains("tous les")) -> {
                val (heure, min) = extraireHeure(texte)
                Trigger.HeureFixe(heure = heure, minute = min)
            }
            texte.contains("batterie") && (texte.contains("faible") || texte.contains("<") || texte.contains("descend")) -> {
                val seuil = extraireNombre(texte, defaut = 20)
                Trigger.NiveauBatterie(seuil = seuil, inferieur = true)
            }
            texte.contains("charge") || texte.contains("branch") -> {
                Trigger.Alimentation(connectee = true)
            }
            (texte.contains("wifi") || texte.contains("wi-fi")) && (texte.contains("quand") || texte.contains("lorsque") || texte.contains("connect") || texte.contains("réseau")) -> {
                val nomReseau = extraireEntreGuillemets(texte)
                Trigger.WifiState(connecte = !texte.contains("déconnect"), ssid = nomReseau)
            }
            (texte.contains("bluetooth") || texte.contains("casque") || texte.contains("écouteur")) && 
                (texte.contains("connect") || texte.contains("lorsque") || texte.contains("quand") || (!texte.contains("coupe") && !texte.contains("désactiv") && !texte.contains("active "))) -> {
                Trigger.BluetoothState(connecte = true, deviceName = if (texte.contains("casque")) "Casque" else null)
            }
            timeRegex.containsMatchIn(texte) -> {
                val (heure, min) = extraireHeure(texte)
                Trigger.HeureFixe(heure = heure, minute = min)
            }
            texte.contains("sms") || texte.contains("message") -> {
                Trigger.SmsRecu(motCleFiltre = extraireEntreGuillemets(texte))
            }
            texte.contains("écran") || texte.contains("ecran") -> {
                Trigger.EcranState(allume = !texte.contains("éteint") && !texte.contains("verrouill"))
            }
            else -> Trigger.Manuel()
        }

        // 2. Détection des Conditions
        val conditions = mutableListOf<Condition>()
        if (texte.contains("nuit") || texte.contains("soir")) {
            conditions.add(Condition.PlageHoraire(22, 0, 7, 0))
        }
        if (texte.contains("semaine") || texte.contains("bureau") || texte.contains("travail")) {
            conditions.add(Condition.JoursSemaine(listOf(1, 2, 3, 4, 5)))
        }
        if (texte.contains("si wifi") || texte.contains("connecté au wifi")) {
            conditions.add(Condition.WifiConnecte(doitEtreConnecte = true))
        }
        if (texte.contains("si en charge") || texte.contains("quand en charge")) {
            conditions.add(Condition.EnCharge(doitEtreEnCharge = true))
        }

        // 3. Détection des Actions
        val actions = mutableListOf<ActionMacro>()

        if (texte.contains("lampe") || texte.contains("torche") || texte.contains("flash")) {
            val allumer = !texte.contains("étein") && !texte.contains("coupe")
            actions.add(ActionMacro.LampeTorche(activer = allumer))
        }

        if (texte.contains("parle") || texte.contains("dis ") || texte.contains("dire ") || texte.contains("vocal") || texte.contains("tts")) {
            val messageVocal = extraireEntreGuillemets(texte) ?: "Alerte système Ctrl"
            actions.add(ActionMacro.TexteParSyntheseVocale(texte = messageVocal))
        }

        if (texte.contains("sms") && (texte.contains("envoi") || texte.contains("envoyer"))) {
            val messageSms = extraireEntreGuillemets(texte) ?: "Message automatique Ctrl"
            actions.add(ActionMacro.EnvoyerSms(destinataire = "", message = messageSms))
        }

        if (texte.contains("ouvre") && (texte.contains("lien") || texte.contains("url") || texte.contains("http") || texte.contains("site"))) {
            val url = extraireEntreGuillemets(texte) ?: "https://google.com"
            actions.add(ActionMacro.OuvrirUrl(url = url))
        }

        if (texte.contains("volume")) {
            val niveau = when {
                texte.contains("muet") || texte.contains("silence") -> 0
                texte.contains("baisse") || texte.contains("rédui") -> 20
                texte.contains("monte") || texte.contains("augmente") -> 80
                else -> extraireNombre(texte, defaut = 50)
            }
            actions.add(ActionMacro.ChangerVolume(niveauPourcentage = niveau, canal = "Média"))
        }

        if (texte.contains("vibr")) {
            actions.add(ActionMacro.Vibrer(dureeMs = 400))
        }

        if (texte.contains("son") || texte.contains("bip") || texte.contains("alarme")) {
            actions.add(ActionMacro.JouerSon(typeSon = "Signal d'alerte"))
        }

        if (texte.contains("coupe le bluetooth") || texte.contains("désactive le bluetooth") || texte.contains("stop bluetooth")) {
            actions.add(ActionMacro.BasculerBluetooth(activer = false))
        } else if (texte.contains("active le bluetooth") || texte.contains("allume le bluetooth")) {
            actions.add(ActionMacro.BasculerBluetooth(activer = true))
        }

        if (texte.contains("coupe le wifi") || texte.contains("désactive le wifi")) {
            actions.add(ActionMacro.BasculerWifi(activer = false))
        } else if (texte.contains("active le wifi") || texte.contains("allume le wifi")) {
            actions.add(ActionMacro.BasculerWifi(activer = true))
        }

        if (texte.contains("clic") || texte.contains("bouton") || texte.contains("appuie sur")) {
            val cible = extraireEntreGuillemets(texte) ?: "Confirmer"
            actions.add(
                ActionMacro.ActionUI(
                    typeGeste = TypeGesteUI.CLIC,
                    selecteurType = TypeSelecteur.TEXTE,
                    valeurCible = cible
                )
            )
        }

        if (texte.contains("toast") || texte.contains("message à l'écran")) {
            actions.add(ActionMacro.AfficherToast(message = "Ctrl : Automatisation déclenchée"))
        }

        // Toujours au moins une notification si aucune action détectée
        if (actions.isEmpty() || texte.contains("notif") || texte.contains("alerte") || texte.contains("préven")) {
            actions.add(
                ActionMacro.EnvoyerNotification(
                    titre = "Ctrl - Automatisation IA",
                    message = "Déclenché par : $prompt"
                )
            )
        }

        val nom = genererTitreMacro(prompt)
        val description = "Générée par IA d'après : \"$prompt\""

        val macro = Macro(
            id = "ai_" + UUID.randomUUID().toString().take(8),
            nom = nom,
            description = description,
            active = true,
            trigger = trigger,
            conditions = conditions,
            actions = actions,
            dateCreation = System.currentTimeMillis(),
            estGenereeParIA = true
        )

        return Result.success(macro)
    }

    private fun genererTitreMacro(prompt: String): String {
        val clean = prompt.capitalizeFirst().take(40)
        return if (clean.length < prompt.length) "$clean..." else clean
    }

    private fun extraireNombre(texte: String, defaut: Int): Int {
        val regex = Regex("""\b(\d{1,3})\b""")
        val match = regex.find(texte)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: defaut
    }

    private fun extraireHeure(texte: String): Pair<Int, Int> {
        val regex = Regex("""(\d{1,2})[h:](\d{0,2})""")
        val match = regex.find(texte)
        if (match != null) {
            val h = match.groupValues[1].toIntOrNull() ?: 12
            val m = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            return Pair(h.coerceIn(0, 23), m.coerceIn(0, 59))
        }
        return Pair(22, 0)
    }

    private fun extraireEntreGuillemets(texte: String): String? {
        val regex = Regex("""["'«]([^"'»]+)["'»]""")
        return regex.find(texte)?.groupValues?.get(1)?.trim()
    }

    private fun String.capitalizeFirst(): String {
        return if (isNotEmpty()) this[0].uppercaseChar() + substring(1) else this
    }
}
