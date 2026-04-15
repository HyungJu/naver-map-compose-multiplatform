package io.github.jude.navermap.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
class MarkerState(
    position: LatLng = LatLng(37.5666102, 126.9783881),
) {
    var position by mutableStateOf(position)

    companion object {
        val Saver = listSaver<MarkerState, Double>(
            save = { state ->
                listOf(state.position.latitude, state.position.longitude)
            },
            restore = { restored ->
                MarkerState(
                    position = LatLng(
                        latitude = restored[0],
                        longitude = restored[1],
                    ),
                )
            },
        )
    }
}

@Composable
fun rememberMarkerState(
    position: LatLng = LatLng(37.5666102, 126.9783881),
): MarkerState = rememberSaveable(saver = MarkerState.Saver) {
    MarkerState(position = position)
}

@Composable
fun rememberUpdatedMarkerState(
    position: LatLng = LatLng(37.5666102, 126.9783881),
): MarkerState = remember {
    MarkerState(position = position)
}.also { it.position = position }

enum class OverlayImage {
    DefaultMarker,
    BlueMarker,
    GrayMarker,
    GreenMarker,
    LightBlueMarker,
    PinkMarker,
    RedMarker,
    YellowMarker,
    BlackMarker,
}

enum class LineCap {
    Butt,
    Round,
    Square,
}

enum class LineJoin {
    Miter,
    Bevel,
    Round,
}

@Immutable
data class OverlayStyle(
    val tag: String? = null,
    val visible: Boolean = true,
    val minZoom: Double = NaverMapConstants.MinZoom,
    val minZoomInclusive: Boolean = true,
    val maxZoom: Double = NaverMapConstants.MaxZoom,
    val maxZoomInclusive: Boolean = true,
    val zIndex: Int = 0,
    val globalZIndex: Int = 0,
) {
    init {
        require(minZoom <= maxZoom) { "오버레이 최소 줌 레벨은 최대 줌 레벨보다 클 수 없습니다." }
    }
}

object MarkerDefaults {
    const val GlobalZIndex: Int = 200_000
    val Icon: OverlayImage = OverlayImage.DefaultMarker
}

object CircleOverlayDefaults {
    const val GlobalZIndex: Int = -200_000
}

object PolygonOverlayDefaults {
    const val GlobalZIndex: Int = -200_000
}

object PolylineOverlayDefaults {
    const val GlobalZIndex: Int = -200_000
    val Width: Dp = 2.5.dp
}

object GroundOverlayDefaults {
    const val GlobalZIndex: Int = -300_000
    val Image: OverlayImage = OverlayImage.DefaultMarker
}

@Composable
fun Marker(
    state: MarkerState = rememberUpdatedMarkerState(),
    icon: OverlayImage = MarkerDefaults.Icon,
    captionText: String = "",
    alpha: Float = 1f,
    style: OverlayStyle = OverlayStyle(globalZIndex = MarkerDefaults.GlobalZIndex),
    onClick: () -> Boolean = { false },
) {
    require(alpha in 0f..1f) { "마커 투명도는 0과 1 사이여야 합니다." }

    rememberOverlay(
        create = ::createPlatformMarkerOverlay,
        update = { handle, overlay ->
            updatePlatformMarkerOverlay(
                handle = handle,
                overlay = overlay,
                position = state.position,
                icon = icon,
                captionText = captionText,
                alpha = alpha,
                style = style,
                onClick = onClick,
            )
        },
        dispose = ::disposePlatformMarkerOverlay,
    )
}

@Composable
fun CircleOverlay(
    center: LatLng,
    radiusMeters: Double,
    fillColor: Color = Color.Transparent,
    outlineWidth: Dp = 0.dp,
    outlineColor: Color = Color.Black,
    style: OverlayStyle = OverlayStyle(globalZIndex = CircleOverlayDefaults.GlobalZIndex),
    onClick: () -> Boolean = { false },
) {
    require(radiusMeters >= 0.0) { "원 오버레이 반경은 0 이상이어야 합니다." }
    val outlineWidthValue = with(LocalDensity.current) { outlineWidth.toPx() }

    rememberOverlay(
        create = ::createPlatformCircleOverlay,
        update = { handle, overlay ->
            updatePlatformCircleOverlay(
                handle = handle,
                overlay = overlay,
                center = center,
                radiusMeters = radiusMeters,
                fillColor = fillColor,
                outlineWidth = outlineWidthValue,
                outlineColor = outlineColor,
                style = style,
                onClick = onClick,
            )
        },
        dispose = ::disposePlatformCircleOverlay,
    )
}

@Composable
fun PolygonOverlay(
    coordinates: List<LatLng>,
    fillColor: Color = Color.Transparent,
    outlineWidth: Dp = 0.dp,
    outlineColor: Color = Color.Black,
    outlinePattern: List<Int> = emptyList(),
    style: OverlayStyle = OverlayStyle(globalZIndex = PolygonOverlayDefaults.GlobalZIndex),
    onClick: () -> Boolean = { false },
) {
    require(coordinates.size >= 3) { "폴리곤 오버레이는 최소 3개의 좌표가 필요합니다." }
    val outlineWidthValue = with(LocalDensity.current) { outlineWidth.toPx() }

    rememberOverlay(
        create = ::createPlatformPolygonOverlay,
        update = { handle, overlay ->
            updatePlatformPolygonOverlay(
                handle = handle,
                overlay = overlay,
                coordinates = coordinates,
                fillColor = fillColor,
                outlineWidth = outlineWidthValue,
                outlineColor = outlineColor,
                outlinePattern = outlinePattern,
                style = style,
                onClick = onClick,
            )
        },
        dispose = ::disposePlatformPolygonOverlay,
    )
}

