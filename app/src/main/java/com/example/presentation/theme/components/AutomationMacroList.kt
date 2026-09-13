package com.example.presentation.theme.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.presentation.theme.CardShape
import com.example.presentation.theme.CtrlColor
import com.example.presentation.theme.PillShape
import java.util.UUID

/**
 * Composant de liste UI principal pour afficher et stocker des macros d'automatisation (déclencheurs et actions).
 * Conçu selon les principes du design system Ctrl : lisibilité maximale, contraste soigné et workflow explicite.
 */
@Composable
fun AutomationMacroList(
    macros: List<Macro>,
    onToggleActive: (String, Boolean) -> Unit,
    onMacroClick: (Macro) -> Unit,
    onTestMacro: (Macro) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteMacro: ((String) -> Unit)? = null,
    onAddMacro: (() -> Unit)? = null,
    onStoreMacro: ((Macro) -> Unit)? = null,
    emptyTitle: String = "Aucune macro enregistrée",
    emptyDescription: String = "Créez votre première règle d'automatisation (déclencheur + actions).",
    contentPadding: PaddingValues = PaddingValues(bottom = 80.dp)
) {
    var showQuickAddDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // En-tête avec compteur et bouton d'ajout rapide si supporté
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${macros.size} règle(s) · ${macros.count { it.active }} active(s)",
                color = CtrlColor.SlateSmoke,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            if (onStoreMacro != null || onAddMacro != null) {
                OutlinedButton(
                    onClick = {
                        if (onStoreMacro != null) {
                            showQuickAddDialog = true
                        } else {
                            onAddMacro?.invoke()
                        }
                    },
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("add_macro_button"),
                    shape = PillShape,
                    border = BorderStroke(1.dp, CtrlColor.WineInk),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CtrlColor.PureWhite,
                        contentColor = CtrlColor.WineInk
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Ajouter une macro",
                        modifier = Modifier.size(16.dp),
                        tint = CtrlColor.WineInk
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Créer une règle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CtrlColor.WineInk
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (macros.isEmpty()) {
            CtrlEmptyState(
                titre = emptyTitle,
                description = emptyDescription,
                buttonText = "Créer une macro",
                onButtonClick = {
                    if (onStoreMacro != null) {
                        showQuickAddDialog = true
                    } else {
                        onAddMacro?.invoke()
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("automation_macro_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = contentPadding
            ) {
                items(macros, key = { it.id }) { macro ->
                    AutomationMacroCard(
                        macro = macro,
                        onToggleActive = { active -> onToggleActive(macro.id, active) },
                        onTestClick = { onTestMacro(macro) },
                        onDetailsClick = { onMacroClick(macro) },
                        onDeleteClick = onDeleteMacro?.let { del -> { del(macro.id) } },
                        modifier = Modifier.testTag("macro_card_${macro.id}")
                    )
                }
            }
        }
    }

    // Dialogue d'ajout rapide pour stocker une macro personnalisée
    if (showQuickAddDialog && onStoreMacro != null) {
        QuickStoreMacroDialog(
            onDismiss = { showQuickAddDialog = false },
            onSaveMacro = { newMacro ->
                onStoreMacro(newMacro)
                showQuickAddDialog = false
            }
        )
    }
}

/**
 * Carte détaillée exposant explicitement le Déclencheur et la séquence d'Actions d'une Macro.
 */
@Composable
fun AutomationMacroCard(
    macro: Macro,
    onToggleActive: (Boolean) -> Unit,
    onTestClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteClick: (() -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDetailsClick() },
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
        border = BorderStroke(1.dp, CtrlColor.LinenBeige),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Ligne 1 : Titre + Tag Actif/Inactif + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (macro.active) Color(0xFF2E7D32) else CtrlColor.SlateSmoke.copy(alpha = 0.5f))
                    )

                    Text(
                        text = macro.nom,
                        color = CtrlColor.WineInk,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.015).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (macro.estGenereeParIA) {
                        CtrlAiRibbon()
                    }
                }

                Switch(
                    checked = macro.active,
                    onCheckedChange = onToggleActive,
                    modifier = Modifier.testTag("switch_macro_${macro.id}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CtrlColor.PureWhite,
                        checkedTrackColor = CtrlColor.WineInk,
                        uncheckedThumbColor = CtrlColor.SlateSmoke,
                        uncheckedTrackColor = CtrlColor.LinenBeige
                    )
                )
            }

            // Description si présente
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

            Spacer(modifier = Modifier.height(12.dp))

            // PIPELINE WORKFLOW : Déclencheur ➔ Actions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CtrlColor.Parchment)
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // BLOC DÉCLENCHEUR
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val triggerIcon = getTriggerIcon(macro.trigger)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CtrlColor.LemonWhisper),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = triggerIcon,
                                contentDescription = null,
                                tint = CtrlColor.WineInk,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DÉCLENCHEUR",
                                color = CtrlColor.SlateSmoke,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = macro.trigger.label,
                                color = CtrlColor.WineInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // CONNECTEUR VISUEL DE WORKFLOW (Flèche descendante)
                    Row(
                        modifier = Modifier.padding(start = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .height(10.dp)
                                .background(CtrlColor.Driftwood)
                        )
                        Text(
                            text = "alors exécuter (${macro.actions.size} action${if (macro.actions.size > 1) "s" else ""})",
                            color = CtrlColor.SlateSmoke,
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    // BLOC ACTIONS
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        macro.actions.take(3).forEachIndexed { index, action ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CtrlColor.PureWhite)
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(CtrlColor.ButterCream),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CtrlColor.WineInk
                                    )
                                }

                                Icon(
                                    imageVector = getActionIcon(action),
                                    contentDescription = null,
                                    tint = CtrlColor.WineInk,
                                    modifier = Modifier.size(15.dp)
                                )

                                Text(
                                    text = action.label,
                                    color = CtrlColor.WineInk,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (macro.actions.size > 3) {
                            Text(
                                text = "+ ${macro.actions.size - 3} autre(s) action(s)...",
                                color = CtrlColor.SlateSmoke,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 28.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = CtrlColor.LinenBeige)
            Spacer(modifier = Modifier.height(8.dp))

            // PIED DE CARTE : Statistiques & Boutons d'action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (macro.nbExecutions == 0) "Prête à l'emploi" else "${macro.nbExecutions} exéc. · ${macro.tauxSuccesPourcentage}% succès",
                    color = CtrlColor.SlateSmoke,
                    fontSize = 12.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDeleteClick != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Supprimer la macro",
                                tint = CtrlColor.SlateSmoke,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    CtrlOutlinedButton(
                        text = "Tester",
                        onClick = onTestClick,
                        modifier = Modifier.defaultMinSize(minHeight = 34.dp),
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
                        modifier = Modifier.defaultMinSize(minHeight = 34.dp)
                    )
                }
            }
        }
    }

    // Confirmation de suppression
    if (showDeleteConfirm && onDeleteClick != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text("Supprimer la macro", color = CtrlColor.WineInk, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text("Voulez-vous supprimer définitivement la macro \"${macro.nom}\" ?", color = CtrlColor.SlateSmoke)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClick()
                    }
                ) {
                    Text("Supprimer", color = CtrlColor.AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler", color = CtrlColor.WineInk)
                }
            },
            containerColor = CtrlColor.PureWhite,
            shape = CardShape
        )
    }
}

