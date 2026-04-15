@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.jude.navermap.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.platform.LocalLayoutDirection
import cocoapods.NMapsMap.NMFCameraPosition
import cocoapods.NMapsMap.NMFCameraUpdate
import cocoapods.NMapsMap.NMFIndoorSelection
import cocoapods.NMapsMap.NMFIndoorSelectionDelegateProtocol
import cocoapods.NMapsMap.NMFLocationManager
import cocoapods.NMapsMap.NMFLocationManagerDelegateProtocol
import cocoapods.NMapsMap.NMFLogoAlign
import cocoapods.NMapsMap.NMFLogoAlignLeftBottom
import cocoapods.NMapsMap.NMFLogoAlignLeftTop
import cocoapods.NMapsMap.NMFLogoAlignRightBottom
import cocoapods.NMapsMap.NMFLogoAlignRightTop
import cocoapods.NMapsMap.NMFMapType
import cocoapods.NMapsMap.NMFMapTypeBasic
import cocoapods.NMapsMap.NMFMapTypeHybrid
import cocoapods.NMapsMap.NMFMapTypeNavi
import cocoapods.NMapsMap.NMFMapTypeNaviHybrid
import cocoapods.NMapsMap.NMFMapTypeNone
import cocoapods.NMapsMap.NMFMapTypeSatellite
import cocoapods.NMapsMap.NMFMapTypeTerrain
import cocoapods.NMapsMap.NMFMapViewCameraDelegateProtocol
import cocoapods.NMapsMap.NMFMapView
import cocoapods.NMapsMap.NMFMapViewLoadDelegateProtocol
import cocoapods.NMapsMap.NMFMapViewOptionDelegateProtocol
import cocoapods.NMapsMap.NMFMapViewTouchDelegateProtocol
import cocoapods.NMapsMap.NMFMyPositionCompass
import cocoapods.NMapsMap.NMFMyPositionDirection
import cocoapods.NMapsMap.NMFMyPositionDisabled
import cocoapods.NMapsMap.NMFMyPositionMode
import cocoapods.NMapsMap.NMFMyPositionNormal
import cocoapods.NMapsMap.NMFNaverMapView
import cocoapods.NMapsMap.NMFSymbol
import cocoapods.NMapsMap.NMF_LAYER_GROUP_BICYCLE
import cocoapods.NMapsMap.NMF_LAYER_GROUP_BUILDING
import cocoapods.NMapsMap.NMF_LAYER_GROUP_CADASTRAL
import cocoapods.NMapsMap.NMF_LAYER_GROUP_MOUNTAIN
import cocoapods.NMapsMap.NMF_LAYER_GROUP_TRAFFIC
import cocoapods.NMapsMap.NMF_LAYER_GROUP_TRANSIT
import cocoapods.NMapsMap.NMGLatLng
import cocoapods.NMapsMap.NMGLatLngBounds
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreLocation.CLLocation
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsMake
import platform.darwin.NSObject

private class ManagedNaverMapView {
    val container = NMFNaverMapView()
    var boundCameraState: CameraPositionState? = null
    var mapHandle: PlatformMapHandle? = null
    private var cameraDelegate: NMFMapViewCameraDelegateProtocol? = null
    private var touchDelegate: NMFMapViewTouchDelegateProtocol? = null
    private var optionDelegate: NMFMapViewOptionDelegateProtocol? = null
    private var loadDelegate: NMFMapViewLoadDelegateProtocol? = null
    private var indoorSelectionDelegate: NMFIndoorSelectionDelegateProtocol? = null
    private var locationDelegate: NMFLocationManagerDelegateProtocol? = null
    private var loadDelivered = false
    private var lastAppliedConfiguration: MapConfiguration? = null
    private var currentOnMapClick: (ScreenPoint, LatLng) -> Unit = { _, _ -> }
    private var currentOnMapLongClick: (ScreenPoint, LatLng) -> Unit = { _, _ -> }
    private var currentOnMapLoaded: () -> Unit = {}
    private var currentOnOptionChange: () -> Unit = {}
    private var currentOnSymbolClick: (MapSymbol) -> Boolean = { false }
    private var currentOnIndoorSelectionChange: (IndoorSelectionInfo?) -> Unit = {}
    private var currentOnLocationChange: (MapLocation) -> Unit = {}

