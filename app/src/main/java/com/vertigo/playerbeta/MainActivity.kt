package com.vertigo.playerbeta

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.vertigo.playerbeta.ui.theme.PlayerbetaTheme

class MainActivity : ComponentActivity() {
    //handles permissions request
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->   //checks if permission is granted
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            //is permission is denied show message or close app
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            PlayerbetaTheme {
                MusicPlayerApp()
            }
        }
    }

    //checks if we have permission to read music files and show notifications
    private fun checkAndRequestPermissions() {
        val permissionsList = mutableListOf<String>()

        // Storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.READ_MEDIA_AUDIO)
            // Notification permission (Required for Android 13+)
            permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // FIND WHICH PERMISSIONS WE DONT HAVE YET
        val needPermission = permissionsList.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        // Request missing permissions
        if (needPermission.isNotEmpty()) {
            permissionLauncher.launch(needPermission.toTypedArray())
        }
    }
}
