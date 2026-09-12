package com.example.data.local.room

import com.example.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Sérialiseur/désérialiseur JSON fiable, complet et sans réflexion pour les modèles polymorphiques Ctrl
 * Conforme aux spécifications du cahier des charges Ctrl et du catalogue complet MacroDroid.
 */
object JsonConverters {

    // --- TRIGGER ---
    fun triggerToJson(trigger: Trigger): String {
        val json = JSONObject()
        when (trigger) {
            is Trigger.SmsRecu -> {
                json.put("type", "SMS_RECU")
                json.put("expediteur", trigger.expediteurFiltre ?: "")
                json.put("motCle", trigger.motCleFiltre ?: "")
            }
            is Trigger.AppelEntrant -> {
                json.put("type", "APPEL_ENTRANT")
                json.put("numero", trigger.numeroFiltre ?: "")
            }
            is Trigger.NiveauBatterie -> {
                json.put("type", "NIVEAU_BATTERIE")
                json.put("seuil", trigger.seuil)
                json.put("inferieur", trigger.inferieur)
            }
            is Trigger.Alimentation -> {
                json.put("type", "ALIMENTATION")
                json.put("connectee", trigger.connectee)
            }
            is Trigger.WifiState -> {
                json.put("type", "WIFI_STATE")
                json.put("connecte", trigger.connecte)
                json.put("ssid", trigger.ssid ?: "")
            }
            is Trigger.BluetoothState -> {
                json.put("type", "BLUETOOTH_STATE")
                json.put("connecte", trigger.connecte)
                json.put("deviceName", trigger.deviceName ?: "")
            }
            is Trigger.HeureFixe -> {
                json.put("type", "HEURE_FIXE")
                json.put("heure", trigger.heure)
                json.put("minute", trigger.minute)
                val arr = JSONArray()
                trigger.jours.forEach { arr.put(it) }
                json.put("jours", arr)
            }
            is Trigger.Intervalle -> {
                json.put("type", "INTERVALLE")
                json.put("minutes", trigger.minutes)
            }
            is Trigger.AppState -> {
                json.put("type", "APP_STATE")
                json.put("package", trigger.packageName)
                json.put("appName", trigger.appName)
                json.put("ouverte", trigger.estOuverte)
            }
            is Trigger.NotificationRecue -> {
                json.put("type", "NOTIFICATION_RECUE")
                json.put("appSource", trigger.appSource ?: "")
                json.put("motCle", trigger.motCle ?: "")
            }
            is Trigger.EcranState -> {
                json.put("type", "ECRAN_STATE")
                json.put("allume", trigger.allume)
            }
            is Trigger.CasqueAudio -> {
                json.put("type", "CASQUE_AUDIO")
                json.put("branche", trigger.branche)
            }
            is Trigger.Secousse -> {
                json.put("type", "SECOUSSE")
                json.put("sensibilite", trigger.sensibilite)
            }
            is Trigger.DemarrageAppareil -> {
                json.put("type", "BOOT_COMPLETED")
            }
            is Trigger.ChangementVariable -> {
                json.put("type", "VAR_CHANGE")
                json.put("nomVariable", trigger.nomVariable)
            }
            is Trigger.Manuel -> {
                json.put("type", "MANUEL")
            }
        }
        return json.toString()
    }

