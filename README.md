# 🕷️ Spider GFX Tool v9.0

[![Android MinSDK](https://img.shields.io/badge/MinSDK-24%20%28Android%207.0%2B%29-brightgreen.svg)](https://developer.android.com/)
[![Target SDK](https://img.shields.io/badge/TargetSDK-35%20%28Android%2015%29-blue.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)](https://kotlinlang.org/)
[![UI Toolkit](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Shizuku API](https://img.shields.io/badge/Shizuku%20API-v13.1.5-orange.svg)](https://shizuku.rikka.app/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Spider GFX Tool v9.0** is an open-source, high-performance Android gaming utility and graphics optimization suite built for Android power users and competitive mobile gamers. Engineered with **Jetpack Compose (Material3)** and powered by the **Shizuku API**, Spider GFX Tool delivers system-level tweaks, frame rate unlocks (60/90/120 FPS), custom aspect ratio scaling (iPad View), real-time system telemetry overlays, and intelligent RAM hibernation—**without requiring Android root access**.

---

## ⚡ Core Highlights & Capabilities

- **🚀 Shizuku-Powered Non-Root Shell Engine (`SmartEngine`)**  
  Leverages Shizuku (v13+) via Wireless Debugging to execute system-level Android shell commands and file transfers without root privileges. Includes automatic parcel error sanitization and multi-target package dispatching.

- **🎮 60 / 90 / 120 FPS Unlocker & Config Injector (`GfxConfigManager`)**  
  Injects custom high-frame-rate game configurations (`Active.sav` & UserCustom profiles) into game storage directories (`/Android/data/<package>/files/`). Supports GitHub-backed versioned downloads with Zip Slip security validation.

- **📱 Custom Resolution & iPad View Overdrive**  
  Provides precise aspect ratio scaling and landscape width compression sliders to enable true wide FOV (iPad View) across all supported Android screen densities.

- **📊 Real-Time HUD & Gaming Space Overlays (`RealTimeHudService`)**  
  Floating `WindowManager` system overlays displaying live FPS, CPU utilization, RAM usage, thermal telemetry, and low-latency custom crosshair generators.

- **❄️ Silent Background Hibernation & RAM Sweeper (`AppHibernationScreen`)**  
  Shizuku-backed background process freezer (`am force-stop`) with a custom Whitelist Manager to terminate resource-hogging background applications prior to gaming.

- **🌐 Smart DNS Switcher & Latency Optimization (`SmartDnsManager`)**  
  Parallel ICMP ping testing engine that dynamically evaluates and identifies the lowest-latency DNS server (Cloudflare `1.1.1.1`, Google `8.8.8.8`, Quad9 `9.9.9.9`, AdGuard) for optimal online game routing.

- **🎧 Audio Overdrive & Hardware Safety Watchdog (`BatterySafetyReceiver`)**  
  Unlocks Ultra Audio profiles and spatial sound enhancements while monitoring battery temperature with automatic state-reset protection upon charger connection.

---

## 🎯 Supported Game Targets

Spider GFX Tool features automatic package detection (`GfxBackendManager.getAutoDetectedGamePackage`) and multi-package execution for all regional variants of PUBG Mobile:

| Region / Variant | Package Name | Status |
| :--- | :--- | :---: |
| **BGMI (India)** | `com.pubg.imobile` | ✅ Supported |
| **Global** | `com.tencent.ig` | ✅ Supported |
| **Korea & Japan** | `com.pubg.krmobile` | ✅ Supported |
| **Vietnam (VNG)** | `com.vng.pubgmobile` | ✅ Supported |
| **Taiwan** | `com.pubg.twmobile` | ✅ Supported |
| **PUBG Mobile Lite** | `com.tencent.igfit` | ✅ Supported |

---

## 🏗️ Architecture & Component Overview

```
                          ┌───────────────────────────────────────────┐
                          │   Spider GFX Tool v9.0 UI (Compose M3)    │
                          └─────────────────────┬─────────────────────┘
                                                │
                 ┌──────────────────────────────┼──────────────────────────────┐
                 ▼                              ▼                              ▼
    ┌──────────────────────────┐  ┌──────────────────────────┐  ┌──────────────────────────┐
    │    GfxBackendManager     │  │     SmartEngine (Core)    │  │   System Services/HUD    │
    ├──────────────────────────┤  ├──────────────────────────┤  ├──────────────────────────┤
    │ • Auto Package Scanning  │  │ • Shizuku Binder Check   │  │ • RealTimeHudService     │
    │ • Multi-Target Dispatch  │  │ • Safe Shell Execution   │  │ • GamingSpaceService     │
    │ • Zip Slip Validation    │  │ • Non-blocking Coroutines│  │ • CrosshairService       │
    └────────────┬─────────────┘  └────────────┬─────────────┘  └────────────┬─────────────┘
                 │                             │                             │
                 └─────────────────────────────┼─────────────────────────────┘
                                               ▼
                              ┌──────────────────────────────────┐
                              │ Shizuku API (v13.1.5) / ADB Shell│
                              └────────────────┬─────────────────┘
                                               │
                                               ▼
                              ┌──────────────────────────────────┐
                              │ Android System Kernel & Storage  │
                              └──────────────────────────────────┘
```

### Module Matrix

- **`SmartEngine.kt`**: Asynchronous, non-blocking shell runner interacting directly with Shizuku binder processes. Handles output sanitization, fallback execution, and permission validation.
- **`GfxConfigManager.kt`**: GitHub release asset downloader and payload unzipper. Safely extracts FPS configurations and injects them directly into target game directories.
- **`GfxBackendManager.kt`**: Multi-package discovery engine, iPad View resolution matrix executor, and silent RAM sweeper manager.
- **`SmartDnsManager.kt`**: Multi-threaded ping benchmark utility testing DNS latency to pick the optimal server route for low-ping gaming.
- **`SystemTelemetryViewModel.kt`**: StateFlow-backed hardware monitoring system emitting real-time CPU, RAM, battery temperature, and FPS telemetry.
- **`GamingSpaceService.kt`**: Floating compose overlay service hosting in-game quick controls, thermal safety triggers, and touch passthrough layout management.

---

## 🛠️ System Requirements & Shizuku Setup

### Requirements
- **OS**: Android 7.0 (API Level 24) or higher (Fully optimized for Android 15 / API 35).
- **Architecture**: `arm64-v8a` or `armeabi-v7a`.
- **Permissions Required**:
  - `SYSTEM_ALERT_WINDOW` (Overlay permission for HUD & Crosshair)
  - `PACKAGE_USAGE_STATS` (Memory telemetry & process listing)
  - `moe.shizuku.manager.permission.API_V23` (Shizuku Binder access)

### Shizuku Setup (Non-Root Wireless Debugging)

1. Download and install **Shizuku** from Google Play or F-Droid.
2. Enable **Developer Options** on your Android device (`Settings` → `About Phone` → Tap `Build Number` 7 times).
3. Open **Developer Options** and enable **Wireless Debugging**.
4. Open **Shizuku**, select **Pairing**, and enter the 6-digit Wireless Debugging pairing code.
5. Tap **Start** in Shizuku.
6. Open **Spider GFX Tool v9.0**, navigate to the **Activation Screen**, and grant Shizuku permission when prompted.

> **Note for Rooted Users**: If your device is rooted via Magisk or KernelSU, Shizuku can be launched directly with 1-click superuser permission without Wireless Debugging.

---

## 🧰 Technology Stack & Dependencies

| Dependency | Purpose | Version |
| :--- | :--- | :---: |
| **Kotlin** | Core Programming Language | `2.0.0` |
| **Jetpack Compose BOM** | Declarative UI Framework | `2024.09.00+` |
| **Material 3** | Cyberpunk-themed Neumorphic UI Design | Built-in |
| **Rikka Shizuku API** | Non-root Android Shell IPC Binder | `13.1.5` |
| **OkHttp3** | Config Payload Downloader | `4.12.0` |
| **Coil Compose** | Asynchronous Asset Image Loading | `2.7.0` |
| **AndroidX Lifecycle & Coroutines** | Reactive State Management & Async I/O | `2.8.x` |

---

## 📁 Repository Structure

```
SpiderGFXTool/
├── app/
│   ├── build.gradle.kts           # App build configuration & dependencies
│   ├── proguard-rules.pro         # R8 / ProGuard optimization rules
│   └── src/main/
│       ├── AndroidManifest.xml    # Manifest, permissions & service definitions
│       └── java/com/example/spidergfxtool/
│           ├── MainActivity.kt               # Main entry point & Compose navigation
│           ├── SmartEngine.kt                # Shizuku safe shell execution engine
│           ├── GfxBackendManager.kt          # Multi-package discovery & GFX logic
│           ├── GfxConfigManager.kt           # FPS config download & injection
│           ├── SmartDnsManager.kt            # Parallel ping latency benchmark engine
│           ├── SystemTelemetryViewModel.kt   # Live CPU, RAM, Temp & FPS state engine
│           ├── GamingSpaceService.kt         # Floating Compose gaming overlay dock
│           ├── RealTimeHudService.kt         # Real-time hardware telemetry HUD service
│           ├── CrosshairService.kt           # Custom draggable crosshair overlay
│           ├── BatterySafetyReceiver.kt      # Hardware battery protection watchdog
│           └── ui/                           # Jetpack Compose screen components
│               ├── GfxOverdriveDashboardScreen.kt
│               ├── GfxDashboardScreen.kt
│               ├── GfxExtremeHudScreen.kt
│               ├── AppHibernationScreen.kt
│               ├── GfxActivationScreen.kt
│               ├── GfxExtraFeaturesScreen.kt
│               └── components/               # Custom Cyberpunk UI design system
└── README.md                              # Technical documentation
```

---

## 🚀 Building & Installation

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or Android Studio Meerkat/2026+
- JDK 11 or higher
- Android SDK 35

### Compilation Commands

```bash
# Clone the repository
git clone https://github.com/GfxGamerLab/spider-gfx-tool.git
cd spider-gfx-tool

# Build Debug APK
./gradlew assembleDebug

# Build Release APK (Optimized & Shrunk via R8)
./gradlew assembleRelease
```

The output APK will be generated at `app/build/outputs/apk/release/app-release.apk`.

---

## ⚠️ Disclaimer & Terms of Use

1. **Third-Party Software**: Spider GFX Tool is an independent open-source utility for Android performance tuning. It is not affiliated with, endorsed by, or associated with Tencent, Krafton, Level Infinite, or PUBG Corporation.
2. **Fair Play**: Modifying game configuration files or using custom resolutions should be done responsibly in accordance with game terms of service. Users assume all responsibility for account safety and device customization.
3. **Hardware Safety**: While Spider GFX Tool includes automated thermal watchdogs (`BatterySafetyReceiver`), aggressive frame rate unlocking may increase device temperatures and battery consumption.

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.

---
<p align="center">Made with ❤️ for Android Power Users & Mobile Gamers.</p>