    fun bindCameraState(cameraPositionState: CameraPositionState) {
        val map = container.mapView
        if (boundCameraState === cameraPositionState) {
            return
        }

        clearCameraBinding()
        boundCameraState = cameraPositionState
        cameraDelegate = object : NSObject(), NMFMapViewCameraDelegateProtocol {
            @ObjCSignatureOverride
            override fun mapView(mapView: NMFMapView, cameraIsChangingByReason: Long) {
                cameraPositionState.updateFromMap(
                    position = mapView.cameraPosition.toCommonCameraPosition(),
                    isMoving = true,
                    cameraUpdateReason = CameraUpdateReason.fromInt(cameraIsChangingByReason.toInt()),
                    locationTrackingMode = mapView.positionMode.toCommonTrackingMode(),
                )
            }

            @ObjCSignatureOverride
            override fun mapView(mapView: NMFMapView, cameraDidChangeByReason: Long, animated: Boolean) {
                cameraPositionState.updateFromMap(
                    position = mapView.cameraPosition.toCommonCameraPosition(),
                    isMoving = false,
                    cameraUpdateReason = CameraUpdateReason.fromInt(cameraDidChangeByReason.toInt()),
                    locationTrackingMode = mapView.positionMode.toCommonTrackingMode(),
                )
            }

            @ObjCSignatureOverride
            override fun mapViewCameraIdle(mapView: NMFMapView) {
                cameraPositionState.updateFromMap(
                    position = mapView.cameraPosition.toCommonCameraPosition(),
                    isMoving = false,
                    locationTrackingMode = mapView.positionMode.toCommonTrackingMode(),
                )
            }
        }.also(map::addCameraDelegate)
        cameraPositionState.bind { position ->
            if (!map.cameraPosition.matches(position)) {
                map.moveCamera(position.toCameraUpdate())
            }
        }
        cameraPositionState.updateFromMap(
            position = map.cameraPosition.toCommonCameraPosition(),
            locationTrackingMode = map.positionMode.toCommonTrackingMode(),
        )
    }

    fun clearCameraBinding() {
        cameraDelegate?.let(container.mapView::removeCameraDelegate)
        boundCameraState?.bind(null)
        cameraDelegate = null
        boundCameraState = null
    }

