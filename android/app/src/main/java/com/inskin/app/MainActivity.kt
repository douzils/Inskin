package com.inskin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inskin.data.db.InskinDatabase

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { App() }
  }
}

@Composable
fun App() {
  MaterialTheme {
    Surface(Modifier.fillMaxSize()) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Inskin Compose Skeleton")
          Spacer(Modifier.height(16.dp))
          Button(onClick = { /* TODO: navigation */ }) {
            Text("Action")
          }
        }
      }
    }
  }
}
