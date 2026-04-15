# Upstream NAVER Map Compose Feature Parity Plan

## Goal

Bring `/Users/jude/Code/naver-map-compose-multiplatform` to semantic feature parity with the upstream `fornewid/naver-map-compose` library, while keeping this repo multiplatform-first for Android and iOS.

The old greeting-dialog flow is explicitly out of scope as a required validation target. Completion should be judged by map feature parity and map-focused mobile validation only.

## Upstream Inventory

### Public API groups in upstream

1. Core map host
- `NaverMap`
- `MapProperties`
- `MapUiSettings`
- `NaverMapConstants`
- `MapType`
- `LocationTrackingMode`

2. Camera and state
- `CameraPositionState`
- `rememberCameraPositionState`
- `CameraUpdateReason`
- `currentCameraPositionState`

3. Overlay composition
- `Marker`
- `MarkerComposable`
- `MarkerState`
- `CircleOverlay`
- `PolygonOverlay`
- `PolylineOverlay`
- `GroundOverlay`
- `LocationOverlay`
- `PathOverlay`
- `MultipartPathOverlay`
- `ArrowheadPathOverlay`
- `Align`
- `LineCap`
- `LineJoin`
- `ColorPart`
- overlay defaults objects

4. Events and raw map access
- map click, long click, double tap, two-finger tap
- map loaded, symbol click, option change, indoor selection, location change
- `MapEffect`
- `DisposableMapEffect`

5. Location helper
- `rememberFusedLocationSource`
- companion `naver-map-location` module with `FusedLocationSource`

### Upstream supported feature set from README and sample app

- map types and layer groups
- display options, indoor map, lite mode, night mode, locale
- min/max zoom, max tilt, extent, content padding, control settings, gesture settings
- camera move, animate, fit bounds, pivot, camera event observation
- overlay min/max zoom, global z-index, overlay collision behavior
- clustering via raw-map integration
- location tracking and custom location source flows

## Current Local Status

| Feature group | Status | Notes |
| --- | --- | --- |
| Core map render | Partial | `NaverMap` exists on Android and iOS |
| Common camera state | Partial | only simple `CameraPosition` and mutable `NaverMapCameraState` |
| Map properties | Minimal | only `isIndoorEnabled` |
| UI settings | Minimal | only compass, scale bar, zoom control, location button |
| Map events | Missing | no click/load/symbol/option/indoor/location callbacks |
| Raw map access | Missing | no `MapEffect` or `DisposableMapEffect` |
| Overlay composition | Missing | no marker or overlay composables |
| Location integration | Missing | no location source abstraction or helper module |
| Demo/sample parity | Missing | local sample is still a single screen plus greeting dialog |
| Validation focus | Misaligned | current smoke artifacts are not a reliable map parity gate |

## Key Design Decision Before Implementation

Upstream cannot be copied 1:1 into common KMP API because its public surface includes Android-specific types such as `android.graphics.PointF`, `android.location.Location`, Android `OverlayImage`, and Android `LocationSource`.

Recommended approach:

- Keep a multiplatform-first common API in this repo.
- Introduce common wrapper types for geometry, screen point, camera updates, overlay images, and location payloads.
- Add Android convenience adapters or compatibility overloads so Android users can migrate from upstream with low friction.
- Preserve upstream naming where practical so the mental model stays aligned.

## Implementation Sequence

### Phase 0: Freeze the parity contract

- Write a checked-in parity matrix from upstream API to local target API.
- Decide package strategy: pure local namespace only, or local namespace plus Android compatibility package.
- Decide whether location remains inside `naver-map-compose` or becomes a new KMP `naver-map-location` module.

Acceptance:

- Every upstream public feature is marked as `direct`, `adapted`, or `deferred with reason`.

### Phase 1: Core common model and state

- Expand common types to cover map bounds, map type, location tracking mode, camera update reason, constants, and richer camera position.
- Replace `NaverMapCameraState` with a real `CameraPositionState` equivalent that supports observation, imperative camera updates, and save/restore.
- Add content padding and locale support to the root map composable.

Dependencies:

- common model types
- Android and iOS camera bridge layers

