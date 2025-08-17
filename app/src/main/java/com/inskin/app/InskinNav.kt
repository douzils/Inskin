package com.inskin.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
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

@Composable
fun InskinNav() {
  val navController = rememberNavController()
  Scaffold(
    bottomBar = { BottomBar(navController) }
  ) { padding ->
    NavHost(navController, startDestination = Screen.Read.route, Modifier.padding(padding)) {
      composable(Screen.Read.route) { ReadScreen() }
      composable(Screen.Write.route) { WriteScreen() }
      composable(Screen.Settings.route) { SettingsScreen() }
    }
  }
}

@Composable
private fun BottomBar(navController: NavHostController) {
  val items = listOf(Screen.Read, Screen.Write, Screen.Settings)
  NavigationBar {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    items.forEach { screen ->
      NavigationBarItem(
        label = { Text(screen.label) },
        selected = currentRoute == screen.route,
        onClick = { navController.navigate(screen.route) }
      )
    }
  }
}
