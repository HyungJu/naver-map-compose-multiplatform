# MarkerComposable Plan

## Goal

Implement `MarkerComposable` for `/Users/jude/Code/naver-map-compose-multiplatform` in a way that preserves native NAVER marker behavior on both Android and iOS.

This plan is intentionally about `MarkerComposable`, not a general `OverlayComposable`.

## Decision Summary

- Recommended direction: render the composable content into an image, then feed that image into the native marker icon.
- Do not implement `MarkerComposable` as a live Compose layer projected above the map.
- Keep a future `OverlayComposable` discussion separate. It solves a different problem and has different tradeoffs.

## Why Image Capture Is The Right Fit

`MarkerComposable` should still behave like a native marker.

That means we want these behaviors to remain native:

- marker collision handling
- marker click dispatch
- min/max zoom visibility
- global z-index ordering
- flat / perspective behavior
- correct movement with camera pan, tilt, and rotation
- stable native sizing semantics during zoom

If we draw a Compose layer above the map with projection math, we lose or degrade several of those native semantics. We would effectively be building a different feature: a projected UI overlay.

For `MarkerComposable`, the image-capture path is closer to upstream and much safer.

## Alternatives Considered

### Option A: Composable -> image -> native marker

Status: adopt

How it works:

- render the composable off-screen
- capture it as a bitmap / image
- convert that into the platform marker icon type
- assign it to the native marker

Strengths:

- preserves native marker semantics
- matches upstream mental model
- simpler click model because native marker click stays in charge
- no per-frame projection updates needed
- easier to keep Android and iOS behavior aligned

Weaknesses:

- the composable is snapshot-based, not interactive
- re-rendering must be controlled carefully to avoid churn
- requires separate Android and iOS off-screen rendering pipelines

### Option B: Live Compose overlay projected above the map

Status: reject for `MarkerComposable`

Why it is attractive:

- easier to keep the content "live"
- can support arbitrary Compose UI and pointer input later

Why it is wrong for this feature:

- does not behave like a native marker
- collision handling with other native overlays becomes unreliable
- click precedence with native overlays is different
- flat / perspective / camera semantics do not match native marker behavior
- requires continuous reprojection during camera movement

This is a possible future direction for a separate `OverlayComposable`, but it should not be the first implementation of `MarkerComposable`.

### Option C: Use native SDK view snapshot APIs directly

Status: partial helper, not the main design

Notes:

- Android upstream already uses a `ComposeView` and snapshots it into a bitmap.
- iOS `NMFOverlayImage` can be built from `UIImage`.
- iOS also exposes `NMFOverlayImageDataSource`, but that path is for APIs like info windows, not for `NMFMarker.iconImage` itself.

Conclusion:

- we still need an explicit image generation path for markers

## Architectural Direction

### Shared API direction

Target API should stay close to upstream:

```kotlin
@Composable
fun MarkerComposable(
    vararg keys: Any?,
    state: MarkerState = rememberUpdatedMarkerState(),
    alpha: Float = 1f,
    captionText: String = "",
    style: OverlayStyle = OverlayStyle(globalZIndex = MarkerDefaults.GlobalZIndex),
    onClick: () -> Boolean = { false },
    content: @Composable () -> Unit,
)
```

We may start with the smaller subset above, then expand toward upstream marker options after the core rendering path is stable.

### Internal representation

Current `OverlayImage` is an enum, which is fine for built-in SDK images but not enough for runtime-rendered marker content.

Recommended internal path:

- evolve `OverlayImage` into a sealed hierarchy, or
- add an internal rendered-image subtype that platform code can recognize

The important requirement is this:

- `MarkerComposable` must be able to pass a runtime-generated image through the same marker update pipeline as built-in marker icons

### Rendering lifecycle

The rendering contract should follow upstream intent:

- `keys` define when the composable image should be regenerated
- theme and composition locals should be inherited from the parent composition
- if content measures to zero width or height, fail with a Korean error message that explains the cause

We should document that `MarkerComposable` is snapshot-based:

- pointer input inside the captured content is not interactive
- dynamic appearance updates require recomposition with changed keys

## Platform Plan

### Android plan

Use the upstream approach as the baseline:

- create an off-screen `ComposeView`
- set the parent composition context
- measure and lay out the content
- draw into a `Bitmap`
- convert with `OverlayImage.fromBitmap(bitmap)`

Notes:

- reuse upstream ideas, but keep our own code style and Korean errors
- avoid regenerating images unless `keys` or required environment values change

### iOS plan

Build the equivalent pipeline for `UIImage`:

- create an off-screen Compose-backed UIKit host
- render the composable with inherited composition context
- measure to its intrinsic content size
- snapshot into `UIImage`
- convert with `NMFOverlayImage.overlayImageWithImage(image, reuseIdentifier)`

Open question to resolve in the implementation spike:

- the cleanest way to inherit Compose composition context for off-screen iOS rendering

This is the highest-risk technical area and should be isolated first.

## Phased Implementation Plan

### Phase A: Rendering spike

- confirm Android rendered marker image pipeline in our codebase
- confirm iOS off-screen Compose capture feasibility
- prove that both platforms can produce a non-zero image from a simple composable chip

Acceptance:

- both platforms can produce a runtime marker icon from shared composable content

### Phase B: Shared image plumbing

- introduce internal runtime marker image support
- preserve current built-in `OverlayImage` behavior
- keep existing `Marker` API working unchanged

Acceptance:

- current marker APIs still work
- runtime-rendered marker image can flow through the marker update pipeline

### Phase C: `MarkerComposable` shared API

- add the new composable API
- connect `keys`, `state`, click handling, alpha, caption, and style
- keep behavior aligned with existing `Marker` semantics

Acceptance:

- `MarkerComposable` works for a simple shared sample on Android and iOS

### Phase D: Sample and docs

- add a sample marker that clearly shows captured Compose content
- document snapshot semantics and `keys` usage
- document current scope and any intentionally deferred upstream options

Acceptance:

- sample app demonstrates `MarkerComposable` clearly

### Phase E: Validation

- run the required preflight builds
- run `$naver-map-mobile-test-harness` at `.codex/skills/naver-map-mobile-test-harness/SKILL.md`
- verify Android and iOS both show the composable marker correctly
- verify marker click still works
- verify zoom and pan keep marker behavior native
- collect screenshot evidence under `artifacts/mobile`

Acceptance:

- harness result is `passed` on both platforms before the feature is presented as finished

## Risks

- iOS off-screen Compose capture may require extra lifecycle handling
- image regeneration can cause memory churn if `keys` are unstable
- zero-size composables need a clear failure path
- font loading or async image loading inside captured content may need explicit constraints in v1
- converting `OverlayImage` away from a simple enum touches multiple overlay types, so the internal migration should be narrow and deliberate

## Non-Goals For The First Pass

- fully interactive Compose UI inside a marker
- a general-purpose `OverlayComposable`
- immediate parity with every upstream `MarkerComposable` parameter on day one
- solving cluster rendering in the same step

## Practical Recommendation

Build `MarkerComposable` first as:

- snapshot-based
- native-marker-backed
- minimal parameter surface
- Android and iOS both proven with the same shared sample

Then expand toward full upstream option parity after the rendering backbone is stable.
