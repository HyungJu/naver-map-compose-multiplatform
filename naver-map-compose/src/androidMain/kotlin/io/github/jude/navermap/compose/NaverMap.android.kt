package io.github.jude.navermap.compose

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.naver.maps.geometry.LatLngBounds as AndroidLatLngBounds
import com.naver.maps.map.CameraPosition as AndroidCameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode as AndroidLocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import java.util.Locale

private class ManagedMapView(context: Context) : MapView(context) {
    var map: NaverMap? = null
    var boundCameraState: CameraPositionState? = null
    private var cameraChangeListener: NaverMap.OnCameraChangeListener? = null
    private var cameraIdleListener: NaverMap.OnCameraIdleListener? = null

    fun bindCameraState(cameraPositionState: CameraPositionState) {
        val currentMap = map ?: return
        if (boundCameraState === cameraPositionState) {
            cameraPositionState.updateFromMap(
                position = currentMap.cameraPosition.toCommonCameraPosition(),
                locationTrackingMode = currentMap.locationTrackingMode.toCommonLocationTrackingMode(),
            )
            return
        }

        clearCameraBinding()
        boundCameraState = cameraPositionState

        cameraChangeListener = NaverMap.OnCameraChangeListener { reason, _ ->
            cameraPositionState.updateFromMap(
                position = currentMap.cameraPosition.toCommonCameraPosition(),
                isMoving = true,
                cameraUpdateReason = CameraUpdateReason.fromInt(reason),
                locationTrackingMode = currentMap.locationTrackingMode.toCommonLocationTrackingMode(),
            )
        }.also(currentMap::addOnCameraChangeListener)

        cameraIdleListener = NaverMap.OnCameraIdleListener {
            cameraPositionState.updateFromMap(
                position = currentMap.cameraPosition.toCommonCameraPosition(),
                isMoving = false,
                locationTrackingMode = currentMap.locationTrackingMode.toCommonLocationTrackingMode(),
            )
        }.also(currentMap::addOnCameraIdleListener)

        cameraPositionState.bind { position ->
            if (!currentMap.cameraPosition.matches(position)) {
                currentMap.moveCamera(CameraUpdate.toCameraPosition(position.toAndroidCameraPosition()))
            }
        }
        cameraPositionState.updateFromMap(
            position = currentMap.cameraPosition.toCommonCameraPosition(),
            locationTrackingMode = currentMap.locationTrackingMode.toCommonLocationTrackingMode(),
        )
    }

    fun clearCameraBinding() {
        val currentMap = map ?: return
        cameraChangeListener?.let(currentMap::removeOnCameraChangeListener)
        cameraIdleListener?.let(currentMap::removeOnCameraIdleListener)
        boundCameraState?.bind(null)
        cameraChangeListener = null
        cameraIdleListener = null
        boundCameraState = null
    }
}

@Composable
internal actual fun PlatformNaverMap(
    modifier: Modifier,
    cameraPositionState: CameraPositionState,
    properties: MapProperties,
    uiSettings: MapUiSettings,
    locale: String?,
    contentPadding: PaddingValues,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val managedMapView = remember { mutableStateOf<ManagedMapView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            ManagedMapView(context).apply {
                onCreate(Bundle())
                getMapAsync { naverMap ->
                    map = naverMap
                    bindCameraState(cameraPositionState)
                    applyMapState(
                        map = naverMap,
                        cameraPositionState = cameraPositionState,
                        properties = properties,
                        uiSettings = uiSettings,
                        locale = locale,
                        contentPadding = contentPadding,
                        density = density,
                        layoutDirection = layoutDirection,
                    )
                }
                managedMapView.value = this
            }
        },
        update = { view ->
            view.bindCameraState(cameraPositionState)
            view.map?.let { naverMap ->
                applyMapState(
                    map = naverMap,
                    cameraPositionState = cameraPositionState,
                    properties = properties,
                    uiSettings = uiSettings,
                    locale = locale,
                    contentPadding = contentPadding,
                    density = density,
                    layoutDirection = layoutDirection,
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
                    Lifecycle.Event.ON_DESTROY -> {
                        mapView.clearCameraBinding()
                        mapView.onDestroy()
                    }
                    else -> Unit
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView.clearCameraBinding()
            }
        }
    }
}

