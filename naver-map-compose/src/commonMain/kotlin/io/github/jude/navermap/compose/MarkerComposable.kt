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
 * 마커의 시각 표현은 `content`가 전담하며, 캡처된 콘텐츠는 스냅샷 기반이라 내부 상호작용은 지원하지 않습니다.
 */
@Composable
fun MarkerComposable(
    state: MarkerState = rememberUpdatedMarkerState(),
    onClick: () -> Boolean = { false },
    content: @Composable () -> Unit,
) {
    val onClickState = rememberUpdatedState(onClick)
    val icon = rememberMarkerComposableImage(content = content)
    val effectiveStyle = if (icon.isReady) {
        OverlayStyle(globalZIndex = MarkerDefaults.GlobalZIndex)
    } else {
        OverlayStyle(
            visible = false,
            globalZIndex = MarkerDefaults.GlobalZIndex,
        )
    }

    rememberOverlay(
        updateKey = listOf(state.position, effectiveStyle, icon),
        create = ::createPlatformMarkerOverlay,
        update = { handle, overlay ->
            updatePlatformMarkerComposableOverlay(
                handle = handle,
                overlay = overlay,
                position = state.position,
                icon = icon,
                style = effectiveStyle,
                onClick = { onClickState.value() },
            )
        },
        dispose = ::disposePlatformMarkerOverlay,
    )
}

@Composable
internal fun rememberMarkerComposableImage(
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
    style: OverlayStyle,
    onClick: () -> Boolean,
)
