package com.example.presentation.macrobuilder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.*
import com.example.presentation.theme.CardShape
import com.example.presentation.theme.CtrlColor
import com.example.presentation.theme.PillShape
import com.example.presentation.theme.components.*

// Couleurs fidèles aux codes MacroDroid
private val MacroDroidRed = Color(0xFFD32F2F)
private val MacroDroidRedHeader = Color(0xFFB71C1C)
private val MacroDroidGreen = Color(0xFF2E7D32)
private val MacroDroidGreenHeader = Color(0xFF1B5E20)
private val MacroDroidBlue = Color(0xFF1976D2)
private val MacroDroidBlueHeader = Color(0xFF0D47A1)

// Enum des types de configuration
private enum class TriggerConfigType {
    HEURE_JOURNEE,
    INTERVALLE,
    NIVEAU_BATTERIE,
    ALIMENTATION,
    WIFI,
    BLUETOOTH,
    APPEL_ENTRANT,
    SMS_RECU,
    APPLICATION,
    ECRAN,
    CASQUE,
    VARIABLE,
    NOTIFICATION,
    CONTENU_ECRAN,
    CLIC_UI
}

private enum class ConditionConfigType {
    NIVEAU_BATTERIE,
    ALIMENTATION,
    WIFI,
    BLUETOOTH,
    PLAGE_HORAIRE,
    JOURS_SEMAINE,
    ECRAN,
    CASQUE,
    APPLICATION,
    VARIABLE
}

private enum class ActionConfigType {
    LAMPE_TORCHE,
    VOLUME,
    VIBRATION,
    TTS,
    NOTIFICATION,
    TOAST,
    SMS,
    APPLICATION,
    URL,
    WIFI,
    BLUETOOTH,
    ATTENDRE,
    INTERFACE_UI,
    VARIABLE,
    IA_GEMINI,
    VERIF_TEXTE_ECRAN
}

private data class TriggerPreset(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val category: String,
    val configType: TriggerConfigType? = null,
    val defaultTrigger: Trigger
)

private data class ConditionPreset(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val category: String,
    val configType: ConditionConfigType? = null,
    val defaultCondition: Condition
)

private data class ActionPreset(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val category: String,
    val configType: ActionConfigType? = null,
    val defaultAction: ActionMacro
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroBuilderScreen(
    viewModel: MacroBuilderViewModel,
    onBackClick: () -> Unit,
    onMacroSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showConditionListDialog by remember { mutableStateOf(false) }
    var showActionListDialog by remember { mutableStateOf(false) }

    var triggerBeingConfigured by remember { mutableStateOf<TriggerPreset?>(null) }
    var conditionBeingConfigured by remember { mutableStateOf<ConditionPreset?>(null) }
    var actionBeingConfigured by remember { mutableStateOf<ActionPreset?>(null) }

    LaunchedEffect(Unit) {
        if (uiState.isSaved) {
            viewModel.reset()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onMacroSaved()
            viewModel.reset()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CtrlColor.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.etapeActuelle) {
                            1 -> "1. Déclencheur"
                            2 -> "2. Conditions"
                            else -> "3. Actions & Nom"
                        },
                        color = CtrlColor.WineInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.reset()
                        onBackClick()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = CtrlColor.WineInk
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CtrlColor.Parchment
                )
            )
        },
        bottomBar = {
            Surface(
                color = CtrlColor.Parchment,
                border = BorderStroke(0.5.dp, CtrlColor.LinenBeige),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.etapeActuelle > 1) {
                        CtrlGhostButton(
                            text = "Précédent",
                            onClick = { viewModel.setEtape(uiState.etapeActuelle - 1) }
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (uiState.etapeActuelle < 3) {
                        CtrlPrimaryButton(
                            text = "Suivant",
                            onClick = { viewModel.setEtape(uiState.etapeActuelle + 1) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = CtrlColor.PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    } else {
                        CtrlPrimaryButton(
                            text = "Enregistrer la macro",
                            onClick = viewModel::enregistrerMacro,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = CtrlColor.PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Indicateur d'étapes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EtapeIndicateur(numero = 1, titre = "Déclencheur", actif = uiState.etapeActuelle == 1, fait = uiState.etapeActuelle > 1)
                HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 6.dp), color = CtrlColor.LinenBeige)
                EtapeIndicateur(numero = 2, titre = "Conditions", actif = uiState.etapeActuelle == 2, fait = uiState.etapeActuelle > 2)
                HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 6.dp), color = CtrlColor.LinenBeige)
                EtapeIndicateur(numero = 3, titre = "Actions", actif = uiState.etapeActuelle == 3, fait = false)
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (uiState.etapeActuelle) {
                1 -> EtapeTriggerGridContent(
                    triggerActuel = uiState.triggerSelectionne,
                    onTriggerClick = { preset ->
                        if (preset.configType != null) {
                            triggerBeingConfigured = preset
                        } else {
                            viewModel.setTrigger(preset.defaultTrigger)
                        }
                    }
                )
                2 -> EtapeConditionsContent(
                    conditions = uiState.conditions,
                    onAjouterClick = { showConditionListDialog = true },
                    onSupprimerCondition = viewModel::supprimerCondition
                )
                3 -> EtapeActionsEtNomContent(
                    nom = uiState.nom,
                    description = uiState.description,
                    actions = uiState.actions,
                    errorMessage = uiState.errorMessage,
                    onNomChanged = viewModel::updateNom,
                    onDescriptionChanged = viewModel::updateDescription,
                    onAjouterActionClick = { showActionListDialog = true },
                    onSupprimerAction = viewModel::supprimerAction
                )
            }
        }
    }

    // Modal de configuration de déclencheur
    triggerBeingConfigured?.let { preset ->
        ConfigurerTriggerDialog(
            preset = preset,
            onDismiss = { triggerBeingConfigured = null },
            onConfirm = { configuredTrigger ->
                viewModel.setTrigger(configuredTrigger)
                triggerBeingConfigured = null
            }
        )
    }

    // Modal catalogue conditions
    if (showConditionListDialog) {
        CatalogueConditionsDialog(
            onDismiss = { showConditionListDialog = false },
            onConditionSelect = { preset ->
                showConditionListDialog = false
                if (preset.configType != null) {
                    conditionBeingConfigured = preset
                } else {
                    viewModel.ajouterCondition(preset.defaultCondition)
                }
            }
        )
    }

    // Modal de configuration de condition
    conditionBeingConfigured?.let { preset ->
        ConfigurerConditionDialog(
            preset = preset,
            onDismiss = { conditionBeingConfigured = null },
            onConfirm = { configuredCondition ->
                viewModel.ajouterCondition(configuredCondition)
                conditionBeingConfigured = null
            }
        )
    }

    // Modal catalogue actions
    if (showActionListDialog) {
        CatalogueActionsDialog(
            onDismiss = { showActionListDialog = false },
            onActionSelect = { preset ->
                showActionListDialog = false
                if (preset.configType != null) {
                    actionBeingConfigured = preset
                } else {
                    viewModel.ajouterAction(preset.defaultAction)
                }
            }
        )
    }

    // Modal de configuration d'action
    actionBeingConfigured?.let { preset ->
        ConfigurerActionDialog(
            preset = preset,
            onDismiss = { actionBeingConfigured = null },
            onConfirm = { configuredAction ->
                viewModel.ajouterAction(configuredAction)
                actionBeingConfigured = null
            }
        )
    }
}

