package io.github.hyungju.navermap.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

@Composable
fun NaverMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    authOptions: NaverMapAuthOptions? = null,
    properties: MapProperties = DefaultMapProperties,
    uiSettings: MapUiSettings = DefaultMapUiSettings,
    locale: String? = null,
    contentPadding: PaddingValues = NoPadding,
    onMapClick: (ScreenPoint, LatLng) -> Unit = { _, _ -> },
    onMapLongClick: (ScreenPoint, LatLng) -> Unit = { _, _ -> },
    onMapDoubleTap: (ScreenPoint, LatLng) -> Boolean = { _, _ -> false },
    onMapTwoFingerTap: (ScreenPoint, LatLng) -> Boolean = { _, _ -> false },
    onMapLoaded: () -> Unit = {},
    onOptionChange: () -> Unit = {},
    onSymbolClick: (MapSymbol) -> Boolean = { false },
    onIndoorSelectionChange: (IndoorSelectionInfo?) -> Unit = {},
    onLocationChange: (MapLocation) -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val resolvedAuthOptions = currentNaverMapAuthOptions(authOptions)

    CompositionLocalProvider(
        LocalCameraPositionState provides cameraPositionState,
    ) {
        PlatformNaverMap(
            modifier = modifier,
            cameraPositionState = cameraPositionState,
            authOptions = resolvedAuthOptions,
            properties = properties,
            uiSettings = uiSettings,
            locale = locale,
            contentPadding = contentPadding,
            onMapClick = onMapClick,
            onMapLongClick = onMapLongClick,
            onMapDoubleTap = onMapDoubleTap,
            onMapTwoFingerTap = onMapTwoFingerTap,
            onMapLoaded = onMapLoaded,
            onOptionChange = onOptionChange,
            onSymbolClick = onSymbolClick,
            onIndoorSelectionChange = onIndoorSelectionChange,
            onLocationChange = onLocationChange,
            content = content,
        )
    }
}

@Composable
internal expect fun PlatformNaverMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState,
    authOptions: NaverMapAuthOptions?,
    properties: MapProperties,
    uiSettings: MapUiSettings,
    locale: String?,
    contentPadding: PaddingValues,
    onMapClick: (ScreenPoint, LatLng) -> Unit,
    onMapLongClick: (ScreenPoint, LatLng) -> Unit,
    onMapDoubleTap: (ScreenPoint, LatLng) -> Boolean,
    onMapTwoFingerTap: (ScreenPoint, LatLng) -> Boolean,
    onMapLoaded: () -> Unit,
    onOptionChange: () -> Unit,
    onSymbolClick: (MapSymbol) -> Boolean,
    onIndoorSelectionChange: (IndoorSelectionInfo?) -> Unit,
    onLocationChange: (MapLocation) -> Unit,
    content: @Composable () -> Unit,
)
