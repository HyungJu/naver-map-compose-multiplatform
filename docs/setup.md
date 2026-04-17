# Setup

## Local secrets

Do not commit NAVER client IDs.

### Android

Add this to `local.properties`:

```properties
naver.map.client.id=YOUR_NCP_KEY_ID_HERE
```

The Android sample injects this value into the `com.naver.maps.map.CLIENT_ID` manifest entry.

### iOS

Copy `iosApp/Config/LocalSecrets.example.xcconfig` to `iosApp/Config/LocalSecrets.xcconfig` and set:

```xcconfig
NAVER_MAP_CLIENT_ID = YOUR_NCP_KEY_ID_HERE
```

The sample app forwards that value into `Info.plist` as `NMFNcpKeyId`, which is the key the NAVER iOS SDK reads during startup.

## Runtime injection from Kotlin

Library consumers can also inject the NAVER client ID from Kotlin instead of relying on manifest / `Info.plist` configuration.

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

When `authOptions` is omitted, the library falls back to the platform SDK defaults, which means Android manifest metadata and iOS `NMFNcpKeyId` still work as before.

## Validation

- Android sample: `./gradlew :composeApp:assembleDebug`
- Shared library publication: `./gradlew :naver-map-compose:publishToMavenLocal`
- Maven Central release flow: `./gradlew :naver-map-compose:publishToMavenCentral`
- iOS sample: run `pod install` in `iosApp/` and open the workspace

배포용 버전, Sonatype 자격 증명, 자동화는 `docs/publishing.md`를 참고하세요.
