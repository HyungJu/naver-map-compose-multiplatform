# Setup

## Local secrets

Do not commit NAVER client IDs.

### Android

Add this to `local.properties`:

```properties
naver.map.client.id=YOUR_NCP_KEY_ID_HERE
```

The Android sample exposes this value as a string resource, then reads it from Kotlin and passes it to `NaverMapAuthProvider`.

### iOS

Copy `iosApp/Config/LocalSecrets.example.xcconfig` to `iosApp/Config/LocalSecrets.xcconfig` and set:

```xcconfig
NAVER_MAP_CLIENT_ID = YOUR_NCP_KEY_ID_HERE
```

The sample app forwards that value into `Info.plist` as `NAVER_MAP_CLIENT_ID`, then reads it from Kotlin and passes it to `NaverMapAuthProvider`.

## Runtime injection from Kotlin

Library consumers should inject the NAVER client ID from Kotlin.

```kotlin
val naverClientId = remember { "YOUR_NCP_KEY_ID_HERE" }

NaverMapAuthProvider(
    ncpKeyId = naverClientId,
) {
    NaverMap()
}
```

Or per map:

```kotlin
val naverClientId = remember { "YOUR_NCP_KEY_ID_HERE" }

NaverMap(
    authOptions = NaverMapAuthOptions(
        ncpKeyId = naverClientId,
    ),
)
```

When `authOptions` and `NaverMapAuthProvider` are both missing, `NaverMap` throws immediately instead of falling back to platform defaults.

## Validation

- Android sample: `./gradlew :composeApp:assembleDebug`
- Shared library publication: `./gradlew :naver-map-compose:publishToMavenLocal`
- Maven Central release flow: `./gradlew :naver-map-compose:publishToMavenCentral`
- iOS sample: run `pod install` in `iosApp/` and open the workspace

배포용 버전, Sonatype 자격 증명, 자동화는 `docs/publishing.md`를 참고하세요.
