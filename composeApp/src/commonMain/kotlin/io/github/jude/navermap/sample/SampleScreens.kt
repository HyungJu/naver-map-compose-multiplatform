package io.github.hyungju.navermap.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hyungju.navermap.compose.ArrowheadPathOverlay
import io.github.hyungju.navermap.compose.CameraPosition
import io.github.hyungju.navermap.compose.CircleOverlay
import io.github.hyungju.navermap.compose.CircleOverlayDefaults
import io.github.hyungju.navermap.compose.ColorPart
import io.github.hyungju.navermap.compose.DisposableMapEffect
import io.github.hyungju.navermap.compose.LatLng
import io.github.hyungju.navermap.compose.LocationOverlay
import io.github.hyungju.navermap.compose.MapEffect
import io.github.hyungju.navermap.compose.MapProperties
import io.github.hyungju.navermap.compose.MapType
import io.github.hyungju.navermap.compose.MapUiSettings
import io.github.hyungju.navermap.compose.Marker
import io.github.hyungju.navermap.compose.MarkerComposable
import io.github.hyungju.navermap.compose.MultipartPathOverlay
import io.github.hyungju.navermap.compose.NaverMap
import io.github.hyungju.navermap.compose.OverlayImage
import io.github.hyungju.navermap.compose.OverlayStyle
import io.github.hyungju.navermap.compose.PathOverlay
import io.github.hyungju.navermap.compose.PolygonOverlay
import io.github.hyungju.navermap.compose.PolygonOverlayDefaults
import io.github.hyungju.navermap.compose.PolylineOverlay
import io.github.hyungju.navermap.compose.PolylineOverlayDefaults
import io.github.hyungju.navermap.compose.currentCameraPositionState
import io.github.hyungju.navermap.compose.rememberCameraPositionState
import io.github.hyungju.navermap.compose.rememberUpdatedMarkerState
import kotlin.math.roundToInt

internal enum class SampleDestination(
    val route: String,
    val title: String,
    val summary: String,
) {
    Home(
        route = "home",
        title = "MarkerComposable 샘플",
        summary = "여러 샘플을 오가며 공용 오버레이와 대량 마커 렌더링을 검증합니다.",
    ),
    Overview(
        route = "overview",
        title = "기본 오버레이",
        summary = "기존 지도/오버레이 API와 MarkerComposable을 한 화면에서 함께 살펴봅니다.",
    ),
    RenderKey(
        route = "render-key",
        title = "renderKey 갱신",
        summary = "동일 위치 마커의 UI가 바뀔 때 renderKey로 안정적으로 다시 캡처되는지 확인합니다.",
    ),
    Nationwide(
        route = "nationwide",
        title = "전국 59개 마커",
        summary = "전국 좌표에 50개 이상 MarkerComposable을 동시에 배치해 iOS 렌더 안정성을 확인합니다.",
    ),
    ;

    companion object {
        private val detailDestinations = listOf(Overview, RenderKey, Nationwide)

        fun detailEntries(): List<SampleDestination> = detailDestinations

        fun fromRoute(route: String?): SampleDestination {
            return entries.firstOrNull { it.route == route } ?: Home
        }
    }
}

