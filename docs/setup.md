# Setup

## Local secrets

Do not commit NAVER client IDs.

Add this to `local.properties`:

```properties
naver.map.client.id=YOUR_NCP_KEY_ID_HERE
```

The sample app generates a common KMP constant from this property at build time, so Android and iOS both use the same Kotlin-level client ID.

## Runtime injection from Kotlin

Library consumers should inject the NAVER client ID from Kotlin.

```kotlin
private const val NAVER_CLIENT_ID = "YOUR_NCP_KEY_ID_HERE"

NaverMapAuthProvider(
    ncpKeyId = NAVER_CLIENT_ID,
) {
    NaverMap()
}
```

Or per map:

```kotlin
private const val NAVER_CLIENT_ID = "YOUR_NCP_KEY_ID_HERE"

NaverMap(
    authOptions = NaverMapAuthOptions(
        ncpKeyId = NAVER_CLIENT_ID,
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
