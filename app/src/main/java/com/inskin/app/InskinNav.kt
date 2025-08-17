package com.inskin.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun InskinNav(vm: NfcViewModel) {
    val navController = rememberNavController()
    val items = listOf("read", "write", "settings")
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { route ->
                    val icon = when (route) {
                        "read" -> Icons.Default.Wifi
                        "write" -> Icons.Default.Edit
                        else -> Icons.Default.Settings
                    }
                    val label = when (route) {
                        "read" -> stringResource(R.string.tab_read)
                        "write" -> stringResource(R.string.tab_write)
                        else -> stringResource(R.string.tab_settings)
                    }
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { androidx.compose.material3.Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "read",
            modifier = Modifier.padding(padding)
        ) {
            composable("read") { ReadScreen(vm.tagInfo) }
            composable("write") { WriteScreen(vm) }
            composable("settings") { SettingsScreen() }
        }
    }
}
