package com.example.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        containerColor = CtrlColor.Parchment,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateMacroClick,
                containerColor = CtrlColor.WineInk,
                contentColor = CtrlColor.LemonWhisper,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Créer une macro",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Bandeau de permission si accessibilité inactive
            if (!uiState.isAccessibilityEnabled) {
                CtrlPermissionBanner(
                    onActivateClick = {
                        CtrlAccessibilityService.openAccessibilitySettings(context)
                    }
                )
            }

            // Top bar Ctrl : Wordmark sobre et élégant
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
                        fontSize = 26.sp,
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

            // Message de retour du test si présent
            uiState.testFeedbackMessage?.let { feedback ->
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

            // 6.5 Rangée de CtrlStatChip scrollable horizontalement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
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

            Spacer(modifier = Modifier.height(8.dp))

            // En-tête de section "Mes macros" et barre de recherche
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mes macros",
                        color = CtrlColor.WineInk,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.015).sp
                    )

                    Text(
                        text = "${uiState.macrosFiltrees.size} macro(s)",
                        color = CtrlColor.SlateSmoke,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Champ de recherche compact
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = {
                        Text(
                            text = "Rechercher une macro...",
                            color = CtrlColor.SlateSmoke,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Recherche",
                            tint = CtrlColor.SlateSmoke,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = CardShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CtrlColor.PureWhite,
                        unfocusedContainerColor = CtrlColor.PureWhite,
                        focusedBorderColor = CtrlColor.WineInk,
                        unfocusedBorderColor = CtrlColor.LinenBeige,
                        cursorColor = CtrlColor.WineInk
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Liste des macros d'automatisation avec déclencheurs et actions
            AutomationMacroList(
                macros = uiState.macrosFiltrees,
                onToggleActive = { id, active ->
                    viewModel.toggleMacroActive(id, active)
                },
                onTestMacro = { macro ->
                    viewModel.testerMacro(macro)
                },
                onMacroClick = { macro ->
                    onMacroDetailClick(macro.id)
                },
                onDeleteMacro = { id ->
                    viewModel.supprimerMacro(id)
                },
                onAddMacro = onCreateMacroClick,
                onStoreMacro = { macro ->
                    viewModel.enregistrerMacro(macro)
                },
                emptyTitle = if (uiState.searchQuery.isBlank()) "Aucune macro enregistrée" else "Aucun résultat",
                emptyDescription = if (uiState.searchQuery.isBlank()) {
                    "Créez votre première règle d'automatisation (déclencheur + actions)."
                } else {
                    "Aucune macro ne correspond à \"${uiState.searchQuery}\"."
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            )
        }
    }
}
