package com.inskin.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String, val label: String) {
    object Read : Screen("read", "Lecture")
    object Write : Screen("write", "Écriture")
    object Settings : Screen("settings", "Paramètres")
}

/**
 * Navigation graph for the application. The currently scanned [tagInfo] is
 * passed down so the read screen can display it.
 */
@Composable
fun InskinNav(tagInfo: TagInfo? = null) {
    val navController = rememberNavController()
    Scaffold(bottomBar = { BottomBar(navController) }) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Read.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Read.route) { ReadScreen(tagInfo) }
            composable(Screen.Write.route) { WriteScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}


@Composable
private fun BottomBar(navController: NavHostController) {
    val items = listOf(Screen.Read, Screen.Write, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            val (icon, contentDesc) = when (screen) {
                is Screen.Read -> Icons.Filled.Home to "Lecture"
                is Screen.Write -> Icons.Filled.Create to "Écriture"
                is Screen.Settings -> Icons.Filled.Settings to "Paramètres"
            }
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = contentDesc) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

