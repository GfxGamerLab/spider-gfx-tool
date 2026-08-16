# Implementation Plan - Hardware Safety Watchdog & UI Audio Refinement

This plan implements a "Hardware Safety Watchdog" system to prevent battery state freezing and kernel panics, and finishes the UI/Audio architecture improvements (SoundPool & Risky Tweak indicators).

## Proposed Changes

### [app](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app)

#### [NEW] [BatterySafetyReceiver.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/BatterySafetyReceiver.kt)
- Create a `BroadcastReceiver` that listens for `Intent.ACTION_POWER_CONNECTED` and `Intent.ACTION_POWER_DISCONNECTED`.
- **Connected**: Execute `dumpsys battery reset` via Shizuku and show a Toast.
- **Disconnected**: Check `SharedPreferences` for `key_fake_temp`. If true, re-apply `dumpsys battery set temp 250`.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/AndroidManifest.xml)
- Register `BatterySafetyReceiver` with the appropriate intent filters.

#### [MODIFY] [SoundEffectsManager.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/SoundEffectsManager.kt)
- Ensure `SoundPool` is used for switch toggles (`switch_on`, `switch_off`).
- Use `SoundPool.Builder().setMaxStreams(4).build()`.

#### [MODIFY] [GfxBackendManager.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/GfxBackendManager.kt)
- Implement `executeShizukuSafe(command: String)`:
    - Wraps `Shizuku.newProcess` in a `try-catch`.
    - Logs errors if the command fails or is unsupported by the kernel.
- Update `runShellInternal` to use this safe execution pattern.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/MainActivity.kt)
- In `onCreate()`, call `GfxBackendManager.runMasterReset(this)` to clear ghost states on startup.

#### [MODIFY] [GamingSpaceService.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/GamingSpaceService.kt)
- In `onDestroy()`, call `GfxBackendManager.runMasterReset(this)` to ensure a clean exit.

#### [MODIFY] [GfxExtremeHudScreen.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/ui/GfxExtremeHudScreen.kt)
- Ensure `CyberTweakToggleRow` uses `SoundEffectsManager.playOn()` and `playOff()` correctly in `onCheckedChange`.
- Verify `isRisky = true` is passed for: ZRAM Bypass, Anti-Throttle, LMK Protection, and ART Compiler Turbo.

## Verification Plan

### Automated Tests
- Run `gradlew app:assembleDebug` to verify compilation.

### Manual Verification
- **Watchdog**: Plug/unplug charger and verify battery state reset/re-apply.
- **Audio**: Toggle switches and verify instant `SoundPool` feedback.
- **Risky UI**: Confirm warning indicators are visible on specified tweaks.
