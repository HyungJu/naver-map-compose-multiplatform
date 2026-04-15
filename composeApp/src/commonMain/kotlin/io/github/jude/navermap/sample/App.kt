package io.github.jude.navermap.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.jude.navermap.compose.CameraPosition
import io.github.jude.navermap.compose.CircleOverlay
import io.github.jude.navermap.compose.CircleOverlayDefaults
import io.github.jude.navermap.compose.LatLng
import io.github.jude.navermap.compose.GroundOverlay
import io.github.jude.navermap.compose.GroundOverlayDefaults
import io.github.jude.navermap.compose.DisposableMapEffect
import io.github.jude.navermap.compose.Marker
import io.github.jude.navermap.compose.OverlayImage
import io.github.jude.navermap.compose.OverlayStyle
import io.github.jude.navermap.compose.MapEffect
import io.github.jude.navermap.compose.MapProperties
import io.github.jude.navermap.compose.MapType
import io.github.jude.navermap.compose.MapUiSettings
import io.github.jude.navermap.compose.NaverMap
import io.github.jude.navermap.compose.currentCameraPositionState
import io.github.jude.navermap.compose.LatLngBounds
import io.github.jude.navermap.compose.PolygonOverlay
import io.github.jude.navermap.compose.PolygonOverlayDefaults
import io.github.jude.navermap.compose.PolylineOverlay
import io.github.jude.navermap.compose.PolylineOverlayDefaults
import io.github.jude.navermap.compose.rememberCameraPositionState
import io.github.jude.navermap.compose.rememberUpdatedMarkerState
import kotlin.math.roundToInt

