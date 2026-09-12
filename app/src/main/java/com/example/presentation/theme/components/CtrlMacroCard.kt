package com.example.presentation.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Macro
import com.example.presentation.theme.CardShape
import com.example.presentation.theme.CtrlColor

/**
 * 6.4 CtrlMacroCard - Élément central de la liste de macros
 * Fond PureWhite, bordure 1dp LinenBeige, rayon 6dp, élévation subtile teintée burgundy
 */
@Composable
fun CtrlMacroCard(
    macro: Macro,
    onToggleActive: (Boolean) -> Unit,
    onTestClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDetailsClick() },
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = CtrlColor.PureWhite
        ),
        border = BorderStroke(1.dp, CtrlColor.LinenBeige),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Ligne 1 : Tag Trigger + Ruban IA éventuel + Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    CtrlTriggerTag(triggerTexte = macro.trigger.label)
                    if (macro.estGenereeParIA) {
                        CtrlAiRibbon()
                    }
                }

                Switch(
                    checked = macro.active,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CtrlColor.Parchment,
                        checkedTrackColor = CtrlColor.WineInk,
                        uncheckedThumbColor = CtrlColor.SlateSmoke,
                        uncheckedTrackColor = CtrlColor.LinenBeige
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ligne 2 : Nom de la macro
            Text(
                text = macro.nom,
                color = CtrlColor.WineInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.015).sp
            )

            // Ligne 3 : Description courte (2 lignes max)
            if (macro.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = macro.description,
                    color = CtrlColor.SlateSmoke,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            // Actions d'automatisation
            if (macro.actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "➔ Actions (${macro.actions.size}):",
                        color = CtrlColor.SlateSmoke,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = macro.actions.take(2).joinToString(", ") { it.label } + if (macro.actions.size > 2) "..." else "",
                        color = CtrlColor.WineInk,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = CtrlColor.LinenBeige)
            Spacer(modifier = Modifier.height(8.dp))

            // Ligne 4 : Stats d'exécution + Actions (Tester / Détails)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (macro.nbExecutions == 0) "Aucune exécution" else "${macro.nbExecutions} exéc. · ${macro.tauxSuccesPourcentage}% succès",
                    color = CtrlColor.SlateSmoke,
                    fontSize = 12.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CtrlOutlinedButton(
                        text = "Tester",
                        onClick = onTestClick,
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = CtrlColor.WineInk,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    CtrlGhostButton(
                        text = "Détails",
                        onClick = onDetailsClick,
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    )
                }
            }
        }
    }
}
