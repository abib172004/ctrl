package com.example.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Design Tokens - Palette de couleurs Daylit pour Ctrl
 * "Wine Ledger — encre burgundy profonde sur parchemin chaleureux"
 */
object CtrlColor {
    // Ancre chromatique unique
    val WineInk = Color(0xFF4D1520)         // Texte principal, boutons pleins, bordures
    val MidnightWine = Color(0xFF360912)    // Surface la plus sombre, bottom bar
    val BlushRust = Color(0xFF662F3D)       // Remplissage secondaire, accents
    val MauveAsh = Color(0xFF825B63)        // Texte de corps atténué, bordures discrètes

    // Accents lumineux rares
    val LemonWhisper = Color(0xFFFAFFA7)    // Badges actifs, highlights subtils
    val CitrinePop = Color(0xFFE6B800)      // Accent saturé, ruban IA, micro-détails
    val ButterCream = Color(0xFFFEFFE1)     // Remplissage doux pour stat chips / highlights

    // Surfaces et neutres chauds
    val Parchment = Color(0xFFFBF9F6)       // Fond d'écran principal (ivoire chaleureux)
    val PureWhite = Color(0xFFFFFFFF)       // Surface de carte, champs de saisie
    val LinenBeige = Color(0xFFF2EEE7)      // Bordures fines, séparateurs
    val Sandstone = Color(0xFFD3CAC3)       // Bordures atténuées secondaires
    val Driftwood = Color(0xFFAAA49F)       // Liens et icônes inactifs
    val SlateSmoke = Color(0xFF717182)      // Texte secondaire, légendes, sous-titres

    // Alerte et actions destructives
    val AlertRed = Color(0xFFB3314A)        // Erreurs d'exécution, suppression

    // Piliers de l'assistant d'automatisation (Déclencheurs, Conditions, Actions)
    val AutomationTrigger = Color(0xFFB71C1C)       // Déclencheurs (Encre carmin)
    val AutomationTriggerHeader = Color(0xFF7F0000)
    val AutomationCondition = Color(0xFF2E7D32)     // Conditions (Vert sauge / forêt)
    val AutomationConditionHeader = Color(0xFF1B5E20)
    val AutomationAction = Color(0xFF1565C0)        // Actions (Bleu cobalt / saphir)
    val AutomationActionHeader = Color(0xFF0D47A1)
}
