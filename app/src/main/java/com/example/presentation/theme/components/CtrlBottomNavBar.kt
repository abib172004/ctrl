package com.example.presentation.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.theme.CtrlColor

enum class NavDestination(val route: String, val titre: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Accueil", Icons.Outlined.SpaceDashboard),
    MACROS("macros", "Macros", Icons.Outlined.Bolt),
    IA("ai_generator", "IA", Icons.Outlined.AutoAwesome),
    JOURNAL("journal", "Journal", Icons.Outlined.History),
    SETTINGS("settings", "Réglages", Icons.Outlined.Tune)
}

/**
 * 6.9 CtrlBottomNavBar - Navigation mobile native Daylit
 */
@Composable
fun CtrlBottomNavBar(
    currentRoute: String,
    onNavigate: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = CtrlColor.LinenBeige)
            .navigationBarsPadding(),
        containerColor = CtrlColor.Parchment,
        tonalElevation = 0.dp
    ) {
        NavDestination.values().forEach { dest ->
            val selected = (currentRoute == dest.route)

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest) },
                icon = {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.titre,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = dest.titre,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CtrlColor.WineInk,
                    selectedTextColor = CtrlColor.WineInk,
                    indicatorColor = CtrlColor.LemonWhisper,
                    unselectedIconColor = CtrlColor.SlateSmoke,
                    unselectedTextColor = CtrlColor.SlateSmoke
                )
            )
        }
    }
}
