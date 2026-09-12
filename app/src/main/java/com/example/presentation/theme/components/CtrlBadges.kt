package com.example.presentation.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.StatutExecution
import com.example.presentation.theme.CtrlColor
import com.example.presentation.theme.PillShape

/**
 * 6.5 CtrlStatChip - Remplaçant du bandeau 4-colonnes desktop, adapté en chip horizontal scrollable
 */
@Composable
fun CtrlStatChip(
    valeur: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(CtrlColor.ButterCream)
            .border(1.dp, CtrlColor.LinenBeige, PillShape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = valeur,
            color = CtrlColor.WineInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = CtrlColor.SlateSmoke,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * 6.6 CtrlStatusBadge - Indicateur d'état de macro (Active, Inactive, Erreur)
 */
@Composable
fun CtrlStatusBadge(
    active: Boolean,
    aEchoue: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when {
        aEchoue -> Triple(CtrlColor.AlertRed, CtrlColor.PureWhite, "ERREUR")
        active -> Triple(CtrlColor.LemonWhisper, CtrlColor.WineInk, "ACTIVE")
        else -> Triple(CtrlColor.LinenBeige, CtrlColor.SlateSmoke, "INACTIVE")
    }

    Box(
        modifier = modifier
            .clip(PillShape)
            .background(bgColor)
            .border(0.5.dp, if (active) CtrlColor.Sandstone else Color.Transparent, PillShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Tag affichant le trigger en police Monospace
 */
@Composable
fun CtrlTriggerTag(
    triggerTexte: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(CtrlColor.LemonWhisper)
            .border(0.5.dp, CtrlColor.Sandstone, PillShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = triggerTexte.uppercase(),
            color = CtrlColor.WineInk,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp
        )
    }
}

/**
 * 6.7 CtrlAiRibbon - Ruban en coin pour signaler une macro générée par IA
 */
@Composable
fun CtrlAiRibbon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(CtrlColor.CitrinePop)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⚡ IA",
            color = CtrlColor.WineInk,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
