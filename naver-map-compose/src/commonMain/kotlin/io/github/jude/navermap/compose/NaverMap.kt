package io.github.jude.navermap.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

data class LatLng(
    val latitude: Double,
    val longitude: Double,
)

data class CameraPosition(
    val target: LatLng = LatLng(37.5666102, 126.9783881),
    val zoom: Double = 14.0,
)

@Stable
class NaverMapCameraState(
    initialPosition: CameraPosition = CameraPosition(),
) {
    var position by mutableStateOf(initialPosition)
}

@Composable
fun rememberNaverMapCameraState(
    initialPosition: CameraPosition = CameraPosition(),
): NaverMapCameraState = remember {
    NaverMapCameraState(initialPosition)
}

data class MapProperties(
    val isIndoorEnabled: Boolean = false,
)

data class MapUiSettings(
    val isCompassEnabled: Boolean = true,
    val isScaleBarEnabled: Boolean = false,
    val isZoomControlEnabled: Boolean = true,
    val isLocationButtonEnabled: Boolean = false,
)

@Composable
fun NaverMap(
    modifier: Modifier = Modifier,
    cameraState: NaverMapCameraState = rememberNaverMapCameraState(),
    properties: MapProperties = MapProperties(),
    uiSettings: MapUiSettings = MapUiSettings(),
) {
    PlatformNaverMap(
        modifier = modifier,
        cameraState = cameraState,
        properties = properties,
        uiSettings = uiSettings,
    )
}

@Composable
internal expect fun PlatformNaverMap(
    modifier: Modifier = Modifier,
    cameraState: NaverMapCameraState,
    properties: MapProperties,
    uiSettings: MapUiSettings,
)
