package com.example.data.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.domain.model.TypeSelecteur

/**
 * Moteur de résolution de sélecteurs UI avec stratégie de fallback multi-niveaux :
 * Id -> Description -> Texte -> Position
 */
class SelectorEngine {

    fun resoudre(
        type: TypeSelecteur,
        valeur: String,
        racine: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        return when (type) {
            TypeSelecteur.ID -> resoudreParId(valeur, racine)
                ?: resoudreParTexte(valeur, racine)
                ?: resoudreParDescription(valeur, racine)

            TypeSelecteur.TEXTE -> resoudreParTexte(valeur, racine)
                ?: resoudreParDescription(valeur, racine)
                ?: resoudreParId(valeur, racine)

            TypeSelecteur.DESCRIPTION -> resoudreParDescription(valeur, racine)
                ?: resoudreParTexte(valeur, racine)
                ?: resoudreParId(valeur, racine)

            TypeSelecteur.COORDONNEES -> resoudreParCoordonnees(valeur, racine)
        }
    }

    private fun resoudreParId(id: String, racine: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val matches = racine.findAccessibilityNodeInfosByViewId(id)
        return matches?.firstOrNull { it.isVisibleToUser }
    }

    private fun resoudreParTexte(texte: String, racine: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val matches = racine.findAccessibilityNodeInfosByText(texte)
        return matches?.firstOrNull { it.isVisibleToUser }
    }

    private fun resoudreParDescription(desc: String, racine: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return trouverNoeudRecursivement(racine) { noeud ->
            val contentDesc = noeud.contentDescription?.toString()
            contentDesc != null && contentDesc.contains(desc, ignoreCase = true)
        }
    }

    private fun resoudreParCoordonnees(coords: String, racine: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val parts = coords.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (parts.size < 2) return null
        val targetX = parts[0]
        val targetY = parts[1]

        return trouverNoeudRecursivement(racine) { noeud ->
            val rect = Rect()
            noeud.getBoundsInScreen(rect)
            rect.contains(targetX, targetY) && (noeud.isClickable || noeud.isFocusable)
        }
    }

    private fun trouverNoeudRecursivement(
        noeud: AccessibilityNodeInfo,
        predicat: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (!noeud.isVisibleToUser) return null
        if (predicat(noeud)) return noeud

        for (i in 0 until noeud.childCount) {
            val enfant = noeud.getChild(i) ?: continue
            val resultat = trouverNoeudRecursivement(enfant, predicat)
            if (resultat != null) return resultat
        }
        return null
    }
}
