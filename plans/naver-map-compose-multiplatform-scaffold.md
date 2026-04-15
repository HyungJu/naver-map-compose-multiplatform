# Naver Map Compose Multiplatform Scaffold Plan

## Goal

Create a Kotlin Multiplatform library that exposes a Compose-first API for NAVER Map on Android and iOS, plus a sample app that proves the API works end to end on both platforms.

The first milestone should optimize for:

- a clean public API
- a runnable Android sample
- a runnable iOS sample
- publishable library structure
- minimal but real map functionality

## Research Summary

### Compose Multiplatform and project structure

- Current Kotlin Multiplatform guidance separates app entry points from shared code, especially for Android with AGP 9+.
- The shared library module should use the Android KMP library plugin (`com.android.kotlin.multiplatform.library`) rather than mixing `org.jetbrains.kotlin.multiplatform` with the legacy Android application or library plugins in one module.
- Native platform views can be embedded in Compose:
  - Android via `AndroidView`
  - iOS via `UIKitView`

### NAVER Map SDK constraints

- Android:
  - minimum Android version is API 21
  - SDK is published from `https://repository.map.naver.com/archive/maven`
  - current guide shows `com.naver.maps:map-sdk:3.23.2`
  - client key can be configured in `AndroidManifest.xml` or in code
- iOS:
  - minimum iOS version is 9+
  - SDK is distributed by CocoaPods or Swift Package Manager
  - current guide shows CocoaPods package `NMapsMap`
  - client key can be configured in `Info.plist` or in code

### Recommendation from the research

For this library, use:

- one KMP library module for the public Compose API and platform implementations
- one Android sample app module
- one iOS sample app shell
- CocoaPods integration for the iOS NAVER Map dependency in the library module

Why CocoaPods first:

- Kotlin Multiplatform officially supports CocoaPods integration when the KMP module imports iOS pod dependencies
- it reduces risk versus inventing a custom cinterop path on day one
- it matches the official NAVER iOS distribution path

## Proposed Repository Shape

```text
/
  settings.gradle.kts
  build.gradle.kts
  gradle/
  gradle.properties
  gradle/libs.versions.toml
  naver-map-compose/
    build.gradle.kts
    src/
      commonMain/
      commonTest/
      androidMain/
      iosMain/
  sample-android/
    build.gradle.kts
    src/main/
  sample-ios/
    sample-ios.xcodeproj
    sample-ios/
  docs/
  plans/
```

## Module Responsibilities

### `naver-map-compose`

This is the publishable library.

- Declares public Compose API in `commonMain`
- Hosts Android actual implementation in `androidMain`
- Hosts iOS actual implementation in `iosMain`
- Owns state models, camera models, events, and overlay abstractions
- Owns publication setup

### `sample-android`

This is the Android sample app entry point.

- Initializes Android sample app
- Supplies NAVER client key through manifest placeholders or local properties
- Demonstrates the library API
- Becomes the fastest dev loop for early work

### `sample-ios`

This is the iOS sample shell.

- Hosts the Compose view controller exposed by the shared code
- Supplies NAVER client key through `Info.plist` or startup code
- Validates CocoaPods integration and UIKit embedding

## Public API Direction

Start small. Avoid trying to mirror the full native SDKs in v1.

### Initial public surface

- `@Composable fun NaverMap(...)`
- `NaverMapCameraState`
- `CameraPosition`
- `LatLng`
- `MapProperties`
- `MapUiSettings`
- `MarkerState`
- marker click callback
- map click callback
- camera move callback

### API design rules

- Keep common API declarative and Compose-native
- Prefer stable Kotlin data models in `commonMain`
- Hide native SDK types from the public common API
- Use `expect` and `actual` only at platform seams
- Prefer adapters and internal interfaces over exporting platform objects

### First features only

Scope the first release to:

- rendering a map
- setting initial camera position
- controlling camera programmatically
- rendering one or more markers
- basic UI settings
- click callbacks

Defer these until later:

- clustering
- shapes and paths
- custom info windows
- location tracking
- snapshot APIs
- advanced gesture control
- full overlay parity with native SDKs

## Technical Architecture

### `commonMain`

Own the Compose-facing contract:

- composable API
- remembered state objects
- immutable map models
- event contracts
- marker and camera abstractions

Suggested shape:

```kotlin
@Composable
fun NaverMap(
    modifier: Modifier = Modifier,
    cameraState: NaverMapCameraState = rememberNaverMapCameraState(),
    properties: MapProperties = MapProperties(),
    uiSettings: MapUiSettings = MapUiSettings(),
    markers: List<MarkerModel> = emptyList(),
    onMapClick: ((LatLng) -> Unit)? = null,
)
```

### `androidMain`

Implement the map using:

- `AndroidView`
- `com.naver.maps.map.MapView` or `MapFragment` integration

Recommended direction:

- prefer `MapView` wrapping inside `AndroidView` for the library composable
- keep lifecycle wiring explicit and minimal
- translate common models into NAVER Android SDK objects internally

### `iosMain`

Implement the map using:

- `UIKitView`
- `NMFMapView`

Recommended direction:

- construct `NMFMapView` inside `UIKitView`
- keep auth configuration outside the composable where possible
- translate common models into NAVER iOS SDK objects internally

## Build and Tooling Plan

### Gradle and plugin setup

1. Initialize Gradle wrapper and version catalog.
2. Add Kotlin Multiplatform and Compose Multiplatform plugins.
3. Configure the library module with:
   - Kotlin Multiplatform
   - Compose Multiplatform
   - Android KMP library plugin
   - `maven-publish`
