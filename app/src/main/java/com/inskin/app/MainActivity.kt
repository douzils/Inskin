package com.inskin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.inskin.app.InskinTheme
import com.inskin.app.InskinNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InskinTheme {
                // Lance la navigation
                InskinNav()
            }
        }
    }
}
