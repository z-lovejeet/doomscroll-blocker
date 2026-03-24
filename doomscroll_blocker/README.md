# Doomscroll Blocker 🛡️

A strict, uncompromising, battery-efficient Android personal utility designed specifically to break the habit of Instagram Reels doomscrolling. 

Instead of simple timers that can be easily dismissed, **Doomscroll Blocker** acts as a strict enforcer. It monitors your Instagram usage, enforces a strict daily limit, gives an intrusive full-screen warning, and physically force-closes the app if you don't listen.

## ✨ Features

- **The 25+5 Rule**: You get exactly 25 minutes of normal Reels watching.
- **Intrusive Warning**: At 25 minutes, a massive, un-ignorable full-screen overlay takes over your screen for 5 seconds warning you to stop. You then have 5 minutes of grace time to finish your video.
- **Zero-Tolerance Force Close**: At 30 minutes, the app triggers a system-level KICK, forcefully throwing you to your smartphone's home screen.
- **The Penalty Box**: Once kicked, you are placed in a 3-hour cooldown penalty. If you try to open the Reels tab during this time, you are instantly kicked back to the home screen.
- **Walk of Shame (Friction Unlock)**: To unlock the app early during a penalty, you are forced to type out a multi-sentence accountability pledge with zero typos.
- **Midnight Reset & Hard Daily Limits**: You only get 3 penalties per day. After 3 strikes, the Reels tab is completely locked until midnight.
- **Live UI Dashboard**: A gorgeous, dark-themed dashboard built in Flutter streams the real-time background countdown straight to your eyes.

## 🏗️ Architecture Stack

This is a Hybrid app built for maximum efficiency:

- **Frontend**: Flutter (Dart) for beautiful, smooth Dashboard and Setup UIs.
- **Backend / Engine**: Native Kotlin (Android) background services.
- **Bridge**: `MethodChannel` and `EventChannel` for constant, real-time communication between the UI and Native background services.
- **Storage**: Native `SharedPreferences` for local, offline state persistence (No databases, zero network calls, 100% private).

## 🔋 Battery First Design

Unlike traditional screen-time apps that constantly wake your processor, this app leverages Android's `AccessibilityService` combined with an XML configuration (`accessibility_service_config.xml`) strictly filtered to only wake up when `com.instagram.android` is active on your screen. When Instagram is closed, this app consumes zero background battery.

## 🔐 Privacy & Security
- **No Internet Required**: The app lacks the `<uses-permission android:name="android.permission.INTERNET" />` permission. Data cannot mathematically leave your device.
- **Local Only**: All penalty timers, session history, and logic run offline in private local storage.

## 🚀 Setup & Installation (Android Only)

Because this app utilizes deep system integrations meant for personal use, it cannot be published to the Google Play Store. You must build and sideload it yourself.

### Prerequisites
- [Flutter SDK](https://docs.flutter.dev/get-started/install)
- [Android SDK (Platform 36, Build tools 36.0.0)](https://developer.android.com/studio)

### Build Instructions
1. Clone this repository.
   ```bash
   git clone <your-repo-url>
   cd doomscroll_blocker
   ```
2. Build the production APK:
   ```bash
   flutter build apk --release
   ```
3. Transfer `build/app/outputs/flutter-apk/app-release.apk` to your Android device via USB or a cloud drive.
4. Install the APK on your device (Allow installation from Unknown Sources).

### App Configuration
When you first open the app, follow the setup UI to grant 2 critical system permissions:
1. **Accessibility Service**: Required to read the screen node tree specifically looking for the Instagram Reels tab.
2. **Display over other apps (`SYSTEM_ALERT_WINDOW`)**: Required to draw the 5-second intrusive warning overlay.

## 🤝 Contributing
This was explicitly built as a personal project, but feel free to fork it, modify the timers within `StorageManager.kt`, or change the specific tracked keywords inside `DoomscrollAccessibilityService.kt` to target other apps like TikTok or YouTube Shorts!
