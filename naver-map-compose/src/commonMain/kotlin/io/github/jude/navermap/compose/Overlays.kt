package io.github.jude.navermap.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    LocationDefault,
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

enum class Align {
    Center,
    Left,
    Right,
    Top,
    Bottom,
    TopLeft,
    TopRight,
    BottomRight,
    BottomLeft,
    ;

    companion object {
        val EDGES = listOf(Bottom, Right, Left, Top)
        val APEXES = listOf(BottomRight, BottomLeft, TopRight, TopLeft)
        val OUTSIDES = listOf(
            Bottom,
            Right,
            Left,
            Top,
            BottomRight,
            BottomLeft,
            TopRight,
            TopLeft,
        )
    }
}

@Immutable
data class ColorPart(
    val color: Color = Color.White,
    val outlineColor: Color = Color.Black,
    val passedColor: Color = Color.White,
    val passedOutlineColor: Color = Color.Black,
)

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

object PathOverlayDefaults {
    const val GlobalZIndex: Int = -100_000
    val Width: Dp = 10.dp
    val OutlineWidth: Dp = 2.dp
    val PatternInterval: Dp = 50.dp
}

object MultipartPathOverlayDefaults {
    const val GlobalZIndex: Int = -100_000
    val Width: Dp = 10.dp
    val OutlineWidth: Dp = 2.dp
    val PatternInterval: Dp = 50.dp
}

object ArrowheadPathOverlayDefaults {
    const val GlobalZIndex: Int = 100_000
    val Width: Dp = 10.dp
    val OutlineWidth: Dp = 2.dp
    val Elevation: Dp = 0.dp
}

object LocationOverlayDefaults {
    const val GlobalZIndex: Int = 300_000
    val Icon: OverlayImage = OverlayImage.LocationDefault
    val SizeAuto: Dp = Dp.Unspecified
    val DefaultAnchor: AnchorPoint = AnchorPoints.Center
    val DefaultSubAnchor: AnchorPoint = AnchorPoints.BottomCenter
    val DefaultCircleColor: Color = Color(0xFF3D8BFF)
    val DefaultCircleRadius: Dp = 18.dp
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
    val onClickState = rememberUpdatedState(onClick)

