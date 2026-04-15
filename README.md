# naver-map-compose-multiplatform

`naver-map-compose-multiplatform`은 NAVER Map SDK를 Android와 iOS에서 공통 Compose API로 다루기 위한 Kotlin Multiplatform 라이브러리입니다. 하나의 선언형 UI 모델로 지도 렌더링, 카메라 상태, 주요 지도 옵션, 이벤트 콜백을 공유하는 것을 목표로 합니다.

이 프로젝트는 업스트림으로 [`fornewid/naver-map-compose`](https://github.com/fornewid/naver-map-compose)를 참고해 시작했으며, Android 중심 API 경험을 유지하면서도 Compose Multiplatform 환경에 맞게 재구성하는 방향으로 발전시키고 있습니다.

## 프로젝트 상태

아직 초기 단계의 오픈소스 라이브러리입니다. 현재는 Android와 iOS 공통 지도를 띄우고, 카메라 상태와 일부 지도 속성 및 이벤트를 공유하는 흐름에 집중하고 있습니다.

- 지원 플랫폼: Android, iOS
- 라이브러리 모듈: `:naver-map-compose`
- 샘플 앱 모듈: `:composeApp`, `:iosApp`
- 현재 버전: `0.1.0-SNAPSHOT`
- 배포 상태: 저장소/소스 기준 사용 권장, 정식 배포 채널은 정리 중

## 현재 제공 범위

- 공통 `NaverMap` 컴포저블
- 공통 `CameraPositionState` 및 상태 저장
- 공통 `MapProperties`, `MapUiSettings`
- 지도 클릭, 롱클릭, 더블탭, 실내지도, 위치 변경 등 주요 이벤트 콜백
- `MapEffect`, `DisposableMapEffect`를 통한 플랫폼 지도 핸들 접근
- Android 및 iOS 샘플 앱을 통한 기본 동작 검증

다음과 같은 영역은 아직 확장 중입니다.

- 오버레이 계층 전반
- 위치 소스 헬퍼의 고도화
- 업스트림 API와의 기능 패리티 확대
- 배포 채널 및 안정화

## 왜 이 프로젝트인가

- NAVER Map 기반 앱을 Android와 iOS에서 함께 개발하고 싶을 때
- 선언형 Compose 스타일로 지도 상태를 다루고 싶을 때
- 업스트림 `naver-map-compose` 경험을 KMP 방향으로 확장하고 싶을 때

## 기술 스택

- Kotlin Multiplatform
- Compose Multiplatform
- NAVER Maps Android SDK
- NAVER Maps iOS SDK (`NMapsMap`, CocoaPods)

## 요구 사항

- JDK 17 이상
- Android SDK
- Xcode 및 CocoaPods
- NAVER Cloud Platform 지도 API 키

## 빠른 시작

### 1. 저장소 클론

```bash
git clone https://github.com/jude/naver-map-compose-multiplatform.git
cd naver-map-compose-multiplatform
```

### 2. Android 로컬 설정

`local.properties.example`을 복사해서 `local.properties`를 만들고 값을 채웁니다.

```properties
sdk.dir=/Users/your-user/Library/Android/sdk
naver.map.client.id=YOUR_NCP_KEY_ID_HERE
```

### 3. iOS 로컬 설정

`iosApp/Config/LocalSecrets.example.xcconfig`를 복사해서 `iosApp/Config/LocalSecrets.xcconfig`를 만들고 동일한 키를 설정합니다.

```xcconfig
NAVER_MAP_CLIENT_ID = YOUR_NCP_KEY_ID_HERE
```

### 4. Android 샘플 실행

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug
```

### 5. iOS 샘플 실행

```bash
cd iosApp
pod install
open iosApp.xcworkspace
```

## 사용 예시

```kotlin
@Composable
fun MapScreen() {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(
            target = LatLng(37.5666102, 126.9783881),
            zoom = 14.0,
        )
    }

    NaverMap(
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapType = MapType.Basic,
            isTrafficLayerGroupEnabled = true,
        ),
        uiSettings = MapUiSettings(
            isCompassEnabled = true,
            isScaleBarEnabled = true,
            isZoomControlEnabled = true,
        ),
        locale = "ko-KR",
        onMapLoaded = {
            println("Map loaded")
        },
        onMapClick = { _, latLng ->
            println("Clicked: ${latLng.latitude}, ${latLng.longitude}")
        },
    )
}
```

샘플 구현은 `composeApp` 모듈에서 더 자세히 확인할 수 있습니다.

## 프로젝트 구조

```text
.
|-- composeApp/          # Android/KMP 샘플 앱
|-- iosApp/              # iOS 앱 엔트리 및 Xcode 프로젝트
|-- naver-map-compose/   # 공통 라이브러리 모듈
`-- plans/               # 기능 패리티 및 작업 계획 문서
```

## 업스트림 크레딧

이 저장소는 [`fornewid/naver-map-compose`](https://github.com/fornewid/naver-map-compose)의 API 설계와 사용 경험을 중요한 참고 대상으로 삼고 있습니다. 다만 이 프로젝트는 Android 전용 구현을 그대로 복제하는 대신, Android와 iOS에서 함께 사용할 수 있는 Compose Multiplatform 친화적 추상화를 목표로 합니다.

## 기여

이슈와 PR은 언제든지 환영합니다. 기능 추가 전에는 현재 API 방향성과 멀티플랫폼 제약을 함께 검토할 수 있도록 간단한 제안이나 설계 메모를 먼저 남겨주시면 도움이 됩니다.

## 라이선스

이 프로젝트는 Apache License 2.0을 따릅니다. 자세한 내용은 [LICENSE](LICENSE)를 참고하세요.

참고로 NAVER Maps Android SDK 및 NAVER Maps iOS SDK의 사용 조건과 약관은 본 저장소의 라이선스와 별개로 적용됩니다.