    fun bindEventCallbacks(
        onMapClick: (ScreenPoint, LatLng) -> Unit,
        onMapLongClick: (ScreenPoint, LatLng) -> Unit,
        onMapLoaded: () -> Unit,
        onOptionChange: () -> Unit,
        onSymbolClick: (MapSymbol) -> Boolean,
        onIndoorSelectionChange: (IndoorSelectionInfo?) -> Unit,
        onLocationChange: (MapLocation) -> Unit,
    ) {
        val map = container.mapView
        currentOnMapClick = onMapClick
        currentOnMapLongClick = onMapLongClick
        currentOnMapLoaded = onMapLoaded
        currentOnOptionChange = onOptionChange
        currentOnSymbolClick = onSymbolClick
        currentOnIndoorSelectionChange = onIndoorSelectionChange
        currentOnLocationChange = onLocationChange

        if (touchDelegate == null) {
            touchDelegate = object : NSObject(), NMFMapViewTouchDelegateProtocol {
                @ObjCSignatureOverride
                override fun mapView(mapView: NMFMapView, didTapMap: NMGLatLng, point: CValue<CGPoint>) {
                    currentOnMapClick(point.toCommonScreenPoint(), didTapMap.toCommonLatLng())
                }

                @ObjCSignatureOverride
                override fun mapView(mapView: NMFMapView, didLongTapMap: NMGLatLng, point: CValue<CGPoint>) {
                    currentOnMapLongClick(point.toCommonScreenPoint(), didLongTapMap.toCommonLatLng())
                }

                @ObjCSignatureOverride
                override fun mapView(mapView: NMFMapView, didTapSymbol: NMFSymbol): Boolean {
                    val position = didTapSymbol.position ?: return false
                    return currentOnSymbolClick(
                        MapSymbol(
                            caption = didTapSymbol.caption ?: "",
                            position = position.toCommonLatLng(),
                        ),
                    )
                }
            }
            map.touchDelegate = touchDelegate
        }

        if (optionDelegate == null) {
            optionDelegate = object : NSObject(), NMFMapViewOptionDelegateProtocol {
                override fun mapViewOptionChanged(mapView: NMFMapView) {
                    currentOnOptionChange()
                }
            }.also(map::addOptionDelegate)
        }

        if (indoorSelectionDelegate == null) {
            indoorSelectionDelegate = object : NSObject(), NMFIndoorSelectionDelegateProtocol {
                override fun indoorSelectionDidChanged(indoorSelection: NMFIndoorSelection?) {
                    currentOnIndoorSelectionChange(indoorSelection?.toIndoorSelectionInfo())
                }
            }.also(map::addIndoorSelectionDelegate)
        }

        if (map.loaded) {
            if (!loadDelivered) {
                loadDelivered = true
                currentOnMapLoaded()
            }
        } else {
            if (loadDelegate == null) {
                loadDelegate = object : NSObject(), NMFMapViewLoadDelegateProtocol {
                    override fun mapViewDidFinishLoadingMap(mapView: NMFMapView) {
                        loadDelivered = true
                        currentOnMapLoaded()
                    }
                }.also(map::addLoadDelegate)
            }
        }

        val sharedLocationManager = NMFLocationManager.sharedInstance()
        if (sharedLocationManager != null) {
            if (locationDelegate == null) {
                locationDelegate = object : NSObject(), NMFLocationManagerDelegateProtocol {
                    override fun locationManager(
                        locationManager: NMFLocationManager?,
                        didUpdateLocations: List<*>?,
                    ) {
                        val lastLocation = didUpdateLocations?.lastOrNull() as? CLLocation ?: return
                        currentOnLocationChange(lastLocation.toCommonMapLocation())
                    }
                }.also(sharedLocationManager::addDelegate)
            }
        } else {
            locationDelegate = null
        }
    }

    fun applyConfiguration(
        configuration: MapConfiguration,
        cameraPositionState: CameraPositionState,
    ) {
        if (lastAppliedConfiguration === configuration) {
            return
        }
        container.applyMapState(
            cameraPositionState = cameraPositionState,
            properties = configuration.properties,
            uiSettings = configuration.uiSettings,
            locale = configuration.locale,
            contentPadding = configuration.contentPadding,
            layoutDirection = configuration.layoutDirection,
        )
        lastAppliedConfiguration = configuration
    }

