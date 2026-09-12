package com.example.presentation.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.StatutExecution
import com.example.presentation.theme.CardShape
import com.example.presentation.theme.CtrlColor
import com.example.presentation.theme.PillShape
import com.example.presentation.theme.components.CtrlGhostButton
import com.example.presentation.theme.components.CtrlLogBubble

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CtrlColor.Parchment)
            .padding(top = 16.dp)
    ) {
        // En-tête
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Journal d'exécution",
                    color = CtrlColor.WineInk,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.02).sp
                )
                Text(
                    text = "${uiState.logsFiltres.size} événement(s) consigné(s)",
                    color = CtrlColor.SlateSmoke,
                    fontSize = 12.sp
                )
            }

            if (uiState.logs.isNotEmpty()) {
                IconButton(onClick = viewModel::effacerJournal) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = "Effacer le journal",
                        tint = CtrlColor.SlateSmoke
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filtres par statut (Chips horizontaux)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.filtreStatut == null,
                onClick = { viewModel.setFiltre(null) },
                label = { Text("Tous") },
                shape = PillShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CtrlColor.WineInk,
                    selectedLabelColor = CtrlColor.Parchment,
                    containerColor = CtrlColor.PureWhite,
                    labelColor = CtrlColor.SlateSmoke
                ),
                border = BorderStroke(1.dp, CtrlColor.LinenBeige)
            )

            StatutExecution.values().forEach { statut ->
                val isSelected = (uiState.filtreStatut == statut)
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setFiltre(if (isSelected) null else statut) },
                    label = { Text(statut.label) },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CtrlColor.WineInk,
                        selectedLabelColor = CtrlColor.Parchment,
                        containerColor = CtrlColor.PureWhite,
                        labelColor = CtrlColor.SlateSmoke
                    ),
                    border = BorderStroke(1.dp, CtrlColor.LinenBeige)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Liste du journal
        if (uiState.logsFiltres.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun événement dans le journal.",
                    color = CtrlColor.SlateSmoke,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(uiState.logsFiltres, key = { it.id }) { log ->
                    CtrlLogBubble(
                        log = log,
                        onClick = { viewModel.selectLog(log) }
                    )
                }
            }
        }
    }

    // Modal de détail du log sélectionné
    uiState.selectedLog?.let { log ->
        AlertDialog(
            onDismissRequest = { viewModel.selectLog(null) },
            title = {
                Text(
                    text = "Détail : ${log.macroNom}",
                    color = CtrlColor.WineInk,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Statut : ${log.statut.label}", color = CtrlColor.WineInk, fontWeight = FontWeight.SemiBold)
                    Text(text = "Message : ${log.message}", color = CtrlColor.SlateSmoke)
                    if (log.details != null) {
                        Text(text = "Détails : ${log.details}", color = CtrlColor.AlertRed, fontSize = 12.sp)
                    }
                    Text(text = "Durée totale : ${log.dureeMs} ms", color = CtrlColor.Driftwood, fontSize = 12.sp)
                }
            },
            confirmButton = {},
            dismissButton = {
                CtrlGhostButton(text = "Fermer", onClick = { viewModel.selectLog(null) })
            },
            shape = CardShape,
            containerColor = CtrlColor.Parchment
        )
    }
}
