package com.example.presentation.theme.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.theme.CtrlColor

/**
 * 6.10 CtrlEmptyState - État vide élégant avec illustration géométrique de nœuds connectés
 */
@Composable
fun CtrlEmptyState(
    titre: String = "Aucune macro configurée",
    description: String = "Créez votre première règle d'automatisation ou utilisez l'IA pour la générer en une phrase.",
    buttonText: String = "Créer ma première macro",
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Illustration géométrique élégante de nœuds connectés
        Box(
            modifier = Modifier
                .size(100.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val left = Offset(size.width * 0.2f, size.height * 0.5f)
                val right = Offset(size.width * 0.8f, size.height * 0.5f)
                val top = Offset(size.width * 0.5f, size.height * 0.2f)

                // Lignes de connexion
                drawLine(
                    color = CtrlColor.Sandstone,
                    start = left,
                    end = center,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = CtrlColor.Sandstone,
                    start = center,
                    end = right,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = CtrlColor.Sandstone,
                    start = center,
                    end = top,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Nœuds
                drawCircle(color = CtrlColor.LemonWhisper, radius = 10.dp.toPx(), center = left)
                drawCircle(color = CtrlColor.WineInk, radius = 4.dp.toPx(), center = left)

                drawCircle(color = CtrlColor.WineInk, radius = 12.dp.toPx(), center = center)
                drawCircle(color = CtrlColor.Parchment, radius = 5.dp.toPx(), center = center)

                drawCircle(color = CtrlColor.CitrinePop, radius = 8.dp.toPx(), center = top)
                drawCircle(color = CtrlColor.ButterCream, radius = 10.dp.toPx(), center = right)
                drawCircle(color = CtrlColor.WineInk, radius = 4.dp.toPx(), center = right)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = titre,
            color = CtrlColor.WineInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            color = CtrlColor.SlateSmoke,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        CtrlPrimaryButton(
            text = buttonText,
            onClick = onButtonClick,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = CtrlColor.Parchment,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        )
    }
}
