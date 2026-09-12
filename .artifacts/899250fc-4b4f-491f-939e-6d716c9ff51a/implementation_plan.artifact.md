# Implementation Plan - Fix tacticalPress Argument Mismatch

Fix the build error in `GfxExtremeHudScreen.kt` where the compiler fails to correctly match the `onClick` argument in the `tacticalPress` modifier call.

## User Review Required

> [!NOTE]
> The error `Argument type mismatch: actual type is 'Function0<Unit>', but 'MutableInteractionSource?' was expected` typically occurs when a lambda is passed as a positional argument to a function where the first parameter is not a function type. While the code currently uses named arguments in some views, switching to a trailing lambda is the standard and most robust approach in Jetpack Compose.

## Proposed Changes

### UI Components

#### [MODIFY] [GfxExtremeHudScreen.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/ui/GfxExtremeHudScreen.kt)
#### [MODIFY] [GfxDashboardScreen.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/ui/GfxDashboardScreen.kt)

Change the `tacticalPress` call in both files to use a trailing lambda for the `onClick` action. This ensures the compiler correctly identifies the argument.

**Example Change:**
```diff
-            .tacticalPress(onClick = onClick)
+            .tacticalPress { onClick() }
```

## Verification Plan

### Automated Tests
- Execute build command:
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
