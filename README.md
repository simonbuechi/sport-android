# Sport Android

A modern, offline-first Android companion app for workout tracking, routine management, and physical progression monitoring.

## Features

- **Workout Logging & Journal**: Track exercises, sets, weights, reps, and RPE with integrated rest timer & haptics.
- **Routines & Templates**: Dense routine management with fluid Material 3 container transformations, set tables, and live editing.
- **Exercise Library**: Searchable exercise database with category/body part filtering, illustrated icons, and historical performance tracking (1RM, volume, PRs).
- **Body Progression**: Log and chart body weight, body fat %, and circumference measurements.
- **Multilingual Support**: Fully localized in English, German (Deutsch), French (Français), Spanish (Español), and Italian (Italiano).
- **Modern Themes**: Dynamic system default, light, and dark mode theming with tactile feedback.
- **Cloud & Offline Sync**: Powered by Firebase Firestore and Authentication with offline caching.

## Tech Stack

- **UI**: Jetpack Compose, Material 3, Navigation Compose
- **Language**: Kotlin 2.0+
- **Architecture**: Clean Architecture / MVVM, Kotlin Coroutines & Flow
- **Dependency Injection**: Dagger Hilt
- **Storage & State**: Jetpack DataStore Preferences
- **Backend & Auth**: Firebase Firestore, Firebase Authentication, Google Identity / Credential Manager
- **Image Loading**: Coil Compose

## Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/simonbuechi/sport-android.git
   cd sport-android
   ```

2. **Configure Firebase**:
   - Create a project on the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with package name `ch.simibu.sport`.
   - Download `google-services.json` and place it in the `app/` directory (see `app/google-services.json.example`).

3. **Build and Run**:
   ```bash
   ./gradlew assembleDebug
   ```
