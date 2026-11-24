# 🕷️ Spider GFX Tool v9.0

[![Android MinSDK](https://img.shields.io/badge/MinSDK-24%20%28Android%207.0%2B%29-brightgreen.svg)](https://developer.android.com/)
[![Target SDK](https://img.shields.io/badge/TargetSDK-35%20%28Android%2015%29-blue.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)](https://kotlinlang.org/)
[![UI Toolkit](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Shizuku API](https://img.shields.io/badge/Shizuku%20API-v13.1.5-orange.svg)](https://shizuku.rikka.app/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Spider GFX Tool v9.0** is an open-source, high-craft Android gaming utility and graphics optimization suite built for Android power users, mobile gamers, and developers. Engineered with **Jetpack Compose (Material3)** and powered by a dual architecture combining **Native Android APIs** with the **Shizuku API IPC Binder**, Spider GFX Tool provides system-level performance tuning, 60/90/120 FPS config injection, iPad View FOV overdrive, real-time telemetry HUD overlays, and background process hibernation—**without requiring Android root access**.

---

## ⚡ Key Features & v9.0 Innovations

- **🚀 Native APIs + Shizuku IPC Shell Engine (`SmartEngine`)**  
  Combines standard Android platform APIs (`PackageManager`, `UsageStatsManager`, `WindowManager`, `BatteryManager`) with Shizuku API (v13+) over Wireless Debugging. Executes privileged system shell commands (`am force-stop`, `dumpsys battery`, `cp -rf`) with automatic parcel error sanitization and multi-package auto-dispatching.

- **🎮 60 / 90 / 120 FPS GitHub 9.0 Pack Extraction & Storage Replacement (`GfxConfigManager`)**  
  Downloads versioned release archives directly from GitHub (`9.0/120FPS.zip`, `90FPS.zip`, `60FPS.zip`). Validates zip binary signatures and Zip Slip path traversal protections, unzips payloads, and replaces target game storage files in `/storage/emulated/0/Android/data/<package>/files/` using Shizuku file management.

- **🎯 Hub Target Lock & Launch Flow (`GfxOverdriveDashboardScreen`)**  
  Features target game auto-detection (`GfxBackendManager.getAutoDetectedGamePackage`) and manual target package locking. Executes a multi-stage Turbo Reactor boost sequence (RAM purge, GPU/cache optimization, Shizuku core injection) before launching the target game via `PackageManager`.

- **🛡️ Full-Screen Gaming Boost Overlay (`GamingSpaceService` & `GamingSpaceOverlay`)**  
  A `WindowManager` system overlay dock (`TYPE_APPLICATION_OVERLAY`) leveraging `TouchPassthroughLayout` for seamless in-game interaction. Hosts live system telemetry, quick performance triggers, and draggable crosshair overlays (`CrosshairService`).

- **📱 Custom Aspect Ratio & iPad View Overdrive**  
  Offers precise landscape width compression and custom resolution matrices to achieve true wide field-of-view (iPad View) across all supported device densities.

- **❄️ Silent Background Hibernation & RAM Sweeper (`AppHibernationScreen`)**  
  Shizuku-backed background process freezer with an interactive Whitelist Manager to silently terminate background applications (`am force-stop`) prior to launch while preserving essential system services.

- **🌐 Smart DNS Switcher & Latency Optimization (`SmartDnsManager`)**  
  Parallel ICMP ping testing engine evaluating DNS providers (Cloudflare `1.1.1.1`, Google `8.8.8.8`, Quad9 `9.9.9.9`, AdGuard) to select the fastest DNS route for low-latency gaming.

- **🎧 Hardware Battery Watchdog & Reset Safety (`BatterySafetyReceiver` & `MainActivity`)**  
  Includes an automated battery watchdog for real-time temperature resets (`dumpsys battery reset`) on charger connect/disconnect, along with global state cleanup on app launch and shutdown.

---

## 🎯 Supported Game Targets

Spider GFX Tool automatically scans installed applications and dispatches configuration commands across all regional PUBG Mobile package identifiers:

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
+--------------------------------------------------------------------------------------------+
|                            Spider GFX Tool v9.0 UI (Compose M3)                            |
+---------------------------------------------+----------------------------------------------+
                                              |
             ┌────────────────────────────────┼────────────────────────────────┐
             │                                │                                │
             ▼                                ▼                                ▼
+--------------------------+     +--------------------------+     +--------------------------+
|    GfxBackendManager /   |     |    SmartEngine (Core)    |     |   System Services & HUD  |
|     GfxConfigManager     |     | (Native APIs + Shizuku)  |     |  (WindowManager Overlay) |
+--------------------------+     +--------------------------+     +--------------------------+
| * GitHub 9.0 FPS Extract |     | * Shizuku IPC Binder Exec|     | * Fullscreen Boost Dock  |
| * Target /files/ Replace |     | * Parcel Error Filtering |     | * Live FPS & Hardware HUD|
| * Hub Target Lock Launch |     | * Native Android System  |     | * Draggable Crosshair UI |
+------------┬-------------+     +------------┬-------------+     +------------┬-------------+
             │                                │                                │
             └────────────────────────────────┴────────────────────────────────┘
                                              │
                                              ▼
+--------------------------------------------------------------------------------------------+
|                     Shizuku API (v13.1.5) / Native Android System APIs                     |
+---------------------------------------------+----------------------------------------------+
                                              |
                                              ▼
+--------------------------------------------------------------------------------------------+
|                  Android System Kernel, Hardware Nodes & Game Data /files/                  |
+--------------------------------------------------------------------------------------------+
```

### Key Module Responsibilities

- **`SmartEngine.kt`**: Core execution engine interfacing with Shizuku binder IPC (`Shizuku.newProcess`). Features input/output sanitization, parcel error handling, and non-blocking coroutines.
- **`GfxConfigManager.kt`**: Remote GitHub v9.0 release payload downloader and secure unpacker. Manages Zip Slip verification and target package `/files/` directory replacement.
- **`GfxBackendManager.kt`**: Manages installed package auto-detection, multi-package command dispatching, resolution scaling, and system resets.
- **`GamingSpaceService.kt`**: WindowManager overlay service providing full-screen Compose boost controls, touch passthrough layout management, and state syncing.
- **`RealTimeHudService.kt` & `CrosshairService.kt`**: System overlay services for real-time telemetry rendering (FPS, CPU, RAM, battery temp) and custom floating crosshairs.
- **`SmartDnsManager.kt`**: Multi-threaded ping utility benchmark engine for low-latency DNS selection.
- **`SystemTelemetryViewModel.kt`**: Reactive hardware monitoring view model emitting real-time telemetry metrics via StateFlow.

---

## 🛠️ System Requirements & Shizuku Setup

### System Requirements
- **OS**: Android 7.0 (API Level 24) or higher (Fully targeted for Android 15 / API 35).
- **Architectures**: `arm64-v8a`, `armeabi-v7a`.
- **Key Permissions**:
  - `SYSTEM_ALERT_WINDOW`: Required for floating HUD overlays and Gaming Space dock.
  - `PACKAGE_USAGE_STATS`: Required for RAM telemetry and background app enumeration.
  - `moe.shizuku.manager.permission.API_V23`: Required for non-root privileged shell access.

### Shizuku Setup Guide (Non-Root Wireless Debugging)

1. Install **Shizuku** from Google Play Store or GitHub.
2. Enable **Developer Options** on your Android device (`Settings` → `About Phone` → Tap `Build Number` 7 times).
3. In **Developer Options**, turn on **Wireless Debugging**.
4. Open **Shizuku**, select **Pairing**, and enter the 6-digit Wireless Debugging code.
5. Tap **Start** in Shizuku to bind the ADB service.
6. Launch **Spider GFX Tool v9.0**, navigate to the **Activation Screen**, and grant Shizuku permission.

> **Rooted Devices**: Devices with Magisk or KernelSU can start Shizuku instantly with 1-click superuser rights without Wireless Debugging.

---

## 🧰 Tech Stack & Library Dependencies

| Dependency | Category | Version | Details |
| :--- | :--- | :---: | :--- |
| **Kotlin** | Language | `2.0.0` | Modern idiomatic Kotlin code base |
| **Jetpack Compose BOM** | UI Framework | `2024.09.00+` | Declarative UI with Material3 components |
| **Rikka Shizuku API** | System IPC | `13.1.5` | Non-root ADB shell command execution |
| **OkHttp3** | Networking | `4.12.0` | GitHub release asset & config downloading |
| **Coil Compose** | Image Loading | `2.7.0` | Async icon & image rendering |
| **AndroidX Coroutines** | Async I/O | `2.8.x` | Non-blocking execution & reactive StateFlow |

---

## 📁 Repository Map

```
SpiderGFXTool/
├── app/
│   ├── build.gradle.kts           # Gradle dependencies & Android SDK configs
│   ├── proguard-rules.pro         # R8 shrinker rules
│   └── src/main/
│       ├── AndroidManifest.xml    # System permissions & service declarations
│       └── java/com/example/spidergfxtool/
│           ├── MainActivity.kt               # App lifecycle, cutout setup & navigation
│           ├── SmartEngine.kt                # Shizuku safe IPC execution core
│           ├── GfxBackendManager.kt          # Target package discovery & tweak logic
│           ├── GfxConfigManager.kt           # GitHub 9.0 FPS download & unzipper
│           ├── SmartDnsManager.kt            # Parallel DNS ping latency tester
│           ├── SystemTelemetryViewModel.kt   # Live system telemetry state engine
│           ├── GamingSpaceService.kt         # Full-screen Compose overlay service
│           ├── RealTimeHudService.kt         # Live hardware telemetry HUD overlay
│           ├── CrosshairService.kt           # Draggable custom crosshair overlay
│           ├── BatterySafetyReceiver.kt      # Battery temperature protection watchdog
│           └── ui/                           # Jetpack Compose screen components
│               ├── GfxOverdriveDashboardScreen.kt # Hub target lock & launch screen
│               ├── GfxDashboardScreen.kt          # Resolution & graphic options screen
│               ├── GfxExtremeHudScreen.kt         # HUD overlay & crosshair controls
│               ├── AppHibernationScreen.kt        # RAM sweeper & whitelist manager
│               ├── GfxActivationScreen.kt         # Shizuku activation guide
│               ├── GfxExtraFeaturesScreen.kt      # DNS & audio overdrive features
│               └── components/                    # Custom Cyberpunk design system
└── README.md                              # Open-source documentation
```

---

## 🚀 Building & Compilation

### Requirements
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 11+
- Android SDK Platform 35

### Commands

```bash
# Clone repository
git clone https://github.com/GfxGamerLab/spider-gfx-tool.git
cd spider-gfx-tool

# Assemble Debug APK
./gradlew assembleDebug

# Assemble Release APK (R8 Minified)
./gradlew assembleRelease
```

Compiled outputs are placed in `app/build/outputs/apk/release/app-release.apk`.

---

## ⚠️ Disclaimer & Terms of Use

1. **Third-Party Open Source Utility**: Spider GFX Tool is an independent open-source project. It is not affiliated with, authorized by, or endorsed by Krafton, Tencent, Level Infinite, or PUBG Corporation.
2. **Account Responsibility**: Modifying graphic configurations or aspect ratios must be done in compliance with game terms of service. Users assume all responsibility when applying system-level tweaks.
3. **Thermal Safety**: Unlocking high frame rates increases CPU/GPU power consumption. Built-in thermal safeguards (`BatterySafetyReceiver`) exist to mitigate heat, but users should monitor hardware limits.

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for details.

---
<p align="center">Built for Android Power Users & Mobile Gamers.</p>
