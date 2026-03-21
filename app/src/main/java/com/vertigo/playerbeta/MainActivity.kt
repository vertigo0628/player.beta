package com.vertigo.playerbeta

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.vertigo.playerbeta.ui.theme.PlayerbetaTheme

class MainActivity : ComponentActivity() {

    // holds files shared from other apps ie telegram
    var sharedUris: List<Uri>? = emptyList()

    // handles permissions request
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->   // checks if permission is granted
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            // if permission is denied show message or close app
            finish()
        }
    }

    // picker for adding music files manually
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // store the uri so viewmodel can find it
            (application as MusicPlayerApplication).pickedUri = it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check if opened via share from other apps
        handleSharedFiles(intent)

        // check permissions
        checkAndRequestPermissions()

        setContent {
            PlayerbetaTheme {
                MusicPlayerApp()
            }
        }
    }

    // launches the system file picker
    fun openFilePicker() {
        filePickerLauncher.launch(arrayOf("audio/*"))
    }

    // called when app is already running and user shares again
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedFiles(intent)
    }

    // extract audio files from share intent
    private fun handleSharedFiles(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }?.let { uri ->
                    sharedUris = listOf(uri)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }?.let { uris ->
                    sharedUris = uris
                }
            }
        }
    }

    // checks if we have permission to read music files and show notifications
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
