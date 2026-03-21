package com.vertigo.playerbeta

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vertigo.playerbeta.ui.screens.PlayerScreen
import com.vertigo.playerbeta.viewmodel.PlayerViewModel

@Composable
fun MusicPlayerApp() {
    // Correct way to initialize ViewModel in Compose using the viewModel() function
    val viewModel: PlayerViewModel = viewModel()
    PlayerScreen(viewModel = viewModel, activity = LocalActivity.current as MainActivity)
}
