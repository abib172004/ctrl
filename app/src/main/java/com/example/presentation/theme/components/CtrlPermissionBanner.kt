package com.example.presentation.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.theme.CtrlColor

/**
 * 6.11 CtrlPermissionBanner - Bandeau d'alerte persistant si le service d'accessibilité n'est pas actif
 */
@Composable
fun CtrlPermissionBanner(
    onActivateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CtrlColor.WineInk)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = CtrlColor.LemonWhisper,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Service d'accessibilité inactif",
                    color = CtrlColor.Parchment,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Requis pour automatiser les clics et l'écran.",
                    color = CtrlColor.Sandstone,
                    fontSize = 11.sp
                )
            }
        }

        TextButton(
            onClick = onActivateClick,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "ACTIVER",
                color = CtrlColor.LemonWhisper,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
