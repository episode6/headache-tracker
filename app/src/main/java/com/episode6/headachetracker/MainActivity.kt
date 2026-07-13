package com.episode6.headachetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.episode6.headachetracker.ui.navigation.HeadacheTrackerNavigation
import com.episode6.headachetracker.ui.theme.HeadacheTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeadacheTrackerTheme {
                HeadacheTrackerNavigation()
            }
        }
    }
}
