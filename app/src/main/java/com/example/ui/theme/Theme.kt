package com.example.ui.theme

import androidx.compose.runtime.Composable
import com.example.presentation.theme.CtrlTheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CtrlTheme(content = content)
}
