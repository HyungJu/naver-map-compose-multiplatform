package io.github.hyungju.navermap.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSBundle

@Composable
actual fun rememberPlatformNaverMapClientId(): String {
    return remember {
        (NSBundle.mainBundle.objectForInfoDictionaryKey("NAVER_MAP_CLIENT_ID") as? String)
            ?.takeIf { it.isNotBlank() }
            ?: error("Missing NAVER_MAP_CLIENT_ID in iosApp Info.plist.")
    }
}
