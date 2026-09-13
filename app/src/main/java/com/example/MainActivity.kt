package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.presentation.aigenerator.AiGeneratorScreen
import com.example.presentation.aigenerator.AiGeneratorViewModel
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.dashboard.DashboardViewModel
import com.example.presentation.journal.JournalScreen
import com.example.presentation.journal.JournalViewModel
import com.example.presentation.macros.MacrosScreen
import com.example.presentation.macros.MacrosViewModel
import com.example.presentation.macrobuilder.MacroBuilderScreen
import com.example.presentation.macrobuilder.MacroBuilderViewModel
import com.example.presentation.macrodetail.MacroDetailScreen
import com.example.presentation.macrodetail.MacroDetailViewModel
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.settings.SettingsViewModel
import com.example.presentation.theme.CtrlTheme
import com.example.presentation.theme.components.CtrlBottomNavBar
import com.example.presentation.theme.components.NavDestination
import com.example.service.CtrlForegroundService

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val macrosViewModel: MacrosViewModel by viewModels()
    private val macroBuilderViewModel: MacroBuilderViewModel by viewModels()
    private val aiGeneratorViewModel: AiGeneratorViewModel by viewModels()
    private val journalViewModel: JournalViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Lancement du service persistant en arrière-plan
        try {
            CtrlForegroundService.demarrer(this)
        } catch (_: Exception) {}

        setContent {
            CtrlTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: NavDestination.DASHBOARD.route

                val isBottomNavVisible = currentRoute in listOf(
                    NavDestination.DASHBOARD.route,
                    NavDestination.MACROS.route,
                    NavDestination.IA.route,
                    NavDestination.JOURNAL.route,
                    NavDestination.SETTINGS.route
                )

                val navigateToTab: (String) -> Unit = { targetRoute ->
                    if (currentRoute != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (isBottomNavVisible) {
                            CtrlBottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { destination ->
                                    navigateToTab(destination.route)
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavDestination.DASHBOARD.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavDestination.DASHBOARD.route) {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onCreateMacroClick = {
                                    macroBuilderViewModel.reset()
                                    navController.navigate("create_macro")
                                },
                                onMacroDetailClick = { macroId ->
                                    navController.navigate("macro_detail/$macroId")
                                },
                                onNavigateToMacros = {
                                    navigateToTab(NavDestination.MACROS.route)
                                },
                                onNavigateToAi = {
                                    navigateToTab(NavDestination.IA.route)
                                },
                                onNavigateToJournal = {
                                    navigateToTab(NavDestination.JOURNAL.route)
                                }
                            )
                        }

                        composable(NavDestination.MACROS.route) {
                            MacrosScreen(
                                viewModel = macrosViewModel,
                                onCreateMacroClick = {
                                    macroBuilderViewModel.reset()
                                    navController.navigate("create_macro")
                                },
                                onMacroDetailClick = { macroId ->
                                    navController.navigate("macro_detail/$macroId")
                                }
                            )
                        }

                        composable(NavDestination.IA.route) {
                            AiGeneratorScreen(
                                viewModel = aiGeneratorViewModel,
                                onMacroSaved = {
                                    navigateToTab(NavDestination.DASHBOARD.route)
                                }
                            )
                        }

                        composable(NavDestination.JOURNAL.route) {
                            JournalScreen(
                                viewModel = journalViewModel
                            )
                        }

                        composable(NavDestination.SETTINGS.route) {
                            SettingsScreen(
                                viewModel = settingsViewModel
                            )
                        }

                        composable("create_macro") {
                            MacroBuilderScreen(
                                viewModel = macroBuilderViewModel,
                                onBackClick = { navController.popBackStack() },
                                onMacroSaved = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(
                            route = "macro_detail/{macroId}",
                            arguments = listOf(navArgument("macroId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val macroId = backStackEntry.arguments?.getString("macroId") ?: ""
                            val detailViewModel = remember(macroId) {
                                MacroDetailViewModel(application, macroId)
                            }

                            MacroDetailScreen(
                                viewModel = detailViewModel,
                                onBackClick = { navController.popBackStack() },
                                onMacroDeleted = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
