package com.example.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CtrlColorScheme = lightColorScheme(
    primary = CtrlColor.WineInk,
    onPrimary = CtrlColor.PureWhite,
    primaryContainer = CtrlColor.BlushRust,
    onPrimaryContainer = CtrlColor.PureWhite,
    secondary = CtrlColor.LemonWhisper,
    onSecondary = CtrlColor.WineInk,
    secondaryContainer = CtrlColor.ButterCream,
    onSecondaryContainer = CtrlColor.WineInk,
    tertiary = CtrlColor.CitrinePop,
    onTertiary = CtrlColor.WineInk,
    background = CtrlColor.Parchment,
    onBackground = CtrlColor.WineInk,
    surface = CtrlColor.PureWhite,
    onSurface = CtrlColor.WineInk,
    surfaceVariant = CtrlColor.LinenBeige,
    onSurfaceVariant = CtrlColor.SlateSmoke,
    error = CtrlColor.AlertRed,
    onError = CtrlColor.PureWhite,
    outline = CtrlColor.LinenBeige,
    outlineVariant = CtrlColor.Sandstone
)

@Composable
fun CtrlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CtrlColorScheme,
        typography = CtrlTypography,
        shapes = CtrlShapes,
        content = content
    )
}
