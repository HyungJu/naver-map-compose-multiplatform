package io.github.hyungju.navermap.compose

import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.naver.maps.geometry.LatLngBounds as AndroidLatLngBounds
import com.naver.maps.map.overlay.ArrowheadPathOverlay
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.GroundOverlay
import com.naver.maps.map.overlay.LocationOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.MultipartPathOverlay
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage as AndroidOverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.overlay.PolygonOverlay
import com.naver.maps.map.overlay.PolylineOverlay
import com.naver.maps.map.util.MarkerIcons

internal actual class PlatformMarkerOverlay(
    val nativeOverlay: Marker,
)

internal actual fun createPlatformMarkerOverlay(handle: PlatformMapHandle): PlatformMarkerOverlay {
    return PlatformMarkerOverlay(Marker())
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
        this.position = position.toAndroidLatLng()
        this.icon = icon.toAndroidOverlayImage()
        this.captionText = captionText
        this.alpha = alpha
        applyCommonStyle(style, onClick)
        map = handle.nativeMap
    }
}

internal actual fun disposePlatformMarkerOverlay(overlay: PlatformMarkerOverlay) {
    overlay.nativeOverlay.onClickListener = null
    overlay.nativeOverlay.map = null
}

internal actual class PlatformCircleOverlay(
    val nativeOverlay: CircleOverlay,
)

internal actual fun createPlatformCircleOverlay(handle: PlatformMapHandle): PlatformCircleOverlay {
    return PlatformCircleOverlay(CircleOverlay())
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
        this.center = center.toAndroidLatLng()
        this.radius = radiusMeters
        color = fillColor.toArgb()
        this.outlineWidth = outlineWidth.toInt()
        this.outlineColor = outlineColor.toArgb()
        applyCommonStyle(style, onClick)
        map = handle.nativeMap
    }
}

internal actual fun disposePlatformCircleOverlay(overlay: PlatformCircleOverlay) {
    overlay.nativeOverlay.onClickListener = null
    overlay.nativeOverlay.map = null
}

internal actual class PlatformPolygonOverlay(
    val nativeOverlay: PolygonOverlay,
)

internal actual fun createPlatformPolygonOverlay(handle: PlatformMapHandle): PlatformPolygonOverlay {
    return PlatformPolygonOverlay(PolygonOverlay())
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
    overlay.nativeOverlay.apply {
        coords = coordinates.map(LatLng::toAndroidLatLng)
        color = fillColor.toArgb()
        this.outlineWidth = outlineWidth.toInt()
        this.outlineColor = outlineColor.toArgb()
        setOutlinePattern(*outlinePattern.toIntArray())
        applyCommonStyle(style, onClick)
        map = handle.nativeMap
    }
}

internal actual fun disposePlatformPolygonOverlay(overlay: PlatformPolygonOverlay) {
    overlay.nativeOverlay.onClickListener = null
    overlay.nativeOverlay.map = null
}

internal actual class PlatformPolylineOverlay(
    val nativeOverlay: PolylineOverlay,
)

internal actual fun createPlatformPolylineOverlay(handle: PlatformMapHandle): PlatformPolylineOverlay {
    return PlatformPolylineOverlay(PolylineOverlay())
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
        coords = coordinates.map(LatLng::toAndroidLatLng)
        this.width = width.toInt()
        this.color = color.toArgb()
        setPattern(*pattern.toIntArray())
        capType = cap.toAndroidLineCap()
        joinType = join.toAndroidLineJoin()
        applyCommonStyle(style, onClick)
        map = handle.nativeMap
    }
}

internal actual fun disposePlatformPolylineOverlay(overlay: PlatformPolylineOverlay) {
    overlay.nativeOverlay.onClickListener = null
    overlay.nativeOverlay.map = null
}

internal actual class PlatformGroundOverlay(
    val nativeOverlay: GroundOverlay,
)

internal actual fun createPlatformGroundOverlay(handle: PlatformMapHandle): PlatformGroundOverlay {
    return PlatformGroundOverlay(GroundOverlay())
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
        this.bounds = bounds.toAndroidBounds()
        this.image = image.toAndroidOverlayImage()
        this.alpha = alpha
        applyCommonStyle(style, onClick)
        map = handle.nativeMap
    }
}

