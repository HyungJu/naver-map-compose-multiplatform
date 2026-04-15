# NAVER Map Compose Multiplatform

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jude.navermap/naver-map-compose.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.jude.navermap/naver-map-compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.3-4285F4.svg)](https://www.jetbrains.com/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

이 라이브러리는 Kotlin Multiplatform과 Compose Multiplatform에서 사용할 수 있는 NAVER Map API를 제공합니다. Android와 iOS에서 공통 Compose API로 지도를 렌더링하고, 카메라 상태와 지도 옵션, 주요 이벤트를 공유하는 것을 목표로 합니다.

이 프로젝트는 [`fornewid/naver-map-compose`](https://github.com/fornewid/naver-map-compose)를 중요한 업스트림 레퍼런스로 삼아 시작했습니다. 다만 Android 전용 API를 그대로 옮기기보다, 멀티플랫폼 환경에 맞는 공통 추상화를 우선하는 방향으로 설계하고 있습니다.

## Sample App

이 저장소에는 Android와 iOS에서 공용 API를 확인할 수 있는 샘플 앱이 포함되어 있습니다. 실행하려면 먼저 NAVER 지도 클라이언트 ID를 준비해야 합니다.

1. [NAVER Maps Android SDK 시작하기](https://navermaps.github.io/android-map-sdk/guide-ko/1.html)를 참고해 클라이언트 ID를 발급받습니다.
2. Android용 `local.properties`를 준비합니다.

```properties
sdk.dir=/Users/your-user/Library/Android/sdk
naver.map.client.id=YOUR_NCP_KEY_ID_HERE
```

3. iOS용 `iosApp/Config/LocalSecrets.xcconfig`를 준비합니다.

```xcconfig
NAVER_MAP_CLIENT_ID = YOUR_NCP_KEY_ID_HERE
```

4. Android 샘플을 빌드하고 실행합니다.

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug
```

5. iOS 샘플을 실행합니다.

```bash
cd iosApp
pod install
open iosApp.xcworkspace
```

## Download

### `naver-map-compose`

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jude.navermap/naver-map-compose.svg?label=naver-map-compose)](https://search.maven.org/artifact/io.github.jude.navermap/naver-map-compose)

```kotlin
repositories {
    google()
    mavenCentral()
    maven("https://repository.map.naver.com/archive/maven")
}

dependencies {
    implementation("io.github.jude.navermap:naver-map-compose:0.1.0")
}
```

## Warnings

이 라이브러리는 내부적으로 NAVER Maps SDK를 사용합니다. 사용 전에 다음 내용을 확인해주세요.

1. NAVER Maps Android SDK는 `https://repository.map.naver.com/archive/maven` 저장소에서 배포됩니다. 따라서 루트 프로젝트의 저장소 설정에 NAVER Maven 저장소를 추가해야 합니다.

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        maven("https://repository.map.naver.com/archive/maven")
        mavenCentral()
    }
}
```

2. 샘플 앱과 실제 앱 모두 NAVER Cloud Platform 지도 클라이언트 ID가 필요합니다.
3. 현재 라이브러리는 공통 지도 호스트, 카메라 상태, 주요 지도 옵션, 이벤트 콜백 중심으로 제공됩니다. 오버레이 전반과 일부 고급 기능은 아직 확장 중입니다.

## Usage

### 지도 추가하기

```kotlin
NaverMap(
    modifier = Modifier.fillMaxSize(),
)
```

### 지도 구성하기

`MapProperties`와 `MapUiSettings`를 사용해 지도 타입, 레이어, 제스처, 컨트롤을 공통 코드에서 설정할 수 있습니다.

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

### Raw Map 객체 다루기

특정 use case에서는 플랫폼의 raw 지도 객체가 필요할 수 있습니다. 이때 `MapEffect` 또는 `DisposableMapEffect`를 사용해 플랫폼 핸들에 접근할 수 있습니다.

```kotlin
NaverMap {
    MapEffect(Unit) { mapHandle ->
        println("Connected to map handle: $mapHandle")
    }

    DisposableMapEffect(Unit) { mapHandle ->
        println("Map handle attached: $mapHandle")
        onDispose {
            println("Map handle detached")
        }
    }
}
```

## Supported Features

현재 저장소에서 우선 지원하는 범위는 다음과 같습니다.

- `NaverMap`
- `CameraPositionState`
- `rememberCameraPositionState`
- `currentCameraPositionState`
- `MapProperties`
- `MapUiSettings`
- `MapEffect`
- `DisposableMapEffect`
- 지도 클릭/롱클릭/더블탭/투핑거탭 이벤트
- 지도 로드 완료, 옵션 변경, 실내지도, 위치 변경 이벤트

다음 기능은 아직 작업 중이거나 설계 단계에 있습니다.

- 마커 및 각종 오버레이 컴포저블
- 위치 소스 헬퍼
- 업스트림 대비 기능 패리티 확대

## Snapshots

개발 중인 버전을 사용하려면 Sonatype snapshot 저장소를 추가해 SNAPSHOT 빌드를 참조할 수 있습니다.

```kotlin
repositories {
    maven("https://central.sonatype.com/repository/maven-snapshots")
}

dependencies {
    implementation("io.github.jude.navermap:naver-map-compose:0.1.1-SNAPSHOT")
}
```

## Contributions

- 버그를 발견했거나 질문이 있다면 이슈를 등록해주세요.
- 새로운 기능을 제안하거나 구현하려면 먼저 이슈에서 방향을 맞춘 뒤 PR을 보내주시면 좋습니다.
- 이 프로젝트는 업스트림 `naver-map-compose`의 사용성을 존중하면서도, KMP 친화적인 API를 지향합니다.

## License

```text
Copyright 2026 Jude

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
