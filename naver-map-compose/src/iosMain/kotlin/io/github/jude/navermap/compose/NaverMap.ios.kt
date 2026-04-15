@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.jude.navermap.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.platform.LocalLayoutDirection
import cocoapods.NMapsMap.NMFCameraPosition
import cocoapods.NMapsMap.NMFCameraUpdate
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
import cocoapods.NMapsMap.NMFMapView
import cocoapods.NMapsMap.NMFMyPositionCompass
import cocoapods.NMapsMap.NMFMyPositionDirection
import cocoapods.NMapsMap.NMFMyPositionDisabled
import cocoapods.NMapsMap.NMFMyPositionMode
import cocoapods.NMapsMap.NMFMyPositionNormal
import cocoapods.NMapsMap.NMFNaverMapView
import cocoapods.NMapsMap.NMF_LAYER_GROUP_BICYCLE
import cocoapods.NMapsMap.NMF_LAYER_GROUP_BUILDING
import cocoapods.NMapsMap.NMF_LAYER_GROUP_CADASTRAL
import cocoapods.NMapsMap.NMF_LAYER_GROUP_MOUNTAIN
import cocoapods.NMapsMap.NMF_LAYER_GROUP_TRAFFIC
import cocoapods.NMapsMap.NMF_LAYER_GROUP_TRANSIT
import cocoapods.NMapsMap.NMGLatLng
import cocoapods.NMapsMap.NMGLatLngBounds
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsMake

@Composable
internal actual fun PlatformNaverMap(
    modifier: Modifier,
    cameraPositionState: CameraPositionState,
    properties: MapProperties,
    uiSettings: MapUiSettings,
    locale: String?,
    contentPadding: PaddingValues,
) {
    val layoutDirection = LocalLayoutDirection.current

    DisposableEffect(cameraPositionState) {
        onDispose {
            cameraPositionState.bind(null)
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            NMFNaverMapView().apply {
                cameraPositionState.bind { position ->
                    val map = mapView
                    if (!map.cameraPosition.matches(position)) {
                        map.moveCamera(position.toCameraUpdate())
                    }
                }
                applyMapState(
                    cameraPositionState = cameraPositionState,
                    properties = properties,
                    uiSettings = uiSettings,
                    locale = locale,
                    contentPadding = contentPadding,
                    layoutDirection = layoutDirection,
                )
            }
        },
        update = { container ->
            cameraPositionState.bind { position ->
                val map = container.mapView
                if (!map.cameraPosition.matches(position)) {
                    map.moveCamera(position.toCameraUpdate())
                }
            }
            container.applyMapState(
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings,
                locale = locale,
                contentPadding = contentPadding,
                layoutDirection = layoutDirection,
            )
        },
    )
}

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
    return UIColor.colorWithRed(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )
}
