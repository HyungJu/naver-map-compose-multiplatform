@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.jude.navermap.compose

import androidx.compose.ui.graphics.Color
import cocoapods.NMapsMap.NMFArrowheadPath
import cocoapods.NMapsMap.NMFCircleOverlay
import cocoapods.NMapsMap.NMFGroundOverlay
import cocoapods.NMapsMap.NMFLocationOverlay
import cocoapods.NMapsMap.NMFMarker
import cocoapods.NMapsMap.NMFMultipartPath
import cocoapods.NMapsMap.NMFOverlay
import cocoapods.NMapsMap.NMFOverlayImage
import cocoapods.NMapsMap.NMFPath
import cocoapods.NMapsMap.NMFPathColor
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
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.UIKit.UIColor
import platform.UIKit.UIScreen
import kotlin.math.roundToInt

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
        val nativeImage = icon.toNativeOverlayImage()
        iconImage = nativeImage
        width = nativeImage.autoPointWidth()
        height = nativeImage.autoPointHeight()
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
        this.outlineWidth = outlineWidth.toUiPoints()
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
        this.outlineWidth = outlineWidth.toUiPointInt().toULong()
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
        this.width = width.toUiPoints()
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

internal actual class PlatformPathOverlay(
    val nativeOverlay: NMFPath,
)

internal actual fun createPlatformPathOverlay(handle: PlatformMapHandle): PlatformPathOverlay {
    return PlatformPathOverlay(NMFPath())
}

internal actual fun updatePlatformPathOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformPathOverlay,
    coordinates: List<LatLng>,
    progress: Double,
    width: Float,
    outlineWidth: Float,
    color: Color,
    outlineColor: Color,
    passedColor: Color,
    passedOutlineColor: Color,
    patternImage: OverlayImage?,
    patternInterval: Float,
    isHideCollidedSymbols: Boolean,
    isHideCollidedMarkers: Boolean,
    isHideCollidedCaptions: Boolean,
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    overlay.nativeOverlay.apply {
        path = NMGLineString.lineStringWithPoints(coordinates.map(LatLng::toNativeLatLng))
        this.progress = progress
        this.width = width.toUiPoints()
        this.outlineWidth = outlineWidth.toUiPoints()
        this.color = color.toUIColor()
        this.outlineColor = outlineColor.toUIColor()
        this.passedColor = passedColor.toUIColor()
        this.passedOutlineColor = passedOutlineColor.toUIColor()
        patternIcon = patternImage?.toNativeOverlayImage()
        this.patternInterval = patternInterval.toUiPointInt().toULong()
        this.isHideCollidedSymbols = isHideCollidedSymbols
        this.isHideCollidedMarkers = isHideCollidedMarkers
        this.isHideCollidedCaptions = isHideCollidedCaptions
        applyCommonStyle(handle, style, onClick)
    }
}

internal actual fun disposePlatformPathOverlay(overlay: PlatformPathOverlay) {
    overlay.nativeOverlay.touchHandler = null
    overlay.nativeOverlay.mapView = null
}

internal actual class PlatformMultipartPathOverlay(
    val nativeOverlay: NMFMultipartPath,
)

internal actual fun createPlatformMultipartPathOverlay(handle: PlatformMapHandle): PlatformMultipartPathOverlay {
    return PlatformMultipartPathOverlay(NMFMultipartPath())
}

internal actual fun updatePlatformMultipartPathOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformMultipartPathOverlay,
    coordinateParts: List<List<LatLng>>,
    colorParts: List<ColorPart>,
    progress: Double,
    width: Float,
    outlineWidth: Float,
    patternImage: OverlayImage?,
    patternInterval: Float,
    isHideCollidedSymbols: Boolean,
    isHideCollidedMarkers: Boolean,
    isHideCollidedCaptions: Boolean,
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    overlay.nativeOverlay.apply {
        lineParts = coordinateParts.map { part ->
            NMGLineString.lineStringWithPoints(part.map(LatLng::toNativeLatLng))
        }
        this.colorParts = colorParts.map(ColorPart::toNativePathColor)
        this.progress = progress
        this.width = width.toUiPoints()
        this.outlineWidth = outlineWidth.toUiPoints()
        patternIcon = patternImage?.toNativeOverlayImage()
        this.patternInterval = patternInterval.toUiPointInt().toULong()
        this.isHideCollidedSymbols = isHideCollidedSymbols
        this.isHideCollidedMarkers = isHideCollidedMarkers
        this.isHideCollidedCaptions = isHideCollidedCaptions
        applyCommonStyle(handle, style, onClick)
    }
}

internal actual fun disposePlatformMultipartPathOverlay(overlay: PlatformMultipartPathOverlay) {
    overlay.nativeOverlay.touchHandler = null
    overlay.nativeOverlay.mapView = null
}

internal actual class PlatformArrowheadPathOverlay(
    val nativeOverlay: NMFArrowheadPath,
)

