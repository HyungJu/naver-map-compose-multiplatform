# NAVER Map Compose Multiplatform

[![Maven Central](https://img.shields.io/maven-central/v/io.github.hyungju.navermap/naver-map-compose.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.hyungju.navermap/naver-map-compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.3-4285F4.svg)](https://www.jetbrains.com/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

이 라이브러리는 Compose Multiplatform에서 사용할 수 있는 NAVER Map API를 제공합니다.

## Sample App

이 저장소에는 Android와 iOS에서 지도 API의 동작을 확인할 수 있는 샘플 앱이 포함되어 있습니다. 

## Download

```kotlin
repositories {
    google()
    mavenCentral()
    maven("https://repository.map.naver.com/archive/maven")
}

dependencies {
    implementation("io.github.hyungju.navermap:naver-map-compose:0.1.0")
}
```


## Usage

### Kotlin 코드에서 client ID 주입하기

앱 코드에서 NAVER client ID를 직접 공급해야 합니다.

```kotlin
NaverMapAuthProvider(
    ncpKeyId = "YOUR_NCP_KEY_ID_HERE",
) {
    NaverMap(
        modifier = Modifier.fillMaxSize(),
    )
}
```


### 지도 추가하기

```kotlin
NaverMapAuthProvider(
    ncpKeyId = naverClientId,
) {
    NaverMap(
        modifier = Modifier.fillMaxSize(),
    )
}
```

### 지도 구성하기

`MapProperties`와 `MapUiSettings`를 사용해 지도 타입, 레이어, 제스처, 컨트롤을 공통 코드에서 설정할 수 있습니다. 아래 예제들은 상위에서 `NaverMapAuthProvider`로 감쌌다고 가정합니다.

```kotlin
var mapProperties by remember {
    mutableStateOf(
        MapProperties(
            mapType = MapType.Basic,
            isTrafficLayerGroupEnabled = true,
            isIndoorEnabled = false,
        )
    )
}

var mapUiSettings by remember {
    mutableStateOf(
        MapUiSettings(
            isCompassEnabled = true,
            isScaleBarEnabled = true,
            isZoomControlEnabled = true,
        )
    )
}

NaverMap(
    modifier = Modifier.fillMaxSize(),
    properties = mapProperties,
    uiSettings = mapUiSettings,
)
```

### 카메라 상태 제어하기

`CameraPositionState`를 통해 카메라 위치를 관찰하고 변경할 수 있습니다.

```kotlin
val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition(
        target = LatLng(37.5666102, 126.9783881),
        zoom = 14.0,
    )
}

Box(Modifier.fillMaxSize()) {
    NaverMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
    )

    Button(
        onClick = {
            cameraPositionState.position = cameraPositionState.position.copy(
                zoom = cameraPositionState.position.zoom + 1,
            )
        }
    ) {
        Text("Zoom In")
    }
}
```

### 오버레이와 MarkerComposable 사용하기

기본 `Marker`뿐 아니라 Compose UI를 그대로 캡처해 지도 아이콘으로 사용하는 `MarkerComposable`과 경로/도형/위치 오버레이를 함께 사용할 수 있습니다.

```kotlin
val cityHall = LatLng(37.5666102, 126.9783881)

NaverMap(
    modifier = Modifier.fillMaxSize(),
) {
    Marker(
        state = rememberUpdatedMarkerState(position = cityHall),
        captionText = "대표 마커",
        icon = OverlayImage.GreenMarker,
    )

    MarkerComposable(
        state = rememberUpdatedMarkerState(
            position = LatLng(37.5673, 126.9792),
        ),
    ) {
        Surface(
            color = Color(0xFF111827),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = "Compose 마커",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }

    CircleOverlay(
        center = cityHall,
        radiusMeters = 450.0,
        fillColor = Color(0x332A6CF0),
        outlineWidth = 2.dp,
        outlineColor = Color(0xFF2A6CF0),
    )
}
```

### 지도 이벤트 처리하기

클릭, 롱클릭, 지도 로드 완료, 옵션 변경, 실내지도 선택, 위치 변경 등의 이벤트를 콜백으로 받을 수 있습니다.

```kotlin
var lastEvent by remember { mutableStateOf("대기 중") }

NaverMap(
    modifier = Modifier.fillMaxSize(),
    onMapLoaded = {
        lastEvent = "지도 로드 완료"
    },
    onMapClick = { _, latLng ->
        lastEvent = "클릭: ${latLng.latitude}, ${latLng.longitude}"
    },
    onMapLongClick = { _, latLng ->
        lastEvent = "롱클릭: ${latLng.latitude}, ${latLng.longitude}"
    },
    onOptionChange = {
        lastEvent = "지도 옵션 변경"
    },
)
```

## Supported Features

다음 기능을 지원합니다.

- `NaverMap`
- `CameraPositionState`
- `rememberCameraPositionState`
- `currentCameraPositionState`
- `MarkerState`
- `rememberMarkerState`
- `rememberUpdatedMarkerState`
- `MapProperties`
- `MapUiSettings`
- `OverlayStyle`
- `MapEffect`
- `DisposableMapEffect`
- `Marker`
- `MarkerComposable`
- `CircleOverlay`
- `PolygonOverlay`
- `PolylineOverlay`
- `GroundOverlay`
- `PathOverlay`
- `MultipartPathOverlay`
- `ArrowheadPathOverlay`
- `LocationOverlay`
- 지도 클릭/롱클릭/더블탭/투핑거탭 이벤트
- 지도 로드 완료, 옵션 변경, 실내지도, 위치 변경 이벤트
