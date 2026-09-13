package com.example.data.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import com.example.data.local.room.CtrlDatabase
import com.example.data.ocr.OcrEngine
import com.example.data.repository.LogRepositoryImpl
import com.example.data.repository.MacroRepositoryImpl
import com.example.data.repository.VariableRepositoryImpl
import com.example.domain.model.Trigger
import com.example.domain.usecase.EvaluerConditionsUseCase
import com.example.domain.usecase.ExecuterMacroUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Service d'accessibilité natif de Ctrl - Cœur technique du projet
 * Permet la lecture, la détection d'événements UI et l'interaction avec l'interface utilisateur des applications.
 * Conforme aux spécifications des sections 4.1, 4.2 et 8.1 du cahier des charges Ctrl.
 */
class CtrlAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private val ocrEngine = OcrEngine()

    private val dao by lazy { CtrlDatabase.getInstance(applicationContext).ctrlDao() }
    private val macroRepo by lazy { MacroRepositoryImpl(dao) }
    private val varRepo by lazy { VariableRepositoryImpl(dao) }
    private val logRepo by lazy { LogRepositoryImpl(dao) }
    private val evaluerUseCase by lazy { EvaluerConditionsUseCase(applicationContext, varRepo) }
    private val executerUseCase by lazy { ExecuterMacroUseCase(applicationContext, evaluerUseCase, macroRepo, varRepo, logRepo) }

    // Cache mémoire des macros actives réactif - évite de requêter Room et parser le JSON à chaque événement
    @Volatile
    private var macrosActivesCache: List<com.example.domain.model.Macro> = emptyList()

    // Anti-spam : TYPE_WINDOW_CONTENT_CHANGED limité à 1 fois / 1000ms
    private var derniereVerifContenuMs = 0L
    private val etatMotifTrouve = mutableMapOf<String, Boolean>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        currentService = WeakReference(this)

        // Abonnement continu au flux des macros pour maintenir le cache ultra-rapide
        serviceScope.launch {
            try {
                macroRepo.getMacros().collect { liste ->
                    macrosActivesCache = liste.filter { it.active }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: ""
        // CRITIQUE : Ne jamais intercepter ni auto-analyser l'interface de Ctrl elle-même
        if (pkgName == packageName) return

        val eventType = event.eventType

        when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (pkgName.isNotBlank() && macrosActivesCache.any { it.trigger is Trigger.AppState }) {
                    verifierDeclencheursApplication(pkgName, estOuverte = true)
                }
            }
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                if (macrosActivesCache.any { it.trigger is Trigger.NotificationRecue }) {
                    val texteNotification = event.text.joinToString(" ")
                    verifierDeclencheursNotification(pkgName, texteNotification)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Vérifier l'écran UNIQUEMENT si au moins une macro active en dépend
                if (macrosActivesCache.any { it.trigger is Trigger.ContenuEcran }) {
                    val maintenant = System.currentTimeMillis()
                    if (maintenant - derniereVerifContenuMs >= 1000L) {
                        derniereVerifContenuMs = maintenant
                        verifierDeclencheursContenuEcran(pkgName)
                    }
                }
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Intercepter les clics UNIQUEMENT si une macro active écoute un clic UI
                if (macrosActivesCache.any { it.trigger is Trigger.ClicUI }) {
                    val noeudClique = event.source
                    val texteClique = noeudClique?.text?.toString()
                        ?: noeudClique?.contentDescription?.toString()
                        ?: event.text?.joinToString(" ")
                    if (!texteClique.isNullOrBlank()) {
                        verifierDeclencheursClicUI(texteClique)
                    }
                }
            }
        }
    }

    private fun verifierDeclencheursClicUI(texteClique: String) {
        serviceScope.launch {
            try {
                val cibles = macrosActivesCache.filter { it.trigger is Trigger.ClicUI }
                for (m in cibles) {
                    val t = m.trigger as Trigger.ClicUI
                    if (t.texteCible.isNotBlank() && texteClique.contains(t.texteCible, ignoreCase = true)) {
                        executerUseCase.executer(m)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun verifierDeclencheursContenuEcran(pkgName: String) {
        val racine = rootInActiveWindow ?: return
        serviceScope.launch {
            try {
                val textes = ocrEngine.extraireTextesNode(racine)
                val cibles = macrosActivesCache.filter { it.trigger is Trigger.ContenuEcran }
                for (m in cibles) {
                    val t = m.trigger as Trigger.ContenuEcran
                    if (t.motifRegex.isNotBlank()) {
                        val sourceMatch = t.appSource.isNullOrBlank() || t.appSource.equals(pkgName, ignoreCase = true)
                        if (!sourceMatch) continue
                        val trouve = ocrEngine.verifierPresenceTexte(textes, t.motifRegex)
                        val etaitTrouve = etatMotifTrouve[m.id] ?: false
                        if (trouve && !etaitTrouve) {
                            executerUseCase.executer(m)
                        }
                        etatMotifTrouve[m.id] = trouve
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun verifierDeclencheursApplication(pkgName: String, estOuverte: Boolean) {
        serviceScope.launch {
            try {
                val cibles = macrosActivesCache.filter { it.trigger is Trigger.AppState }
                for (m in cibles) {
                    val t = m.trigger as Trigger.AppState
                    if (t.packageName.equals(pkgName, ignoreCase = true) && t.estOuverte == estOuverte) {
                        executerUseCase.executer(m)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun verifierDeclencheursNotification(pkgName: String, texte: String) {
        serviceScope.launch {
            try {
                val cibles = macrosActivesCache.filter { it.trigger is Trigger.NotificationRecue }
                for (m in cibles) {
                    val t = m.trigger as Trigger.NotificationRecue
                    val sourceMatch = t.appSource.isNullOrBlank() || t.appSource.equals(pkgName, ignoreCase = true)
                    val motCleMatch = t.motCle.isNullOrBlank() || texte.contains(t.motCle, ignoreCase = true)
                    if (sourceMatch && motCleMatch) {
                        executerUseCase.executer(m)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onInterrupt() {
        currentService = null
    }

    override fun onDestroy() {
        super.onDestroy()
        currentService = null
    }

    companion object {
        private var currentService: WeakReference<CtrlAccessibilityService>? = null

        fun getService(): CtrlAccessibilityService? = currentService?.get()

        /**
         * Vérifie si le service d'accessibilité Ctrl est actuellement actif dans les réglages système
         */
        fun isEnabled(context: Context): Boolean {
            if (currentService?.get() != null) return true
            val expectedServiceName = "${context.packageName}/${CtrlAccessibilityService::class.java.canonicalName}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        /**
         * Ouvre l'écran des réglages d'accessibilité du système
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