@Composable
private fun EtapeIndicateur(numero: Int, titre: String, actif: Boolean, fait: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(PillShape)
                .background(
                    when {
                        fait -> CtrlColor.WineInk
                        actif -> CtrlColor.LemonWhisper
                        else -> CtrlColor.LinenBeige
                    }
                )
                .border(
                    1.dp,
                    if (actif) CtrlColor.WineInk else Color.Transparent,
                    PillShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (fait) "✓" else "$numero",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (fait) CtrlColor.Parchment else CtrlColor.WineInk
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = titre,
            fontSize = 12.sp,
            color = if (actif) CtrlColor.WineInk else CtrlColor.SlateSmoke,
            fontWeight = if (actif) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun MacroDroidTile(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = if (isSelected) BorderStroke(2.dp, Color.White) else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Sélectionné",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MacroDroidCategoryHeader(
    title: String,
    icon: ImageVector,
    headerColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = headerColor,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = headerColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// -------------------------------------------------------------
// ÉTAPE 1 : DÉCLENCHEURS AVEC CATALOGUE EXACT MACRODROID
// -------------------------------------------------------------

@Composable
private fun EtapeTriggerGridContent(
    triggerActuel: Trigger,
    onTriggerClick: (TriggerPreset) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val allTriggers = remember {
        listOf(
            // Date / Heure
            TriggerPreset("time_of_day", "Heure de la Journée", Icons.Default.AccessTime, "Date / Heure", TriggerConfigType.HEURE_JOURNEE, Trigger.HeureFixe(heure = 12, minute = 0)),
            TriggerPreset("interval", "À intervalles réguliers", Icons.Default.Timelapse, "Date / Heure", TriggerConfigType.INTERVALLE, Trigger.Intervalle(minutes = 30)),
            TriggerPreset("calendar", "Événement du Calendrier", Icons.Default.CalendarMonth, "Date / Heure", null, Trigger.Manuel()),
            TriggerPreset("stopwatch", "Chronomètre", Icons.Default.Schedule, "Date / Heure", null, Trigger.Manuel()),

            // Batterie / Alimentation
            TriggerPreset("battery_level", "Niveau de la Batterie", Icons.Default.BatteryChargingFull, "Batterie / Alimentation", TriggerConfigType.NIVEAU_BATTERIE, Trigger.NiveauBatterie(seuil = 20, inferieur = true)),
            TriggerPreset("power_connected", "Alimentation Connectée/Déconnectée", Icons.Default.Power, "Batterie / Alimentation", TriggerConfigType.ALIMENTATION, Trigger.Alimentation(connectee = true)),
            TriggerPreset("battery_temp", "Température de la batterie", Icons.Default.Thermostat, "Batterie / Alimentation", null, Trigger.TemperatureBatterie(seuilCelsius = 40, superieur = true)),
            TriggerPreset("battery_saver", "État de l'économiseur de batterie", Icons.Default.BatteryAlert, "Batterie / Alimentation", null, Trigger.EconomiseurBatterie(actif = true)),

            // Connectivité
            TriggerPreset("wifi_state", "Changement d'état du Wi-Fi", Icons.Default.Wifi, "Connectivité", TriggerConfigType.WIFI, Trigger.WifiState(connecte = true)),
            TriggerPreset("bluetooth_event", "Événement Bluetooth", Icons.Default.Bluetooth, "Connectivité", TriggerConfigType.BLUETOOTH, Trigger.BluetoothState(connecte = true)),
            TriggerPreset("headset_plugged", "Casque Audio Branché/Débranché", Icons.Default.Headset, "Connectivité", TriggerConfigType.CASQUE, Trigger.CasqueAudio(branche = true)),
            TriggerPreset("hotspot_state", "Point d'Accès Activé/Désactivé", Icons.Default.WifiTethering, "Connectivité", null, Trigger.HotspotState(actif = true)),
            TriggerPreset("vpn_state", "Changement d'état du VPN", Icons.Default.VpnKey, "Connectivité", null, Trigger.VpnState(actif = true)),
            TriggerPreset("usb_connection", "Connexion USB", Icons.Default.Usb, "Connectivité", null, Trigger.UsbConnexion(connecte = true)),

            // Appel / SMS
            TriggerPreset("incoming_call", "Appel Entrant", Icons.Default.Phone, "Appel / SMS", TriggerConfigType.APPEL_ENTRANT, Trigger.AppelEntrant()),
            TriggerPreset("outgoing_call", "Appel Sortant", Icons.Default.Call, "Appel / SMS", null, Trigger.AppelSortant()),
            TriggerPreset("missed_call", "Appel Manqué", Icons.Default.CallMissed, "Appel / SMS", null, Trigger.AppelManque()),
            TriggerPreset("call_ended", "Appel Terminé", Icons.Default.CallEnd, "Appel / SMS", null, Trigger.AppelTermine()),
            TriggerPreset("sms_received", "SMS Reçu", Icons.Default.Sms, "Appel / SMS", TriggerConfigType.SMS_RECU, Trigger.SmsRecu()),
            TriggerPreset("sms_sent", "SMS Envoyé", Icons.Default.Send, "Appel / SMS", null, Trigger.SmsEnvoye()),

            // Capteurs
            TriggerPreset("shake_device", "Appareil secoué", Icons.Default.Vibration, "Capteurs", null, Trigger.Secousse(sensibilite = 12)),
            TriggerPreset("flip_device", "Appareil Retourné", Icons.Default.ScreenRotation, "Capteurs", null, Trigger.Secousse(sensibilite = 16)),
            TriggerPreset("light_sensor", "Capteur de Luminosité", Icons.Default.WbSunny, "Capteurs", null, Trigger.CapteurLuminosite(seuilLux = 10, inferieur = true)),
            TriggerPreset("proximity_sensor", "Capteur de Proximité", Icons.Default.Sensors, "Capteurs", null, Trigger.CapteurProximite(proche = true)),
            TriggerPreset("screen_orientation", "Orientation de l'écran", Icons.Default.ScreenRotation, "Capteurs", null, Trigger.OrientationEcran(portrait = true)),

            // Événements de l'appareil
            TriggerPreset("device_boot", "Démarrage de l'Appareil", Icons.Default.RestartAlt, "Événements de l'appareil", null, Trigger.DemarrageAppareil()),
            TriggerPreset("screen_state", "Écran Allumé/Éteint", Icons.Default.Smartphone, "Événements de l'appareil", TriggerConfigType.ECRAN, Trigger.EcranState(allume = true)),
            TriggerPreset("screen_unlocked", "Écran Déverrouillé", Icons.Default.LockOpen, "Événements de l'appareil", null, Trigger.EcranDeverrouille()),
            TriggerPreset("torch_state", "Torche allumée/éteinte", Icons.Default.FlashlightOn, "Événements de l'appareil", null, Trigger.TorcheState(allumee = true)),
            TriggerPreset("silent_mode", "Mode Silencieux Activé/Désactivé", Icons.Default.VolumeOff, "Événements de l'appareil", null, Trigger.ModeSilencieux(actif = true)),
            TriggerPreset("clipboard_changed", "Modification du Presse-Papier", Icons.Default.ContentPaste, "Événements de l'appareil", null, Trigger.PressePapierModifie()),

            // Applications & Saisie
            TriggerPreset("app_opened_closed", "Application Lancée/Fermée", Icons.Default.Apps, "Applications & Saisie", TriggerConfigType.APPLICATION, Trigger.AppState()),
            TriggerPreset("notification_received", "Notification", Icons.Default.Notifications, "Applications & Saisie", TriggerConfigType.NOTIFICATION, Trigger.NotificationRecue()),
            TriggerPreset("screen_content", "Contenu de l'écran", Icons.Default.FindInPage, "Applications & Saisie", TriggerConfigType.CONTENU_ECRAN, Trigger.ContenuEcran()),
            TriggerPreset("ui_click", "Clic sur l'interface utilisateur", Icons.Default.TouchApp, "Applications & Saisie", TriggerConfigType.CLIC_UI, Trigger.ClicUI()),
            TriggerPreset("variable_change", "Changement d'une Variable", Icons.Default.DataObject, "Applications & Saisie", TriggerConfigType.VARIABLE, Trigger.ChangementVariable("mode")),
            TriggerPreset("manual_trigger", "Déclencheur Manuel", Icons.Default.PlayArrow, "Applications & Saisie", null, Trigger.Manuel())
        )
    }

    val filtered = allTriggers.filter {
        searchQuery.isBlank() ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
    }

    val grouped = filtered.groupBy { it.category }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher un déclencheur...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MacroDroidRed) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Effacer", tint = CtrlColor.SlateSmoke)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CtrlColor.PureWhite,
                unfocusedContainerColor = CtrlColor.PureWhite,
                focusedBorderColor = MacroDroidRed,
                unfocusedBorderColor = CtrlColor.LinenBeige
            ),
            singleLine = true
        )

        // Déclencheur actuellement sélectionné
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
            border = BorderStroke(1.dp, MacroDroidRed.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MacroDroidRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Configuré : ${triggerActuel.label}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CtrlColor.WineInk
                    )
                    Text(
                        text = "Touchez une tuile pour choisir et personnaliser ses paramètres",
                        fontSize = 11.sp,
                        color = CtrlColor.SlateSmoke
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            grouped.forEach { (category, items) ->
                item {
                    val catIcon = when {
                        category.contains("Date") -> Icons.Default.AccessTime
                        category.contains("Batterie") -> Icons.Default.BatteryChargingFull
                        category.contains("Connect") -> Icons.Default.Wifi
                        category.contains("Appel") -> Icons.Default.Phone
                        category.contains("Capteur") -> Icons.Default.Sensors
                        category.contains("Événement") -> Icons.Default.Smartphone
                        else -> Icons.Default.Apps
                    }
                    MacroDroidCategoryHeader(title = category, icon = catIcon, headerColor = MacroDroidRedHeader)
                }

                items(items.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (preset in pair) {
                            MacroDroidTile(
                                title = preset.title,
                                icon = preset.icon,
                                containerColor = MacroDroidRed,
                                isSelected = (triggerActuel.id == preset.defaultTrigger.id),
                                onClick = { onTriggerClick(preset) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGUE DE CONFIGURATION DU DÉCLENCHEUR
// -------------------------------------------------------------

@Composable
private fun ConfigurerTriggerDialog(
    preset: TriggerPreset,
    onDismiss: () -> Unit,
    onConfirm: (Trigger) -> Unit
) {
    var heure by remember { mutableIntStateOf(12) }
    var minute by remember { mutableIntStateOf(0) }
    var intervalMinutes by remember { mutableIntStateOf(15) }
    var batteryLevel by remember { mutableFloatStateOf(20f) }
    var batteryInferieur by remember { mutableStateOf(true) }
    var powerConnected by remember { mutableStateOf(true) }
    var wifiConnected by remember { mutableStateOf(true) }
    var bluetoothConnected by remember { mutableStateOf(true) }
    var screenOn by remember { mutableStateOf(true) }
    var headsetPlugged by remember { mutableStateOf(true) }
    var appName by remember { mutableStateOf("YouTube") }
    var appOpened by remember { mutableStateOf(true) }
    var filterText by remember { mutableStateOf("") }
    var variableName by remember { mutableStateOf("mon_statut") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Configurer : ${preset.title}",
                color = MacroDroidRedHeader,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (preset.configType) {
                    TriggerConfigType.HEURE_JOURNEE -> {
                        Text("Choisissez l'heure de déclenchement :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NumberStepper(value = heure, onValueChange = { heure = it }, min = 0, max = 23, label = "Heure")
                            Text(" : ", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            NumberStepper(value = minute, onValueChange = { minute = it }, min = 0, max = 59, label = "Min")
                        }
                    }
                    TriggerConfigType.INTERVALLE -> {
                        Text("Intervalle d'exécution récurrente :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(5, 15, 30, 60).forEach { mins ->
                                FilterChip(
                                    selected = intervalMinutes == mins,
                                    onClick = { intervalMinutes = mins },
                                    label = { Text("${mins}m") }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = intervalMinutes.toString(),
                            onValueChange = { intervalMinutes = it.toIntOrNull() ?: 1 },
                            label = { Text("Minutes personnalisées") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TriggerConfigType.NIVEAU_BATTERIE -> {
                        Text("Seuil de batterie : ${batteryLevel.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = batteryLevel,
                            onValueChange = { batteryLevel = it },
                            valueRange = 1f..100f,
                            colors = SliderDefaults.colors(thumbColor = MacroDroidRed, activeTrackColor = MacroDroidRed)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = batteryInferieur,
                                onClick = { batteryInferieur = true },
                                label = { Text("Inférieur à (<)") }
                            )
                            FilterChip(
                                selected = !batteryInferieur,
                                onClick = { batteryInferieur = false },
                                label = { Text("Supérieur à (>)") }
                            )
                        }
                    }
                    TriggerConfigType.ALIMENTATION -> {
                        Text("État du chargeur :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = powerConnected,
                                onClick = { powerConnected = true },
                                label = { Text("Chargeur branché") }
                            )
                            FilterChip(
                                selected = !powerConnected,
                                onClick = { powerConnected = false },
                                label = { Text("Chargeur débranché") }
                            )
                        }
                    }
                    TriggerConfigType.WIFI -> {
                        Text("État du Wi-Fi :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = wifiConnected, onClick = { wifiConnected = true }, label = { Text("Wi-Fi Connecté") })
                            FilterChip(selected = !wifiConnected, onClick = { wifiConnected = false }, label = { Text("Wi-Fi Déconnecté") })
                        }
                        OutlinedTextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            label = { Text("SSID réseau (Optionnel)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TriggerConfigType.BLUETOOTH -> {
                        Text("État Bluetooth :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = bluetoothConnected, onClick = { bluetoothConnected = true }, label = { Text("Connecté") })
                            FilterChip(selected = !bluetoothConnected, onClick = { bluetoothConnected = false }, label = { Text("Déconnecté") })
                        }
                        OutlinedTextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            label = { Text("Nom appareil (Optionnel)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TriggerConfigType.APPEL_ENTRANT -> {
                        Text("Filtre d'appel entrant :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            label = { Text("Numéro de téléphone (Laisser vide pour tous)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TriggerConfigType.SMS_RECU -> {
                        Text("Filtre de message SMS :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            label = { Text("Mot-clé ou contact (Laisser vide pour tous)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TriggerConfigType.APPLICATION -> {
                        Text("Application cible :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = appOpened, onClick = { appOpened = true }, label = { Text("Lancée") })
                            FilterChip(selected = !appOpened, onClick = { appOpened = false }, label = { Text("Fermée") })
                        }
                        OutlinedTextField(
                            value = appName,
                            onValueChange = { appName = it },
                            label = { Text("Nom de l'application ou package") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TriggerConfigType.ECRAN -> {
                        Text("État de l'écran :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = screenOn, onClick = { screenOn = true }, label = { Text("Écran Allumé") })
                            FilterChip(selected = !screenOn, onClick = { screenOn = false }, label = { Text("Écran Verrouillé") })
                        }
                    }
                    TriggerConfigType.CASQUE -> {
                        Text("État du casque :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = headsetPlugged, onClick = { headsetPlugged = true }, label = { Text("Branché") })
                            FilterChip(selected = !headsetPlugged, onClick = { headsetPlugged = false }, label = { Text("Débranché") })
                        }
                    }
                    TriggerConfigType.VARIABLE -> {
                        Text("Variable à surveiller :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = variableName,
                            onValueChange = { variableName = it },
                            label = { Text("Nom de la variable") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TriggerConfigType.NOTIFICATION -> {
                        Text("Filtre notification :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            label = { Text("Mot-clé ou source (Optionnel)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TriggerConfigType.CONTENU_ECRAN -> {
                        Text("Motif à rechercher à l'écran :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            label = { Text("Texte ou expression régulière") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Ex : \"Solde insuffisant\" ou une regex comme \\\\d{4}€",
                            fontSize = 11.sp,
                            color = CtrlColor.SlateSmoke
                        )
                    }
                    TriggerConfigType.CLIC_UI -> {
                        Text("Élément à surveiller :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            label = { Text("Texte du bouton/élément à cliquer") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    null -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val configured: Trigger = when (preset.configType) {
                        TriggerConfigType.HEURE_JOURNEE -> Trigger.HeureFixe(heure = heure, minute = minute)
                        TriggerConfigType.INTERVALLE -> Trigger.Intervalle(minutes = intervalMinutes)
                        TriggerConfigType.NIVEAU_BATTERIE -> Trigger.NiveauBatterie(seuil = batteryLevel.toInt(), inferieur = batteryInferieur)
                        TriggerConfigType.ALIMENTATION -> Trigger.Alimentation(connectee = powerConnected)
                        TriggerConfigType.WIFI -> Trigger.WifiState(connecte = wifiConnected, ssid = filterText.ifBlank { null })
                        TriggerConfigType.BLUETOOTH -> Trigger.BluetoothState(connecte = bluetoothConnected, deviceName = filterText.ifBlank { null })
                        TriggerConfigType.APPEL_ENTRANT -> Trigger.AppelEntrant(numeroFiltre = filterText.ifBlank { null })
                        TriggerConfigType.SMS_RECU -> Trigger.SmsRecu(motCleFiltre = filterText.ifBlank { null })
                        TriggerConfigType.APPLICATION -> Trigger.AppState(appName = appName, estOuverte = appOpened)
                        TriggerConfigType.ECRAN -> Trigger.EcranState(allume = screenOn)
                        TriggerConfigType.CASQUE -> Trigger.CasqueAudio(branche = headsetPlugged)
                        TriggerConfigType.VARIABLE -> Trigger.ChangementVariable(nomVariable = variableName)
                        TriggerConfigType.NOTIFICATION -> Trigger.NotificationRecue(motCle = filterText.ifBlank { null })
                        TriggerConfigType.CONTENU_ECRAN -> Trigger.ContenuEcran(motifRegex = filterText)
                        TriggerConfigType.CLIC_UI -> Trigger.ClicUI(texteCible = filterText)
                        null -> preset.defaultTrigger
                    }
                    onConfirm(configured)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MacroDroidRed)
            ) {
                Text("Valider", color = Color.White)
            }
        },
        dismissButton = {
            CtrlGhostButton(text = "Annuler", onClick = onDismiss)
        },
        containerColor = CtrlColor.Parchment
    )
}

// -------------------------------------------------------------
// ÉTAPE 2 : CONDITIONS (VERT MACRODROID)
// -------------------------------------------------------------

@Composable
private fun EtapeConditionsContent(
    conditions: List<Condition>,
    onAjouterClick: () -> Unit,
    onSupprimerCondition: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Conditions d'exécution (Optionnel)",
            color = CtrlColor.WineInk,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "La macro vérifiera ces conditions avant d'exécuter ses actions.",
            color = CtrlColor.SlateSmoke,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onAjouterClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MacroDroidGreen,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("+ Ajouter une condition", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (conditions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucune condition requise.\nLa macro s'exécutera à chaque déclenchement.",
                    color = CtrlColor.SlateSmoke,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(conditions) { index, c ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
                        border = BorderStroke(1.dp, CtrlColor.LinenBeige)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MacroDroidGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MacroDroidGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = c.label,
                                    color = CtrlColor.WineInk,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            IconButton(
                                onClick = { onSupprimerCondition(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Supprimer",
                                    tint = CtrlColor.AlertRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogueConditionsDialog(
    onDismiss: () -> Unit,
    onConditionSelect: (ConditionPreset) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val allConditions = remember {
        listOf(
            // Date / Heure
            ConditionPreset("time_range", "Heure de la Journée", Icons.Default.AccessTime, "Date / Heure", ConditionConfigType.PLAGE_HORAIRE, Condition.PlageHoraire(22, 0, 7, 0)),
            ConditionPreset("day_of_week", "Jour de la Semaine", Icons.Default.CalendarToday, "Date / Heure", ConditionConfigType.JOURS_SEMAINE, Condition.JoursSemaine()),
            ConditionPreset("day_of_month", "Jour du Mois", Icons.Default.CalendarMonth, "Date / Heure", null, Condition.JourDuMois(jours = listOf(1))),

            // Batterie / Alimentation
            ConditionPreset("battery_power", "Alimentation Externe Connectée", Icons.Default.Power, "Batterie / Alimentation", ConditionConfigType.ALIMENTATION, Condition.EnCharge(true)),
            ConditionPreset("battery_level", "Niveau de la Batterie", Icons.Default.BatteryChargingFull, "Batterie / Alimentation", ConditionConfigType.NIVEAU_BATTERIE, Condition.NiveauBatterie(50, true)),
            ConditionPreset("battery_saver", "État de l'économiseur de batterie", Icons.Default.BatteryAlert, "Batterie / Alimentation", null, Condition.EconomiseurBatterieActif(true)),

            // Connectivité
            ConditionPreset("wifi_state", "État du Wi-Fi", Icons.Default.Wifi, "Connectivité", ConditionConfigType.WIFI, Condition.WifiConnecte(true)),
            ConditionPreset("bluetooth_state", "État du Bluetooth", Icons.Default.Bluetooth, "Connectivité", ConditionConfigType.BLUETOOTH, Condition.BluetoothActif(true)),
            ConditionPreset("headset_state", "Connexion Casque Audio", Icons.Default.Headset, "Connectivité", ConditionConfigType.CASQUE, Condition.CasqueBranche(true)),
            ConditionPreset("vpn_state", "État du VPN", Icons.Default.VpnKey, "Connectivité", null, Condition.VpnActif(true)),

            // Écran & Appareil
            ConditionPreset("screen_state", "Écran Allumé/Éteint", Icons.Default.Smartphone, "Écran & Appareil", ConditionConfigType.ECRAN, Condition.EcranAllume(true)),
            ConditionPreset("device_locked", "Appareil Verrouillé/Déverrouillé", Icons.Default.ScreenLockPortrait, "Écran & Appareil", null, Condition.AppareilVerrouille(true)),
            ConditionPreset("app_running", "Application en Cours d'Exécution", Icons.Default.Apps, "Écran & Appareil", ConditionConfigType.APPLICATION, Condition.AppAuPremierPlan()),
            ConditionPreset("accessibility_active", "État du service d'accessibilité", Icons.Default.Accessibility, "Écran & Appareil", null, Condition.ServiceAccessibiliteActif(true)),

            // Variables & Logique
            ConditionPreset("variable_val", "Variable MacroDroid", Icons.Default.DataObject, "Variables & Logique", ConditionConfigType.VARIABLE, Condition.VariableValeur("mode", "actif")),
            ConditionPreset("variable_compare", "Comparer des valeurs", Icons.Default.Rule, "Variables & Logique", null, Condition.VariableComparaison("mode", OperateurComparaison.EGAL, "actif")),
            ConditionPreset("macro_not_running", "Macro pas déjà en cours (anti-doublon)", Icons.Default.Block, "Variables & Logique", null, Condition.MacroEnCoursExecution(macroId = "", nomMacro = "cette macro", doitEtreEnCours = false))
        )
    }

    val filtered = allConditions.filter {
        searchQuery.isBlank() ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
    }
    val grouped = filtered.groupBy { it.category }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ajouter une Condition",
                color = MacroDroidGreenHeader,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher une condition...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MacroDroidGreen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CtrlColor.PureWhite,
                        unfocusedContainerColor = CtrlColor.PureWhite,
                        focusedBorderColor = MacroDroidGreen,
                        unfocusedBorderColor = CtrlColor.LinenBeige
                    ),
                    singleLine = true
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    grouped.forEach { (category, items) ->
                        item {
                            MacroDroidCategoryHeader(title = category, icon = Icons.Default.Check, headerColor = MacroDroidGreenHeader)
                        }

                        items(items.chunked(2)) { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (preset in pair) {
                                    MacroDroidTile(
                                        title = preset.title,
                                        icon = preset.icon,
                                        containerColor = MacroDroidGreen,
                                        onClick = { onConditionSelect(preset) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            CtrlGhostButton(text = "Fermer", onClick = onDismiss)
        },
        containerColor = CtrlColor.Parchment
    )
}

@Composable
private fun ConfigurerConditionDialog(
    preset: ConditionPreset,
    onDismiss: () -> Unit,
    onConfirm: (Condition) -> Unit
) {
    var hDebut by remember { mutableIntStateOf(22) }
    var mDebut by remember { mutableIntStateOf(0) }
    var hFin by remember { mutableIntStateOf(7) }
    var mFin by remember { mutableIntStateOf(0) }
    var batteryThreshold by remember { mutableFloatStateOf(50f) }
    var batteryGreater by remember { mutableStateOf(true) }
    var inCharge by remember { mutableStateOf(true) }
    var wifiReq by remember { mutableStateOf(true) }
    var bluetoothReq by remember { mutableStateOf(true) }
    var screenOnReq by remember { mutableStateOf(true) }
    var headsetReq by remember { mutableStateOf(true) }
    var appTarget by remember { mutableStateOf("YouTube") }
    var varKey by remember { mutableStateOf("mode") }
    var varExpected by remember { mutableStateOf("actif") }
    var selectedDays by remember { mutableStateOf(listOf(1, 2, 3, 4, 5)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Paramétrer : ${preset.title}",
                color = MacroDroidGreenHeader,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (preset.configType) {
                    ConditionConfigType.PLAGE_HORAIRE -> {
                        Text("Plage horaire autorisée :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Text("Heure de Début :", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NumberStepper(value = hDebut, onValueChange = { hDebut = it }, min = 0, max = 23, label = "Heure")
                            Text(" : ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            NumberStepper(value = mDebut, onValueChange = { mDebut = it }, min = 0, max = 59, label = "Min")
                        }
                        Text("Heure de Fin :", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NumberStepper(value = hFin, onValueChange = { hFin = it }, min = 0, max = 23, label = "Heure")
                            Text(" : ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            NumberStepper(value = mFin, onValueChange = { mFin = it }, min = 0, max = 59, label = "Min")
                        }
                    }
                    ConditionConfigType.JOURS_SEMAINE -> {
                        Text("Sélectionnez les jours :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        val dayNames = listOf("Lun" to 1, "Mar" to 2, "Mer" to 3, "Jeu" to 4, "Ven" to 5, "Sam" to 6, "Dim" to 7)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                            dayNames.forEach { (name, id) ->
                                val sel = selectedDays.contains(id)
                                FilterChip(
                                    selected = sel,
                                    onClick = {
                                        selectedDays = if (sel) selectedDays - id else selectedDays + id
                                    },
                                    label = { Text(name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    ConditionConfigType.NIVEAU_BATTERIE -> {
                        Text("Niveau de batterie requis : ${batteryThreshold.toInt()}%", fontSize = 13.sp)
                        Slider(
                            value = batteryThreshold,
                            onValueChange = { batteryThreshold = it },
                            valueRange = 1f..100f,
                            colors = SliderDefaults.colors(thumbColor = MacroDroidGreen, activeTrackColor = MacroDroidGreen)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = batteryGreater, onClick = { batteryGreater = true }, label = { Text("Supérieur ou égal (>=)") })
                            FilterChip(selected = !batteryGreater, onClick = { batteryGreater = false }, label = { Text("Inférieur ou égal (<=)") })
                        }
                    }
                    ConditionConfigType.ALIMENTATION -> {
                        Text("État d'alimentation requis :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = inCharge, onClick = { inCharge = true }, label = { Text("Sur Chargeur") })
                            FilterChip(selected = !inCharge, onClick = { inCharge = false }, label = { Text("Sur Batterie") })
                        }
                    }
                    ConditionConfigType.WIFI -> {
                        Text("Connexion Wi-Fi requise :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = wifiReq, onClick = { wifiReq = true }, label = { Text("Wi-Fi Connecté") })
                            FilterChip(selected = !wifiReq, onClick = { wifiReq = false }, label = { Text("Wi-Fi Déconnecté") })
                        }
                    }
                    ConditionConfigType.BLUETOOTH -> {
                        Text("Bluetooth requis :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = bluetoothReq, onClick = { bluetoothReq = true }, label = { Text("Bluetooth Actif") })
                            FilterChip(selected = !bluetoothReq, onClick = { bluetoothReq = false }, label = { Text("Bluetooth Inactif") })
                        }
                    }
                    ConditionConfigType.ECRAN -> {
                        Text("État de l'écran requis :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = screenOnReq, onClick = { screenOnReq = true }, label = { Text("Écran Allumé") })
                            FilterChip(selected = !screenOnReq, onClick = { screenOnReq = false }, label = { Text("Écran Éteint") })
                        }
                    }
                    ConditionConfigType.CASQUE -> {
                        Text("Casque audio requis :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = headsetReq, onClick = { headsetReq = true }, label = { Text("Casque Branché") })
                            FilterChip(selected = !headsetReq, onClick = { headsetReq = false }, label = { Text("Casque Débranché") })
                        }
                    }
                    ConditionConfigType.APPLICATION -> {
                        Text("Application requise au premier plan :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = appTarget,
                            onValueChange = { appTarget = it },
                            label = { Text("Nom ou Package de l'application") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ConditionConfigType.VARIABLE -> {
                        Text("Variable et valeur requise :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = varKey,
                            onValueChange = { varKey = it },
                            label = { Text("Nom de la variable") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = varExpected,
                            onValueChange = { varExpected = it },
                            label = { Text("Valeur attendue") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    null -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cond: Condition = when (preset.configType) {
                        ConditionConfigType.PLAGE_HORAIRE -> Condition.PlageHoraire(hDebut, mDebut, hFin, mFin)
                        ConditionConfigType.JOURS_SEMAINE -> Condition.JoursSemaine(selectedDays)
                        ConditionConfigType.NIVEAU_BATTERIE -> Condition.NiveauBatterie(batteryThreshold.toInt(), batteryGreater)
                        ConditionConfigType.ALIMENTATION -> Condition.EnCharge(inCharge)
                        ConditionConfigType.WIFI -> Condition.WifiConnecte(wifiReq)
                        ConditionConfigType.BLUETOOTH -> Condition.BluetoothActif(bluetoothReq)
                        ConditionConfigType.ECRAN -> Condition.EcranAllume(screenOnReq)
                        ConditionConfigType.CASQUE -> Condition.CasqueBranche(headsetReq)
                        ConditionConfigType.APPLICATION -> Condition.AppAuPremierPlan(nomApp = appTarget)
                        ConditionConfigType.VARIABLE -> Condition.VariableValeur(varKey, varExpected)
                        null -> preset.defaultCondition
                    }
                    onConfirm(cond)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MacroDroidGreen)
            ) {
                Text("Valider", color = Color.White)
            }
        },
        dismissButton = {
            CtrlGhostButton(text = "Annuler", onClick = onDismiss)
        },
        containerColor = CtrlColor.Parchment
    )
}

// -------------------------------------------------------------
// ÉTAPE 3 : ACTIONS (BLEU MACRODROID)
// -------------------------------------------------------------

@Composable
private fun EtapeActionsEtNomContent(
    nom: String,
    description: String,
    actions: List<ActionMacro>,
    errorMessage: String?,
    onNomChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onAjouterActionClick: () -> Unit,
    onSupprimerAction: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Nom & Actions",
            color = CtrlColor.WineInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = nom,
            onValueChange = onNomChanged,
            label = { Text("Nom de la macro (requis)") },
            placeholder = { Text("Ex: Mode Sommeil Automatique") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null,
            shape = CardShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CtrlColor.PureWhite,
                unfocusedContainerColor = CtrlColor.PureWhite,
                focusedBorderColor = CtrlColor.WineInk,
                unfocusedBorderColor = CtrlColor.LinenBeige
            ),
            singleLine = true
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = CtrlColor.AlertRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            label = { Text("Description (optionnel)") },
            placeholder = { Text("Ex: Éteint le Wi-Fi et baisse le volume") },
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CtrlColor.PureWhite,
                unfocusedContainerColor = CtrlColor.PureWhite,
                focusedBorderColor = CtrlColor.WineInk,
                unfocusedBorderColor = CtrlColor.LinenBeige
            ),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Actions à exécuter (${actions.size})",
                color = CtrlColor.WineInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = onAjouterActionClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MacroDroidBlue,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter", color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        actions.forEachIndexed { index, a ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
                border = BorderStroke(1.dp, CtrlColor.LinenBeige)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${index + 1}. ${a.label}",
                            color = CtrlColor.WineInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = a.categorie.titre,
                            color = MacroDroidBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (actions.size > 1) {
                        IconButton(
                            onClick = { onSupprimerAction(index) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Supprimer",
                                tint = CtrlColor.AlertRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
private fun CatalogueActionsDialog(
    onDismiss: () -> Unit,
    onActionSelect: (ActionPreset) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val allActions = remember {
        listOf(
            // Actions de l'appareil
            ActionPreset("torch", "Allumer/Éteindre la Lampe Torche", Icons.Default.FlashlightOn, "Actions de l'appareil", ActionConfigType.LAMPE_TORCHE, ActionMacro.LampeTorche(true)),
            ActionPreset("vibrate", "Vibration", Icons.Default.Vibration, "Actions de l'appareil", ActionConfigType.VIBRATION, ActionMacro.Vibrer(300)),
            ActionPreset("tts", "Texte par Synthèse Vocale", Icons.Default.RecordVoiceOver, "Actions de l'appareil", ActionConfigType.TTS, ActionMacro.TexteParSyntheseVocale("Action Ctrl exécutée")),
            ActionPreset("back_btn", "Appuyer sur le Bouton Retour", Icons.AutoMirrored.Filled.ArrowBack, "Actions de l'appareil", null, ActionMacro.ActionUI(TypeGesteUI.RETOUR)),
            ActionPreset("home_btn", "Lancer l'Écran d'Accueil", Icons.Default.Home, "Actions de l'appareil", null, ActionMacro.ActionUI(TypeGesteUI.ACCUEIL)),
            ActionPreset("ui_interaction", "Interaction avec l'Interface", Icons.Default.TouchApp, "Actions de l'appareil", ActionConfigType.INTERFACE_UI, ActionMacro.ActionUI(TypeGesteUI.CLIC, TypeSelecteur.TEXTE, "Valider")),

            // Volume & Audio
            ActionPreset("volume_change", "Changer le Volume", Icons.AutoMirrored.Filled.VolumeUp, "Volume & Audio", ActionConfigType.VOLUME, ActionMacro.ChangerVolume(50, "Média")),
            ActionPreset("silent_mode", "Silencieux - Vibreur désactivé", Icons.Default.VolumeOff, "Volume & Audio", null, ActionMacro.ChangerVolume(0, "Média")),
            ActionPreset("play_sound", "Jouer/Arrêter un Son", Icons.Default.MusicNote, "Volume & Audio", null, ActionMacro.JouerSon("Bip confirmation")),

            // Notifications & Messagerie
            ActionPreset("notification", "Afficher une Notification", Icons.Default.Notifications, "Notifications & Messagerie", ActionConfigType.NOTIFICATION, ActionMacro.EnvoyerNotification("Ctrl", "Macro exécutée")),
            ActionPreset("toast", "Message Flottant", Icons.Default.Feedback, "Notifications & Messagerie", ActionConfigType.TOAST, ActionMacro.AfficherToast("Macro exécutée avec succès")),
            ActionPreset("sms", "Envoyer un SMS", Icons.Default.Sms, "Notifications & Messagerie", ActionConfigType.SMS, ActionMacro.EnvoyerSms("0600000000", "Alerte Ctrl")),

            // Applications & Web
            ActionPreset("open_app", "Lancer une Application", Icons.Default.Apps, "Applications & Web", ActionConfigType.APPLICATION, ActionMacro.OuvrirApp("com.google.android.youtube", "YouTube")),
            ActionPreset("open_url", "Ouvrir un Site Web", Icons.Default.Language, "Applications & Web", ActionConfigType.URL, ActionMacro.OuvrirUrl("https://google.com")),
            ActionPreset("call", "Passer un Appel", Icons.Default.Call, "Applications & Web", null, ActionMacro.PasserAppel("0600000000")),
            ActionPreset("open_call_log", "Ouvrir le Journal d'Appels", Icons.Default.History, "Applications & Web", null, ActionMacro.OuvrirJournalAppels()),
            ActionPreset("share_text", "Partager du Texte", Icons.Default.Share, "Applications & Web", null, ActionMacro.PartagerTexte("Partagé depuis Ctrl")),
            ActionPreset("clipboard_fill", "Remplir le Presse-Papier", Icons.Default.ContentPaste, "Applications & Web", null, ActionMacro.RemplirPressePapier("")),
            ActionPreset("send_intent", "Envoyer un Intent", Icons.Default.Send, "Applications & Web", null, ActionMacro.EnvoyerIntent(action = "android.intent.action.VIEW")),
            ActionPreset("http_request", "Requête HTTP", Icons.Default.Http, "Applications & Web", null, ActionMacro.RequeteHttp(url = "https://", methode = MethodeHttp.GET)),
            ActionPreset("json_parse", "Analyse JSON", Icons.Default.DataObject, "Applications & Web", null, ActionMacro.AnalyseJson()),

            // Connectivité
            ActionPreset("wifi_toggle", "Configurer le Wi-Fi", Icons.Default.Wifi, "Connectivité", ActionConfigType.WIFI, ActionMacro.BasculerWifi(true)),
            ActionPreset("bluetooth_toggle", "Configurer le Bluetooth", Icons.Default.Bluetooth, "Connectivité", ActionConfigType.BLUETOOTH, ActionMacro.BasculerBluetooth(true)),

            // Contrôle de Flux
            ActionPreset("wait", "Attendre avant la Prochaine Action", Icons.Default.Timer, "Contrôle de Flux", ActionConfigType.ATTENDRE, ActionMacro.Attendre(2)),
            ActionPreset("emergency_stop", "Arrêt d'Urgence", Icons.Default.Cancel, "Contrôle de Flux", null, ActionMacro.ArretUrgence("Arrêt demandé")),

            // Notifications (complément)
            ActionPreset("clear_notifs", "Effacer les Notifications", Icons.Default.ClearAll, "Notifications & Messagerie", null, ActionMacro.EffacerNotifications()),

            // Variables & IA
            ActionPreset("set_var", "Définir une variable", Icons.Default.DataObject, "Variables & IA", ActionConfigType.VARIABLE, ActionMacro.DefinirVariable("statut", "actif")),
            ActionPreset("manip_liste", "Manipulation des Tableaux", Icons.Default.List, "Variables & IA", null, ActionMacro.ManipulerListe()),
            ActionPreset("verif_texte_ecran", "Vérifier le Texte à l'Écran (OCR)", Icons.Default.FindInPage, "Variables & IA", ActionConfigType.VERIF_TEXTE_ECRAN, ActionMacro.VerifierTexteEcran()),
            ActionPreset("ai_query", "AI LLM Query (Gemini)", Icons.Default.Psychology, "Variables & IA", ActionConfigType.IA_GEMINI, ActionMacro.AppelIA("Résumer les notifications récentes"))
        )
    }

    val filtered = allActions.filter {
        searchQuery.isBlank() ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
    }
    val grouped = filtered.groupBy { it.category }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ajouter une Action",
                color = MacroDroidBlueHeader,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher une action...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MacroDroidBlue) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CtrlColor.PureWhite,
                        unfocusedContainerColor = CtrlColor.PureWhite,
                        focusedBorderColor = MacroDroidBlue,
                        unfocusedBorderColor = CtrlColor.LinenBeige
                    ),
                    singleLine = true
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    grouped.forEach { (category, items) ->
                        item {
                            MacroDroidCategoryHeader(title = category, icon = Icons.Default.PlayArrow, headerColor = MacroDroidBlueHeader)
                        }

                        items(items.chunked(2)) { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (preset in pair) {
                                    MacroDroidTile(
                                        title = preset.title,
                                        icon = preset.icon,
                                        containerColor = MacroDroidBlue,
                                        onClick = { onActionSelect(preset) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            CtrlGhostButton(text = "Fermer", onClick = onDismiss)
        },
        containerColor = CtrlColor.Parchment
    )
}

@Composable
private fun ConfigurerActionDialog(
    preset: ActionPreset,
    onDismiss: () -> Unit,
    onConfirm: (ActionMacro) -> Unit
) {
    var torchOn by remember { mutableStateOf(true) }
    var volumePct by remember { mutableFloatStateOf(50f) }
    var volumeChannel by remember { mutableStateOf("Média") }
    var vibrationMs by remember { mutableIntStateOf(300) }
    var ttsText by remember { mutableStateOf("Action exécutée") }
    var notifTitle by remember { mutableStateOf("Ctrl - Alerte") }
    var notifMsg by remember { mutableStateOf("Macro exécutée avec succès") }
    var toastMsg by remember { mutableStateOf("Opération réussie") }
    var smsPhone by remember { mutableStateOf("0600000000") }
    var smsText by remember { mutableStateOf("Message automatique") }
    var appName by remember { mutableStateOf("YouTube") }
    var webUrl by remember { mutableStateOf("https://google.com") }
    var wifiEnable by remember { mutableStateOf(true) }
    var bluetoothEnable by remember { mutableStateOf(true) }
    var waitSecs by remember { mutableIntStateOf(2) }
    var uiTargetText by remember { mutableStateOf("Valider") }
    var varName by remember { mutableStateOf("mode") }
    var varVal by remember { mutableStateOf("actif") }
    var aiPrompt by remember { mutableStateOf("Résumer les alertes récentes") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Paramétrer : ${preset.title}",
                color = MacroDroidBlueHeader,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (preset.configType) {
                    ActionConfigType.LAMPE_TORCHE -> {
                        Text("Action sur la lampe torche :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = torchOn, onClick = { torchOn = true }, label = { Text("Allumer") })
                            FilterChip(selected = !torchOn, onClick = { torchOn = false }, label = { Text("Éteindre") })
                        }
                    }
                    ActionConfigType.VOLUME -> {
                        Text("Canal audio :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Média", "Sonnerie", "Notif", "Alarme").forEach { ch ->
                                FilterChip(
                                    selected = volumeChannel == ch,
                                    onClick = { volumeChannel = ch },
                                    label = { Text(ch, fontSize = 11.sp) }
                                )
                            }
                        }
                        Text("Niveau du volume : ${volumePct.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = volumePct,
                            onValueChange = { volumePct = it },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = MacroDroidBlue, activeTrackColor = MacroDroidBlue)
                        )
                    }
                    ActionConfigType.VIBRATION -> {
                        Text("Durée de la vibration : ${vibrationMs} ms", fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(150 to "Courte", 400 to "Moyenne", 1000 to "Longue").forEach { (ms, lbl) ->
                                FilterChip(
                                    selected = vibrationMs == ms,
                                    onClick = { vibrationMs = ms },
                                    label = { Text("$lbl ($ms ms)", fontSize = 11.sp) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = vibrationMs.toString(),
                            onValueChange = { vibrationMs = it.toIntOrNull() ?: 100 },
                            label = { Text("Durée personnalisée (ms)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ActionConfigType.TTS -> {
                        Text("Texte à lire par synthèse vocale :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = ttsText,
                            onValueChange = { ttsText = it },
                            label = { Text("Texte à prononcer") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ActionConfigType.NOTIFICATION -> {
                        Text("Contenu de la notification :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = notifTitle,
                            onValueChange = { notifTitle = it },
                            label = { Text("Titre de la notification") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = notifMsg,
                            onValueChange = { notifMsg = it },
                            label = { Text("Message de la notification") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ActionConfigType.TOAST -> {
                        Text("Message flottant (Toast) :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = toastMsg,
                            onValueChange = { toastMsg = it },
                            label = { Text("Message à afficher") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ActionConfigType.SMS -> {
                        Text("Envoi d'un SMS :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = smsPhone,
                            onValueChange = { smsPhone = it },
                            label = { Text("Numéro du destinataire") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = smsText,
                            onValueChange = { smsText = it },
                            label = { Text("Message du SMS") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ActionConfigType.APPLICATION -> {
                        Text("Application à lancer :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = appName,
                            onValueChange = { appName = it },
                            label = { Text("Nom ou package de l'app (ex: YouTube, Maps)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ActionConfigType.URL -> {
                        Text("URL du site web à ouvrir :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = webUrl,
                            onValueChange = { webUrl = it },
                            label = { Text("Adresse Web (https://...)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ActionConfigType.WIFI -> {
                        Text("Configuration du Wi-Fi :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = wifiEnable, onClick = { wifiEnable = true }, label = { Text("Activer le Wi-Fi") })
                            FilterChip(selected = !wifiEnable, onClick = { wifiEnable = false }, label = { Text("Désactiver le Wi-Fi") })
                        }
                    }
                    ActionConfigType.BLUETOOTH -> {
                        Text("Configuration du Bluetooth :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = bluetoothEnable, onClick = { bluetoothEnable = true }, label = { Text("Activer Bluetooth") })
                            FilterChip(selected = !bluetoothEnable, onClick = { bluetoothEnable = false }, label = { Text("Désactiver Bluetooth") })
                        }
                    }
                    ActionConfigType.ATTENDRE -> {
                        Text("Délai d'attente :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 2, 5, 10).forEach { s ->
                                FilterChip(
                                    selected = waitSecs == s,
                                    onClick = { waitSecs = s },
                                    label = { Text("${s}s") }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = waitSecs.toString(),
                            onValueChange = { waitSecs = it.toIntOrNull() ?: 1 },
                            label = { Text("Secondes personnalisées") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ActionConfigType.INTERFACE_UI -> {
                        Text("Interaction avec l'écran (Accessibilité) :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = uiTargetText,
                            onValueChange = { uiTargetText = it },
                            label = { Text("Texte du bouton sur lequel cliquer") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ActionConfigType.VARIABLE -> {
                        Text("Définir une variable globale :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = varName,
                            onValueChange = { varName = it },
                            label = { Text("Nom de la variable") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = varVal,
                            onValueChange = { varVal = it },
                            label = { Text("Valeur à assigner") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ActionConfigType.IA_GEMINI -> {
                        Text("Requête IA générative (Gemini) :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            label = { Text("Prompt pour l'IA") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                    ActionConfigType.VERIF_TEXTE_ECRAN -> {
                        Text("Vérifier un texte à l'écran :", fontSize = 13.sp, color = CtrlColor.SlateSmoke)
                        OutlinedTextField(
                            value = uiTargetText,
                            onValueChange = { uiTargetText = it },
                            label = { Text("Texte ou expression régulière recherchée") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Le résultat (trouvé/non trouvé + texte correspondant) est stocké dans des variables, utilisables ensuite dans une 'Clause Si'.",
                            fontSize = 11.sp,
                            color = CtrlColor.SlateSmoke
                        )
                    }
                    null -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val act: ActionMacro = when (preset.configType) {
                        ActionConfigType.LAMPE_TORCHE -> ActionMacro.LampeTorche(torchOn)
                        ActionConfigType.VOLUME -> ActionMacro.ChangerVolume(volumePct.toInt(), volumeChannel)
                        ActionConfigType.VIBRATION -> ActionMacro.Vibrer(vibrationMs.toLong())
                        ActionConfigType.TTS -> ActionMacro.TexteParSyntheseVocale(ttsText)
                        ActionConfigType.NOTIFICATION -> ActionMacro.EnvoyerNotification(notifTitle, notifMsg)
                        ActionConfigType.TOAST -> ActionMacro.AfficherToast(toastMsg)
                        ActionConfigType.SMS -> ActionMacro.EnvoyerSms(smsPhone, smsText)
                        ActionConfigType.APPLICATION -> ActionMacro.OuvrirApp(nomApp = appName)
                        ActionConfigType.URL -> ActionMacro.OuvrirUrl(webUrl)
                        ActionConfigType.WIFI -> ActionMacro.BasculerWifi(wifiEnable)
                        ActionConfigType.BLUETOOTH -> ActionMacro.BasculerBluetooth(bluetoothEnable)
                        ActionConfigType.ATTENDRE -> ActionMacro.Attendre(waitSecs)
                        ActionConfigType.INTERFACE_UI -> ActionMacro.ActionUI(TypeGesteUI.CLIC, TypeSelecteur.TEXTE, uiTargetText)
                        ActionConfigType.VARIABLE -> ActionMacro.DefinirVariable(varName, varVal)
                        ActionConfigType.IA_GEMINI -> ActionMacro.AppelIA(aiPrompt)
                        ActionConfigType.VERIF_TEXTE_ECRAN -> ActionMacro.VerifierTexteEcran(motifRegex = uiTargetText)
                        null -> preset.defaultAction
                    }
                    onConfirm(act)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MacroDroidBlue)
            ) {
                Text("Valider", color = Color.White)
            }
        },
        dismissButton = {
            CtrlGhostButton(text = "Annuler", onClick = onDismiss)
        },
        containerColor = CtrlColor.Parchment
    )
}

@Composable
private fun NumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = CtrlColor.SlateSmoke)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (value > min) onValueChange(value - 1) else onValueChange(max) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("–", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CtrlColor.WineInk)
            }
            Text(
                text = String.format("%02d", value),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CtrlColor.WineInk,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = { if (value < max) onValueChange(value + 1) else onValueChange(min) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CtrlColor.WineInk)
            }
        }
    }
}