internal actual fun disposePlatformGroundOverlay(overlay: PlatformGroundOverlay) {
    overlay.nativeOverlay.onClickListener = null
    overlay.nativeOverlay.map = null
}

internal actual class PlatformPathOverlay(
    val nativeOverlay: PathOverlay,
)

internal actual fun createPlatformPathOverlay(handle: PlatformMapHandle): PlatformPathOverlay {
    return PlatformPathOverlay(PathOverlay())
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
        coords = coordinates.map(LatLng::toAndroidLatLng)
        this.progress = progress
        this.width = width.toInt()
        this.outlineWidth = outlineWidth.toInt()
        this.color = color.toArgb()
        this.outlineColor = outlineColor.toArgb()
        this.passedColor = passedColor.toArgb()
        this.passedOutlineColor = passedOutlineColor.toArgb()
        this.patternImage = patternImage?.toAndroidOverlayImage()
        this.patternInterval = patternInterval.toInt()
        this.isHideCollidedSymbols = isHideCollidedSymbols
        this.isHideCollidedMarkers = isHideCollidedMarkers
        this.isHideCollidedCaptions = isHideCollidedCaptions
        applyCommonStyle(style, onClick)
        map = handle.nativeMap
    }
}

internal actual fun disposePlatformPathOverlay(overlay: PlatformPathOverlay) {
    overlay.nativeOverlay.onClickListener = null
    overlay.nativeOverlay.map = null
}

internal actual class PlatformMultipartPathOverlay(
    val nativeOverlay: MultipartPathOverlay,
)

internal actual fun createPlatformMultipartPathOverlay(handle: PlatformMapHandle): PlatformMultipartPathOverlay {
    return PlatformMultipartPathOverlay(MultipartPathOverlay())
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
        coordParts = coordinateParts.map { part -> part.map(LatLng::toAndroidLatLng) }
        this.colorParts = colorParts.map(ColorPart::toAndroidColorPart)
        this.progress = progress
        this.width = width.toInt()
        this.outlineWidth = outlineWidth.toInt()
        this.patternImage = patternImage?.toAndroidOverlayImage()
        this.patternInterval = patternInterval.toInt()
        this.isHideCollidedSymbols = isHideCollidedSymbols
        this.isHideCollidedMarkers = isHideCollidedMarkers
        this.isHideCollidedCaptions = isHideCollidedCaptions
        applyCommonStyle(style, onClick)
        map = handle.nativeMap
    }
}

internal actual fun disposePlatformMultipartPathOverlay(overlay: PlatformMultipartPathOverlay) {
    overlay.nativeOverlay.onClickListener = null
    overlay.nativeOverlay.map = null
}

internal actual class PlatformArrowheadPathOverlay(
    val nativeOverlay: ArrowheadPathOverlay,
)

internal actual fun createPlatformArrowheadPathOverlay(handle: PlatformMapHandle): PlatformArrowheadPathOverlay {
    return PlatformArrowheadPathOverlay(ArrowheadPathOverlay())
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
        coords = coordinates.map(LatLng::toAndroidLatLng)
        this.width = width.toInt()
        this.headSizeRatio = headSizeRatio
        this.color = color.toArgb()
        this.outlineWidth = outlineWidth.toInt()
        this.outlineColor = outlineColor.toArgb()
        this.elevation = elevation.toInt()
        applyCommonStyle(style, onClick)
        map = handle.nativeMap
    }
}

internal actual fun disposePlatformArrowheadPathOverlay(overlay: PlatformArrowheadPathOverlay) {
    overlay.nativeOverlay.onClickListener = null
    overlay.nativeOverlay.map = null
}

