package com.example.presentation.settings

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.accessibility.CtrlAccessibilityService
import com.example.domain.model.TypeVariable
import com.example.presentation.theme.CardShape
import com.example.presentation.theme.CtrlColor
import com.example.presentation.theme.PillShape
import com.example.presentation.theme.components.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddVarDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var jsonImportText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.actualiserStatutAccessibilite()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CtrlColor.Parchment)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Réglages & Système",
            color = CtrlColor.WineInk,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.02).sp
        )
        Text(
            text = "Gestion des autorisations Android, variables globales et sauvegardes.",
            color = CtrlColor.SlateSmoke,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback toast / alert
        uiState.feedbackMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = CtrlColor.ButterCream)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = msg, color = CtrlColor.WineInk, fontSize = 13.sp)
                    IconButton(onClick = viewModel::clearFeedback, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Fermer", tint = CtrlColor.WineInk)
                    }
                }
            }
        }

        // Section 1 : Service d'accessibilité
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Service d'accessibilité Ctrl",
                            color = CtrlColor.WineInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (uiState.isAccessibilityEnabled) "Actif et opérationnel" else "Désactivé dans Android",
                            color = if (uiState.isAccessibilityEnabled) CtrlColor.WineInk else CtrlColor.AlertRed,
                            fontSize = 12.sp
                        )
                    }

                    CtrlStatusBadge(active = uiState.isAccessibilityEnabled)
                }

                Spacer(modifier = Modifier.height(12.dp))

                CtrlOutlinedButton(
                    text = "Ouvrir les réglages d'accessibilité",
                    onClick = {
                        CtrlAccessibilityService.openAccessibilitySettings(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2 : Permissions Android à l'exécution
        var permissionsManquantes by remember { mutableStateOf(PermissionsHelper.manquantes(context)) }
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {
            permissionsManquantes = PermissionsHelper.manquantes(context)
        }
        LaunchedEffect(Unit) {
            permissionsManquantes = PermissionsHelper.manquantes(context)
        }

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Permissions Android", color = CtrlColor.WineInk, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (permissionsManquantes.isEmpty()) "Toutes accordées" else "${permissionsManquantes.size} permission(s) manquante(s)",
                            color = if (permissionsManquantes.isEmpty()) CtrlColor.WineInk else CtrlColor.AlertRed,
                            fontSize = 12.sp
                        )
                    }
                    CtrlStatusBadge(active = permissionsManquantes.isEmpty())
                }

                if (permissionsManquantes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    permissionsManquantes.forEach { p ->
                        Text(
                            text = "• ${p.label} — ${p.raison}",
                            color = CtrlColor.SlateSmoke,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    CtrlPrimaryButton(
                        text = "Activer les permissions manquantes",
                        onClick = { permissionLauncher.launch(permissionsManquantes.map { it.permission }.toTypedArray()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3 : Batterie — exclusion de l'optimisation (fiabilité arrière-plan)
        var batterieOptimisee by remember {
            mutableStateOf(
                !(context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager)
                    .isIgnoringBatteryOptimizations(context.packageName)
            )
        }
        LaunchedEffect(Unit) {
            batterieOptimisee = !(context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager)
                .isIgnoringBatteryOptimizations(context.packageName)
        }

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Optimisation de batterie", color = CtrlColor.WineInk, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (batterieOptimisee) "Android peut tuer Ctrl en arrière-plan" else "Ctrl est exclu (fiabilité maximale)",
                            color = if (batterieOptimisee) CtrlColor.AlertRed else CtrlColor.WineInk,
                            fontSize = 12.sp
                        )
                    }
                    CtrlStatusBadge(active = !batterieOptimisee)
                }
                if (batterieOptimisee) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CtrlOutlinedButton(
                        text = "Désactiver l'optimisation pour Ctrl",
                        onClick = {
                            try {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    if (!am.canScheduleExactAlarms()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠ Alarmes exactes non autorisées : les macros \"Heure Fixe\" peuvent avoir du retard.",
                            color = CtrlColor.AlertRed,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CtrlOutlinedButton(
                            text = "Autoriser les alarmes exactes",
                            onClick = {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section IA : Clé API Gemini
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
            border = BorderStroke(1.dp, CtrlColor.LinenBeige)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                        text = "Intelligence Artificielle (Gemini)",
                        color = CtrlColor.WineInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Permet aux actions 'Requête IA Gemini' et au Générateur IA de fonctionner.",
                    color = CtrlColor.SlateSmoke,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Badge statut clé injectée via Secrets
                if (uiState.isSecretKeyConfigured) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = PillShape,
                        colors = CardDefaults.cardColors(containerColor = CtrlColor.LemonWhisper),
                        border = BorderStroke(0.5.dp, CtrlColor.CitrinePop)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = CtrlColor.WineInk,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Clé secrète AI Studio active dans l'environnement",
                                color = CtrlColor.WineInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                var inputKey by remember(uiState.geminiApiKey) { mutableStateOf(uiState.geminiApiKey) }

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    label = { Text("Clé API Gemini (AI Studio)") },
                    placeholder = { Text("Collez votre clé API AIza...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CtrlColor.PureWhite,
                        unfocusedContainerColor = CtrlColor.PureWhite
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    CtrlPrimaryButton(
                        text = "Enregistrer la clé",
                        onClick = {
                            viewModel.sauvegarderGeminiApiKey(inputKey)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2 : Foreground Service
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
            border = BorderStroke(1.dp, CtrlColor.LinenBeige)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Exécution en arrière-plan",
                        color = CtrlColor.WineInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Maintient l'écoute active des déclencheurs via une notification discrète.",
                        color = CtrlColor.SlateSmoke,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = uiState.isForegroundServiceActive,
                    onCheckedChange = viewModel::toggleForegroundService,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CtrlColor.Parchment,
                        checkedTrackColor = CtrlColor.WineInk,
                        uncheckedThumbColor = CtrlColor.SlateSmoke,
                        uncheckedTrackColor = CtrlColor.LinenBeige
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section 3 : Variables globales
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Variables globales",
                    color = CtrlColor.WineInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${uiState.variables.size} variable(s) enregistrée(s)",
                    color = CtrlColor.SlateSmoke,
                    fontSize = 12.sp
                )
            }

            CtrlGhostButton(
                text = "+ Ajouter",
                onClick = { showAddVarDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.variables.isEmpty()) {
            Text(
                text = "Aucune variable définie. Les variables permettent de partager des valeurs entre différentes macros.",
                color = CtrlColor.SlateSmoke,
                fontSize = 13.sp
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.variables.forEach { variable ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = CtrlColor.PureWhite),
                        border = BorderStroke(1.dp, CtrlColor.LinenBeige)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = variable.nom,
                                    color = CtrlColor.WineInk,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Valeur : ${variable.valeur} (${variable.type.name})",
                                    color = CtrlColor.SlateSmoke,
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(
                                onClick = { viewModel.supprimerVariable(variable.nom) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
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

        Spacer(modifier = Modifier.height(24.dp))

        // Section 4 : Sauvegarde & Restauration (Export / Import JSON)
        Text(
            text = "Sauvegarde & Synchronisation",
            color = CtrlColor.WineInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Exportez vos macros en JSON ou restaurez une configuration existante.",
            color = CtrlColor.SlateSmoke,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CtrlOutlinedButton(
                text = "Exporter JSON",
                onClick = viewModel::exporterConfiguration,
                modifier = Modifier.weight(1f)
            )

            CtrlOutlinedButton(
                text = "Importer JSON",
                onClick = { showImportDialog = true },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Section À Propos
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ctrl v3.0",
                color = CtrlColor.WineInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Conçu pour l'automatisation native haute performance.",
                color = CtrlColor.SlateSmoke,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(70.dp))
    }

    // Modal dialogue d'ajout de variable
    if (showAddVarDialog) {
        var nom by remember { mutableStateOf("") }
        var valeur by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(TypeVariable.TEXTE) }

        AlertDialog(
            onDismissRequest = { showAddVarDialog = false },
            title = { Text("Nouvelle variable", color = CtrlColor.WineInk, fontWeight = FontWeight.Medium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nom,
                        onValueChange = { nom = it },
                        label = { Text("Nom de la variable") },
                        placeholder = { Text("compteur_cycle") },
                        shape = CardShape,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = valeur,
                        onValueChange = { valeur = it },
                        label = { Text("Valeur initiale") },
                        placeholder = { Text("0") },
                        shape = CardShape,
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                CtrlPrimaryButton(
                    text = "Enregistrer",
                    onClick = {
                        if (nom.isNotBlank()) {
                            viewModel.ajouterOuModifierVariable(nom, valeur, type)
                            showAddVarDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                CtrlGhostButton(text = "Annuler", onClick = { showAddVarDialog = false })
            },
            containerColor = CtrlColor.Parchment
        )
    }

    // Modal dialogue d'exportation
    uiState.exportJsonResult?.let { json ->
        AlertDialog(
            onDismissRequest = viewModel::clearFeedback,
            title = { Text("Exportation JSON", color = CtrlColor.WineInk, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text(
                        text = "Copiez ce bloc JSON pour sauvegarder ou transférer vos macros :",
                        color = CtrlColor.SlateSmoke,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = json,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.height(220.dp),
                        shape = CardShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CtrlColor.PureWhite,
                            unfocusedContainerColor = CtrlColor.PureWhite
                        )
                    )
                }
            },
            confirmButton = {
                CtrlPrimaryButton(text = "Fermer", onClick = viewModel::clearFeedback)
            },
            containerColor = CtrlColor.Parchment
        )
    }

    // Modal dialogue d'importation
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importer des macros", color = CtrlColor.WineInk, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text(
                        text = "Collez ici le JSON exporté depuis Ctrl :",
                        color = CtrlColor.SlateSmoke,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonImportText,
                        onValueChange = { jsonImportText = it },
                        placeholder = { Text("Coller le JSON ici...") },
                        modifier = Modifier.height(200.dp),
                        shape = CardShape
                    )
                }
            },
            confirmButton = {
                CtrlPrimaryButton(
                    text = "Importer",
                    onClick = {
                        if (jsonImportText.isNotBlank()) {
                            viewModel.importerConfiguration(jsonImportText)
                            showImportDialog = false
                            jsonImportText = ""
                        }
                    }
                )
            },
            dismissButton = {
                CtrlGhostButton(text = "Annuler", onClick = { showImportDialog = false })
            },
            containerColor = CtrlColor.Parchment
        )
    }
}