@Composable
fun PolylineOverlay(
    coordinates: List<LatLng>,
    width: Dp = PolylineOverlayDefaults.Width,
    color: Color = Color.Black,
    pattern: List<Int> = emptyList(),
    cap: LineCap = LineCap.Butt,
    join: LineJoin = LineJoin.Miter,
    style: OverlayStyle = OverlayStyle(globalZIndex = PolylineOverlayDefaults.GlobalZIndex),
    onClick: () -> Boolean = { false },
) {
    require(coordinates.size >= 2) { "폴리라인 오버레이는 최소 2개의 좌표가 필요합니다." }
    val widthValue = with(LocalDensity.current) { width.toPx() }

    rememberOverlay(
        create = ::createPlatformPolylineOverlay,
        update = { handle, overlay ->
            updatePlatformPolylineOverlay(
                handle = handle,
                overlay = overlay,
                coordinates = coordinates,
                width = widthValue,
                color = color,
                pattern = pattern,
                cap = cap,
                join = join,
                style = style,
                onClick = onClick,
            )
        },
        dispose = ::disposePlatformPolylineOverlay,
    )
}

@Composable
fun GroundOverlay(
    bounds: LatLngBounds,
    image: OverlayImage = GroundOverlayDefaults.Image,
    alpha: Float = 1f,
    style: OverlayStyle = OverlayStyle(globalZIndex = GroundOverlayDefaults.GlobalZIndex),
    onClick: () -> Boolean = { false },
) {
    require(alpha in 0f..1f) { "지상 오버레이 투명도는 0과 1 사이여야 합니다." }

    rememberOverlay(
        create = ::createPlatformGroundOverlay,
        update = { handle, overlay ->
            updatePlatformGroundOverlay(
                handle = handle,
                overlay = overlay,
                bounds = bounds,
                image = image,
                alpha = alpha,
                style = style,
                onClick = onClick,
            )
        },
        dispose = ::disposePlatformGroundOverlay,
    )
}

@Composable
private fun <T> rememberOverlay(
    create: (PlatformMapHandle) -> T,
    update: (PlatformMapHandle, T) -> Unit,
    dispose: (T) -> Unit,
) {
    val handle = LocalPlatformMapHandle.current ?: return
    val overlay = remember(handle) { create(handle) }

    SideEffect {
        update(handle, overlay)
    }

    DisposableEffect(handle, overlay) {
        onDispose {
            dispose(overlay)
        }
    }
}

internal expect class PlatformMarkerOverlay

internal expect fun createPlatformMarkerOverlay(handle: PlatformMapHandle): PlatformMarkerOverlay

internal expect fun updatePlatformMarkerOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformMarkerOverlay,
    position: LatLng,
    icon: OverlayImage,
    captionText: String,
    alpha: Float,
    style: OverlayStyle,
    onClick: () -> Boolean,
)

internal expect fun disposePlatformMarkerOverlay(overlay: PlatformMarkerOverlay)

internal expect class PlatformCircleOverlay

internal expect fun createPlatformCircleOverlay(handle: PlatformMapHandle): PlatformCircleOverlay

internal expect fun updatePlatformCircleOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformCircleOverlay,
    center: LatLng,
    radiusMeters: Double,
    fillColor: Color,
    outlineWidth: Float,
    outlineColor: Color,
    style: OverlayStyle,
    onClick: () -> Boolean,
)

internal expect fun disposePlatformCircleOverlay(overlay: PlatformCircleOverlay)

internal expect class PlatformPolygonOverlay

internal expect fun createPlatformPolygonOverlay(handle: PlatformMapHandle): PlatformPolygonOverlay

internal expect fun updatePlatformPolygonOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformPolygonOverlay,
    coordinates: List<LatLng>,
    fillColor: Color,
    outlineWidth: Float,
    outlineColor: Color,
    outlinePattern: List<Int>,
    style: OverlayStyle,
    onClick: () -> Boolean,
)

internal expect fun disposePlatformPolygonOverlay(overlay: PlatformPolygonOverlay)

internal expect class PlatformPolylineOverlay

internal expect fun createPlatformPolylineOverlay(handle: PlatformMapHandle): PlatformPolylineOverlay

internal expect fun updatePlatformPolylineOverlay(
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
)

internal expect fun disposePlatformPolylineOverlay(overlay: PlatformPolylineOverlay)

internal expect class PlatformGroundOverlay

internal expect fun createPlatformGroundOverlay(handle: PlatformMapHandle): PlatformGroundOverlay

internal expect fun updatePlatformGroundOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformGroundOverlay,
    bounds: LatLngBounds,
    image: OverlayImage,
    alpha: Float,
    style: OverlayStyle,
    onClick: () -> Boolean,
)

internal expect fun disposePlatformGroundOverlay(overlay: PlatformGroundOverlay)
