package com.example.presentation.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.theme.CtrlColor
import com.example.presentation.theme.CtrlShapes
import com.example.presentation.theme.PillShape

/**
 * 6.1 CtrlPrimaryButton - Bouton plein WineInk pour l'action principale
 */
@Composable
fun CtrlPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPill: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .heightIn(min = 48.dp),
        enabled = enabled,
        shape = if (isPill) PillShape else CtrlShapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = CtrlColor.WineInk,
            contentColor = CtrlColor.PureWhite,
            disabledContainerColor = CtrlColor.Driftwood,
            disabledContentColor = CtrlColor.LinenBeige
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon.invoke()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = CtrlColor.PureWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.01).sp
        )
    }
}

/**
 * 6.2 CtrlGhostButton - Bouton texte discret pour action secondaire
 */
@Composable
fun CtrlGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = CtrlColor.WineInk,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 36.dp),
        shape = CtrlShapes.small,
        colors = ButtonDefaults.textButtonColors(
            contentColor = textColor
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon.invoke()
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 6.3 CtrlOutlinedButton - Bouton contour fin 1dp WineInk
 */
@Composable
fun CtrlOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 36.dp),
        enabled = enabled,
        shape = CtrlShapes.small,
        border = BorderStroke(1.dp, CtrlColor.WineInk),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = CtrlColor.WineInk
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon.invoke()
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = CtrlColor.WineInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
