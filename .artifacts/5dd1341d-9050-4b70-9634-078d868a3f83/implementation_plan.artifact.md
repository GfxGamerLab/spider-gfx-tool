# Implementation Plan - Hub 'LAUNCH GAME' Button, Terminal Output UX & Dynamic Target Management

## Problem Statement
1. Need a prominent **LAUNCH GAME** button in the Hub section under the Target Lock area that launches the selected game via `PackageManager.getLaunchIntentForPackage()`, showing an "App not installed" Toast if the intent is null.
2. Terminal output displays raw shell return strings like `'[OUTPUT] Command executed safely: null'`. Need to replace empty/`null` strings with `'Optimization applied successfully.'` and color-code log prefixes in bright green (`#00FF00` for `[SUCCESS]`) and cyan (`#00FFFF` for `[SYSTEM]` and `[INFO]`).
3. Fix hardcoded package name issue by ensuring `getAutoDetectedGamePackage(context)` always reads the active `key_selected_target_pkg` from `SharedPreferences` (saved whenever a game is selected in Hub or Dashboard), replacing all default `com.tencent.ig` references dynamically across System Engine tweaks.

## Proposed Changes

### 1. Add 'LAUNCH GAME' Button in Hub (`GfxOverdriveDashboardScreen.kt`)
#### [MODIFY] [GfxOverdriveDashboardScreen.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/ui/GfxOverdriveDashboardScreen.kt)
- Add a prominent Surface/Button labeled **LAUNCH GAME** directly below the TARGET LOCK area.
- Click handler:
  ```kotlin
  val launchIntent = context.packageManager.getLaunchIntentForPackage(selectedPackageName)
  if (launchIntent != null) {
      launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(launchIntent)
  } else {
      Toast.makeText(context, "App not installed", Toast.LENGTH_SHORT).show()
  }
  ```

### 2. Beginner-Friendly Terminal Output & Color Prefixing (`SmartEngine.kt` & `CyberComponents.kt`)
#### [MODIFY] [SmartEngine.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/SmartEngine.kt)
- Update `sanitizeShizukuOutput` and `runAiAgentTweak`:
  - If output is `null`, `"null"`, empty, or contains `"Command executed safely: null"`, convert to `'Optimization applied successfully.'`.

#### [MODIFY] [CyberComponents.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/ui/components/CyberComponents.kt)
- Enhance `TypewriterText` to render `AnnotatedString` where:
  - `[SUCCESS]` is styled in bright green (`#00FF00`)
  - `[SYSTEM]` and `[INFO]` are styled in cyan (`#00FFFF`)
  - `[ERROR]` and `[WARNING]` are styled in crimson (`#FF1744`)

### 3. Dynamic Package Target Management (`GfxBackendManager.kt` & `GfxDashboardScreen.kt`)
#### [MODIFY] [GfxBackendManager.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/GfxBackendManager.kt)
- Update `getAutoDetectedGamePackage(context)` to check `spider_gfx_prefs` `key_selected_target_pkg` first. If set, return that package name (e.g. `com.dts.freefireth`); otherwise fallback to auto-detection or default.

#### [MODIFY] [GfxDashboardScreen.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/ui/GfxDashboardScreen.kt)
- Save selected package name to `spider_gfx_prefs` `key_selected_target_pkg` when a game chip is selected.

## Verification Plan
### Automated Tests
- Run `gradle_build` (`app:assembleDebug`) to verify compilation.
