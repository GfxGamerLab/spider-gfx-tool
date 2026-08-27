# Implementation Plan - Global Community Footer Integration

This plan integrates a high-quality "Tutorial & Social Links Footer" across the main screens of the Spider GFX Tool, featuring video thumbnails and stylized social buttons.

## Proposed Changes

### [app](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app)

#### [MODIFY] [build.gradle.kts](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/build.gradle.kts)
- Add `io.coil-kt:coil-compose:2.7.0` to dependencies for image loading.

#### [MODIFY] [CyberComponents.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/ui/components/CyberComponents.kt)
- **New Composable**: `GlobalCommunityFooter`.
    - **Video Tutorials**: Two `Card`s with `AsyncImage` thumbnails for English and Hindi/Urdu tutorials.
    - **Social Links**: "JOIN OUR COMMUNITY" header with a `Row` of three buttons for YouTube, Telegram, and WhatsApp.
    - **Logic**: Use `Intent.ACTION_VIEW` for all external links.

#### [MODIFY] [GfxActivationScreen.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/ui/GfxActivationScreen.kt)
- Append `GlobalCommunityFooter()` at the end of the scrollable `Column`.

#### [MODIFY] [GfxDashboardScreen.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/ui/GfxDashboardScreen.kt)
- Append `GlobalCommunityFooter()` at the end of the scrollable `Column`.

#### [MODIFY] [GfxExtremeHudScreen.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/ui/GfxExtremeHudScreen.kt)
- Append `GlobalCommunityFooter()` at the end of the scrollable `Column` in both Dashboard and Detail views (or just Dashboard depending on design preference, but request says "at the bottom").

#### [MODIFY] [GfxExtraFeaturesScreen.kt](file:///C:/Users/123/AndroidStudioProjects/SpiderGFXTool/app/src/main/java/com/example/spidergfxtool/ui/GfxExtraFeaturesScreen.kt)
- Append `GlobalCommunityFooter()` at the end of the scrollable `Column`.

## Verification Plan

### Automated Tests
- Run `gradlew app:assembleDebug` to verify compilation and dependency resolution.

### Manual Verification
- **UI Audit**: Navigate to each of the 4 screens and scroll to the bottom to verify:
    - Thumbnails load correctly via Coil.
    - Text labels match the requested strings.
    - "JOIN OUR COMMUNITY" section is styled correctly.
- **Intent Testing**: Click each tutorial card and social button to ensure they launch the correct YouTube videos or social channels.
