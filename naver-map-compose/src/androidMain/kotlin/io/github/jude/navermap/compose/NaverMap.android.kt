package io.github.hyungju.navermap.compose

import android.content.Context
import android.graphics.PointF
import android.os.Bundle
import android.location.Location
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
import com.naver.maps.map.NaverMapSdk
import java.util.Locale

private class ManagedMapView(context: Context) : MapView(context) {
    var map: NaverMap? = null
    var boundCameraState: CameraPositionState? = null
    var mapHandle: PlatformMapHandle? = null
    private var cameraChangeListener: NaverMap.OnCameraChangeListener? = null
    private var cameraIdleListener: NaverMap.OnCameraIdleListener? = null
    private var optionChangeListener: NaverMap.OnOptionChangeListener? = null
    private var loadListener: NaverMap.OnLoadListener? = null
    private var indoorSelectionChangeListener: NaverMap.OnIndoorSelectionChangeListener? = null
    private var locationChangeListener: NaverMap.OnLocationChangeListener? = null
    private var loadDelivered = false

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

    fun bindEventCallbacks(
        onMapClick: (ScreenPoint, LatLng) -> Unit,
        onMapLongClick: (ScreenPoint, LatLng) -> Unit,
        onMapDoubleTap: (ScreenPoint, LatLng) -> Boolean,
        onMapTwoFingerTap: (ScreenPoint, LatLng) -> Boolean,
        onMapLoaded: () -> Unit,
        onOptionChange: () -> Unit,
        onSymbolClick: (MapSymbol) -> Boolean,
        onIndoorSelectionChange: (IndoorSelectionInfo?) -> Unit,
        onLocationChange: (MapLocation) -> Unit,
    ) {
        val currentMap = map ?: return

        currentMap.setOnMapClickListener { point, latLng ->
            onMapClick(point.toCommonScreenPoint(), latLng.toCommonLatLng())
        }
        currentMap.setOnMapLongClickListener { point, latLng ->
            onMapLongClick(point.toCommonScreenPoint(), latLng.toCommonLatLng())
        }
        currentMap.setOnMapDoubleTapListener { point, latLng ->
            onMapDoubleTap(point.toCommonScreenPoint(), latLng.toCommonLatLng())
        }
        currentMap.setOnMapTwoFingerTapListener { point, latLng ->
            onMapTwoFingerTap(point.toCommonScreenPoint(), latLng.toCommonLatLng())
        }
        currentMap.setOnSymbolClickListener { symbol ->
            onSymbolClick(
                MapSymbol(
                    caption = symbol.caption,
                    position = symbol.position.toCommonLatLng(),
                ),
            )
        }

        optionChangeListener?.let(currentMap::removeOnOptionChangeListener)
        optionChangeListener = NaverMap.OnOptionChangeListener {
            onOptionChange()
        }.also(currentMap::addOnOptionChangeListener)

        indoorSelectionChangeListener?.let(currentMap::removeOnIndoorSelectionChangeListener)
        indoorSelectionChangeListener = NaverMap.OnIndoorSelectionChangeListener { selection ->
            onIndoorSelectionChange(selection?.toIndoorSelectionInfo())
        }.also(currentMap::addOnIndoorSelectionChangeListener)

        locationChangeListener?.let(currentMap::removeOnLocationChangeListener)
        locationChangeListener = NaverMap.OnLocationChangeListener { location ->
            onLocationChange(location.toCommonMapLocation())
        }.also(currentMap::addOnLocationChangeListener)

        if (currentMap.isLoaded) {
            if (!loadDelivered) {
                loadDelivered = true
            }
            onMapLoaded()
        } else {
            loadListener?.let(currentMap::removeOnLoadListener)
            loadListener = NaverMap.OnLoadListener {
                loadDelivered = true
                onMapLoaded()
            }.also(currentMap::addOnLoadListener)
        }
    }

