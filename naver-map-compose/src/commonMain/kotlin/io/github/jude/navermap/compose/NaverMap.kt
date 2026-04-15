package io.github.jude.navermap.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NaverMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    properties: MapProperties = DefaultMapProperties,
    uiSettings: MapUiSettings = DefaultMapUiSettings,
    locale: String? = null,
    contentPadding: PaddingValues = NoPadding,
) {
    PlatformNaverMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = uiSettings,
        locale = locale,
        contentPadding = contentPadding,
    )
}

@Composable
internal expect fun PlatformNaverMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState,
    properties: MapProperties,
    uiSettings: MapUiSettings,
    locale: String?,
    contentPadding: PaddingValues,
)
