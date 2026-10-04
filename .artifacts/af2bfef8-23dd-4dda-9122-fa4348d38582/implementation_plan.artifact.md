# Implementation Plan - Advanced Floating Overlays (v0.7 Revamp)

Revamp the floating overlay system to include a "Drag-to-Delete" mechanic, live system telemetry (FPS & Thermals), and real-time state synchronization with the app's UI.

## User Review Required

> [!IMPORTANT]
> The new "Drag-to-Delete" mechanic will introduce a red trash zone at the bottom of the screen whenever an overlay is being moved. Releasing the overlay in this zone will stop the service and update the app's toggle state.

## Proposed Changes

### Core State Management

#### [NEW] [OverlayStateManager.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/OverlayStateManager.kt)
- Singleton to hold `StateFlow` for `isCrosshairActive` and `isHudActive`.
- Used by Services to signal their death and by Compose UI to stay in sync.

### Resources

#### [NEW] [ic_crosshair_v7.xml](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/res/drawable/ic_crosshair_v7.xml)
- A clean, high-visibility vector drawable for the crosshair.

### Services

#### [NEW] [CrosshairService.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/CrosshairService.kt)
- Lightweight service using an `ImageView` with `ic_crosshair_v7`.
- Implements "Drag-to-Delete" with the bottom Trash Zone.
- Syncs state via `OverlayStateManager`.

#### [NEW] [RealTimeHudService.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/RealTimeHudService.kt)
- Revamped HUD service.
- **FPS Engine**: Uses `Choreographer.FrameCallback` for accurate real-time FPS calculation.
- **Thermal Engine**: Uses `BatteryManager` broadcast and `/sys/class/thermal` fallback for real-time temperature.
- Implements "Drag-to-Delete".
- Syncs state via `OverlayStateManager`.

### UI Integration

#### [MODIFY] [GfxExtremeHudScreen.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/ui/GfxExtremeHudScreen.kt)
- Update "REAL-TIME HUD" and "SMART FLOATING OVERLAY" toggles to collect state from `OverlayStateManager`.
- Ensure the switches turn OFF when services are killed via drag-to-delete.

### Manifest

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/AndroidManifest.xml)
- Register `CrosshairService` and `RealTimeHudService`.
- Remove legacy service entries if necessary.

## Verification Plan

### Manual Verification
- Start both services from the app.
- Drag each overlay:
    - Verify the Trash Zone (Red 'X') appears at the bottom center.
    - Verify the Trash Zone scales up when the overlay hovers over it.
    - Release over the Trash Zone: Verify the service stops and the app's switch turns OFF.
- Check Real-Time HUD:
    - Verify FPS changes dynamically (not static 60).
    - Verify CPU/Battery temp updates in real-time.
- Check Crosshair:
    - Verify it is a clean transparent icon without borders or text.
