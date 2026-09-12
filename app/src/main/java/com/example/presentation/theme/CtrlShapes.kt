package com.example.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formes et rayons de courbure Daylit pour Ctrl
 */
val CtrlShapes = Shapes(
    small = RoundedCornerShape(6.dp),       // Boutons standards, champs de saisie
    medium = RoundedCornerShape(6.dp),      // Cartes Macro
    large = RoundedCornerShape(16.dp),      // Panneaux et CTA hero
    extraLarge = RoundedCornerShape(12.dp)  // Illustrations et modales
)

val PillShape = RoundedCornerShape(999.dp)
val CardShape = RoundedCornerShape(6.dp)
val InputShape = RoundedCornerShape(6.dp)
