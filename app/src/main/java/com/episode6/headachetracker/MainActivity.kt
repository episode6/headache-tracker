package com.episode6.headachetracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.episode6.headachetracker.ui.navigation.HeadacheTrackerNavigation
import com.episode6.headachetracker.ui.theme.HeadacheTrackerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialEditDate = intent.getStringExtra(EXTRA_EDIT_DATE)
        setContent {
            HeadacheTrackerTheme {
                RequestNotificationPermission()
                HeadacheTrackerNavigation(initialEditDate = initialEditDate)
            }
        }
    }

    companion object {
        const val EXTRA_EDIT_DATE = "extra_edit_date"
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }
}
