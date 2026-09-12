package com.example.presentation.aigenerator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
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
import com.example.presentation.theme.PillShape
import com.example.presentation.theme.components.*

@Composable
fun AiGeneratorScreen(
    viewModel: AiGeneratorViewModel,
    onMacroSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val exemples = listOf(
        "Coupe le Bluetooth et baisse le volume à 23h00",
        "Alerte et vibre si la batterie descend sous 15%",
        "Active le son média à 80% quand le casque est connecté",
        "Affiche un message de bienvenue quand je me connecte au wifi Maison"
    )

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.resetSaved()
            onMacroSaved()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CtrlColor.Parchment)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // En-tête
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = CtrlColor.WineInk,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Générateur IA",
                color = CtrlColor.WineInk,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.02).sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Décrivez votre besoin en français ou en anglais. Ctrl synthétise instantanément le déclencheur, les conditions et les actions correspondantes.",
            color = CtrlColor.SlateSmoke,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Champ de saisie prompt
        OutlinedTextField(
            value = uiState.prompt,
            onValueChange = viewModel::onPromptChange,
            placeholder = {
                Text(
                    text = "Ex: Dès que je branche mon chargeur le soir, active le mode avion et règle le volume à zéro...",
                    color = CtrlColor.SlateSmoke,
                    fontSize = 14.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            shape = CardShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CtrlColor.PureWhite,
                unfocusedContainerColor = CtrlColor.PureWhite,
                focusedBorderColor = CtrlColor.WineInk,
                unfocusedBorderColor = CtrlColor.LinenBeige,
                cursorColor = CtrlColor.WineInk
            ),
            maxLines = 5
        )

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = uiState.errorMessage ?: "",
                color = CtrlColor.AlertRed,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Exemples cliquables
        Text(
            text = "Suggestions rapides :",
            color = CtrlColor.WineInk,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            exemples.forEach { ex ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.appliquerExemple(ex) },
                    shape = PillShape,
                    colors = CardDefaults.cardColors(containerColor = CtrlColor.ButterCream),
                    border = BorderStroke(0.5.dp, CtrlColor.LinenBeige)
                ) {
                    Text(
                        text = "⚡ $ex",
                        color = CtrlColor.WineInk,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bouton de génération
        CtrlPrimaryButton(
            text = if (uiState.isGenerating) "Synthèse en cours..." else "Générer la macro",
            onClick = viewModel::generer,
            enabled = !uiState.isGenerating && uiState.prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = CtrlColor.PureWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        // Résultat généré
        uiState.macroGeneree?.let { macro ->
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 0.5.dp, color = CtrlColor.LinenBeige)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Macro proposée par l'IA",
                color = CtrlColor.WineInk,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            CtrlMacroCard(
                macro = macro,
                onToggleActive = {},
                onTestClick = {},
                onDetailsClick = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CtrlGhostButton(
                    text = "Régénérer",
                    onClick = viewModel::generer,
                    modifier = Modifier.weight(1f)
                )

                CtrlPrimaryButton(
                    text = "Valider et enregistrer",
                    onClick = viewModel::validerEtEnregistrer,
                    modifier = Modifier.weight(1.5f),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = CtrlColor.PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}