Acceptance:

- both platforms can render a map, apply core properties, and perform camera moves from shared code

### Phase 2: Map host callbacks and effect hooks

- Add shared callback surface for map taps, long presses, double taps, two-finger taps, map loaded, option changes, indoor selection, symbol taps, and location updates.
- Implement `MapEffect` and `DisposableMapEffect` with a platform-specific raw map handle.
- Add `currentCameraPositionState`-style composition access if still needed after API review.

Dependencies:

- delegate/listener bridge on Android `NaverMap`
- delegate bridge on iOS `NMFMapView`

Acceptance:

- map events can be observed from shared Compose code on both platforms

### Phase 3: Basic overlay system

- Build the internal map-node/applier/update infrastructure needed for child overlay composables.
- Implement `Marker`, `MarkerState`, `CircleOverlay`, `PolygonOverlay`, `PolylineOverlay`, and `GroundOverlay`.
- Support common overlay properties: visibility, z-index, tag, min/max zoom, click handling.

Acceptance:

- basic overlays render, update, and respond to clicks on Android and iOS

### Phase 4: Advanced overlays and styling parity

- Implement `PathOverlay`, `MultipartPathOverlay`, `ArrowheadPathOverlay`, `LocationOverlay`.
- Add collision flags, global z-index, line cap/join, color part support, and caption alignment primitives.
- Implement `MarkerComposable` last, because it likely requires Compose-to-image rendering per platform.

Acceptance:

- all upstream overlay categories exist and behave consistently enough for shared samples

### Phase 5: Location parity

- Add a multiplatform location-source abstraction.
- Provide Android fused-location implementation compatible with upstream expectations.
- Provide iOS CoreLocation-backed implementation with matching shared behavior.
- Expose a shared `rememberFusedLocationSource` equivalent, with Android naming compatibility if feasible.

Acceptance:

- shared sample can follow user location and toggle location tracking mode on both platforms

### Phase 6: Sample app and validation harness realignment

- Replace the scaffold sample with a parity sample organized around feature groups: core map, camera, map options, overlays, events, location.
- Remove the greeting dialog from required test coverage.
- Add stable screen labels and controls so the mobile harness can validate map scenarios deterministically.

Acceptance:

- sample app exposes enough UI to manually and automatically verify every major parity group

## Mobile Validation Strategy

Use the local `naver-map-mobile-test-harness` workflow for all user-visible parity checkpoints.

Required harness scenarios:

1. Map render smoke
- verify the sample launches on Android and iOS
- verify live map tiles or a stable map surface are visible
- capture one screenshot per platform

2. Camera parity
- move or zoom the map from shared UI
- verify camera state changes are reflected visually

3. Map options parity
- toggle representative properties and UI settings such as indoor, controls, gestures, map type, and content padding

4. Overlay parity
- render representative marker, polygon, polyline/path, ground, and location overlays
- verify click handling where supported

5. Event and location parity
- validate at least one map tap callback, one map-loaded callback, and one location-tracking flow

Harness rules:

- keep the mother-agent preflight buildability check
- use `mobile-mcp` through the harness, not ad hoc UI driving
- report `passed`, `failed`, or `blocked` with screenshot evidence under `artifacts/mobile`
- do not treat the greeting dialog as required evidence

## Main Risks and Unknowns

- Exact API shape: full source compatibility with upstream is not realistic in common code because upstream types are Android-specific.
- `MarkerComposable` is likely the hardest feature due to cross-platform bitmap generation and lifecycle syncing.
- Some camera/projection/tile-cover APIs may not map perfectly between Android and iOS and may need documented adaptation.
- Location APIs need a KMP design decision: mirror upstream naming, or introduce a cleaner shared abstraction with Android aliases.
- Clustering is sample-supported upstream but not a first-class public API; treat it as secondary to core parity unless product goals say otherwise.

## Done Definition

This plan is complete when:

- every upstream public API group has a local equivalent or an approved documented adaptation
- Android and iOS both expose the same shared feature set for core map, camera, overlays, events, and location
- the parity sample passes harness-based map scenarios on both platforms
- no required validation depends on the old greeting-dialog flow
