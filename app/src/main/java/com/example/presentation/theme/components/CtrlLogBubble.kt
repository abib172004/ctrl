package com.example.presentation.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
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
import com.example.domain.model.LogEntry
import com.example.domain.model.StatutExecution
import com.example.presentation.theme.CardShape
import com.example.presentation.theme.CtrlColor
import java.text.SimpleDateFormat
import java.util.*

/**
 * 6.8 CtrlLogBubble - Entrée du journal d'exécution
 */
@Composable
fun CtrlLogBubble(
    log: LogEntry,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (icon, iconColor, surfaceColor) = when (log.statut) {
        StatutExecution.SUCCES -> Triple(Icons.Filled.CheckCircle, CtrlColor.WineInk, CtrlColor.PureWhite)
        StatutExecution.ECHEC -> Triple(Icons.Filled.Error, CtrlColor.AlertRed, CtrlColor.PureWhite)
        StatutExecution.CONDITIONS_NON_REMPLIES -> Triple(Icons.Filled.Info, CtrlColor.SlateSmoke, CtrlColor.ButterCream)
        StatutExecution.EN_COURS -> Triple(Icons.Filled.Info, CtrlColor.CitrinePop, CtrlColor.PureWhite)
    }

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(log.timestamp))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(surfaceColor)
            .border(0.5.dp, CtrlColor.LinenBeige, CardShape)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = log.statut.label,
                tint = iconColor,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.macroNom,
                        color = CtrlColor.WineInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = formattedTime,
                        color = CtrlColor.SlateSmoke,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = log.message,
                    color = if (log.statut == StatutExecution.ECHEC) CtrlColor.AlertRed else CtrlColor.SlateSmoke,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )

                if (log.dureeMs > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Durée : ${log.dureeMs}ms",
                        color = CtrlColor.Driftwood,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
