package com.example.presentation.macrodetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.theme.CardShape
import com.example.presentation.theme.CtrlColor
import com.example.presentation.theme.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroDetailScreen(
    viewModel: MacroDetailViewModel,
    onBackClick: () -> Unit,
    onMacroDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onMacroDeleted()
        }
    }

    val macro = uiState.macro

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CtrlColor.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Détail de la macro",
                        color = CtrlColor.WineInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = CtrlColor.WineInk
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            tint = CtrlColor.AlertRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CtrlColor.Parchment
                )
            )
        }
    ) { innerPadding ->
        if (macro == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CtrlColor.WineInk)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // En-tête : Nom, description, statut et switch
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
                        border = BorderStroke(1.dp, CtrlColor.LinenBeige)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CtrlStatusBadge(active = macro.active)
                                    if (macro.estGenereeParIA) {
                                        CtrlAiRibbon()
                                    }
                                }

                                Switch(
                                    checked = macro.active,
                                    onCheckedChange = viewModel::toggleActive,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CtrlColor.Parchment,
                                        checkedTrackColor = CtrlColor.WineInk,
                                        uncheckedThumbColor = CtrlColor.SlateSmoke,
                                        uncheckedTrackColor = CtrlColor.LinenBeige
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = macro.nom,
                                color = CtrlColor.WineInk,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium
                            )

                            if (macro.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = macro.description,
                                    color = CtrlColor.SlateSmoke,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = CtrlColor.LinenBeige)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${macro.nbExecutions} exécution(s) enregistrée(s) · ${macro.tauxSuccesPourcentage}% de succès",
                                color = CtrlColor.SlateSmoke,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Section Déclencheur
                item {
                    Column {
                        Text(
                            text = "Déclencheur",
                            color = CtrlColor.WineInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
                            border = BorderStroke(1.dp, CtrlColor.LinenBeige)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CtrlTriggerTag(triggerTexte = macro.trigger.label)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = macro.trigger.categorie.titre,
                                    color = CtrlColor.SlateSmoke,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Section Conditions
                item {
                    Column {
                        Text(
                            text = "Conditions (${macro.conditions.size})",
                            color = CtrlColor.WineInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (macro.conditions.isEmpty()) {
                            Text(
                                text = "Aucune condition requise (déclenchement direct).",
                                color = CtrlColor.SlateSmoke,
                                fontSize = 13.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                macro.conditions.forEach { c ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = CardShape,
                                        colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
                                        border = BorderStroke(1.dp, CtrlColor.LinenBeige)
                                    ) {
                                        Text(
                                            text = "• ${c.label}",
                                            color = CtrlColor.WineInk,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section Actions
                item {
                    Column {
                        Text(
                            text = "Actions (${macro.actions.size})",
                            color = CtrlColor.WineInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            macro.actions.forEachIndexed { i, a ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = CardShape,
                                    colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
                                    border = BorderStroke(1.dp, CtrlColor.LinenBeige)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${i + 1}.",
                                            color = CtrlColor.WineInk,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = a.label,
                                                color = CtrlColor.WineInk,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = a.categorie.titre,
                                                color = CtrlColor.SlateSmoke,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section Test en direct
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = CtrlColor.ButterCream),
                        border = BorderStroke(1.dp, CtrlColor.LinenBeige)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Tester manuellement",
                                color = CtrlColor.WineInk,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Exécute la chaîne d'actions immédiatement et consigne le résultat dans le journal.",
                                color = CtrlColor.SlateSmoke,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            CtrlOutlinedButton(
                                text = if (uiState.isTesting) "Exécution..." else "Tester maintenant",
                                onClick = viewModel::testerMacro,
                                enabled = !uiState.isTesting,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = CtrlColor.WineInk,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            uiState.lastTestResult?.let { result ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Résultat (${result.dureeMs}ms) : ${result.message}",
                                    color = if (result.succes) CtrlColor.WineInk else CtrlColor.AlertRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Historique d'exécution récent
                item {
                    Text(
                        text = "Historique d'exécution (${uiState.logs.size})",
                        color = CtrlColor.WineInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (uiState.logs.isEmpty()) {
                    item {
                        Text(
                            text = "Aucune exécution récente enregistrée pour cette macro.",
                            color = CtrlColor.SlateSmoke,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    items(uiState.logs.take(5)) { log ->
                        CtrlLogBubble(log = log)
                    }
                }
            }
        }
    }

    // Dialogue de confirmation de suppression
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "Supprimer la macro ?",
                    color = CtrlColor.WineInk,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    text = "Cette action supprimera définitivement \"${macro?.nom}\" de vos automatisations.",
                    color = CtrlColor.SlateSmoke,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.supprimerMacro()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CtrlColor.AlertRed),
                    shape = CardShape
                ) {
                    Text("Supprimer", color = CtrlColor.PureWhite)
                }
            },
            dismissButton = {
                CtrlGhostButton(
                    text = "Annuler",
                    onClick = { showDeleteConfirmation = false }
                )
            },
            containerColor = CtrlColor.Parchment
        )
    }
}
