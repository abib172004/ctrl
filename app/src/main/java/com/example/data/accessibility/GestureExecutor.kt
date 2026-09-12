package com.example.data.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.example.domain.model.TypeGesteUI
import com.example.domain.model.TypeSelecteur
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Exécuteur de gestes et d'interactions UI pour Ctrl
 */
class GestureExecutor(
    private val selectorEngine: SelectorEngine = SelectorEngine()
) {

    suspend fun executerGeste(
        service: CtrlAccessibilityService,
        geste: TypeGesteUI,
        selecteurType: TypeSelecteur,
        cible: String,
        texteSaisie: String = ""
    ): Boolean {
        when (geste) {
            TypeGesteUI.RETOUR -> {
                return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            }
            TypeGesteUI.ACCUEIL -> {
                return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }
            TypeGesteUI.CLIC -> {
                val root = service.rootInActiveWindow ?: return false
                val noeud = selectorEngine.resoudre(selecteurType, cible, root)
                if (noeud != null) {
                    if (noeud.isClickable && noeud.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        return true
                    }
                    // Fallback: clic par coordonnées précises au centre du composant
                    val rect = Rect()
                    noeud.getBoundsInScreen(rect)
                    return simulerClicCoordonnees(service, rect.centerX().toFloat(), rect.centerY().toFloat())
                }
                return false
            }
            TypeGesteUI.SAISIE -> {
                val root = service.rootInActiveWindow ?: return false
                val noeud = selectorEngine.resoudre(selecteurType, cible, root)
                if (noeud != null) {
                    noeud.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    val args = Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            texteSaisie
                        )
                    }
                    return noeud.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                }
                return false
            }
        }
    }

    private suspend fun simulerClicCoordonnees(
        service: CtrlAccessibilityService,
        x: Float,
        y: Float
    ): Boolean = withTimeoutOrNull(2000L) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 80)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val deferred = CompletableDeferred<Boolean>()
        service.dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    deferred.complete(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    deferred.complete(false)
                }
            },
            null
        )
        deferred.await()
    } ?: false
}