@Composable
fun App() {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(
            target = LatLng(37.5666102, 126.9783881),
            zoom = 14.0,
        )
    }
    var mapType by remember { mutableStateOf(MapType.Basic) }
    var trafficEnabled by remember { mutableStateOf(false) }
    var indoorEnabled by remember { mutableStateOf(false) }
    var mapLoaded by remember { mutableStateOf(false) }
    var rawMapEffectState by remember { mutableStateOf("연결 대기 중") }
    var effectLifecycleState by remember { mutableStateOf("비활성") }
    var lastMapEvent by remember { mutableStateOf("대기 중") }
    var compositionLocalZoom by remember { mutableStateOf(cameraPositionState.position.zoom) }
    val markerState = rememberUpdatedMarkerState(position = cityHall)

    val properties = remember(mapType, trafficEnabled, indoorEnabled) {
        MapProperties(
            mapType = mapType,
            isTrafficLayerGroupEnabled = trafficEnabled,
            isIndoorEnabled = indoorEnabled,
        )
    }
    val uiSettings = remember {
        MapUiSettings(
            isCompassEnabled = true,
            isScaleBarEnabled = true,
            isZoomControlEnabled = true,
        )
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "NAVER Map KMP 샘플",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "공용 Compose API로 지도 타입, 카메라 상태, 기본 오버레이를 Android와 iOS에서 함께 검증합니다.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "현재 줌 ${cameraPositionState.position.zoom}, 지도 타입 ${mapType.name}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "지도 로드 상태: ${if (mapLoaded) "완료" else "대기 중"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "원시 지도 이펙트: $rawMapEffectState / 수명주기: $effectLifecycleState",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "CompositionLocal 줌 ${compositionLocalZoom.formatZoom()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "마지막 지도 이벤트: $lastMapEvent",
                    style = MaterialTheme.typography.bodyMedium,
                )
                NaverMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    cameraPositionState = cameraPositionState,
                    properties = properties,
                    uiSettings = uiSettings,
                    locale = "ko-KR",
                    onMapLoaded = {
                        mapLoaded = true
                        lastMapEvent = "지도 로드 완료"
                    },
                    onMapClick = { _, latLng ->
                        lastMapEvent = "지도 탭 ${latLng.latitude.formatCoordinate()}, ${latLng.longitude.formatCoordinate()}"
                    },
                    onMapLongClick = { _, latLng ->
                        lastMapEvent = "지도 롱탭 ${latLng.latitude.formatCoordinate()}, ${latLng.longitude.formatCoordinate()}"
                    },
                    onOptionChange = {
                        lastMapEvent = "지도 옵션 변경"
                    },
                    onIndoorSelectionChange = { selection ->
                        lastMapEvent = if (selection == null) {
                            "실내지도 선택 해제"
                        } else {
                            "실내지도 ${selection.zoneId ?: "알 수 없음"} / ${selection.levelId ?: "알 수 없음"}"
                        }
                    },
                    onLocationChange = { location ->
                        lastMapEvent = "위치 변경 ${location.latitude.formatCoordinate()}, ${location.longitude.formatCoordinate()}"
                    },
                ) {
                    val localCameraState = currentCameraPositionState

                    LaunchedEffect(localCameraState.position) {
                        compositionLocalZoom = localCameraState.position.zoom
                    }
                    MapEffect(Unit) {
                        rawMapEffectState = "연결됨"
                        if (!mapLoaded) {
                            mapLoaded = true
                            if (lastMapEvent == "대기 중") {
                                lastMapEvent = "지도 준비 완료"
                            }
                        }
                    }
                    DisposableMapEffect(Unit) {
                        effectLifecycleState = "활성"
                        onDispose {
                            rawMapEffectState = "연결 해제"
                            effectLifecycleState = "비활성"
                        }
                    }
                    Marker(
                        state = markerState,
                        captionText = "서울시청",
                        icon = OverlayImage.GreenMarker,
                        style = OverlayStyle(
                            tag = "city-hall-marker",
                            globalZIndex = 200_000,
                        ),
                        onClick = {
                            lastMapEvent = "마커 클릭 서울시청"
                            true
                        },
                    )
                    CircleOverlay(
                        center = cityHall,
                        radiusMeters = 450.0,
                        fillColor = Color(0x332A6CF0),
                        outlineWidth = 2.dp,
                        outlineColor = Color(0xFF2A6CF0),
                        style = OverlayStyle(
                            tag = "city-hall-circle",
                            globalZIndex = CircleOverlayDefaults.GlobalZIndex,
                        ),
                        onClick = {
                            lastMapEvent = "원 오버레이 클릭"
                            true
                        },
                    )
                    PolylineOverlay(
                        coordinates = routePoints,
                        width = 6.dp,
                        color = Color(0xFF0F8A5F),
                        style = OverlayStyle(
                            tag = "route-line",
                            globalZIndex = PolylineOverlayDefaults.GlobalZIndex,
                        ),
                        onClick = {
                            lastMapEvent = "폴리라인 클릭"
                            true
                        },
                    )
                    PolygonOverlay(
                        coordinates = plazaPolygon,
                        fillColor = Color(0x33F28C28),
                        outlineWidth = 2.dp,
                        outlineColor = Color(0xFFF28C28),
                        style = OverlayStyle(
                            tag = "plaza-polygon",
                            globalZIndex = PolygonOverlayDefaults.GlobalZIndex,
                        ),
                        onClick = {
                            lastMapEvent = "폴리곤 클릭"
                            true
                        },
                    )
                    GroundOverlay(
                        bounds = groundBounds,
                        image = GroundOverlayDefaults.Image,
                        alpha = 0.45f,
                        style = OverlayStyle(
                            tag = "ground-highlight",
                            globalZIndex = GroundOverlayDefaults.GlobalZIndex,
                        ),
                        onClick = {
                            lastMapEvent = "지상 오버레이 클릭"
                            true
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            cameraPositionState.position = cameraPositionState.position.copy(
                                zoom = cameraPositionState.position.zoom + 1,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("줌 인")
                    }
                    OutlinedButton(
                        onClick = {
                            cameraPositionState.position = cameraPositionState.position.copy(
                                zoom = (cameraPositionState.position.zoom - 1).coerceAtLeast(0.0),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("줌 아웃")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            mapType = if (mapType == MapType.Basic) {
                                MapType.Hybrid
                            } else {
                                MapType.Basic
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("지도 타입 전환")
                    }
                    OutlinedButton(
                        onClick = { trafficEnabled = !trafficEnabled },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (trafficEnabled) "교통 끄기" else "교통 켜기")
                    }
                }
                OutlinedButton(
                    onClick = { indoorEnabled = !indoorEnabled },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (indoorEnabled) "실내지도 끄기" else "실내지도 켜기")
                }
            }
        }
    }
}

private val cityHall = LatLng(37.5666102, 126.9783881)

private val routePoints = listOf(
    LatLng(37.56555, 126.97732),
    LatLng(37.56605, 126.97805),
    LatLng(37.5667, 126.97885),
    LatLng(37.56718, 126.97953),
)

private val plazaPolygon = listOf(
    LatLng(37.56594, 126.97778),
    LatLng(37.56628, 126.97875),
    LatLng(37.56693, 126.97834),
    LatLng(37.56657, 126.97736),
)

private val groundBounds = LatLngBounds(
    southWest = LatLng(37.56572, 126.97764),
    northEast = LatLng(37.56682, 126.97892),
)

private fun Double.formatCoordinate(): String {
    return ((this * 100000).roundToInt() / 100000.0).toString()
}

private fun Double.formatZoom(): String {
    return ((this * 10).roundToInt() / 10.0).toString()
}
