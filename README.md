# Player.beta 🎵

Player.beta is a modern, lightweight Android music player built entirely with **Jetpack Compose** and **Material 3**. It allows users to browse and play local audio files with a clean, intuitive interface and background playback support.

## ✨ Features

- **Advanced Library Organization**: Browse music by **Songs**, **Albums**, **Artists**, **Folders**, and **Playlists**.
- **Deep Search**: Instant and recursive search across your entire local library.
- **Material You Design**: Personalize your player with dynamic colors that match your device's wallpaper.
- **Smart Playlists**: Automatic collection of **Favorites**, **Recently Added**, and **Recently Played** tracks.
- **Audio Enhancements**:
    - **5-Band Equalizer** with manual tuning.
    - **System Presets** (Pop, Rock, Jazz, etc.).
    - **Bass Boost** for enhanced low-frequency performance.
- **Gapless Playback**: Seamlessly transition between tracks with zero gaps (pre-loaded next track).
- **Sleep Timer**: Schedule playback to stop automatically after a set duration.
- **Background Playback**: High-performance foreground service for uninterrupted listening.
- **Modern UI**: Clean, responsive design built with Jetpack Compose and Material 3.
- **Playlist Management**: Create, view, and organize your favorite music into custom playlists.
- **Dynamic Updates**: Real-time library syncing with Android MediaStore.
- **Permission Support**: Full support for Android 13+ and legacy storage permissions.

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Manual (ViewModel via `viewModel()`)
- **Background Tasks**: Android Services & Foreground Services
- **Data Source**: MediaStore API
- **Design System**: Material Design 3

## 📸 Screenshots

| Song List | Player Controls |
|-----------|-----------------|
| *Coming Soon* | *Coming Soon* |

## 🚀 Getting Started

### Prerequisites

- Android Studio Ladybug or newer.
- Android Device or Emulator running API 26 (Oreo) or higher.

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/Playerbeta.git
   ```
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Run the app on your device or emulator.

## 📱 Permissions

The app requires the following permissions to function:
- `READ_MEDIA_AUDIO` (Android 13+): To access audio files.
- `READ_EXTERNAL_STORAGE` (Below Android 13): To access audio files.
- `FOREGROUND_SERVICE`: To maintain playback when the app is in the background.
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Required for media playback on Android 14+.


---
Developed with ❤️ by [Vertigo]
