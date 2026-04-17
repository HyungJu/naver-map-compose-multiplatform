package io.github.hyungju.navermap.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.applyCanvas
import com.naver.maps.map.overlay.OverlayImage as AndroidOverlayImage

private val transparentMarkerPlaceholder: PlatformMarkerComposableImage by lazy {
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    PlatformMarkerComposableImage(
        nativeImage = AndroidOverlayImage.fromBitmap(bitmap),
        isReady = false,
    )
}

internal actual class PlatformMarkerComposableImage(
    val nativeImage: AndroidOverlayImage,
    actual val isReady: Boolean,
)

@Composable
internal actual fun rememberPlatformMarkerComposableImage(
    renderKey: Any?,
    appearanceKey: Any?,
    density: androidx.compose.ui.unit.Density,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    content: @Composable () -> Unit,
): PlatformMarkerComposableImage {
    val parent = LocalView.current as? ViewGroup
        ?: throw IllegalStateException("MarkerComposable은 ViewGroup 안에서만 렌더링할 수 있습니다.")
    val compositionContext = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val renderedImage = remember {
        mutableStateOf(transparentMarkerPlaceholder)
    }

    LaunchedEffect(
        parent,
        compositionContext,
        density.density,
        density.fontScale,
        layoutDirection,
        renderKey,
        appearanceKey,
    ) {
        while (!parent.isAttachedToWindow) {
            withFrameNanos { }
        }

        renderedImage.value = renderMarkerComposableToImage(
            parent = parent,
            compositionContext = compositionContext,
            content = currentContent,
        )
    }
    return renderedImage.value
}

internal actual fun updatePlatformMarkerComposableOverlay(
    handle: PlatformMapHandle,
    overlay: PlatformMarkerOverlay,
    position: LatLng,
    icon: PlatformMarkerComposableImage,
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    overlay.nativeOverlay.apply {
        this.position = position.toMarkerAndroidLatLng()
        this.icon = icon.nativeImage
        this.captionText = ""
        this.alpha = 1f
        applyMarkerCommonStyle(style, onClick)
        map = handle.nativeMap
    }
}

private suspend fun renderMarkerComposableToImage(
    parent: ViewGroup,
    compositionContext: CompositionContext,
    content: @Composable () -> Unit,
): PlatformMarkerComposableImage {
    val composeView = ComposeView(parent.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        setParentCompositionContext(compositionContext)
        setContent(content)
    }
    parent.addView(composeView)

    try {
        repeat(2) {
            withFrameNanos { }
            composeView.measure(unspecifiedMeasureSpec, unspecifiedMeasureSpec)
            if (composeView.measuredWidth > 0 && composeView.measuredHeight > 0) {
                return@repeat
            }
        }

        composeView.measure(unspecifiedMeasureSpec, unspecifiedMeasureSpec)

        require(composeView.measuredWidth > 0 && composeView.measuredHeight > 0) {
            "MarkerComposable 콘텐츠의 너비와 높이는 0보다 커야 합니다."
        }

        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

        val bitmap = Bitmap.createBitmap(
            composeView.measuredWidth,
            composeView.measuredHeight,
            Bitmap.Config.ARGB_8888,
        )
        bitmap.applyCanvas { composeView.draw(this) }
        return PlatformMarkerComposableImage(
            nativeImage = AndroidOverlayImage.fromBitmap(bitmap),
            isReady = true,
        )
    } finally {
        parent.removeView(composeView)
    }
}

private fun LatLng.toMarkerAndroidLatLng(): com.naver.maps.geometry.LatLng {
    return com.naver.maps.geometry.LatLng(latitude, longitude)
}

private fun com.naver.maps.map.overlay.Overlay.applyMarkerCommonStyle(
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    tag = style.tag
    isVisible = style.visible
    minZoom = style.minZoom
    isMinZoomInclusive = style.minZoomInclusive
    maxZoom = style.maxZoom
    isMaxZoomInclusive = style.maxZoomInclusive
    zIndex = style.zIndex
    globalZIndex = style.globalZIndex
    onClickListener = com.naver.maps.map.overlay.Overlay.OnClickListener { onClick() }
}

private val unspecifiedMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