    rememberOverlay(
        updateKey = listOf(state.position, icon, captionText, alpha, style),
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
                onClick = { onClickState.value() },
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
    val onClickState = rememberUpdatedState(onClick)

    rememberOverlay(
        updateKey = listOf(center, radiusMeters, fillColor, outlineWidth, outlineColor, style),
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
                onClick = { onClickState.value() },
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
    val onClickState = rememberUpdatedState(onClick)

    rememberOverlay(
        updateKey = listOf(coordinates, fillColor, outlineWidth, outlineColor, outlinePattern, style),
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
                onClick = { onClickState.value() },
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
    val onClickState = rememberUpdatedState(onClick)

    rememberOverlay(
        updateKey = listOf(coordinates, width, color, pattern, cap, join, style),
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
                onClick = { onClickState.value() },
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
    val onClickState = rememberUpdatedState(onClick)

    rememberOverlay(
        updateKey = listOf(bounds, image, alpha, style),
        create = ::createPlatformGroundOverlay,
        update = { handle, overlay ->
            updatePlatformGroundOverlay(
                handle = handle,
                overlay = overlay,
                bounds = bounds,
                image = image,
                alpha = alpha,
                style = style,
                onClick = { onClickState.value() },
            )
        },
        dispose = ::disposePlatformGroundOverlay,
    )
}

@Composable
fun PathOverlay(
    coordinates: List<LatLng>,
    progress: Double = 0.0,
    width: Dp = PathOverlayDefaults.Width,
    outlineWidth: Dp = PathOverlayDefaults.OutlineWidth,
    color: Color = Color.White,
    outlineColor: Color = Color.Black,
    passedColor: Color = Color.White,
    passedOutlineColor: Color = Color.Black,
    patternImage: OverlayImage? = null,
    patternInterval: Dp = PathOverlayDefaults.PatternInterval,
    isHideCollidedSymbols: Boolean = false,
    isHideCollidedMarkers: Boolean = false,
    isHideCollidedCaptions: Boolean = false,
    style: OverlayStyle = OverlayStyle(globalZIndex = PathOverlayDefaults.GlobalZIndex),
    onClick: () -> Boolean = { false },
) {
    require(coordinates.size >= 2) { "경로 오버레이는 최소 2개의 좌표가 필요합니다." }
    require(progress in -1.0..1.0) { "경로 진척률은 -1과 1 사이여야 합니다." }
    require(patternInterval.value >= 0f) { "경로 패턴 간격은 0 이상이어야 합니다." }
    val widthValue = with(LocalDensity.current) { width.toPx() }
    val outlineWidthValue = with(LocalDensity.current) { outlineWidth.toPx() }
    val patternIntervalValue = with(LocalDensity.current) { patternInterval.toPx() }
    val onClickState = rememberUpdatedState(onClick)

    rememberOverlay(
        updateKey = listOf(
            coordinates,
            progress,
            width,
            outlineWidth,
            color,
            outlineColor,
            passedColor,
            passedOutlineColor,
            patternImage,
            patternInterval,
            isHideCollidedSymbols,
            isHideCollidedMarkers,
            isHideCollidedCaptions,
            style,
        ),
        create = ::createPlatformPathOverlay,
        update = { handle, overlay ->
            updatePlatformPathOverlay(
                handle = handle,
                overlay = overlay,
                coordinates = coordinates,
                progress = progress,
                width = widthValue,
                outlineWidth = outlineWidthValue,
                color = color,
                outlineColor = outlineColor,
                passedColor = passedColor,
                passedOutlineColor = passedOutlineColor,
                patternImage = patternImage,
                patternInterval = patternIntervalValue,
                isHideCollidedSymbols = isHideCollidedSymbols,
                isHideCollidedMarkers = isHideCollidedMarkers,
                isHideCollidedCaptions = isHideCollidedCaptions,
                style = style,
                onClick = { onClickState.value() },
            )
        },
        dispose = ::disposePlatformPathOverlay,
    )
}

@Composable
fun MultipartPathOverlay(
    coordinateParts: List<List<LatLng>>,
    colorParts: List<ColorPart>,
    progress: Double = 0.0,
    width: Dp = MultipartPathOverlayDefaults.Width,
    outlineWidth: Dp = MultipartPathOverlayDefaults.OutlineWidth,
    patternImage: OverlayImage? = null,
    patternInterval: Dp = MultipartPathOverlayDefaults.PatternInterval,
    isHideCollidedSymbols: Boolean = false,
    isHideCollidedMarkers: Boolean = false,
    isHideCollidedCaptions: Boolean = false,
    style: OverlayStyle = OverlayStyle(globalZIndex = MultipartPathOverlayDefaults.GlobalZIndex),
    onClick: () -> Boolean = { false },
) {
    require(coordinateParts.isNotEmpty()) { "멀티 파트 경로 오버레이는 최소 1개의 경로 파트가 필요합니다." }
    require(coordinateParts.all { it.size >= 2 }) { "모든 멀티 파트 경로는 최소 2개의 좌표가 필요합니다." }
    require(colorParts.isNotEmpty()) { "멀티 파트 경로 색상 파트는 비어 있을 수 없습니다." }
    require(coordinateParts.size == colorParts.size) { "경로 파트 수와 색상 파트 수는 같아야 합니다." }
    require(progress in -1.0..1.0) { "경로 진척률은 -1과 1 사이여야 합니다." }
    require(patternInterval.value >= 0f) { "경로 패턴 간격은 0 이상이어야 합니다." }
    val widthValue = with(LocalDensity.current) { width.toPx() }
    val outlineWidthValue = with(LocalDensity.current) { outlineWidth.toPx() }
    val patternIntervalValue = with(LocalDensity.current) { patternInterval.toPx() }
    val onClickState = rememberUpdatedState(onClick)

    rememberOverlay(
        updateKey = listOf(
            coordinateParts,
            colorParts,
            progress,
            width,
            outlineWidth,
            patternImage,
            patternInterval,
            isHideCollidedSymbols,
            isHideCollidedMarkers,
            isHideCollidedCaptions,
            style,
        ),
        create = ::createPlatformMultipartPathOverlay,
        update = { handle, overlay ->
            updatePlatformMultipartPathOverlay(
                handle = handle,
                overlay = overlay,
                coordinateParts = coordinateParts,
                colorParts = colorParts,
                progress = progress,
                width = widthValue,
                outlineWidth = outlineWidthValue,
                patternImage = patternImage,
                patternInterval = patternIntervalValue,
                isHideCollidedSymbols = isHideCollidedSymbols,
                isHideCollidedMarkers = isHideCollidedMarkers,
                isHideCollidedCaptions = isHideCollidedCaptions,
                style = style,
                onClick = { onClickState.value() },
            )
        },
        dispose = ::disposePlatformMultipartPathOverlay,
    )
}

@Composable
fun ArrowheadPathOverlay(
    coordinates: List<LatLng>,
    width: Dp = ArrowheadPathOverlayDefaults.Width,
    headSizeRatio: Float = 2.5f,
    color: Color = Color.White,
    outlineWidth: Dp = ArrowheadPathOverlayDefaults.OutlineWidth,
    outlineColor: Color = Color.Black,
    elevation: Dp = ArrowheadPathOverlayDefaults.Elevation,
    style: OverlayStyle = OverlayStyle(globalZIndex = ArrowheadPathOverlayDefaults.GlobalZIndex),
    onClick: () -> Boolean = { false },
) {
    require(coordinates.size >= 2) { "화살표 경로 오버레이는 최소 2개의 좌표가 필요합니다." }
    require(headSizeRatio > 0f) { "화살표 머리 배율은 0보다 커야 합니다." }
    val widthValue = with(LocalDensity.current) { width.toPx() }
    val outlineWidthValue = with(LocalDensity.current) { outlineWidth.toPx() }
    val elevationValue = with(LocalDensity.current) { elevation.toPx() }
    val onClickState = rememberUpdatedState(onClick)

    rememberOverlay(
        updateKey = listOf(
            coordinates,
            width,
            headSizeRatio,
            color,
            outlineWidth,
            outlineColor,
            elevation,
            style,
        ),
        create = ::createPlatformArrowheadPathOverlay,
        update = { handle, overlay ->
            updatePlatformArrowheadPathOverlay(
                handle = handle,
                overlay = overlay,
                coordinates = coordinates,
                width = widthValue,
                headSizeRatio = headSizeRatio,
                color = color,
                outlineWidth = outlineWidthValue,
                outlineColor = outlineColor,
                elevation = elevationValue,
                style = style,
                onClick = { onClickState.value() },
            )
        },
        dispose = ::disposePlatformArrowheadPathOverlay,
    )
}

@Composable
fun LocationOverlay(
    position: LatLng,
    bearing: Float = 0f,
    icon: OverlayImage = LocationOverlayDefaults.Icon,
    iconWidth: Dp = LocationOverlayDefaults.SizeAuto,
    iconHeight: Dp = LocationOverlayDefaults.SizeAuto,
    iconAlpha: Float = 1f,
    anchor: AnchorPoint = LocationOverlayDefaults.DefaultAnchor,
    subIcon: OverlayImage? = null,
    subIconWidth: Dp = LocationOverlayDefaults.SizeAuto,
    subIconHeight: Dp = LocationOverlayDefaults.SizeAuto,
    subIconAlpha: Float = 1f,
    subAnchor: AnchorPoint = LocationOverlayDefaults.DefaultSubAnchor,
    circleRadius: Dp = LocationOverlayDefaults.DefaultCircleRadius,
    circleColor: Color = LocationOverlayDefaults.DefaultCircleColor,
    circleOutlineWidth: Dp = 0.dp,
    circleOutlineColor: Color = Color.Transparent,
    style: OverlayStyle = OverlayStyle(globalZIndex = LocationOverlayDefaults.GlobalZIndex),
    onClick: () -> Boolean = { false },
) {
    require(bearing in 0f..360f) { "위치 오버레이 방위는 0도에서 360도 사이여야 합니다." }
    require(iconAlpha in 0f..1f) { "위치 오버레이 아이콘 투명도는 0과 1 사이여야 합니다." }
    require(subIconAlpha in 0f..1f) { "위치 오버레이 보조 아이콘 투명도는 0과 1 사이여야 합니다." }
    require(circleRadius.value >= 0f) { "위치 오버레이 원 반경은 0 이상이어야 합니다." }
    requireAutoOrNonNegative(iconWidth, "위치 오버레이 아이콘 너비")
    requireAutoOrNonNegative(iconHeight, "위치 오버레이 아이콘 높이")
    requireAutoOrNonNegative(subIconWidth, "위치 오버레이 보조 아이콘 너비")
    requireAutoOrNonNegative(subIconHeight, "위치 오버레이 보조 아이콘 높이")
    val density = LocalDensity.current
    val iconWidthValue = iconWidth.toAutoSizePxOrNull(density)
    val iconHeightValue = iconHeight.toAutoSizePxOrNull(density)
    val subIconWidthValue = subIconWidth.toAutoSizePxOrNull(density)
    val subIconHeightValue = subIconHeight.toAutoSizePxOrNull(density)
    val circleRadiusValue = with(density) { circleRadius.toPx() }
    val circleOutlineWidthValue = with(density) { circleOutlineWidth.toPx() }
    val onClickState = rememberUpdatedState(onClick)

    rememberOverlay(
        updateKey = listOf(
            position,
            bearing,
            icon,
            iconWidth,
            iconHeight,
            iconAlpha,
            anchor,
            subIcon,
            subIconWidth,
            subIconHeight,
            subIconAlpha,
            subAnchor,
            circleRadius,
            circleColor,
            circleOutlineWidth,
            circleOutlineColor,
            style,
        ),
        create = ::createPlatformLocationOverlay,
        update = { handle, overlay ->
            updatePlatformLocationOverlay(
                handle = handle,
                overlay = overlay,
                position = position,
                bearing = bearing,
                icon = icon,
                iconWidth = iconWidthValue,
                iconHeight = iconHeightValue,
                iconAlpha = iconAlpha,
                anchor = anchor,
                subIcon = subIcon,
                subIconWidth = subIconWidthValue,
                subIconHeight = subIconHeightValue,
                subIconAlpha = subIconAlpha,
                subAnchor = subAnchor,
                circleRadius = circleRadiusValue,
                circleColor = circleColor,
                circleOutlineWidth = circleOutlineWidthValue,
                circleOutlineColor = circleOutlineColor,
                style = style,
                onClick = { onClickState.value() },
            )
        },
        dispose = ::disposePlatformLocationOverlay,
    )
}

@Composable
private fun <T> rememberOverlay(
    updateKey: Any?,
    create: (PlatformMapHandle) -> T,
    update: (PlatformMapHandle, T) -> Unit,
    dispose: (T) -> Unit,
) {
    val handle = LocalPlatformMapHandle.current ?: return
    val overlay = remember(handle) { create(handle) }

    DisposableEffect(handle, overlay, updateKey) {
        update(handle, overlay)
        onDispose { }
    }

    DisposableEffect(handle, overlay) {
        onDispose {
            dispose(overlay)
        }
    }
}

private fun requireAutoOrNonNegative(size: Dp, label: String) {
    if (size != Dp.Unspecified) {
        require(size.value >= 0f) { "$label 값은 0 이상이거나 자동이어야 합니다." }
    }
}

private fun Dp.toAutoSizePxOrNull(density: androidx.compose.ui.unit.Density): Float? {
    return if (this == Dp.Unspecified) {
        null
    } else {
        with(density) { toPx() }
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

internal expect class PlatformPathOverlay

internal expect fun createPlatformPathOverlay(handle: PlatformMapHandle): PlatformPathOverlay

internal expect fun updatePlatformPathOverlay(
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
)

internal expect fun disposePlatformPathOverlay(overlay: PlatformPathOverlay)

internal expect class PlatformMultipartPathOverlay

internal expect fun createPlatformMultipartPathOverlay(handle: PlatformMapHandle): PlatformMultipartPathOverlay

internal expect fun updatePlatformMultipartPathOverlay(
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
)

internal expect fun disposePlatformMultipartPathOverlay(overlay: PlatformMultipartPathOverlay)

internal expect class PlatformArrowheadPathOverlay

internal expect fun createPlatformArrowheadPathOverlay(handle: PlatformMapHandle): PlatformArrowheadPathOverlay

internal expect fun updatePlatformArrowheadPathOverlay(
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
)

internal expect fun disposePlatformArrowheadPathOverlay(overlay: PlatformArrowheadPathOverlay)

internal expect class PlatformLocationOverlay

internal expect fun createPlatformLocationOverlay(handle: PlatformMapHandle): PlatformLocationOverlay

internal expect fun updatePlatformLocationOverlay(
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
)

internal expect fun disposePlatformLocationOverlay(overlay: PlatformLocationOverlay)