    fun jsonToTrigger(jsonStr: String): Trigger {
        return try {
            val json = JSONObject(jsonStr)
            when (json.optString("type")) {
                "SMS_RECU" -> Trigger.SmsRecu(
                    expediteurFiltre = json.optString("expediteur").ifEmpty { null },
                    motCleFiltre = json.optString("motCle").ifEmpty { null }
                )
                "APPEL_ENTRANT" -> Trigger.AppelEntrant(
                    numeroFiltre = json.optString("numero").ifEmpty { null }
                )
                "NIVEAU_BATTERIE" -> Trigger.NiveauBatterie(
                    seuil = json.optInt("seuil", 20),
                    inferieur = json.optBoolean("inferieur", true)
                )
                "ALIMENTATION" -> Trigger.Alimentation(
                    connectee = json.optBoolean("connectee", true)
                )
                "WIFI_STATE" -> Trigger.WifiState(
                    connecte = json.optBoolean("connecte", true),
                    ssid = json.optString("ssid").ifEmpty { null }
                )
                "BLUETOOTH_STATE" -> Trigger.BluetoothState(
                    connecte = json.optBoolean("connecte", true),
                    deviceName = json.optString("deviceName").ifEmpty { null }
                )
                "HEURE_FIXE" -> {
                    val arr = json.optJSONArray("jours")
                    val jours = mutableListOf<Int>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) jours.add(arr.getInt(i))
                    } else {
                        jours.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                    }
                    Trigger.HeureFixe(
                        heure = json.optInt("heure", 22),
                        minute = json.optInt("minute", 0),
                        jours = jours
                    )
                }
                "INTERVALLE" -> Trigger.Intervalle(
                    minutes = json.optInt("minutes", 30)
                )
                "APP_STATE" -> Trigger.AppState(
                    packageName = json.optString("package"),
                    appName = json.optString("appName", "Application"),
                    estOuverte = json.optBoolean("ouverte", true)
                )
                "NOTIFICATION_RECUE" -> Trigger.NotificationRecue(
                    appSource = json.optString("appSource").ifEmpty { null },
                    motCle = json.optString("motCle").ifEmpty { null }
                )
                "ECRAN_STATE" -> Trigger.EcranState(
                    allume = json.optBoolean("allume", true)
                )
                "CASQUE_AUDIO" -> Trigger.CasqueAudio(
                    branche = json.optBoolean("branche", true)
                )
                "SECOUSSE" -> Trigger.Secousse(
                    sensibilite = json.optInt("sensibilite", 12)
                )
                "BOOT_COMPLETED" -> Trigger.DemarrageAppareil()
                "VAR_CHANGE" -> Trigger.ChangementVariable(
                    nomVariable = json.optString("nomVariable")
                )
                else -> Trigger.Manuel()
            }
        } catch (_: Exception) {
            Trigger.Manuel()
        }
    }

    // --- CONDITIONS ---
    fun conditionsToJson(conditions: List<Condition>): String {
        val arr = JSONArray()
        for (c in conditions) {
            arr.put(conditionToJsonObject(c))
        }
        return arr.toString()
    }

    private fun conditionToJsonObject(c: Condition): JSONObject {
        val json = JSONObject()
        when (c) {
            is Condition.PlageHoraire -> {
                json.put("type", "PLAGE_HORAIRE")
                json.put("hDebut", c.heureDebut)
                json.put("mDebut", c.minuteDebut)
                json.put("hFin", c.heureFin)
                json.put("mFin", c.minuteFin)
            }
            is Condition.JoursSemaine -> {
                json.put("type", "JOURS_SEMAINE")
                val jArr = JSONArray()
                c.jours.forEach { jArr.put(it) }
                json.put("jours", jArr)
            }
            is Condition.NiveauBatterie -> {
                json.put("type", "NIVEAU_BATTERIE")
                json.put("seuil", c.seuil)
                json.put("superieur", c.estSuperieur)
            }
            is Condition.EnCharge -> {
                json.put("type", "EN_CHARGE")
                json.put("charge", c.doitEtreEnCharge)
            }
            is Condition.WifiConnecte -> {
                json.put("type", "WIFI_CONNECTE")
                json.put("connecte", c.doitEtreConnecte)
                json.put("nom", c.nomReseau ?: "")
            }
            is Condition.BluetoothActif -> {
                json.put("type", "BT_ACTIF")
                json.put("actif", c.doitEtreActif)
            }
            is Condition.EcranAllume -> {
                json.put("type", "ECRAN_ALLUME")
                json.put("allume", c.doitEtreAllume)
            }
            is Condition.VariableValeur -> {
                json.put("type", "VAR_VALEUR")
                json.put("nom", c.nomVariable)
                json.put("valeur", c.valeurAttendue)
            }
            is Condition.ServiceAccessibiliteActif -> {
                json.put("type", "ACCESSIBILITE_ACTIVE")
                json.put("requis", c.requis)
            }
            is Condition.CasqueBranche -> {
                json.put("type", "CASQUE_BRANCHE")
                json.put("branche", c.doitEtreBranche)
            }
            is Condition.AppAuPremierPlan -> {
                json.put("type", "APP_PREMIER_PLAN")
                json.put("package", c.packageName)
                json.put("nomApp", c.nomApp)
            }
            is Condition.Compose -> {
                json.put("type", "COMPOSE")
                json.put("operateur", c.operateur.name)
                val sousArr = JSONArray()
                c.sousConditions.forEach { sousArr.put(conditionToJsonObject(it)) }
                json.put("sousConditions", sousArr)
            }
        }
        return json
    }

    fun jsonToConditions(jsonStr: String): List<Condition> {
        val list = mutableListOf<Condition>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val json = arr.getJSONObject(i)
                jsonObjectToCondition(json)?.let { list.add(it) }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun jsonObjectToCondition(json: JSONObject): Condition? {
        return when (json.optString("type")) {
            "PLAGE_HORAIRE" -> Condition.PlageHoraire(
                heureDebut = json.optInt("hDebut", 22),
                minuteDebut = json.optInt("mDebut", 0),
                heureFin = json.optInt("hFin", 7),
                minuteFin = json.optInt("mFin", 0)
            )
            "JOURS_SEMAINE" -> {
                val jArr = json.optJSONArray("jours")
                val jList = mutableListOf<Int>()
                if (jArr != null) for (j in 0 until jArr.length()) jList.add(jArr.getInt(j))
                Condition.JoursSemaine(jours = jList)
            }
            "NIVEAU_BATTERIE" -> Condition.NiveauBatterie(
                seuil = json.optInt("seuil", 20),
                estSuperieur = json.optBoolean("superieur", true)
            )
            "EN_CHARGE" -> Condition.EnCharge(
                doitEtreEnCharge = json.optBoolean("charge", true)
            )
            "WIFI_CONNECTE" -> Condition.WifiConnecte(
                doitEtreConnecte = json.optBoolean("connecte", true),
                nomReseau = json.optString("nom").ifEmpty { null }
            )
            "BT_ACTIF" -> Condition.BluetoothActif(
                doitEtreActif = json.optBoolean("actif", true)
            )
            "ECRAN_ALLUME" -> Condition.EcranAllume(
                doitEtreAllume = json.optBoolean("allume", true)
            )
            "VAR_VALEUR" -> Condition.VariableValeur(
                nomVariable = json.optString("nom"),
                valeurAttendue = json.optString("valeur")
            )
            "ACCESSIBILITE_ACTIVE" -> Condition.ServiceAccessibiliteActif(
                requis = json.optBoolean("requis", true)
            )
            "CASQUE_BRANCHE" -> Condition.CasqueBranche(
                doitEtreBranche = json.optBoolean("branche", true)
            )
            "APP_PREMIER_PLAN" -> Condition.AppAuPremierPlan(
                packageName = json.optString("package"),
                nomApp = json.optString("nomApp", "App")
            )
            "COMPOSE" -> {
                val opStr = json.optString("operateur", "ET")
                val op = try { OperateurLogique.valueOf(opStr) } catch (_: Exception) { OperateurLogique.ET }
                val sousArr = json.optJSONArray("sousConditions")
                val sousList = mutableListOf<Condition>()
                if (sousArr != null) {
                    for (k in 0 until sousArr.length()) {
                        jsonObjectToCondition(sousArr.getJSONObject(k))?.let { sousList.add(it) }
                    }
                }
                Condition.Compose(operateur = op, sousConditions = sousList)
            }
            else -> null
        }
    }

    // --- ACTIONS ---
    fun actionsToJson(actions: List<ActionMacro>): String {
        val arr = JSONArray()
        for (a in actions) {
            arr.put(actionToJsonObject(a))
        }
        return arr.toString()
    }

    private fun actionToJsonObject(a: ActionMacro): JSONObject {
        val json = JSONObject()
        when (a) {
            is ActionMacro.EnvoyerNotification -> {
                json.put("type", "NOTIFICATION")
                json.put("titre", a.titre)
                json.put("message", a.message)
            }
            is ActionMacro.AfficherToast -> {
                json.put("type", "TOAST")
                json.put("message", a.message)
            }
            is ActionMacro.Vibrer -> {
                json.put("type", "VIBRER")
                json.put("duree", a.dureeMs)
            }
            is ActionMacro.ChangerVolume -> {
                json.put("type", "VOLUME")
                json.put("niveau", a.niveauPourcentage)
                json.put("canal", a.canal)
            }
            is ActionMacro.JouerSon -> {
                json.put("type", "SON")
                json.put("typeSon", a.typeSon)
            }
            is ActionMacro.BasculerWifi -> {
                json.put("type", "WIFI")
                json.put("activer", a.activer)
            }
            is ActionMacro.BasculerBluetooth -> {
                json.put("type", "BT")
                json.put("activer", a.activer)
            }
            is ActionMacro.LampeTorche -> {
                json.put("type", "LAMPE_TORCHE")
                json.put("activer", a.activer)
            }
            is ActionMacro.TexteParSyntheseVocale -> {
                json.put("type", "TTS")
                json.put("texte", a.texte)
                json.put("langue", a.langue)
            }
            is ActionMacro.EnvoyerSms -> {
                json.put("type", "SMS")
                json.put("destinataire", a.destinataire)
                json.put("message", a.message)
            }
            is ActionMacro.OuvrirUrl -> {
                json.put("type", "OUVRIR_URL")
                json.put("url", a.url)
            }
            is ActionMacro.ManipulerTexte -> {
                json.put("type", "MANIP_TEXTE")
                json.put("source", a.texteSource)
                json.put("operation", a.operation.name)
                json.put("argument", a.argument)
                json.put("sortie", a.variableSortie)
            }
            is ActionMacro.OuvrirApp -> {
                json.put("type", "OUVRIR_APP")
                json.put("package", a.packageName)
                json.put("nom", a.nomApp)
            }
            is ActionMacro.ActionUI -> {
                json.put("type", "ACTION_UI")
                json.put("geste", a.typeGeste.name)
                json.put("selecteur", a.selecteurType.name)
                json.put("cible", a.valeurCible)
                json.put("texteSaisie", a.texteSaisie)
            }
            is ActionMacro.Attendre -> {
                json.put("type", "ATTENDRE")
                json.put("secondes", a.secondes)
            }
            is ActionMacro.DefinirVariable -> {
                json.put("type", "DEF_VAR")
                json.put("nom", a.nomVariable)
                json.put("valeur", a.valeur)
            }
            is ActionMacro.AppelIA -> {
                json.put("type", "APPEL_IA")
                json.put("prompt", a.promptUtilisateur)
                json.put("sortie", a.variableSortie)
            }
            is ActionMacro.ControleFlux -> {
                json.put("type", "CONTROLE_FLUX")
                when (val flux = a.typeControle) {
                    is TypeControleFlux.ClauseSi -> {
                        json.put("fluxType", "SI")
                        json.put("condition", conditionToJsonObject(flux.condition))
                        val vraiArr = JSONArray()
                        flux.actionsSiVrai.forEach { vraiArr.put(actionToJsonObject(it)) }
                        json.put("siVrai", vraiArr)
                        val sinonArr = JSONArray()
                        flux.actionsSinon.forEach { sinonArr.put(actionToJsonObject(it)) }
                        json.put("sinon", sinonArr)
                    }
                    is TypeControleFlux.Repeter -> {
                        json.put("fluxType", "REPETER")
                        json.put("fois", flux.nombreFois)
                        val repArr = JSONArray()
                        flux.actions.forEach { repArr.put(actionToJsonObject(it)) }
                        json.put("actions", repArr)
                    }
                    is TypeControleFlux.RepeterTantQue -> {
                        json.put("fluxType", "REPETER_TANT_QUE")
                        json.put("condition", conditionToJsonObject(flux.condition))
                        val repArr = JSONArray()
                        flux.actions.forEach { repArr.put(actionToJsonObject(it)) }
                        json.put("actions", repArr)
                    }
                    is TypeControleFlux.SortirDeLaBoucle -> {
                        json.put("fluxType", "BREAK")
                    }
                }
            }
            is ActionMacro.DeclencherMacro -> {
                json.put("type", "TRIGGER_MACRO")
                json.put("cibleId", a.cibleMacroId)
                json.put("nomCible", a.nomCible)
            }
            is ActionMacro.ArretUrgence -> {
                json.put("type", "ARRET_URGENCE")
                json.put("raison", a.raison)
            }
        }
        return json
    }

    fun jsonToActions(jsonStr: String): List<ActionMacro> {
        val list = mutableListOf<ActionMacro>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val json = arr.getJSONObject(i)
                jsonObjectToAction(json)?.let { list.add(it) }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun jsonObjectToAction(json: JSONObject): ActionMacro? {
        return when (json.optString("type")) {
            "NOTIFICATION" -> ActionMacro.EnvoyerNotification(
                titre = json.optString("titre", "Ctrl"),
                message = json.optString("message", "")
            )
            "TOAST" -> ActionMacro.AfficherToast(
                message = json.optString("message", "")
            )
            "VIBRER" -> ActionMacro.Vibrer(
                dureeMs = json.optLong("duree", 300)
            )
            "VOLUME" -> ActionMacro.ChangerVolume(
                niveauPourcentage = json.optInt("niveau", 50),
                canal = json.optString("canal", "Média")
            )
            "SON" -> ActionMacro.JouerSon(
                typeSon = json.optString("typeSon", "Bip confirmation")
            )
            "WIFI" -> ActionMacro.BasculerWifi(
                activer = json.optBoolean("activer", true)
            )
            "BT" -> ActionMacro.BasculerBluetooth(
                activer = json.optBoolean("activer", true)
            )
            "LAMPE_TORCHE" -> ActionMacro.LampeTorche(
                activer = json.optBoolean("activer", true)
            )
            "TTS" -> ActionMacro.TexteParSyntheseVocale(
                texte = json.optString("texte", ""),
                langue = json.optString("langue", "fr")
            )
            "SMS" -> ActionMacro.EnvoyerSms(
                destinataire = json.optString("destinataire"),
                message = json.optString("message")
            )
            "OUVRIR_URL" -> ActionMacro.OuvrirUrl(
                url = json.optString("url")
            )
            "MANIP_TEXTE" -> ActionMacro.ManipulerTexte(
                texteSource = json.optString("source"),
                operation = try {
                    OperationTexte.valueOf(json.optString("operation", "MAJUSCULE"))
                } catch (_: Exception) {
                    OperationTexte.MAJUSCULE
                },
                argument = json.optString("argument"),
                variableSortie = json.optString("sortie", "texte_modifie")
            )
            "OUVRIR_APP" -> ActionMacro.OuvrirApp(
                packageName = json.optString("package"),
                nomApp = json.optString("nom", "App")
            )
            "ACTION_UI" -> ActionMacro.ActionUI(
                typeGeste = try {
                    TypeGesteUI.valueOf(json.optString("geste", "CLIC"))
                } catch (_: Exception) {
                    TypeGesteUI.CLIC
                },
                selecteurType = try {
                    TypeSelecteur.valueOf(json.optString("selecteur", "TEXTE"))
                } catch (_: Exception) {
                    TypeSelecteur.TEXTE
                },
                valeurCible = json.optString("cible", "Valider"),
                texteSaisie = json.optString("texteSaisie", "")
            )
            "ATTENDRE" -> ActionMacro.Attendre(
                secondes = json.optInt("secondes", 2)
            )
            "DEF_VAR" -> ActionMacro.DefinirVariable(
                nomVariable = json.optString("nom"),
                valeur = json.optString("valeur")
            )
            "APPEL_IA" -> ActionMacro.AppelIA(
                promptUtilisateur = json.optString("prompt"),
                variableSortie = json.optString("sortie", "resultat_ia")
            )
            "CONTROLE_FLUX" -> {
                val fluxType = json.optString("fluxType", "BREAK")
                val typeControle = when (fluxType) {
                    "SI" -> {
                        val condJson = json.optJSONObject("condition")
                        val cond = if (condJson != null) jsonObjectToCondition(condJson) else null
                        val vraiArr = json.optJSONArray("siVrai")
                        val vraiList = mutableListOf<ActionMacro>()
                        if (vraiArr != null) {
                            for (x in 0 until vraiArr.length()) jsonObjectToAction(vraiArr.getJSONObject(x))?.let { vraiList.add(it) }
                        }
                        val sinonArr = json.optJSONArray("sinon")
                        val sinonList = mutableListOf<ActionMacro>()
                        if (sinonArr != null) {
                            for (x in 0 until sinonArr.length()) jsonObjectToAction(sinonArr.getJSONObject(x))?.let { sinonList.add(it) }
                        }
                        TypeControleFlux.ClauseSi(
                            condition = cond ?: Condition.EcranAllume(true),
                            actionsSiVrai = vraiList,
                            actionsSinon = sinonList
                        )
                    }
                    "REPETER" -> {
                        val fois = json.optInt("fois", 3)
                        val repArr = json.optJSONArray("actions")
                        val repList = mutableListOf<ActionMacro>()
                        if (repArr != null) {
                            for (x in 0 until repArr.length()) jsonObjectToAction(repArr.getJSONObject(x))?.let { repList.add(it) }
                        }
                        TypeControleFlux.Repeter(nombreFois = fois, actions = repList)
                    }
                    "REPETER_TANT_QUE" -> {
                        val condJson = json.optJSONObject("condition")
                        val cond = if (condJson != null) jsonObjectToCondition(condJson) else null
                        val repArr = json.optJSONArray("actions")
                        val repList = mutableListOf<ActionMacro>()
                        if (repArr != null) {
                            for (x in 0 until repArr.length()) jsonObjectToAction(repArr.getJSONObject(x))?.let { repList.add(it) }
                        }
                        TypeControleFlux.RepeterTantQue(
                            condition = cond ?: Condition.EcranAllume(true),
                            actions = repList
                        )
                    }
                    else -> TypeControleFlux.SortirDeLaBoucle
                }
                ActionMacro.ControleFlux(typeControle = typeControle)
            }
            "TRIGGER_MACRO" -> ActionMacro.DeclencherMacro(
                cibleMacroId = json.optString("cibleId"),
                nomCible = json.optString("nomCible")
            )
            "ARRET_URGENCE" -> ActionMacro.ArretUrgence(
                raison = json.optString("raison", "Arrêt demandé")
            )
            else -> null
        }
    }
}