private fun applyMapState(
    map: NaverMap,
    cameraPositionState: CameraPositionState,
    properties: MapProperties,
    uiSettings: MapUiSettings,
    locale: String?,
    contentPadding: PaddingValues,
    density: androidx.compose.ui.unit.Density,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
) {
    map.extent = properties.extent?.toAndroidLatLngBounds()
    map.minZoom = properties.minZoom
    map.maxZoom = properties.maxZoom
    map.maxTilt = properties.maxTilt
    map.defaultCameraAnimationDuration = properties.defaultCameraAnimationDuration
    map.fpsLimit = properties.fpsLimit
    map.mapType = properties.mapType.toAndroidMapType()
    map.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BUILDING, properties.isBuildingLayerGroupEnabled)
    map.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, properties.isTransitLayerGroupEnabled)
    map.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BICYCLE, properties.isBicycleLayerGroupEnabled)
    map.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRAFFIC, properties.isTrafficLayerGroupEnabled)
    map.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, properties.isCadastralLayerGroupEnabled)
    map.setLayerGroupEnabled(NaverMap.LAYER_GROUP_MOUNTAIN, properties.isMountainLayerGroupEnabled)
    map.isLiteModeEnabled = properties.isLiteModeEnabled
    map.isNightModeEnabled = properties.isNightModeEnabled
    map.isIndoorEnabled = properties.isIndoorEnabled
    map.buildingHeight = properties.buildingHeight
    map.lightness = properties.lightness
    map.symbolScale = properties.symbolScale
    map.symbolPerspectiveRatio = properties.symbolPerspectiveRatio
    map.backgroundColor = properties.backgroundColor.toArgb()
    map.locationTrackingMode = properties.locationTrackingMode.toAndroidLocationTrackingMode()
    map.locale = locale?.let(Locale::forLanguageTag) ?: Locale.getDefault()

    with(density) {
        map.indoorFocusRadius = properties.indoorFocusRadius.roundToPx()
        map.setContentPadding(
            contentPadding.calculateLeftPadding(layoutDirection).roundToPx(),
            contentPadding.calculateTopPadding().roundToPx(),
            contentPadding.calculateRightPadding(layoutDirection).roundToPx(),
            contentPadding.calculateBottomPadding().roundToPx(),
        )
        map.uiSettings.pickTolerance = uiSettings.pickTolerance.roundToPx()
        map.uiSettings.setLogoMargin(
            uiSettings.logoMargin.calculateLeftPadding(layoutDirection).roundToPx(),
            uiSettings.logoMargin.calculateTopPadding().roundToPx(),
            uiSettings.logoMargin.calculateRightPadding(layoutDirection).roundToPx(),
            uiSettings.logoMargin.calculateBottomPadding().roundToPx(),
        )
    }

    map.uiSettings.isScrollGesturesEnabled = uiSettings.isScrollGesturesEnabled
    map.uiSettings.isZoomGesturesEnabled = uiSettings.isZoomGesturesEnabled
    map.uiSettings.isTiltGesturesEnabled = uiSettings.isTiltGesturesEnabled
    map.uiSettings.isRotateGesturesEnabled = uiSettings.isRotateGesturesEnabled
    map.uiSettings.isStopGesturesEnabled = uiSettings.isStopGesturesEnabled
    map.uiSettings.scrollGesturesFriction = uiSettings.scrollGesturesFriction
    map.uiSettings.zoomGesturesFriction = uiSettings.zoomGesturesFriction
    map.uiSettings.rotateGesturesFriction = uiSettings.rotateGesturesFriction
    map.uiSettings.isCompassEnabled = uiSettings.isCompassEnabled
    map.uiSettings.isScaleBarEnabled = uiSettings.isScaleBarEnabled
    map.uiSettings.isZoomControlEnabled = uiSettings.isZoomControlEnabled
    map.uiSettings.isIndoorLevelPickerEnabled = uiSettings.isIndoorLevelPickerEnabled
    map.uiSettings.isLocationButtonEnabled = uiSettings.isLocationButtonEnabled
    map.uiSettings.isLogoClickEnabled = uiSettings.isLogoClickEnabled
    map.uiSettings.logoGravity = uiSettings.logoAlignment.toAndroidLogoGravity()

    if (!map.cameraPosition.matches(cameraPositionState.position)) {
        map.moveCamera(CameraUpdate.toCameraPosition(cameraPositionState.position.toAndroidCameraPosition()))
    }
}