internal actual fun createPlatformArrowheadPathOverlay(handle: PlatformMapHandle): PlatformArrowheadPathOverlay {
    return PlatformArrowheadPathOverlay(NMFArrowheadPath())
}

internal actual fun updatePlatformArrowheadPathOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformArrowheadPathOverlay,
    coordinates: List<LatLng>,
    width: Float,
    headSizeRatio: Float,
    color: Color,
    outlineWidth: Float,
    outlineColor: Color,
    elevation: Float,
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    overlay.nativeOverlay.apply {
        points = coordinates.map(LatLng::toNativeLatLng)
        this.width = width.toUiPoints()
        this.headSizeRatio = headSizeRatio.toDouble()
        this.color = color.toUIColor()
        this.outlineWidth = outlineWidth.toUiPoints()
        this.outlineColor = outlineColor.toUIColor()
        this.elevation = elevation.toUiPoints()
        applyCommonStyle(handle, style, onClick)
    }
}

internal actual fun disposePlatformArrowheadPathOverlay(overlay: PlatformArrowheadPathOverlay) {
    overlay.nativeOverlay.touchHandler = null
    overlay.nativeOverlay.mapView = null
}

internal actual class PlatformLocationOverlay(
    val nativeOverlay: NMFLocationOverlay,
)

internal actual fun createPlatformLocationOverlay(handle: PlatformMapHandle): PlatformLocationOverlay {
    return PlatformLocationOverlay(handle.nativeMap.locationOverlay)
}

internal actual fun updatePlatformLocationOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformLocationOverlay,
    position: LatLng,
    bearing: Float,
    icon: OverlayImage,
    iconWidth: Float?,
    iconHeight: Float?,
    iconAlpha: Float,
    anchor: AnchorPoint,
    subIcon: OverlayImage?,
    subIconWidth: Float?,
    subIconHeight: Float?,
    subIconAlpha: Float,
    subAnchor: AnchorPoint,
    circleRadius: Float,
    circleColor: Color,
    circleOutlineWidth: Float,
    circleOutlineColor: Color,
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    overlay.nativeOverlay.apply {
        location = position.toNativeLatLng()
        heading = bearing.toDouble()
        val nativeIcon = icon.toNativeOverlayImage()
        this.icon = nativeIcon
        this.iconWidth = iconWidth?.toUiPoints() ?: nativeIcon.autoPointWidth()
        this.iconHeight = iconHeight?.toUiPoints() ?: nativeIcon.autoPointHeight()
        this.iconAlpha = iconAlpha.toDouble()
        this.anchor = anchor.toNativeAnchor()
        val nativeSubIcon = subIcon?.toNativeOverlayImage()
        this.subIcon = nativeSubIcon
        this.subIconWidth = subIconWidth?.toUiPoints() ?: nativeSubIcon?.autoPointWidth() ?: 0.0
        this.subIconHeight = subIconHeight?.toUiPoints() ?: nativeSubIcon?.autoPointHeight() ?: 0.0
        this.subIconAlpha = subIconAlpha.toDouble()
        this.subAnchor = subAnchor.toNativeAnchor()
        this.circleRadius = circleRadius.toUiPoints()
        this.circleColor = circleColor.toUIColor()
        this.circleOutlineWidth = circleOutlineWidth.toUiPoints()
        this.circleOutlineColor = circleOutlineColor.toUIColor()
        applyCommonStyle(handle, style, onClick, attachToMap = false)
        hidden = !style.visible
    }
}

internal actual fun disposePlatformLocationOverlay(overlay: PlatformLocationOverlay) {
    overlay.nativeOverlay.touchHandler = null
    overlay.nativeOverlay.hidden = true
}

private fun NMFOverlay.applyCommonStyle(
    handle: PlatformMapHandle,
    style: OverlayStyle,
    onClick: () -> Boolean,
    attachToMap: Boolean = true,
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
    if (attachToMap) {
        mapView = handle.nativeMap
    }
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

private fun AnchorPoint.toNativeAnchor() = CGPointMake(x.toDouble(), y.toDouble())

private fun Float.toUiPoints(): Double = (this / UIScreen.mainScreen.scale.toFloat()).toDouble()

private fun Float.toUiPointInt(): Int = (this / UIScreen.mainScreen.scale.toFloat()).roundToInt()

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
        OverlayImage.LocationDefault -> NMFLocationOverlay.defaultIconImage()
    }
}

private fun NMFOverlayImage.autoPointWidth(): Double = image.size.useContents { width }

private fun NMFOverlayImage.autoPointHeight(): Double = image.size.useContents { height }

private fun ColorPart.toNativePathColor(): NMFPathColor {
    return NMFPathColor.pathColorWithColor(
        color = color.toUIColor(),
        outlineColor = outlineColor.toUIColor(),
        passedColor = passedColor.toUIColor(),
        passedOutlineColor = passedOutlineColor.toUIColor(),
    )
}

private fun Color.toUIColor(): UIColor {
    return UIColor(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )
}
