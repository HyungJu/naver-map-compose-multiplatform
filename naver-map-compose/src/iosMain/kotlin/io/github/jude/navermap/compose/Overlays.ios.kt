@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.jude.navermap.compose

import androidx.compose.ui.graphics.Color
import cocoapods.NMapsMap.NMFCircleOverlay
import cocoapods.NMapsMap.NMFGroundOverlay
import cocoapods.NMapsMap.NMFMarker
import cocoapods.NMapsMap.NMFOverlay
import cocoapods.NMapsMap.NMFOverlayImage
import cocoapods.NMapsMap.NMFPolygonOverlay
import cocoapods.NMapsMap.NMFPolylineOverlay
import cocoapods.NMapsMap.NMF_MARKER_IMAGE_BLACK
import cocoapods.NMapsMap.NMF_MARKER_IMAGE_BLUE
import cocoapods.NMapsMap.NMF_MARKER_IMAGE_DEFAULT
import cocoapods.NMapsMap.NMF_MARKER_IMAGE_GRAY
import cocoapods.NMapsMap.NMF_MARKER_IMAGE_GREEN
import cocoapods.NMapsMap.NMF_MARKER_IMAGE_LIGHTBLUE
import cocoapods.NMapsMap.NMF_MARKER_IMAGE_PINK
import cocoapods.NMapsMap.NMF_MARKER_IMAGE_RED
import cocoapods.NMapsMap.NMF_MARKER_IMAGE_YELLOW
import cocoapods.NMapsMap.NMGLatLng
import cocoapods.NMapsMap.NMGLatLngBounds
import cocoapods.NMapsMap.NMGLineString
import cocoapods.NMapsMap.NMGPolygon
import platform.UIKit.UIColor

internal actual class PlatformMarkerOverlay(
    val nativeOverlay: NMFMarker,
)

internal actual fun createPlatformMarkerOverlay(handle: PlatformMapHandle): PlatformMarkerOverlay {
    return PlatformMarkerOverlay(NMFMarker())
}

internal actual fun updatePlatformMarkerOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformMarkerOverlay,
    position: LatLng,
    icon: OverlayImage,
    captionText: String,
    alpha: Float,
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    overlay.nativeOverlay.apply {
        this.position = position.toNativeLatLng()
        iconImage = icon.toNativeOverlayImage()
        this.captionText = captionText
        this.alpha = alpha.toDouble()
        applyCommonStyle(handle, style, onClick)
    }
}

internal actual fun disposePlatformMarkerOverlay(overlay: PlatformMarkerOverlay) {
    overlay.nativeOverlay.touchHandler = null
    overlay.nativeOverlay.mapView = null
}

internal actual class PlatformCircleOverlay(
    val nativeOverlay: NMFCircleOverlay,
)

internal actual fun createPlatformCircleOverlay(handle: PlatformMapHandle): PlatformCircleOverlay {
    return PlatformCircleOverlay(NMFCircleOverlay())
}

internal actual fun updatePlatformCircleOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformCircleOverlay,
    center: LatLng,
    radiusMeters: Double,
    fillColor: Color,
    outlineWidth: Float,
    outlineColor: Color,
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    overlay.nativeOverlay.apply {
        this.center = center.toNativeLatLng()
        radius = radiusMeters
        this.fillColor = fillColor.toUIColor()
        this.outlineWidth = outlineWidth.toDouble()
        this.outlineColor = outlineColor.toUIColor()
        applyCommonStyle(handle, style, onClick)
    }
}

internal actual fun disposePlatformCircleOverlay(overlay: PlatformCircleOverlay) {
    overlay.nativeOverlay.touchHandler = null
    overlay.nativeOverlay.mapView = null
}

internal actual class PlatformPolygonOverlay(
    val nativeOverlay: NMFPolygonOverlay,
)

internal actual fun createPlatformPolygonOverlay(handle: PlatformMapHandle): PlatformPolygonOverlay {
    return PlatformPolygonOverlay(NMFPolygonOverlay())
}

internal actual fun updatePlatformPolygonOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformPolygonOverlay,
    coordinates: List<LatLng>,
    fillColor: Color,
    outlineWidth: Float,
    outlineColor: Color,
    outlinePattern: List<Int>,
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    val exteriorRing = NMGLineString.lineStringWithPoints(coordinates.map(LatLng::toNativeLatLng))
    overlay.nativeOverlay.apply {
        polygon = NMGPolygon.polygonWithRing(exteriorRing)
        this.fillColor = fillColor.toUIColor()
        this.outlineWidth = outlineWidth.toULong()
        this.outlineColor = outlineColor.toUIColor()
        this.outlinePattern = outlinePattern.map(Int::toLong)
        applyCommonStyle(handle, style, onClick)
    }
}

