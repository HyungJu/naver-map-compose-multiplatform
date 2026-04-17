package io.github.hyungju.navermap.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPlatformNaverMapClientId(): String? {
    val context = LocalContext.current
    return remember(context) {
        context.getString(R.string.naver_map_sdk_ncp_key_id)
            .takeIf { it.isNotBlank() }
    }
}