    fun clearEventBindings() {
        val map = container.mapView
        map.touchDelegate = null
        optionDelegate?.let(map::removeOptionDelegate)
        loadDelegate?.let(map::removeLoadDelegate)
        indoorSelectionDelegate?.let(map::removeIndoorSelectionDelegate)
        val sharedLocationManager = NMFLocationManager.sharedInstance()
        if (sharedLocationManager != null) {
            locationDelegate?.let(sharedLocationManager::removeDelegate)
        }
        touchDelegate = null
        optionDelegate = null
        loadDelegate = null
        indoorSelectionDelegate = null
        locationDelegate = null
        loadDelivered = false
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
    val layoutDirection = LocalLayoutDirection.current
    val mapConfiguration = remember(properties, uiSettings, locale, contentPadding, layoutDirection) {
        MapConfiguration(
            properties = properties,
            uiSettings = uiSettings,
            locale = locale,
            contentPadding = contentPadding,
            layoutDirection = layoutDirection,
        )
    }
    val managedMapView = remember { mutableStateOf<ManagedNaverMapView?>(null) }
    val platformMapHandleState = remember { mutableStateOf<PlatformMapHandle?>(null) }

    CompositionLocalProvider(LocalPlatformMapHandle provides platformMapHandleState.value) {
        UIKitView(
            modifier = modifier,
            factory = {
                val managed = ManagedNaverMapView()
                managed.mapHandle = PlatformMapHandle(managed.container.mapView)
                platformMapHandleState.value = managed.mapHandle
                managed.bindCameraState(cameraPositionState)
                managed.bindEventCallbacks(
                    onMapClick = onMapClick,
                    onMapLongClick = onMapLongClick,
                    onMapLoaded = onMapLoaded,
                    onOptionChange = onOptionChange,
                    onSymbolClick = onSymbolClick,
                    onIndoorSelectionChange = onIndoorSelectionChange,
                    onLocationChange = onLocationChange,
                )
                managed.applyConfiguration(mapConfiguration, cameraPositionState)
                managedMapView.value = managed
                managed.container
            },
            update = { container ->
                val managed = managedMapView.value ?: return@UIKitView
                platformMapHandleState.value = managed.mapHandle
                managed.bindCameraState(cameraPositionState)
                managed.bindEventCallbacks(
                    onMapClick = onMapClick,
                    onMapLongClick = onMapLongClick,
                    onMapLoaded = onMapLoaded,
                    onOptionChange = onOptionChange,
                    onSymbolClick = onSymbolClick,
                    onIndoorSelectionChange = onIndoorSelectionChange,
                    onLocationChange = onLocationChange,
                )
                managed.applyConfiguration(mapConfiguration, cameraPositionState)
            },
        )
        content()
    }

    DisposableEffect(cameraPositionState, managedMapView.value) {
        onDispose {
            managedMapView.value?.clearCameraBinding()
            managedMapView.value?.clearEventBindings()
            platformMapHandleState.value = null
        }
    }
}

private data class MapConfiguration(
    val properties: MapProperties,
    val uiSettings: MapUiSettings,
    val locale: String?,
    val contentPadding: PaddingValues,
    val layoutDirection: androidx.compose.ui.unit.LayoutDirection,
)

private fun NMFNaverMapView.applyMapState(
    cameraPositionState: CameraPositionState,
    properties: MapProperties,
    uiSettings: MapUiSettings,
    locale: String?,
    contentPadding: PaddingValues,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
) {
    val map = mapView

    map.extent = properties.extent?.toNativeBounds()
    map.minZoomLevel = properties.minZoom
    map.maxZoomLevel = properties.maxZoom
    map.maxTilt = properties.maxTilt
    map.animationDuration = properties.defaultCameraAnimationDuration / 1000.0
    map.preferredFramesPerSecond = properties.fpsLimit.toDouble()
    map.mapType = properties.mapType.toNativeMapType()
    map.setLayerGroup(NMF_LAYER_GROUP_BUILDING, properties.isBuildingLayerGroupEnabled)
    map.setLayerGroup(NMF_LAYER_GROUP_TRANSIT, properties.isTransitLayerGroupEnabled)
    map.setLayerGroup(NMF_LAYER_GROUP_BICYCLE, properties.isBicycleLayerGroupEnabled)
    map.setLayerGroup(NMF_LAYER_GROUP_TRAFFIC, properties.isTrafficLayerGroupEnabled)
    map.setLayerGroup(NMF_LAYER_GROUP_CADASTRAL, properties.isCadastralLayerGroupEnabled)
    map.setLayerGroup(NMF_LAYER_GROUP_MOUNTAIN, properties.isMountainLayerGroupEnabled)
    map.liteModeEnabled = properties.isLiteModeEnabled
    map.nightModeEnabled = properties.isNightModeEnabled
    map.indoorMapEnabled = properties.isIndoorEnabled
    map.indoorFocusRadius = properties.indoorFocusRadius.value.toDouble()
    map.buildingHeight = properties.buildingHeight
    map.lightness = properties.lightness.toDouble()
    map.symbolScale = properties.symbolScale.toDouble()
    map.symbolPerspectiveRatio = properties.symbolPerspectiveRatio.toDouble()
    map.backgroundColor = properties.backgroundColor.toUIColor()
    map.positionMode = properties.locationTrackingMode.toNativeTrackingMode()
    map.locale = locale
    map.contentInset = UIEdgeInsetsMake(
        top = contentPadding.calculateTopPadding().value.toDouble(),
        left = contentPadding.calculateLeftPadding(layoutDirection).value.toDouble(),
        bottom = contentPadding.calculateBottomPadding().value.toDouble(),
        right = contentPadding.calculateRightPadding(layoutDirection).value.toDouble(),
    )

    map.pickTolerance = uiSettings.pickTolerance.value.toLong()
    map.scrollGestureEnabled = uiSettings.isScrollGesturesEnabled
    map.zoomGestureEnabled = uiSettings.isZoomGesturesEnabled
    map.tiltGestureEnabled = uiSettings.isTiltGesturesEnabled
    map.rotateGestureEnabled = uiSettings.isRotateGesturesEnabled
    map.stopGestureEnabled = uiSettings.isStopGesturesEnabled
    map.scrollFriction = uiSettings.scrollGesturesFriction.toDouble()
    map.zoomFriction = uiSettings.zoomGesturesFriction.toDouble()
    map.rotateFriction = uiSettings.rotateGesturesFriction.toDouble()
    map.logoInteractionEnabled = uiSettings.isLogoClickEnabled
    map.logoAlign = uiSettings.logoAlignment.toNativeLogoAlign()
    map.logoMargin = UIEdgeInsetsMake(
        top = uiSettings.logoMargin.calculateTopPadding().value.toDouble(),
        left = uiSettings.logoMargin.calculateLeftPadding(layoutDirection).value.toDouble(),
        bottom = uiSettings.logoMargin.calculateBottomPadding().value.toDouble(),
        right = uiSettings.logoMargin.calculateRightPadding(layoutDirection).value.toDouble(),
    )

    showCompass = uiSettings.isCompassEnabled
    showScaleBar = uiSettings.isScaleBarEnabled
    showZoomControls = uiSettings.isZoomControlEnabled
    showIndoorLevelPicker = uiSettings.isIndoorLevelPickerEnabled
    showLocationButton = uiSettings.isLocationButtonEnabled

    if (!map.cameraPosition.matches(cameraPositionState.position)) {
        map.moveCamera(cameraPositionState.position.toCameraUpdate())
    }
    cameraPositionState.updateFromMap(
        position = map.cameraPosition.toCommonCameraPosition(),
        locationTrackingMode = map.positionMode.toCommonTrackingMode(),
    )
}

private fun CameraPosition.toCameraUpdate(): NMFCameraUpdate {
    return NMFCameraUpdate.cameraUpdateWithPosition(
        NMFCameraPosition.cameraPosition(
            target = target.toNativeLatLng(),
            zoom = zoom,
            tilt = tilt,
            heading = bearing,
        ),
    )
}

private fun NMFCameraPosition.matches(position: CameraPosition): Boolean {
    return target().lat() == position.target.latitude &&
        target().lng() == position.target.longitude &&
        zoom() == position.zoom &&
        tilt() == position.tilt &&
        heading() == position.bearing
}

private fun NMFCameraPosition.toCommonCameraPosition(): CameraPosition {
    return CameraPosition(
        target = target().toCommonLatLng(),
        zoom = zoom(),
        tilt = tilt(),
        bearing = heading(),
    )
}

private fun LatLng.toNativeLatLng(): NMGLatLng {
    return NMGLatLng.latLngWithLat(latitude, longitude)
}

private fun NMGLatLng.toCommonLatLng(): LatLng {
    return LatLng(
        latitude = lat(),
        longitude = lng(),
    )
}

private fun CValue<CGPoint>.toCommonScreenPoint(): ScreenPoint {
    return useContents {
        ScreenPoint(x = x.toFloat(), y = y.toFloat())
    }
}

private fun NMFIndoorSelection.toIndoorSelectionInfo(): IndoorSelectionInfo {
    return IndoorSelectionInfo(
        zoneId = zone.zoneId,
        levelId = level.indoorView.levelId,
        zoneIndex = zoneIndex.toInt(),
        levelIndex = levelIndex.toInt(),
    )
}

private fun CLLocation.toCommonMapLocation(): MapLocation {
    val coordinate = coordinate.useContents {
        LatLng(latitude = latitude, longitude = longitude)
    }
    return MapLocation(
        latitude = coordinate.latitude,
        longitude = coordinate.longitude,
        accuracyMeters = horizontalAccuracy.takeIf { it >= 0.0 }?.toFloat(),
        bearing = course.takeIf { it >= 0.0 }?.toFloat(),
        speedMetersPerSecond = speed.takeIf { it >= 0.0 }?.toFloat(),
        altitudeMeters = altitude,
        timestampMillis = (timestamp.timeIntervalSince1970 * 1000).toLong(),
    )
}

private fun LatLngBounds.toNativeBounds(): NMGLatLngBounds {
    return NMGLatLngBounds.latLngBoundsSouthWest(
        southWest = southWest.toNativeLatLng(),
        northEast = northEast.toNativeLatLng(),
    )
}

private fun MapType.toNativeMapType(): NMFMapType {
    return when (this) {
        MapType.Basic -> NMFMapTypeBasic
        MapType.Navi -> NMFMapTypeNavi
        MapType.Satellite -> NMFMapTypeSatellite
        MapType.Hybrid -> NMFMapTypeHybrid
        MapType.NaviHybrid -> NMFMapTypeNaviHybrid
        MapType.Terrain -> NMFMapTypeTerrain
        MapType.None -> NMFMapTypeNone
    }
}

private fun LocationTrackingMode.toNativeTrackingMode(): NMFMyPositionMode {
    return when (this) {
        LocationTrackingMode.None -> NMFMyPositionDisabled
        LocationTrackingMode.NoFollow -> NMFMyPositionNormal
        LocationTrackingMode.Follow -> NMFMyPositionDirection
        LocationTrackingMode.Face -> NMFMyPositionCompass
    }
}

private fun NMFMyPositionMode.toCommonTrackingMode(): LocationTrackingMode {
    return when (this) {
        NMFMyPositionDisabled -> LocationTrackingMode.None
        NMFMyPositionNormal -> LocationTrackingMode.NoFollow
        NMFMyPositionDirection -> LocationTrackingMode.Follow
        NMFMyPositionCompass -> LocationTrackingMode.Face
        else -> LocationTrackingMode.None
    }
}

private fun LogoAlignment.toNativeLogoAlign(): NMFLogoAlign {
    return when (this) {
        LogoAlignment.BottomStart -> NMFLogoAlignLeftBottom
        LogoAlignment.BottomEnd -> NMFLogoAlignRightBottom
        LogoAlignment.TopStart -> NMFLogoAlignLeftTop
        LogoAlignment.TopEnd -> NMFLogoAlignRightTop
    }
}

private fun Color.toUIColor(): UIColor {
    return UIColor(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )
}

actual class PlatformMapHandle(
    val nativeMap: NMFMapView,
)