@Composable
internal fun SampleHomeScreen(
    onOpenSample: (SampleDestination) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "MarkerComposable 샘플 허브",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "구현 안정화 이후 어떤 시나리오가 가능한지 바로 확인할 수 있도록 샘플을 화면별로 분리했습니다.",
            style = MaterialTheme.typography.bodyLarge,
        )
        SampleDestination.detailEntries().forEach { destination ->
            ElevatedCard(
                onClick = { onOpenSample(destination) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = destination.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun OverviewSampleScreen() {
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
    var displayedZoom by remember { mutableStateOf(cameraPositionState.position.zoom) }
    var compositionLocalZoom by remember { mutableStateOf(cameraPositionState.position.zoom) }
    val markerState = rememberUpdatedMarkerState(position = demoMarker)

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

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            displayedZoom = cameraPositionState.position.zoom
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "공용 API 한 화면 샘플",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "지도 타입, 카메라 상태, 기본/고급 오버레이와 MarkerComposable을 Android와 iOS에서 함께 비교합니다.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "현재 줌 ${displayedZoom.formatZoom()}, 지도 타입 ${mapType.name}",
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
                    .weight(1f),
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

                LaunchedEffect(localCameraState.isMoving) {
                    if (!localCameraState.isMoving) {
                        compositionLocalZoom = localCameraState.position.zoom
                    }
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
                    captionText = "대표 마커",
                    icon = OverlayImage.GreenMarker,
                    style = OverlayStyle(
                        tag = "demo-marker",
                        globalZIndex = 200_000,
                    ),
                    onClick = {
                        lastMapEvent = "마커 클릭 대표 마커"
                        true
                    },
                )
                MarkerComposable(
                    state = rememberUpdatedMarkerState(position = composeMarker),
                    renderKey = "overview-compose-marker",
                    onClick = {
                        lastMapEvent = "마커 클릭 Compose 마커"
                        true
                    },
                ) {
                    Surface(
                        color = Color(0xFF111827),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                    ) {
                        Text(
                            text = "Compose 마커",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
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
                PathOverlay(
                    coordinates = pathPoints,
                    progress = 0.38,
                    width = 10.dp,
                    outlineWidth = 3.dp,
                    color = Color(0xFF2563EB),
                    outlineColor = Color.White,
                    passedColor = Color(0xFFF97316),
                    passedOutlineColor = Color.White,
                    style = OverlayStyle(
                        tag = "phase4-path",
                        globalZIndex = -100_000,
                    ),
                    onClick = {
                        lastMapEvent = "경로 오버레이 클릭"
                        true
                    },
                )
                MultipartPathOverlay(
                    coordinateParts = multipartPathParts,
                    colorParts = multipartPathColors,
                    progress = 0.2,
                    width = 9.dp,
                    outlineWidth = 2.dp,
                    style = OverlayStyle(
                        tag = "phase4-multipart-path",
                        globalZIndex = -100_000,
                    ),
                    onClick = {
                        lastMapEvent = "멀티 파트 경로 클릭"
                        true
                    },
                )
                ArrowheadPathOverlay(
                    coordinates = arrowPathPoints,
                    width = 8.dp,
                    headSizeRatio = 2.6f,
                    color = Color(0xFF111827),
                    outlineWidth = 2.dp,
                    outlineColor = Color.White,
                    elevation = 2.dp,
                    style = OverlayStyle(
                        tag = "phase4-arrowhead-path",
                        globalZIndex = 100_000,
                    ),
                    onClick = {
                        lastMapEvent = "화살표 경로 클릭"
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
                LocationOverlay(
                    position = demoLocation,
                    bearing = 16f,
                    icon = OverlayImage.LocationDefault,
                    circleRadius = 22.dp,
                    circleColor = Color(0x443B82F6),
                    circleOutlineWidth = 2.dp,
                    circleOutlineColor = Color(0xFF1D4ED8),
                    style = OverlayStyle(
                        tag = "demo-location-overlay",
                        globalZIndex = 300_000,
                    ),
                    onClick = {
                        lastMapEvent = "위치 오버레이 클릭"
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

@Composable
internal fun RenderKeyMarkerSampleScreen() {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(
            target = LatLng(37.548, 126.994),
            zoom = 11.9,
        )
    }
    var refreshRound by remember { mutableIntStateOf(0) }
    var highlightDeals by remember { mutableStateOf(false) }
    var selectedMarker by remember { mutableStateOf("아직 선택한 마커가 없습니다.") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "renderKey 재캡처 샘플",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "동일한 위치에 있는 마커라도 값이 바뀌면 renderKey를 통해 이미지를 다시 만들고, 직전 정상 이미지는 유지합니다.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "현재 라운드 ${refreshRound + 1} / 선택 마커: $selectedMarker",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { refreshRound += 1 },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("가격 갱신")
                }
                OutlinedButton(
                    onClick = { highlightDeals = !highlightDeals },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (highlightDeals) "강조 해제" else "특가 강조")
                }
            }
            NaverMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                cameraPositionState = cameraPositionState,
                uiSettings = remember {
                    MapUiSettings(
                        isCompassEnabled = true,
                        isScaleBarEnabled = false,
                        isZoomControlEnabled = true,
                    )
                },
                locale = "ko-KR",
            ) {
                Marker(
                    state = rememberUpdatedMarkerState(position = LatLng(37.5513, 126.9882)),
                    captionText = "Native 기준 마커",
                    icon = OverlayImage.GreenMarker,
                )

                renderKeyStations.forEachIndexed { index, station ->
                    val offsetStep = ((refreshRound + index) % 5) - 2
                    val price = station.basePrice + (offsetStep * station.step)
                    val estimatedWait = 2 + ((refreshRound + index) % 6)
                    val highlight = highlightDeals && ((refreshRound + index) % 2 == 0)

                    MarkerComposable(
                        state = rememberUpdatedMarkerState(position = station.position),
                        renderKey = listOf(
                            station.id,
                            price,
                            estimatedWait,
                            highlight,
                        ),
                        onClick = {
                            selectedMarker = "${station.name} ${price}원"
                            true
                        },
                    ) {
                        DynamicPriceMarker(
                            station = station,
                            price = price,
                            estimatedWait = estimatedWait,
                            highlight = highlight,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NationwideMarkerSampleScreen() {
    val defaultFocus = nationwideRegionFocuses.first()
    val cameraPositionState = rememberCameraPositionState {
        position = defaultFocus.cameraPosition
    }
    var selectedMarker by remember { mutableStateOf("아직 선택한 전국 마커가 없습니다.") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "전국 MarkerComposable 스트레스 샘플",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "전국 59개 위치에 MarkerComposable을 동시에 렌더링합니다. iOS에서는 동시 렌더를 제한하고, 실패 시 마지막 정상 이미지를 유지하는 흐름을 확인할 수 있습니다.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "총 ${nationwideMarkers.size}개 마커 / 선택: $selectedMarker",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                nationwideRegionFocuses.forEach { focus ->
                    OutlinedButton(
                        onClick = {
                            cameraPositionState.position = focus.cameraPosition
                        },
                    ) {
                        Text(focus.label)
                    }
                }
            }
            NaverMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                cameraPositionState = cameraPositionState,
                uiSettings = remember {
                    MapUiSettings(
                        isCompassEnabled = true,
                        isScaleBarEnabled = true,
                        isZoomControlEnabled = true,
                    )
                },
                locale = "ko-KR",
            ) {
                nationwideMarkers.forEach { marker ->
                    MarkerComposable(
                        state = rememberUpdatedMarkerState(position = marker.position),
                        renderKey = marker.id,
                        onClick = {
                            selectedMarker = "${marker.badge} ${marker.name} · ${marker.category}"
                            true
                        },
                    ) {
                        NationwideMarkerChip(marker = marker)
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicPriceMarker(
    station: DynamicMarkerStation,
    price: Int,
    estimatedWait: Int,
    highlight: Boolean,
) {
    val containerColor = if (highlight) station.accentColor else Color.White
    val contentColor = if (highlight) Color.White else Color(0xFF111827)
    val borderColor = if (highlight) Color.Transparent else station.accentColor.copy(alpha = 0.28f)

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${station.brand} ${station.name}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${price}원/L",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "대기 ${estimatedWait}분",
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) Color.White.copy(alpha = 0.9f) else station.accentColor,
            )
        }
    }
}

@Composable
private fun NationwideMarkerChip(
    marker: NationwideMarkerSample,
) {
    Surface(
        color = Color.White,
        contentColor = Color(0xFF111827),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, marker.accentColor.copy(alpha = 0.22f)),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = marker.accentColor,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = marker.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column {
                Text(
                    text = marker.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = marker.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = marker.accentColor,
                )
            }
        }
    }
}

private val cityHall = LatLng(37.5666102, 126.9783881)
private val demoMarker = LatLng(37.56722, 126.97992)
private val composeMarker = LatLng(37.56805, 126.98055)

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

private val pathPoints = listOf(
    LatLng(37.56705, 126.97688),
    LatLng(37.56732, 126.97756),
    LatLng(37.56748, 126.97837),
    LatLng(37.5673, 126.97904),
)

private val multipartPathParts = listOf(
    listOf(
        LatLng(37.5653, 126.97675),
        LatLng(37.56572, 126.97735),
        LatLng(37.56602, 126.97798),
    ),
    listOf(
        LatLng(37.56602, 126.97798),
        LatLng(37.56642, 126.97864),
        LatLng(37.56692, 126.97928),
    ),
)

private val multipartPathColors = listOf(
    ColorPart(
        color = Color(0xFF22C55E),
        outlineColor = Color.White,
        passedColor = Color(0xFF15803D),
        passedOutlineColor = Color.White,
    ),
    ColorPart(
        color = Color(0xFFEF4444),
        outlineColor = Color.White,
        passedColor = Color(0xFFB91C1C),
        passedOutlineColor = Color.White,
    ),
)

private val arrowPathPoints = listOf(
    LatLng(37.56615, 126.97905),
    LatLng(37.56682, 126.97962),
)

private val demoLocation = LatLng(37.56562, 126.97888)

private fun Double.formatCoordinate(): String {
    return ((this * 100000).roundToInt() / 100000.0).toString()
}

private fun Double.formatZoom(): String {
    return ((this * 10).roundToInt() / 10.0).toString()
}
