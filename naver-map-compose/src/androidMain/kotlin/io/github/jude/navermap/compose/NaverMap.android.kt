package io.github.jude.navermap.compose

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap

private class ManagedMapView(context: Context) : MapView(context) {
    var map: NaverMap? = null
}

@Composable
internal actual fun PlatformNaverMap(
    modifier: Modifier,
    cameraState: NaverMapCameraState,
    properties: MapProperties,
    uiSettings: MapUiSettings,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val managedMapView = remember { mutableStateOf<ManagedMapView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            ManagedMapView(context).apply {
                onCreate(Bundle())
                getMapAsync { naverMap ->
                    map = naverMap
                    applyMapState(
                        map = naverMap,
                        cameraState = cameraState,
                        properties = properties,
                        uiSettings = uiSettings,
                    )
                }
                managedMapView.value = this
            }
        },
        update = { view ->
            view.map?.let { naverMap ->
                applyMapState(
                    map = naverMap,
                    cameraState = cameraState,
                    properties = properties,
                    uiSettings = uiSettings,
                )
            }
        },
    )

    DisposableEffect(lifecycleOwner, managedMapView.value) {
        val mapView = managedMapView.value
        if (mapView == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> Unit
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }
}

private fun applyMapState(
    map: NaverMap,
    cameraState: NaverMapCameraState,
    properties: MapProperties,
    uiSettings: MapUiSettings,
) {
    map.isIndoorEnabled = properties.isIndoorEnabled
    map.uiSettings.isCompassEnabled = uiSettings.isCompassEnabled
    map.uiSettings.isScaleBarEnabled = uiSettings.isScaleBarEnabled
    map.uiSettings.isZoomControlEnabled = uiSettings.isZoomControlEnabled
    map.uiSettings.isLocationButtonEnabled = uiSettings.isLocationButtonEnabled
    map.cameraPosition = com.naver.maps.map.CameraPosition(
        com.naver.maps.geometry.LatLng(
            cameraState.position.target.latitude,
            cameraState.position.target.longitude,
        ),
        cameraState.position.zoom,
    )
}
