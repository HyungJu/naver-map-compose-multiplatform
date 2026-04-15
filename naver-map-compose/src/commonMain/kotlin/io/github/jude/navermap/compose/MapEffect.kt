package io.github.jude.navermap.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope

internal val LocalPlatformMapHandle = staticCompositionLocalOf<PlatformMapHandle?> { null }

@Composable
fun DisposableMapEffect(
    vararg keys: Any?,
    effect: DisposableEffectScope.(PlatformMapHandle) -> DisposableEffectResult,
) {
    val handle = LocalPlatformMapHandle.current
    if (handle != null) {
        DisposableEffect(handle, *keys) {
            effect(handle)
        }
    }
}

@Composable
fun MapEffect(
    vararg keys: Any?,
    block: suspend CoroutineScope.(PlatformMapHandle) -> Unit,
) {
    val handle = LocalPlatformMapHandle.current
    if (handle != null) {
        LaunchedEffect(handle, *keys) {
            block(handle)
        }
    }
}

expect class PlatformMapHandle
