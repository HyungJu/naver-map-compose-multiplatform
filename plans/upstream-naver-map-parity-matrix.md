# Upstream NAVER Map Compose Parity Matrix

## Phase 0 Decisions

- Common shared API stays in `io.github.jude.navermap.compose`.
- We will not promise source-compatible reuse of Android-only upstream types in common code.
- When upstream APIs are Android-shaped, this repo will provide a multiplatform-first shared abstraction and optionally add Android convenience adapters later.
- Location support stays inside `naver-map-compose` for the first parity pass. Split-out to a separate module is deferred unless the API grows enough to justify it.
- Greeting-dialog validation is not part of parity acceptance. Validation is map-focused only.

## Parity Matrix

| Upstream area | Upstream symbol(s) | Local target | Parity mode | Phase | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Core host | `NaverMap` | `NaverMap` | direct | 1 | partial | Shared host now supports `cameraPositionState`, `locale`, `contentPadding`; callbacks/effects are still Phase 2 work. |
| Core properties | `MapProperties` | `MapProperties` | adapted | 1 | partial | Android+iOS both apply extent, zoom/tilt bounds, layer groups, lite/night/indoor, background color, and location tracking mode. |
| UI settings | `MapUiSettings` | `MapUiSettings` | adapted | 1 | partial | Gesture toggles, friction, logo placement, and built-in controls now map on both platforms. |
| Constants | `NaverMapConstants` | `NaverMapConstants` | adapted | 1 | partial | Shared constants exist with KMP-safe defaults instead of leaking native SDK constants directly. |
| Map type | `MapType` | `MapType` | direct | 1 | partial | Shared enum and platform mapping are in place on Android and iOS. |
| Location tracking mode | `LocationTrackingMode` | `LocationTrackingMode` | direct | 1 | partial | Shared enum and platform mapping are in place on Android and iOS. |
| Camera state | `CameraPositionState` | `CameraPositionState` | adapted | 1 | partial | Saver-backed shared state now replaces `NaverMapCameraState`; advanced animation/projection APIs remain. |
| Camera remember helpers | `rememberCameraPositionState`, `currentCameraPositionState` | same names | adapted | 1-2 | partial | `rememberCameraPositionState` landed; `currentCameraPositionState` is still pending. |
| Camera update reason | `CameraUpdateReason` | `CameraUpdateReason` | direct | 1 | partial | Shared enum exists and Android camera listeners populate it; iOS raw camera callbacks land in Phase 2. |
| Raw map effect hooks | `MapEffect`, `DisposableMapEffect` | same names | adapted | 2 | missing | Shared API will expose raw native map through platform bridge. |
| Event callbacks | map click/load/symbol/option/indoor/location callbacks | `NaverMap` callback params | adapted | 2 | missing | Shared callback surface should be multiplatform-first. |
| Marker state | `MarkerState` | `MarkerState` | adapted | 3 | missing | Shared state object with platform overlay backing. |
| Marker overlay | `Marker` | `Marker` | adapted | 3 | missing | Click handling and zoom gating included. |
| Circle overlay | `CircleOverlay` | `CircleOverlay` | adapted | 3 | missing | Radius, fill, outline, z-index, visibility. |
| Polygon overlay | `PolygonOverlay` | `PolygonOverlay` | adapted | 3 | missing | Coordinates, holes, fill/outline, click handling. |
| Polyline overlay | `PolylineOverlay` | `PolylineOverlay` | adapted | 3 | missing | Width, pattern, line cap/join, click handling. |
| Ground overlay | `GroundOverlay` | `GroundOverlay` | adapted | 3 | missing | Image abstraction must be multiplatform-safe. |
| Advanced path overlays | `PathOverlay`, `MultipartPathOverlay`, `ArrowheadPathOverlay` | same names | adapted | 4 | missing | `ColorPart`, caps, joins, elevation, global z-index. |
| Location overlay | `LocationOverlay` | `LocationOverlay` | adapted | 4-5 | missing | Upstream uses Android-only image and `PointF` types; shared wrappers needed. |
| Overlay enums | `Align`, `LineCap`, `LineJoin` | same names | direct | 4 | missing | Shared enums mapped to platform values. |
| Overlay defaults | `MarkerDefaults`, `CircleOverlayDefaults`, `GroundOverlayDefaults`, `PathOverlayDefaults`, `MultipartPathOverlayDefaults`, `PolygonOverlayDefaults`, `PolylineOverlayDefaults`, `ArrowheadPathOverlayDefaults`, `LocationOverlayDefaults` | same names where sensible | adapted | 4 | missing | Some defaults depend on Android-only assets and should be wrapped. |
| Marker composable | `MarkerComposable` | `MarkerComposable` | adapted | 4 | missing | Hardest item; requires Compose-to-image pipeline per platform. |
| Location source helper | `rememberFusedLocationSource` | shared location helper | adapted | 5 | missing | Android-compatible convenience API may coexist with multiplatform helper. |
| Sample feature coverage | upstream demo screens | parity sample screens | adapted | 6 | partial | Greeting flow was removed from the primary sample; current screen validates zoom, map type, traffic, and indoor toggles. |
| Clustering demo path | `DisposableMapEffect`-based clustering sample | sample-only parity target | deferred | 6+ | missing | Not first-class public API, but should remain possible via raw map access. |

## Acceptance For Phase 0

- Every major upstream public API group is mapped as `direct`, `adapted`, or `deferred`.
- The shared package strategy is decided.
- The location module strategy is decided.
- The next implementation phase can proceed without re-litigating the high-level API direction.
