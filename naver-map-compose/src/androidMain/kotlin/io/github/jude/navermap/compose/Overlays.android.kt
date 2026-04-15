package io.github.jude.navermap.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.naver.maps.geometry.LatLngBounds as AndroidLatLngBounds
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.GroundOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage as AndroidOverlayImage
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

private fun Overlay.applyCommonStyle(
    style: OverlayStyle,
    onClick: () -> Boolean,
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
    }
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
