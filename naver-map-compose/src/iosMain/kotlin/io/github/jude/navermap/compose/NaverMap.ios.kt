package io.github.jude.navermap.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import cocoapods.NMapsMap.NMFCameraUpdate
import cocoapods.NMapsMap.NMFMapView
import cocoapods.NMapsMap.NMGLatLng
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun PlatformNaverMap(
    modifier: Modifier,
    cameraState: NaverMapCameraState,
    properties: MapProperties,
    uiSettings: MapUiSettings,
) {
    UIKitView(
        modifier = modifier,
        factory = {
            NMFMapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
                indoorMapEnabled = properties.isIndoorEnabled
                zoomGestureEnabled = uiSettings.isZoomControlEnabled
                moveCamera(cameraUpdate(cameraState))
            }
        },
        update = { mapView ->
            mapView.indoorMapEnabled = properties.isIndoorEnabled
            mapView.zoomGestureEnabled = uiSettings.isZoomControlEnabled
            mapView.moveCamera(cameraUpdate(cameraState))
        },
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun cameraUpdate(cameraState: NaverMapCameraState): NMFCameraUpdate {
    val target = NMGLatLng.latLngWithLat(
        cameraState.position.target.latitude,
        cameraState.position.target.longitude,
    )
    return NMFCameraUpdate.cameraUpdateWithScrollTo(
        target = target,
        zoomTo = cameraState.position.zoom,
    )
}