    fun clearEventBindings() {
        val currentMap = map ?: return
        currentMap.setOnMapClickListener(null)
        currentMap.setOnMapLongClickListener(null)
        currentMap.setOnMapDoubleTapListener(null)
        currentMap.setOnMapTwoFingerTapListener(null)
        currentMap.setOnSymbolClickListener(null)
        optionChangeListener?.let(currentMap::removeOnOptionChangeListener)
        loadListener?.let(currentMap::removeOnLoadListener)
        indoorSelectionChangeListener?.let(currentMap::removeOnIndoorSelectionChangeListener)
        locationChangeListener?.let(currentMap::removeOnLocationChangeListener)
        optionChangeListener = null
        loadListener = null
        indoorSelectionChangeListener = null
        locationChangeListener = null
        loadDelivered = false
    }
}

@Composable
internal actual fun PlatformNaverMap(
    modifier: Modifier,
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
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val managedMapView = remember { mutableStateOf<ManagedMapView?>(null) }
    val platformMapHandleState = remember { mutableStateOf<PlatformMapHandle?>(null) }

    CompositionLocalProvider(LocalPlatformMapHandle provides platformMapHandleState.value) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                authOptions?.let { options ->
                    NaverMapSdk.getInstance(context.applicationContext).client =
                        NaverMapSdk.NcpKeyClient(options.ncpKeyId)
                }
                ManagedMapView(context).apply {
                    onCreate(Bundle())
                    getMapAsync { naverMap ->
                        map = naverMap
                        val mapHandle = PlatformMapHandle(naverMap).also {
                            this.mapHandle = it
                            platformMapHandleState.value = it
                        }
                        bindCameraState(cameraPositionState)
                        bindEventCallbacks(
                            onMapClick = onMapClick,
                            onMapLongClick = onMapLongClick,
                            onMapDoubleTap = onMapDoubleTap,
                            onMapTwoFingerTap = onMapTwoFingerTap,
                            onMapLoaded = onMapLoaded,
                            onOptionChange = onOptionChange,
                            onSymbolClick = onSymbolClick,
                            onIndoorSelectionChange = onIndoorSelectionChange,
                            onLocationChange = onLocationChange,
                        )
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
                authOptions?.let { options ->
                    NaverMapSdk.getInstance(context.applicationContext).client =
                        NaverMapSdk.NcpKeyClient(options.ncpKeyId)
                }
                platformMapHandleState.value = view.mapHandle
                view.bindCameraState(cameraPositionState)
                view.bindEventCallbacks(
                    onMapClick = onMapClick,
                    onMapLongClick = onMapLongClick,
                    onMapDoubleTap = onMapDoubleTap,
                    onMapTwoFingerTap = onMapTwoFingerTap,
                    onMapLoaded = onMapLoaded,
                    onOptionChange = onOptionChange,
                    onSymbolClick = onSymbolClick,
                    onIndoorSelectionChange = onIndoorSelectionChange,
                    onLocationChange = onLocationChange,
                )
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
        content()
    }

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
                        mapView.clearEventBindings()
                        platformMapHandleState.value = null
                        mapView.onDestroy()
                    }
                    else -> Unit
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView.clearCameraBinding()
                mapView.clearEventBindings()
                platformMapHandleState.value = null
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

private fun PointF.toCommonScreenPoint(): ScreenPoint {
    return ScreenPoint(x = x, y = y)
}

private fun com.naver.maps.geometry.LatLng.toCommonLatLng(): LatLng {
    return LatLng(latitude = latitude, longitude = longitude)
}

private fun Location.toCommonMapLocation(): MapLocation {
    return MapLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        bearing = if (hasBearing()) bearing else null,
        speedMetersPerSecond = if (hasSpeed()) speed else null,
        altitudeMeters = if (hasAltitude()) altitude else null,
        timestampMillis = time,
    )
}

private fun com.naver.maps.map.indoor.IndoorSelection.toIndoorSelectionInfo(): IndoorSelectionInfo {
    return IndoorSelectionInfo(
        zoneId = zone.zoneId,
        levelId = level.indoorView.levelId,
        zoneIndex = zoneIndex,
        levelIndex = levelIndex,
    )
}

actual class PlatformMapHandle(
    val nativeMap: NaverMap,
)
