package com.aquigs.sp21ace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aquigs.sp21ace.ui.AppShell
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Sp21AceTheme {
                AppShell()
            }
        }
    }
}