4. Configure the Android sample app as a separate app module.
5. Configure CocoaPods for the shared library module so `iosMain` can use `NMapsMap`.

### Versioning strategy

- Put Kotlin, Compose, AGP, and NAVER SDK versions in `libs.versions.toml`
- Keep sample apps consuming the project module, not published artifacts
- Add `group` and `version` from day one so publication tasks stay real

### Local secrets strategy

Do not hardcode NAVER client IDs.

Use:

- `local.properties` for local development
- manifest placeholders for Android sample
- `xcconfig` or build settings for iOS sample

Add example files:

- `local.properties.example`
- `sample-ios/Config/Secrets.example.xcconfig`

## Sample App Plan

The sample should prove both API ergonomics and platform setup.

### Sample screens

- Basic map screen
- Marker demo screen
- Camera control demo screen
- Interaction demo screen

### Shared sample UI

Keep most demo UI in shared Compose code inside the library or a small shared sample package so both platforms exercise the same public API.

### Platform-specific sample responsibilities

- Android:
  - manifest key wiring
  - app theme and activity
- iOS:
  - plist or startup auth wiring
  - host view controller setup

## Milestones

## Phase 1: Project bootstrap

- create root Gradle project
- add version catalog
- create `naver-map-compose` library module
- create `sample-android` app module
- create `sample-ios` Xcode shell
- verify empty sample boots on Android and iOS

Exit criteria:

- Android sample launches
- iOS sample launches
- shared Compose content renders on both platforms

## Phase 2: Native SDK plumbing

- add NAVER Android Maven repository and dependency
- add CocoaPods integration for `NMapsMap`
- wire Android client ID injection
- wire iOS client ID injection
- validate raw native map objects render on both platforms

Exit criteria:

- native NAVER map is visible on Android
- native NAVER map is visible on iOS

## Phase 3: Compose wrapper MVP

- introduce `NaverMap` composable
- add camera state abstraction
- add properties and UI settings models
- add marker model and rendering
- add click callbacks

Exit criteria:

- public API is used by the sample app only through common code
- both platforms support the same MVP feature set

## Phase 4: Developer experience and publication

- add Dokka or basic API docs
- add publication metadata
- add `publishToMavenLocal` validation
- add CI for Android build and common tests
- add CI note that Apple/cinterop publication must run on macOS

Exit criteria:

- library publishes locally
- sample apps build from a clean checkout with documented setup

## Phase 5: Hardening

- stabilize recomposition behavior
- add smoke tests where practical
- document lifecycle and auth expectations
- document unsupported features and roadmap

Exit criteria:

- scaffold is ready for external contributors
- roadmap is explicit and honest

## Risks and Mitigations

### Risk: iOS dependency integration is the hardest part

Mitigation:

- start with CocoaPods because it is the lowest-risk official KMP path
- prove direct access to `NMFMapView` before designing a broad API

### Risk: lifecycle bugs in wrapped native views

Mitigation:

- keep first implementation thin
- add explicit attach and dispose handling
- avoid over-abstracting lifecycle until basic rendering is stable

### Risk: API drift between Android and iOS implementations

Mitigation:

- define the common API from the sample usage first
- add features only when both platforms can support them cleanly

### Risk: overscoping the first release

Mitigation:

- lock MVP to map, camera, markers, and clicks
- move advanced overlays and location features to later milestones

## Suggested First Execution Order

1. Bootstrap a fresh Compose Multiplatform workspace with separate library and sample app modules.
2. Make shared Compose content run on Android and iOS without NAVER SDK integration yet.
3. Add Android NAVER SDK and render a raw native map in `AndroidView`.
4. Add iOS CocoaPods integration and render a raw `NMFMapView` in `UIKitView`.
5. Wrap both native implementations behind a small common `NaverMap` composable.
6. Add camera state and markers.
7. Turn the demos into a proper sample app.
8. Add publication metadata and CI.

## Definition of Done for the Scaffold

The scaffold is successful when:

- a new contributor can clone the repo and understand the module layout quickly
- Android and iOS sample apps both render a NAVER map
- the sample uses the public Compose API rather than platform-native SDK calls directly
- secrets are not committed
- the library can be published locally
- the roadmap for post-scaffold features is documented

## Notes for the Next Implementation Pass

When implementing, prefer proving the vertical slice before polishing:

1. map visible on Android
2. map visible on iOS
3. one common composable API
4. sample app consumes that API

That order will keep the project honest and reduce the chance of building a polished abstraction over an unproven platform integration.

## Official References

- Kotlin Multiplatform: Updating multiplatform projects with Android apps to use AGP 9
  - https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html
- Kotlin Multiplatform: iOS integration methods
  - https://kotlinlang.org/docs/multiplatform/multiplatform-ios-integration-overview.html
- Compose Multiplatform: Integration with the UIKit framework
  - https://kotlinlang.org/docs/multiplatform/compose-uikit-integration.html
- Kotlin Multiplatform: Setting up multiplatform library publication
  - https://kotlinlang.org/docs/multiplatform/multiplatform-publish-lib-setup.html
- NAVER Map Android SDK: 시작하기
  - https://navermaps.github.io/android-map-sdk/guide-ko/1.html
- NAVER Map iOS SDK: 시작하기
  - https://navermaps.github.io/ios-map-sdk/guide-ko/1.html
