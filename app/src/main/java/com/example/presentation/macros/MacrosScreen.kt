package com.example.presentation.macros

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.*
import com.example.presentation.theme.CardShape
import com.example.presentation.theme.CtrlColor
import com.example.presentation.theme.PillShape
import com.example.presentation.theme.components.CtrlMacroCard
import java.util.UUID

@Composable
fun MacrosScreen(
    viewModel: MacrosViewModel,
    onCreateMacroClick: () -> Unit,
    onMacroDetailClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }
    var showPresetsSection by remember { mutableStateOf(false) }

    val presets = remember {
        listOf(
            Macro(
                id = UUID.randomUUID().toString(),
                nom = "Flash sur Secousse",
                description = "Allume la torche lorsque le téléphone subit une secousse rapide.",
                active = true,
                trigger = Trigger.Secousse(sensibilite = 12),
                actions = listOf(ActionMacro.LampeTorche(activer = true))
            ),
            Macro(
                id = UUID.randomUUID().toString(),
                nom = "Mode Nuit Silencieux",
                description = "Réduit le volume de sonnerie à 0 à 22h00.",
                active = true,
                trigger = Trigger.HeureFixe(heure = 22, minute = 0),
                actions = listOf(
                    ActionMacro.ChangerVolume(canal = "Sonnerie", niveauPourcentage = 0),
                    ActionMacro.AfficherToast("Mode nuit activé")
                )
            ),
            Macro(
                id = UUID.randomUUID().toString(),
                nom = "Éco-Énergie 15%",
                description = "Vibre et désactive le Bluetooth quand la batterie passe sous 15%.",
                active = true,
                trigger = Trigger.NiveauBatterie(seuil = 15, inferieur = true),
                actions = listOf(
                    ActionMacro.Vibrer(dureeMs = 400),
                    ActionMacro.BasculerBluetooth(activer = false)
                )
            )
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CtrlColor.Parchment,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateMacroClick,
                containerColor = CtrlColor.WineInk,
                contentColor = CtrlColor.LemonWhisper,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Créer une macro",
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = "Nouvelle macro",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // En-tête principal de la bibliothèque de macros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Macros",
                        color = CtrlColor.WineInk,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.02).sp
                    )
                    Text(
                        text = "${uiState.totalMacros} règle(s) • ${uiState.totalActives} active(s)",
                        color = CtrlColor.SlateSmoke,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bouton Basculer Modèles suggérés
                    IconButton(
                        onClick = { showPresetsSection = !showPresetsSection },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showPresetsSection) CtrlColor.WineInk.copy(alpha = 0.1f) else CtrlColor.PureWhite,
                            contentColor = CtrlColor.WineInk
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = "Modèles prêts à l'emploi",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Bouton Menu Tri
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = CtrlColor.PureWhite,
                                contentColor = CtrlColor.WineInk
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sort,
                                contentDescription = "Trier les macros",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(CtrlColor.PureWhite)
                        ) {
                            TriMacro.values().forEach { triOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = triOption.label,
                                            fontWeight = if (uiState.tri == triOption) FontWeight.Bold else FontWeight.Normal,
                                            color = CtrlColor.WineInk
                                        )
                                    },
                                    onClick = {
                                        viewModel.setTri(triOption)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.tri == triOption) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = CtrlColor.WineInk
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Message de retour d'action (feedback toast-like)
            uiState.feedbackMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = CtrlColor.ButterCream)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            color = CtrlColor.WineInk,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissFeedback() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Fermer",
                                tint = CtrlColor.WineInk,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Barre de recherche
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = "Rechercher par nom, déclencheur...",
                        color = CtrlColor.SlateSmoke,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Recherche",
                        tint = CtrlColor.SlateSmoke,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Effacer",
                                tint = CtrlColor.SlateSmoke,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .heightIn(min = 46.dp),
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

            // Filtres par statut (Toutes / Actives / Inactives)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatutFiltreMacro.values().forEach { statut ->
                    val isSelected = uiState.filtreStatut == statut
                    val count = when (statut) {
                        StatutFiltreMacro.TOUTES -> uiState.totalMacros
                        StatutFiltreMacro.ACTIVES -> uiState.totalActives
                        StatutFiltreMacro.INACTIVES -> uiState.totalInactives
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFiltreStatut(statut) },
                        label = {
                            Text(
                                text = "${statut.label} ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CtrlColor.WineInk,
                            selectedLabelColor = CtrlColor.LemonWhisper,
                            containerColor = CtrlColor.PureWhite,
                            labelColor = CtrlColor.WineInk
                        ),
                        border = BorderStroke(1.dp, if (isSelected) CtrlColor.WineInk else CtrlColor.LinenBeige),
                        shape = PillShape
                    )
                }
            }

            // Filtres par catégories de déclencheur (scroll horizontal)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Option "Tous"
                val isAllSelected = uiState.filtreCategorie == null
                SuggestionChip(
                    onClick = { viewModel.setFiltreCategorie(null) },
                    label = {
                        Text(
                            text = "Toutes catégories",
                            fontSize = 11.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isAllSelected) CtrlColor.WineInk.copy(alpha = 0.08f) else CtrlColor.PureWhite,
                        labelColor = CtrlColor.WineInk
                    ),
                    border = BorderStroke(1.dp, if (isAllSelected) CtrlColor.WineInk else CtrlColor.LinenBeige),
                    shape = PillShape
                )

                CategorieTrigger.values().forEach { cat ->
                    val isSelected = uiState.filtreCategorie == cat
                    SuggestionChip(
                        onClick = { viewModel.setFiltreCategorie(if (isSelected) null else cat) },
                        label = {
                            Text(
                                text = cat.titre,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) CtrlColor.WineInk.copy(alpha = 0.08f) else CtrlColor.PureWhite,
                            labelColor = CtrlColor.WineInk
                        ),
                        border = BorderStroke(1.dp, if (isSelected) CtrlColor.WineInk else CtrlColor.LinenBeige),
                        shape = PillShape
                    )
                }
            }

            // Section rétractable des modèles prédéfinis
            AnimatedVisibility(visible = showPresetsSection) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
                    border = BorderStroke(1.dp, CtrlColor.LinenBeige)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = CtrlColor.WineInk,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Modèles prêts à l'emploi",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CtrlColor.WineInk
                                )
                            }
                            TextButton(
                                onClick = { showPresetsSection = false },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Masquer", fontSize = 11.sp, color = CtrlColor.SlateSmoke)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 4.dp)
                        ) {
                            items(presets) { preset ->
                                Card(
                                    modifier = Modifier.width(200.dp),
                                    shape = CardShape,
                                    colors = CardDefaults.cardColors(containerColor = CtrlColor.Parchment),
                                    border = BorderStroke(1.dp, CtrlColor.LinenBeige)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = preset.nom,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = CtrlColor.WineInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = preset.description,
                                            fontSize = 10.sp,
                                            color = CtrlColor.SlateSmoke,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = { viewModel.ajouterTemplate(preset) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CtrlColor.WineInk,
                                                contentColor = CtrlColor.LemonWhisper
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = PillShape,
                                            modifier = Modifier.fillMaxWidth().height(28.dp)
                                        ) {
                                            Text("Ajouter", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Liste principale des macros filtrées
            val macrosAffichees = uiState.macrosFiltrees

            if (macrosAffichees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = CtrlColor.SlateSmoke,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (uiState.searchQuery.isBlank()) "Aucune macro dans cette vue" else "Aucun résultat trouvé",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = CtrlColor.WineInk
                        )
                        Text(
                            text = if (uiState.searchQuery.isBlank()) {
                                "Créez une macro avec le bouton ci-dessous ou importez un modèle prêt à l'emploi."
                            } else {
                                "Essayez un autre mot-clé ou réinitialisez les filtres."
                            },
                            fontSize = 13.sp,
                            color = CtrlColor.SlateSmoke,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        if (uiState.searchQuery.isNotBlank() || uiState.filtreCategorie != null || uiState.filtreStatut != StatutFiltreMacro.TOUTES) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.onSearchQueryChanged("")
                                    viewModel.setFiltreCategorie(null)
                                    viewModel.setFiltreStatut(StatutFiltreMacro.TOUTES)
                                },
                                shape = PillShape,
                                border = BorderStroke(1.dp, CtrlColor.WineInk)
                            ) {
                                Text("Réinitialiser les filtres", color = CtrlColor.WineInk, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
                ) {
                    items(
                        items = macrosAffichees,
                        key = { it.id }
                    ) { macro ->
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
        }
    }
}
