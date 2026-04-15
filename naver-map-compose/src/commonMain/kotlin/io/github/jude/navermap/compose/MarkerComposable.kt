package io.github.jude.navermap.compose

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * Composable 콘텐츠를 이미지로 캡처해 NAVER 지도 native marker icon으로 사용하는 API입니다.
 *
 * `keys`가 바뀌면 마커 이미지를 다시 생성하며, 캡처된 콘텐츠는 스냅샷 기반이라 내부 상호작용은 지원하지 않습니다.
 */
@Composable
fun MarkerComposable(
    vararg keys: Any?,
    state: MarkerState = rememberUpdatedMarkerState(),
    captionText: String = "",
    alpha: Float = 1f,
    style: OverlayStyle = OverlayStyle(globalZIndex = MarkerDefaults.GlobalZIndex),
    onClick: () -> Boolean = { false },
    content: @Composable () -> Unit,
) {
    require(alpha in 0f..1f) { "마커 투명도는 0과 1 사이여야 합니다." }
    val onClickState = rememberUpdatedState(onClick)
    val icon = rememberMarkerComposableImage(*keys, content = content)
    val effectiveStyle = if (icon.isReady) {
        style
    } else {
        style.copy(visible = false)
    }

    rememberOverlay(
        updateKey = listOf(state.position, captionText, alpha, effectiveStyle, icon),
        create = ::createPlatformMarkerOverlay,
        update = { handle, overlay ->
            updatePlatformMarkerComposableOverlay(
                handle = handle,
                overlay = overlay,
                position = state.position,
                icon = icon,
                captionText = captionText,
                alpha = alpha,
                style = effectiveStyle,
                onClick = { onClickState.value() },
            )
        },
        dispose = ::disposePlatformMarkerOverlay,
    )
}

@Composable
internal fun rememberMarkerComposableImage(
    vararg keys: Any?,
    content: @Composable () -> Unit,
): PlatformMarkerComposableImage {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    val contentColor = LocalContentColor.current
    val textStyle = LocalTextStyle.current
    val currentContent by rememberUpdatedState(content)

    return rememberPlatformMarkerComposableImage(
        density = density,
        layoutDirection = layoutDirection,
        keys = keys,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalTextStyle provides textStyle,
            ) {
                currentContent()
            }
        }
    }
}

@Composable
internal expect fun rememberPlatformMarkerComposableImage(
    density: androidx.compose.ui.unit.Density,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    keys: Array<out Any?>,
    content: @Composable () -> Unit,
): PlatformMarkerComposableImage

internal expect class PlatformMarkerComposableImage {
    val isReady: Boolean
}

internal expect fun updatePlatformMarkerComposableOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformMarkerOverlay,
    position: LatLng,
    icon: PlatformMarkerComposableImage,
    captionText: String,
    alpha: Float,
    style: OverlayStyle,
    onClick: () -> Boolean,
)
