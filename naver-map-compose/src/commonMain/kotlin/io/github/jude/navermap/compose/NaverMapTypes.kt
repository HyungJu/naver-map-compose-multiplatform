package io.github.jude.navermap.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class LatLng(
    val latitude: Double,
    val longitude: Double,
)

data class LatLngBounds(
    val southWest: LatLng,
    val northEast: LatLng,
) {
    init {
        require(southWest.latitude <= northEast.latitude) {
            "남서쪽 위도는 북동쪽 위도보다 클 수 없습니다."
        }
        require(southWest.longitude <= northEast.longitude) {
            "남서쪽 경도는 북동쪽 경도보다 클 수 없습니다."
        }
    }
}

data class CameraPosition(
    val target: LatLng = LatLng(37.5666102, 126.9783881),
    val zoom: Double = 14.0,
    val tilt: Double = 0.0,
    val bearing: Double = 0.0,
)

data class ScreenPoint(
    val x: Float,
    val y: Float,
)

data class AnchorPoint(
    val x: Float,
    val y: Float,
) {
    init {
        require(x in 0f..1f) { "앵커 X 좌표는 0과 1 사이여야 합니다." }
        require(y in 0f..1f) { "앵커 Y 좌표는 0과 1 사이여야 합니다." }
    }
}

object AnchorPoints {
    val Center = AnchorPoint(x = 0.5f, y = 0.5f)
    val BottomCenter = AnchorPoint(x = 0.5f, y = 1f)
}

data class MapSymbol(
    val caption: String,
    val position: LatLng,
)

data class IndoorSelectionInfo(
    val zoneId: String?,
    val levelId: String?,
    val zoneIndex: Int,
    val levelIndex: Int,
)

data class MapLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val bearing: Float? = null,
    val speedMetersPerSecond: Float? = null,
    val altitudeMeters: Double? = null,
    val timestampMillis: Long? = null,
)

enum class MapType {
    Basic,
    Navi,
    Satellite,
    Hybrid,
    NaviHybrid,
    Terrain,
    None,
}

enum class LocationTrackingMode {
    None,
    NoFollow,
    Follow,
    Face,
}

@Suppress("ClassName")
enum class CameraUpdateReason(
    val value: Int,
) {
    UNKNOWN(2),
    NO_MOVEMENT_YET(1),
    DEVELOPER(0),
    GESTURE(-1),
    CONTROL(-2),
    LOCATION(-3),
    ;

    companion object {
        fun fromInt(reason: Int): CameraUpdateReason {
            return entries.firstOrNull { it.value == reason } ?: UNKNOWN
        }
    }
}

enum class LogoAlignment {
    BottomStart,
    BottomEnd,
    TopStart,
    TopEnd,
}

object NaverMapConstants {
    const val MinZoom: Double = 0.0
    const val MaxZoom: Double = 20.0
    const val MinTilt: Double = 0.0
    const val MaxTilt: Double = 63.0
    const val DefaultMaxTilt: Double = 60.0
    const val MinBearing: Double = -180.0
    const val MaxBearing: Double = 180.0
    const val DefaultCameraAnimationDuration: Int = 200

    val DefaultIndoorFocusRadius: Dp = 40.dp
    val DefaultPickTolerance: Dp = 2.dp

    const val DefaultScrollGesturesFriction: Float = 0.0879999995f
    const val DefaultZoomGesturesFriction: Float = 0.12375f
    const val DefaultRotateGesturesFriction: Float = 0.19333f

    val DefaultCameraPosition: CameraPosition = CameraPosition()
    val DefaultBackgroundColorLight: Color = Color(0xFFF7F9FC)
    val DefaultBackgroundColorDark: Color = Color(0xFF20242C)
}

internal val DefaultMapProperties = MapProperties()
internal val DefaultMapUiSettings = MapUiSettings()
internal val NoPadding = PaddingValues(0.dp)

data class MapProperties(
    val mapType: MapType = MapType.Basic,
    val extent: LatLngBounds? = null,
    val minZoom: Double = NaverMapConstants.MinZoom,
    val maxZoom: Double = NaverMapConstants.MaxZoom,
    val maxTilt: Double = NaverMapConstants.DefaultMaxTilt,
    val defaultCameraAnimationDuration: Int = NaverMapConstants.DefaultCameraAnimationDuration,
    val fpsLimit: Int = 0,
    val isBuildingLayerGroupEnabled: Boolean = true,
    val isTransitLayerGroupEnabled: Boolean = false,
    val isBicycleLayerGroupEnabled: Boolean = false,
    val isTrafficLayerGroupEnabled: Boolean = false,
    val isCadastralLayerGroupEnabled: Boolean = false,
    val isMountainLayerGroupEnabled: Boolean = false,
    val isLiteModeEnabled: Boolean = false,
    val isNightModeEnabled: Boolean = false,
    val isIndoorEnabled: Boolean = false,
    val indoorFocusRadius: Dp = NaverMapConstants.DefaultIndoorFocusRadius,
    val buildingHeight: Float = 1f,
    val lightness: Float = 0f,
    val symbolScale: Float = 1f,
    val symbolPerspectiveRatio: Float = 1f,
    val backgroundColor: Color = NaverMapConstants.DefaultBackgroundColorLight,
    val locationTrackingMode: LocationTrackingMode = LocationTrackingMode.None,
) {
    init {
        require(minZoom <= maxZoom) { "최소 줌 레벨은 최대 줌 레벨보다 클 수 없습니다." }
        require(fpsLimit >= 0) { "FPS 제한은 0 이상이어야 합니다." }
        require(buildingHeight in 0f..1f) { "건물 높이 배율은 0과 1 사이여야 합니다." }
        require(lightness in -1f..1f) { "밝기 값은 -1과 1 사이여야 합니다." }
        require(symbolScale in 0f..2f) { "심벌 크기 배율은 0과 2 사이여야 합니다." }
        require(symbolPerspectiveRatio in 0f..1f) {
            "심벌 원근 비율은 0과 1 사이여야 합니다."
        }
    }
}

data class MapUiSettings(
    val pickTolerance: Dp = NaverMapConstants.DefaultPickTolerance,
    val isScrollGesturesEnabled: Boolean = true,
    val isZoomGesturesEnabled: Boolean = true,
    val isTiltGesturesEnabled: Boolean = true,
    val isRotateGesturesEnabled: Boolean = true,
    val isStopGesturesEnabled: Boolean = true,
    val scrollGesturesFriction: Float = NaverMapConstants.DefaultScrollGesturesFriction,
    val zoomGesturesFriction: Float = NaverMapConstants.DefaultZoomGesturesFriction,
    val rotateGesturesFriction: Float = NaverMapConstants.DefaultRotateGesturesFriction,
    val isCompassEnabled: Boolean = true,
    val isScaleBarEnabled: Boolean = true,
    val isZoomControlEnabled: Boolean = true,
    val isIndoorLevelPickerEnabled: Boolean = false,
    val isLocationButtonEnabled: Boolean = false,
    val isLogoClickEnabled: Boolean = true,
    val logoAlignment: LogoAlignment = LogoAlignment.BottomStart,
    val logoMargin: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
) {
    init {
        require(scrollGesturesFriction in 0f..1f) {
            "스크롤 제스처 마찰 계수는 0과 1 사이여야 합니다."
        }
        require(zoomGesturesFriction in 0f..1f) {
            "줌 제스처 마찰 계수는 0과 1 사이여야 합니다."
        }
        require(rotateGesturesFriction in 0f..1f) {
            "회전 제스처 마찰 계수는 0과 1 사이여야 합니다."
        }
    }
}
