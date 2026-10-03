package com.sagun12.vozemcena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sagun12.vozemcena.ui.navigation.MainNavigationApp
import com.sagun12.vozemcena.ui.theme.VozEmCenaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VozEmCenaTheme {
                MainNavigationApp()
            }
        }
    }
}
