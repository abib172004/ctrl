package com.example.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.accessibility.CtrlAccessibilityService
import com.example.presentation.theme.CardShape
import com.example.presentation.theme.CtrlColor
import com.example.presentation.theme.PillShape
import com.example.presentation.theme.components.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCreateMacroClick: () -> Unit,
    onMacroDetailClick: (String) -> Unit,
    onNavigateToMacros: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToJournal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Vérification du statut d'accessibilité à chaque affichage
    LaunchedEffect(Unit) {
        viewModel.verifierAccessibilite()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CtrlColor.Parchment
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Bandeau de permission si accessibilité inactive
            if (!uiState.isAccessibilityEnabled) {
                item {
                    CtrlPermissionBanner(
                        onActivateClick = {
                            CtrlAccessibilityService.openAccessibilitySettings(context)
                        }
                    )
                }
            }

            // 2. Top bar Ctrl : Wordmark sobre et élégant
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ctrl",
                            color = CtrlColor.WineInk,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.02).sp
                        )
                        Text(
                            text = "Take control.",
                            color = CtrlColor.SlateSmoke,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.declencherArretUrgence() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = CtrlColor.AlertRed.copy(alpha = 0.12f),
                                contentColor = CtrlColor.AlertRed
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = PillShape
                        ) {
                            Text("Arrêt d'urgence", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        CtrlStatusBadge(
                            active = uiState.isAccessibilityEnabled,
                            aEchoue = false
                        )
                    }
                }
            }

            // 3. Message de retour (feedback)
            uiState.testFeedbackMessage?.let { feedback ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = CtrlColor.ButterCream)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = feedback,
                                color = CtrlColor.WineInk,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissFeedback() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Fermer",
                                    tint = CtrlColor.WineInk
                                )
                            }
                        }
                    }
                }
            }

            // 4. Rangée de CtrlStatChip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CtrlStatChip(
                        valeur = "${uiState.totalActives}",
                        label = "actives"
                    )
                    CtrlStatChip(
                        valeur = "${uiState.totalExecutions}",
                        label = "exécutions"
                    )
                    CtrlStatChip(
                        valeur = uiState.tauxSuccesGlobal,
                        label = "taux de succès"
                    )
                }
            }

            // 5. Actions Rapides (Hub d'accès direct)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Actions rapides",
                        color = CtrlColor.WineInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.01).sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            titre = "Nouvelle règle",
                            sousTitre = "Créer de zéro",
                            icon = Icons.Outlined.AddCircleOutline,
                            onClick = onCreateMacroClick,
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            titre = "Assistant IA",
                            sousTitre = "Générer en prompt",
                            icon = Icons.Outlined.AutoAwesome,
                            onClick = onNavigateToAi,
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            titre = "Bibliothèque",
                            sousTitre = "${uiState.macros.size} macros",
                            icon = Icons.Outlined.Bolt,
                            onClick = onNavigateToMacros,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6. Section "En surveillance active"
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Surveillance active",
                            color = CtrlColor.WineInk,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.01).sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(CtrlColor.WineInk.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${uiState.totalActives}",
                                color = CtrlColor.WineInk,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    TextButton(
                        onClick = onNavigateToMacros,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Voir toutes →",
                            color = CtrlColor.WineInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            val actives = uiState.macrosActives
            if (actives.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
                        border = BorderStroke(1.dp, CtrlColor.LinenBeige)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsPaused,
                                contentDescription = null,
                                tint = CtrlColor.SlateSmoke,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Aucune macro active actuellement",
                                fontSize = 13.sp,
                                color = CtrlColor.WineInk,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Activez une règle existante ou créez-en une nouvelle pour démarrer l'automatisation.",
                                fontSize = 11.sp,
                                color = CtrlColor.SlateSmoke,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Button(
                                onClick = onNavigateToMacros,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CtrlColor.WineInk,
                                    contentColor = CtrlColor.LemonWhisper
                                ),
                                shape = PillShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Ouvrir les macros", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(
                    items = actives.take(3),
                    key = { it.id }
                ) { macro ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        CtrlMacroCard(
                            macro = macro,
                            onToggleActive = { active ->
                                viewModel.toggleMacroActive(macro.id, active)
                            },
                            onTestClick = {
                                viewModel.testerMacro(macro)
                            },
                            onDetailsClick = {
                                onMacroDetailClick(macro.id)
                            }
                        )
                    }
                }
            }

            // 7. Section "Activité récente (Live Stream)"
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activité récente",
                        color = CtrlColor.WineInk,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.01).sp
                    )

                    TextButton(
                        onClick = onNavigateToJournal,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Journal complet →",
                            color = CtrlColor.WineInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            val logs = uiState.recentLogs
            if (logs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
                        border = BorderStroke(1.dp, CtrlColor.LinenBeige)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = CtrlColor.SlateSmoke,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Aucun événement consigné pour l'instant",
                                    fontSize = 13.sp,
                                    color = CtrlColor.WineInk,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Les déclenchements et exécutions s'afficheront ici en direct.",
                                    fontSize = 11.sp,
                                    color = CtrlColor.SlateSmoke
                                )
                            }
                        }
                    }
                }
            } else {
                items(
                    items = logs,
                    key = { it.id }
                ) { log ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        CtrlLogBubble(
                            log = log,
                            onClick = onNavigateToJournal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    titre: String,
    sousTitre: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(CardShape)
            .clickable { onClick() },
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
        border = BorderStroke(1.dp, CtrlColor.LinenBeige)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CtrlColor.WineInk.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CtrlColor.WineInk,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = titre,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CtrlColor.WineInk,
                lineHeight = 15.sp
            )
            Text(
                text = sousTitre,
                fontSize = 10.sp,
                color = CtrlColor.SlateSmoke,
                lineHeight = 12.sp
            )
        }
    }
}