/**
 * Dialogue intuitif pour composer et stocker directement une macro définie par l'utilisateur
 * avec un déclencheur et une ou plusieurs actions.
 */
@Composable
fun QuickStoreMacroDialog(
    onDismiss: () -> Unit,
    onSaveMacro: (Macro) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Choix du déclencheur
    val triggerOptions = remember {
        listOf(
            "Heure (22h30)" to Trigger.HeureFixe(heure = 22, minute = 30),
            "Batterie (< 20%)" to Trigger.NiveauBatterie(seuil = 20, inferieur = true),
            "Chargeur branché" to Trigger.Alimentation(connectee = true),
            "Wi-Fi connecté" to Trigger.WifiState(connecte = true),
            "Casque audio" to Trigger.CasqueAudio(branche = true),
            "Bouton Manuel" to Trigger.Manuel()
        )
    }
    var selectedTriggerIndex by remember { mutableIntStateOf(0) }

    // Actions sélectionnées
    val actionOptions = remember {
        listOf(
            "Notification confirmation" to ActionMacro.EnvoyerNotification(
                titre = "Ctrl Automatisation",
                message = "Règle exécutée avec succès !"
            ),
            "Vibration d'alerte (400ms)" to ActionMacro.Vibrer(dureeMs = 400),
            "Volume média à 50%" to ActionMacro.ChangerVolume(niveauPourcentage = 50, canal = "Média"),
            "Désactiver Bluetooth" to ActionMacro.BasculerBluetooth(activer = false),
            "Activer le Wi-Fi" to ActionMacro.BasculerWifi(activer = true),
            "Afficher un message Toast" to ActionMacro.AfficherToast(message = "Action terminée !")
        )
    }
    val selectedActions = remember { mutableStateListOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = CtrlColor.WineInk,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Nouvelle macro d'automatisation",
                    color = CtrlColor.WineInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nom de la macro
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom de la règle") },
                    placeholder = { Text("Ex: Mode Nuit, Alerte batterie...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CtrlColor.WineInk,
                        unfocusedBorderColor = CtrlColor.LinenBeige,
                        cursorColor = CtrlColor.WineInk
                    )
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optionnel)") },
                    placeholder = { Text("Ex: Automatise mes réglages du soir") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CtrlColor.WineInk,
                        unfocusedBorderColor = CtrlColor.LinenBeige,
                        cursorColor = CtrlColor.WineInk
                    )
                )

                // Sélecteur Déclencheur
                Text(
                    text = "1. DÉCLENCHEUR :",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CtrlColor.SlateSmoke
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    triggerOptions.take(3).forEachIndexed { index, (label, _) ->
                        FilterChip(
                            selected = selectedTriggerIndex == index,
                            onClick = { selectedTriggerIndex = index },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = PillShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CtrlColor.WineInk,
                                selectedLabelColor = CtrlColor.PureWhite,
                                containerColor = CtrlColor.Parchment,
                                labelColor = CtrlColor.WineInk
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    triggerOptions.drop(3).forEachIndexed { indexOffset, (label, _) ->
                        val actualIndex = indexOffset + 3
                        FilterChip(
                            selected = selectedTriggerIndex == actualIndex,
                            onClick = { selectedTriggerIndex = actualIndex },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = PillShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CtrlColor.WineInk,
                                selectedLabelColor = CtrlColor.PureWhite,
                                containerColor = CtrlColor.Parchment,
                                labelColor = CtrlColor.WineInk
                            )
                        )
                    }
                }

                // Sélecteur Actions
                Text(
                    text = "2. ACTIONS À EXÉCUTER :",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CtrlColor.SlateSmoke
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    actionOptions.forEachIndexed { index, (label, _) ->
                        val isSelected = selectedActions.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CardShape)
                                .clickable {
                                    if (isSelected) {
                                        if (selectedActions.size > 1) selectedActions.remove(index)
                                    } else {
                                        selectedActions.add(index)
                                    }
                                }
                                .background(if (isSelected) CtrlColor.LemonWhisper else CtrlColor.Parchment)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedActions.add(index)
                                    else if (selectedActions.size > 1) selectedActions.remove(index)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CtrlColor.WineInk,
                                    uncheckedColor = CtrlColor.SlateSmoke
                                )
                            )
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = CtrlColor.WineInk,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            CtrlPrimaryButton(
                text = "Enregistrer dans Room",
                enabled = nom.isNotBlank() && selectedActions.isNotEmpty(),
                onClick = {
                    val finalActions = selectedActions.map { actionOptions[it].second }
                    val finalTrigger = triggerOptions[selectedTriggerIndex].second
                    val newMacro = Macro(
                        id = "user_macro_${UUID.randomUUID().toString().take(8)}",
                        nom = nom.trim(),
                        description = description.trim(),
                        active = true,
                        trigger = finalTrigger,
                        conditions = emptyList(),
                        actions = finalActions,
                        dateCreation = System.currentTimeMillis()
                    )
                    onSaveMacro(newMacro)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = CtrlColor.WineInk)
            }
        },
        containerColor = CtrlColor.PureWhite,
        shape = CardShape
    )
}