private fun CameraPosition.toAndroidCameraPosition(): AndroidCameraPosition {
    return AndroidCameraPosition(
        com.naver.maps.geometry.LatLng(
            target.latitude,
            target.longitude,
        ),
        zoom,
        tilt,
        bearing,
    )
}

private fun AndroidCameraPosition.toCommonCameraPosition(): CameraPosition {
    return CameraPosition(
        target = LatLng(
            latitude = target.latitude,
            longitude = target.longitude,
        ),
        zoom = zoom,
        tilt = tilt,
        bearing = bearing,
    )
}

private fun LatLngBounds.toAndroidLatLngBounds(): AndroidLatLngBounds {
    return AndroidLatLngBounds(
        com.naver.maps.geometry.LatLng(
            southWest.latitude,
            southWest.longitude,
        ),
        com.naver.maps.geometry.LatLng(
            northEast.latitude,
            northEast.longitude,
        ),
    )
}

private fun MapType.toAndroidMapType(): NaverMap.MapType {
    return when (this) {
        MapType.Basic -> NaverMap.MapType.Basic
        MapType.Navi -> NaverMap.MapType.Navi
        MapType.Satellite -> NaverMap.MapType.Satellite
        MapType.Hybrid -> NaverMap.MapType.Hybrid
        MapType.NaviHybrid -> NaverMap.MapType.NaviHybrid
        MapType.Terrain -> NaverMap.MapType.Terrain
        MapType.None -> NaverMap.MapType.None
    }
}

private fun LocationTrackingMode.toAndroidLocationTrackingMode(): AndroidLocationTrackingMode {
    return when (this) {
        LocationTrackingMode.None -> AndroidLocationTrackingMode.None
        LocationTrackingMode.NoFollow -> AndroidLocationTrackingMode.NoFollow
        LocationTrackingMode.Follow -> AndroidLocationTrackingMode.Follow
        LocationTrackingMode.Face -> AndroidLocationTrackingMode.Face
    }
}

private fun AndroidLocationTrackingMode.toCommonLocationTrackingMode(): LocationTrackingMode {
    return when (this) {
        AndroidLocationTrackingMode.None -> LocationTrackingMode.None
        AndroidLocationTrackingMode.NoFollow -> LocationTrackingMode.NoFollow
        AndroidLocationTrackingMode.Follow -> LocationTrackingMode.Follow
        AndroidLocationTrackingMode.Face -> LocationTrackingMode.Face
    }
}

private fun LogoAlignment.toAndroidLogoGravity(): Int {
    return when (this) {
        LogoAlignment.BottomStart -> android.view.Gravity.BOTTOM or android.view.Gravity.START
        LogoAlignment.BottomEnd -> android.view.Gravity.BOTTOM or android.view.Gravity.END
        LogoAlignment.TopStart -> android.view.Gravity.TOP or android.view.Gravity.START
        LogoAlignment.TopEnd -> android.view.Gravity.TOP or android.view.Gravity.END
    }
}

private fun AndroidCameraPosition.matches(position: CameraPosition): Boolean {
    return target.latitude == position.target.latitude &&
        target.longitude == position.target.longitude &&
        zoom == position.zoom &&
        tilt == position.tilt &&
        bearing == position.bearing
}
