package com.inskin.app.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import androidx.navigation.NavHostController
import com.inskin.app.nfc.NfcController
import com.inskin.app.ui.ReadScreen
import com.inskin.app.ui.WriteScreen

@Composable
fun InskinNav(nfc: NfcController) {
  val nav = rememberNavController()
  Scaffold(
    bottomBar = {
      NavigationBar {
        NavigationBarItem(selected = currentRoute(nav)=="read",
          onClick = { nav.navigate("read"){ launchSingleTop=true } },
          label = { Text("Lecture") }, icon = { Icon(Icons.Filled.Info, null) })
        NavigationBarItem(selected = currentRoute(nav)=="write",
          onClick = { nav.navigate("write"){ launchSingleTop=true } },
          label = { Text("Écriture") }, icon = { Icon(Icons.Filled.Edit, null) })
      }
    }
  ){ padding ->
    NavHost(nav, "read", Modifier.padding(padding).fillMaxSize()) {
      composable("read"){ ReadScreen(nfc) }
      composable("write"){ WriteScreen(nfc) }
    }
  }
}

@Composable
private fun currentRoute(nav: NavHostController): String? =
  nav.currentBackStackEntryFlow.collectAsState(initial = nav.currentBackStackEntry).value?.destination?.route