internal actual class PlatformLocationOverlay(
    val nativeOverlay: LocationOverlay,
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
        this.position = position.toAndroidLatLng()
        this.bearing = bearing
        this.icon = icon.toAndroidOverlayImage()
        this.iconWidth = iconWidth?.toInt() ?: LocationOverlay.SIZE_AUTO
        this.iconHeight = iconHeight?.toInt() ?: LocationOverlay.SIZE_AUTO
        this.iconAlpha = iconAlpha
        this.anchor = anchor.toAndroidPointF()
        this.subIcon = subIcon?.toAndroidOverlayImage()
        this.subIconWidth = subIconWidth?.toInt() ?: LocationOverlay.SIZE_AUTO
        this.subIconHeight = subIconHeight?.toInt() ?: LocationOverlay.SIZE_AUTO
        this.subIconAlpha = subIconAlpha
        this.subAnchor = subAnchor.toAndroidPointF()
        this.circleRadius = circleRadius.toInt()
        this.circleColor = circleColor.toArgb()
        this.circleOutlineWidth = circleOutlineWidth.toInt()
        this.circleOutlineColor = circleOutlineColor.toArgb()
        applyCommonStyle(style, onClick, attachToMap = false)
        isVisible = style.visible
    }
}

internal actual fun disposePlatformLocationOverlay(overlay: PlatformLocationOverlay) {
    overlay.nativeOverlay.onClickListener = null
    overlay.nativeOverlay.isVisible = false
}

private fun Overlay.applyCommonStyle(
    style: OverlayStyle,
    onClick: () -> Boolean,
    attachToMap: Boolean = true,
) {
    tag = style.tag
    isVisible = style.visible
    minZoom = style.minZoom
    isMinZoomInclusive = style.minZoomInclusive
    maxZoom = style.maxZoom
    isMaxZoomInclusive = style.maxZoomInclusive
    zIndex = style.zIndex
    globalZIndex = style.globalZIndex
    onClickListener = Overlay.OnClickListener { onClick() }
    if (attachToMap && map == null) {
        // No-op: map attachment is handled by each caller after style updates.
    }
}

private fun LatLng.toAndroidLatLng(): com.naver.maps.geometry.LatLng {
    return com.naver.maps.geometry.LatLng(latitude, longitude)
}

private fun LatLngBounds.toAndroidBounds(): AndroidLatLngBounds {
    return AndroidLatLngBounds(
        southWest.toAndroidLatLng(),
        northEast.toAndroidLatLng(),
    )
}

private fun AnchorPoint.toAndroidPointF(): PointF {
    return PointF(x, y)
}

private fun OverlayImage.toAndroidOverlayImage(): AndroidOverlayImage {
    return when (this) {
        OverlayImage.DefaultMarker -> Marker.DEFAULT_ICON
        OverlayImage.BlueMarker -> MarkerIcons.BLUE
        OverlayImage.GrayMarker -> MarkerIcons.GRAY
        OverlayImage.GreenMarker -> MarkerIcons.GREEN
        OverlayImage.LightBlueMarker -> MarkerIcons.LIGHTBLUE
        OverlayImage.PinkMarker -> MarkerIcons.PINK
        OverlayImage.RedMarker -> MarkerIcons.RED
        OverlayImage.YellowMarker -> MarkerIcons.YELLOW
        OverlayImage.BlackMarker -> MarkerIcons.BLACK
        OverlayImage.LocationDefault -> LocationOverlay.DEFAULT_ICON
    }
}

private fun ColorPart.toAndroidColorPart(): MultipartPathOverlay.ColorPart {
    return MultipartPathOverlay.ColorPart(
        color.toArgb(),
        outlineColor.toArgb(),
        passedColor.toArgb(),
        passedOutlineColor.toArgb(),
    )
}

private fun LineCap.toAndroidLineCap(): PolylineOverlay.LineCap {
    return when (this) {
        LineCap.Butt -> PolylineOverlay.LineCap.Butt
        LineCap.Round -> PolylineOverlay.LineCap.Round
        LineCap.Square -> PolylineOverlay.LineCap.Square
    }
}

private fun LineJoin.toAndroidLineJoin(): PolylineOverlay.LineJoin {
    return when (this) {
        LineJoin.Miter -> PolylineOverlay.LineJoin.Miter
        LineJoin.Bevel -> PolylineOverlay.LineJoin.Bevel
        LineJoin.Round -> PolylineOverlay.LineJoin.Round
    }
}
