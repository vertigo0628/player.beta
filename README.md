# Player.beta 🎵

Player.beta is a modern, lightweight Android music player built entirely with **Jetpack Compose** and **Material 3**. It allows users to browse and play local audio files with a clean, intuitive interface and background playback support.

## ✨ Features

- **Local Music Library**: Automatically scans your device for audio files using MediaStore.
- **Background Playback**: Keep the music going even when the app is minimized, thanks to a robust Foreground Service.
- **Modern UI**: Built with Jetpack Compose and Material 3 for a beautiful, responsive experience.
- **Playback Controls**:
    - Play/Pause, Skip Next, Skip Previous.
    - Interactive Seek Bar.
    - Shuffle and Repeat modes (Off, All, One).
- **Persistent Notifications**: Control your music directly from the Android notification drawer.
- **Dynamic Updates**: Listens for system media scans to update your library as soon as new music is added.
- **Permission Handling**: Seamlessly handles storage permissions for Android 13+ (Media Audio) and older versions.

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

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
Developed with ❤️ by [Vertigo]