internal actual fun disposePlatformPolygonOverlay(overlay: PlatformPolygonOverlay) {
    overlay.nativeOverlay.touchHandler = null
    overlay.nativeOverlay.mapView = null
}

internal actual class PlatformPolylineOverlay(
    val nativeOverlay: NMFPolylineOverlay,
)

internal actual fun createPlatformPolylineOverlay(handle: PlatformMapHandle): PlatformPolylineOverlay {
    return PlatformPolylineOverlay(NMFPolylineOverlay())
}

internal actual fun updatePlatformPolylineOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformPolylineOverlay,
    coordinates: List<LatLng>,
    width: Float,
    color: Color,
    pattern: List<Int>,
    cap: LineCap,
    join: LineJoin,
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    overlay.nativeOverlay.apply {
        line = NMGLineString.lineStringWithPoints(coordinates.map(LatLng::toNativeLatLng))
        this.width = width.toDouble()
        this.color = color.toUIColor()
        this.pattern = pattern.map(Int::toLong)
        applyCommonStyle(handle, style, onClick)
    }
}

internal actual fun disposePlatformPolylineOverlay(overlay: PlatformPolylineOverlay) {
    overlay.nativeOverlay.touchHandler = null
    overlay.nativeOverlay.mapView = null
}

internal actual class PlatformGroundOverlay(
    val nativeOverlay: NMFGroundOverlay,
)

internal actual fun createPlatformGroundOverlay(handle: PlatformMapHandle): PlatformGroundOverlay {
    return PlatformGroundOverlay(NMFGroundOverlay())
}

internal actual fun updatePlatformGroundOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformGroundOverlay,
    bounds: LatLngBounds,
    image: OverlayImage,
    alpha: Float,
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    overlay.nativeOverlay.apply {
        this.bounds = bounds.toNativeBounds()
        overlayImage = image.toNativeOverlayImage()
        this.alpha = alpha.toDouble()
        applyCommonStyle(handle, style, onClick)
    }
}

internal actual fun disposePlatformGroundOverlay(overlay: PlatformGroundOverlay) {
    overlay.nativeOverlay.touchHandler = null
    overlay.nativeOverlay.mapView = null
}

private fun NMFOverlay.applyCommonStyle(
    handle: PlatformMapHandle,
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    userInfo = style.tag?.let { mapOf("tag" to it) } ?: emptyMap<Any?, Any?>()
    hidden = !style.visible
    minZoom = style.minZoom
    isMinZoomInclusive = style.minZoomInclusive
    maxZoom = style.maxZoom
    isMaxZoomInclusive = style.maxZoomInclusive
    zIndex = style.zIndex.toLong()
    globalZIndex = style.globalZIndex.toLong()
    touchHandler = { _ -> onClick() }
    mapView = handle.nativeMap
}

private fun LatLng.toNativeLatLng(): NMGLatLng {
    return NMGLatLng.latLngWithLat(latitude, longitude)
}

private fun LatLngBounds.toNativeBounds(): NMGLatLngBounds {
    return NMGLatLngBounds.latLngBoundsSouthWest(
        southWest = southWest.toNativeLatLng(),
        northEast = northEast.toNativeLatLng(),
    )
}

private fun OverlayImage.toNativeOverlayImage(): NMFOverlayImage {
    return when (this) {
        OverlayImage.DefaultMarker -> NMF_MARKER_IMAGE_DEFAULT
        OverlayImage.BlueMarker -> NMF_MARKER_IMAGE_BLUE
        OverlayImage.GrayMarker -> NMF_MARKER_IMAGE_GRAY
        OverlayImage.GreenMarker -> NMF_MARKER_IMAGE_GREEN
        OverlayImage.LightBlueMarker -> NMF_MARKER_IMAGE_LIGHTBLUE
        OverlayImage.PinkMarker -> NMF_MARKER_IMAGE_PINK
        OverlayImage.RedMarker -> NMF_MARKER_IMAGE_RED
        OverlayImage.YellowMarker -> NMF_MARKER_IMAGE_YELLOW
        OverlayImage.BlackMarker -> NMF_MARKER_IMAGE_BLACK
    }
}

private fun Color.toUIColor(): UIColor {
    return UIColor(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )
}