/**
 * Associe une icône représentative au type de déclencheur
 */
private fun getTriggerIcon(trigger: Trigger): ImageVector {
    return when (trigger) {
        is Trigger.NiveauBatterie, is Trigger.Alimentation -> Icons.Outlined.BatteryChargingFull
        is Trigger.HeureFixe, is Trigger.Intervalle -> Icons.Outlined.Schedule
        is Trigger.WifiState -> Icons.Outlined.Wifi
        is Trigger.BluetoothState -> Icons.Outlined.Bluetooth
        is Trigger.SmsRecu -> Icons.Outlined.Sms
        is Trigger.AppelEntrant -> Icons.Outlined.Call
        is Trigger.CasqueAudio -> Icons.Outlined.Headphones
        is Trigger.Secousse -> Icons.Outlined.Vibration
        is Trigger.EcranState -> Icons.Outlined.Smartphone
        is Trigger.NotificationRecue -> Icons.Outlined.Notifications
        is Trigger.AppState -> Icons.Outlined.Apps
        is Trigger.DemarrageAppareil -> Icons.Outlined.PowerSettingsNew
        is Trigger.Manuel -> Icons.Outlined.TouchApp
        is Trigger.AppelSortant, is Trigger.AppelManque, is Trigger.AppelTermine -> Icons.Outlined.Call
        is Trigger.SmsEnvoye -> Icons.Outlined.Send
        is Trigger.TemperatureBatterie -> Icons.Outlined.Thermostat
        is Trigger.EconomiseurBatterie -> Icons.Outlined.BatteryAlert
        is Trigger.VpnState -> Icons.Outlined.VpnKey
        is Trigger.UsbConnexion -> Icons.Outlined.Usb
        is Trigger.HotspotState -> Icons.Outlined.WifiTethering
        is Trigger.CapteurLuminosite -> Icons.Outlined.WbSunny
        is Trigger.CapteurProximite -> Icons.Outlined.Sensors
        is Trigger.OrientationEcran -> Icons.Outlined.ScreenRotation
        is Trigger.EcranDeverrouille -> Icons.Outlined.LockOpen
        is Trigger.TorcheState -> Icons.Outlined.FlashlightOn
        is Trigger.ModeSilencieux -> Icons.Outlined.VolumeOff
        is Trigger.PressePapierModifie -> Icons.Outlined.ContentPaste
        is Trigger.ContenuEcran -> Icons.Outlined.FindInPage
        is Trigger.ClicUI -> Icons.Outlined.TouchApp
        else -> Icons.Outlined.Bolt
    }
}

/**
 * Associe une icône représentative au type d'action
 */
private fun getActionIcon(action: ActionMacro): ImageVector {
    return when (action) {
        is ActionMacro.EnvoyerNotification -> Icons.Outlined.Notifications
        is ActionMacro.AfficherToast -> Icons.Outlined.Chat
        is ActionMacro.Vibrer -> Icons.Outlined.Vibration
        is ActionMacro.ChangerVolume -> Icons.Outlined.VolumeUp
        is ActionMacro.JouerSon -> Icons.Outlined.MusicNote
        is ActionMacro.BasculerWifi -> Icons.Outlined.Wifi
        is ActionMacro.BasculerBluetooth -> Icons.Outlined.Bluetooth
        is ActionMacro.LampeTorche -> Icons.Outlined.FlashlightOn
        is ActionMacro.Attendre -> Icons.Outlined.HourglassEmpty
        is ActionMacro.ActionUI -> Icons.Outlined.TouchApp
        is ActionMacro.AppelIA -> Icons.Outlined.AutoAwesome
        is ActionMacro.OuvrirApp -> Icons.Outlined.Launch
        is ActionMacro.OuvrirUrl -> Icons.Outlined.Language
        is ActionMacro.TexteParSyntheseVocale -> Icons.Outlined.RecordVoiceOver
        is ActionMacro.PasserAppel -> Icons.Outlined.Call
        is ActionMacro.PartagerTexte -> Icons.Outlined.Share
        is ActionMacro.RemplirPressePapier -> Icons.Outlined.ContentPaste
        is ActionMacro.EnvoyerIntent -> Icons.Outlined.Send
        is ActionMacro.RequeteHttp -> Icons.Outlined.Http
        is ActionMacro.AnalyseJson -> Icons.Outlined.DataObject
        is ActionMacro.EffacerNotifications -> Icons.Outlined.ClearAll
        is ActionMacro.OuvrirJournalAppels -> Icons.Outlined.History
        is ActionMacro.ManipulerListe -> Icons.Outlined.List
        is ActionMacro.VerifierTexteEcran -> Icons.Outlined.FindInPage
        else -> Icons.Outlined.CheckCircle
    }
}
