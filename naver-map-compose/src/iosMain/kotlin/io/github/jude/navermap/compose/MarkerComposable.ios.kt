@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package io.github.hyungju.navermap.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import cocoapods.NMapsMap.NMFOverlay
import cocoapods.NMapsMap.NMFOverlayImage
import kotlinx.coroutines.CancellationException
import kotlinx.cinterop.useContents
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSLog
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.removeFromParentViewController

private val transparentMarkerPlaceholder: PlatformMarkerComposableImage by lazy {
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(1.0, 1.0), false, 1.0)
    val image = try {
        UIGraphicsGetImageFromCurrentImageContext()
            ?: throw IllegalStateException("MarkerComposable 플레이스홀더 이미지를 생성하지 못했습니다.")
    } finally {
        UIGraphicsEndImageContext()
    }
    PlatformMarkerComposableImage(
        nativeImage = NMFOverlayImage.overlayImageWithImage(
            image,
            reuseIdentifier = "marker-composable-placeholder",
        ),
        widthPoints = 1.0,
        heightPoints = 1.0,
        isReady = false,
    )
}

private const val markerRenderConcurrency = 2
private val markerRenderSemaphore = Semaphore(permits = markerRenderConcurrency)
private const val markerRenderRetryCount = 2
private const val markerRenderRetryFrames = 2
private const val markerMeasureTimeoutFrames = 6
private const val markerSnapshotMeasurementWidthPoints = 512.0
private const val markerSnapshotMeasurementHeightPoints = 256.0
private const val markerSnapshotCaptureWidthSlackPoints = 4.0
private const val markerSnapshotCaptureHeightSlackPoints = 10.0
private const val markerImageCacheMaxEntries = 256
private val markerSnapshotPadding = 8.dp

internal data class MarkerComposableCacheKey(
    val renderKey: Any,
    val appearanceKey: Any?,
    val density: Float,
    val fontScale: Float,
    val layoutDirection: androidx.compose.ui.unit.LayoutDirection,
)

internal actual class PlatformMarkerComposableImage(
    val nativeImage: NMFOverlayImage,
    val widthPoints: Double,
    val heightPoints: Double,
    actual val isReady: Boolean,
)

private class MarkerComposableImageCache(
    private val maxEntries: Int,
) {
    private val values = mutableMapOf<MarkerComposableCacheKey, PlatformMarkerComposableImage>()
    private val accessOrder = mutableListOf<MarkerComposableCacheKey>()

    fun get(key: MarkerComposableCacheKey): PlatformMarkerComposableImage? {
        val value = values[key] ?: return null
        touch(key)
        return value
    }

    fun put(key: MarkerComposableCacheKey, value: PlatformMarkerComposableImage) {
        values[key] = value
        touch(key)
        trimToSize()
    }

    fun clear() {
        values.clear()
        accessOrder.clear()
    }

    private fun touch(key: MarkerComposableCacheKey) {
        accessOrder.remove(key)
        accessOrder.add(key)
    }

    private fun trimToSize() {
        while (values.size > maxEntries && accessOrder.isNotEmpty()) {
            val eldest = accessOrder.removeAt(0)
            values.remove(eldest)
        }
    }
}

internal class MarkerComposableRenderer(
    private val parentViewController: UIViewController,
) {
    private val imageCache = MarkerComposableImageCache(maxEntries = markerImageCacheMaxEntries)
    private var hostPool: MarkerSnapshotHostPool? = null
    private var nextImageId: Long = 0
    private var cacheHitCount: Int = 0
    private var renderCount: Int = 0

    fun prewarm() {
        getOrCreateHostPool()
    }

    fun cachedImage(key: MarkerComposableCacheKey): PlatformMarkerComposableImage? {
        return imageCache.get(key)?.also {
            cacheHitCount += 1
        }
    }

    suspend fun render(
        cacheKey: MarkerComposableCacheKey?,
        density: androidx.compose.ui.unit.Density,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        content: @Composable () -> Unit,
    ): PlatformMarkerComposableImage {
        renderCount += 1
        val image = markerRenderSemaphore.withPermit {
            getOrCreateHostPool().render(
                density = density,
                layoutDirection = layoutDirection,
                content = content,
            )
        }
        if (cacheKey != null && image.isReady) {
            imageCache.put(cacheKey, image)
        }
        return image
    }

    fun dispose() {
        if (cacheHitCount > 0 || renderCount > 0) {
            NSLog(
                "[MarkerComposable] renderer summary cacheHits=%ld renders=%ld",
                cacheHitCount.toLong(),
                renderCount.toLong(),
            )
        }
        hostPool?.dispose()
        hostPool = null
        imageCache.clear()
    }

    private fun getOrCreateHostPool(): MarkerSnapshotHostPool {
        val existing = hostPool
        if (existing != null) {
            return existing
        }

        return MarkerSnapshotHostPool(
            parentViewController = parentViewController,
            imageFactory = ::createPlatformMarkerComposableImage,
        ).also { pool ->
            hostPool = pool
        }
    }

    private fun createPlatformMarkerComposableImage(
        image: UIImage,
        widthPoints: Double,
        heightPoints: Double,
    ): PlatformMarkerComposableImage {
        val imageId = ++nextImageId
        NSLog(
            "[MarkerComposable] rendered image %.1fx%.1f",
            widthPoints,
            heightPoints,
        )
        return PlatformMarkerComposableImage(
            nativeImage = NMFOverlayImage.overlayImageWithImage(
                image,
                reuseIdentifier = "marker-composable-$imageId",
            ),
            widthPoints = widthPoints,
            heightPoints = heightPoints,
            isReady = true,
        )
    }
}

@Composable
internal actual fun rememberPlatformMarkerComposableImage(
    renderKey: Any?,
    appearanceKey: Any?,
    density: androidx.compose.ui.unit.Density,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    content: @Composable () -> Unit,
): PlatformMarkerComposableImage {
    val mapHandle = LocalPlatformMapHandle.current ?: return transparentMarkerPlaceholder
    val renderer = remember(mapHandle) { mapHandle.markerComposableRenderer }
    val currentContent by rememberUpdatedState(content)
    val cacheKey = renderKey?.let {
        MarkerComposableCacheKey(
            renderKey = it,
            appearanceKey = appearanceKey,
            density = density.density,
            fontScale = density.fontScale,
            layoutDirection = layoutDirection,
        )
    }
    val renderedImage = remember(renderer, cacheKey) {
        mutableStateOf(
            cacheKey?.let(renderer::cachedImage) ?: transparentMarkerPlaceholder,
        )
    }

    LaunchedEffect(renderer, cacheKey) {
        cacheKey?.let(renderer::cachedImage)?.let { cachedImage ->
            if (cachedImage.isReady) {
                renderedImage.value = cachedImage
                return@LaunchedEffect
            }
        }

        try {
            var lastFailure: Throwable? = null

            repeat(markerRenderRetryCount) { attempt ->
                val image = try {
                    renderer.render(
                        cacheKey = cacheKey,
                        density = density,
                        layoutDirection = layoutDirection,
                        content = currentContent,
                    )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    lastFailure = throwable
                    null
                }

                if (image != null && image.isReady) {
                    renderedImage.value = image
                    return@LaunchedEffect
                }

                if (attempt < markerRenderRetryCount - 1) {
                    repeat(markerRenderRetryFrames) {
                        withFrameNanos { }
                    }
                }
            }

            if (lastFailure != null) {
                NSLog(
                    "[MarkerComposable] render failed after retries: %@",
                    lastFailure.message ?: "unknown",
                )
            } else if (!renderedImage.value.isReady) {
                NSLog("[MarkerComposable] render stayed at placeholder after retry budget")
            }
        } catch (_: CancellationException) {
            // Composition 종료로 인한 정상 취소입니다.
        } catch (throwable: Throwable) {
            NSLog(
                "[MarkerComposable] render failed: %@",
                throwable.message ?: "unknown",
            )
        }
    }

    return renderedImage.value
}

@OptIn(ExperimentalComposeUiApi::class)
private class MarkerSnapshotHost(
    private val parentViewController: UIViewController,
    private val imageFactory: (UIImage, Double, Double) -> PlatformMarkerComposableImage,
) {
    private val densityState: MutableState<androidx.compose.ui.unit.Density> = mutableStateOf(
        androidx.compose.ui.unit.Density(
            density = 1f,
            fontScale = 1f,
        ),
    )
    private val layoutDirectionState = mutableStateOf(androidx.compose.ui.unit.LayoutDirection.Ltr)
    private val contentState = mutableStateOf<(@Composable () -> Unit)?>(null)
    private val measuredContentSizeState = mutableStateOf(IntSize.Zero)
    private var lastMeasuredContentSize = IntSize.Zero
    private val snapshotViewController = ComposeUIViewController(
        configure = {
            opaque = false
            enforceStrictPlistSanityCheck = false
        },
    ) {
        val currentContent = contentState.value
        if (currentContent != null) {
            CompositionLocalProvider(
                LocalDensity provides densityState.value,
                LocalLayoutDirection provides layoutDirectionState.value,
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(markerSnapshotPadding)
                        .onSizeChanged { size ->
                            measuredContentSizeState.value = size
                        },
                ) {
                    currentContent()
                }
            }
        }
    }

    init {
        snapshotViewController.view.apply {
            backgroundColor = UIColor.clearColor
            alpha = 1.0
            userInteractionEnabled = false
            setFrame(
                CGRectMake(
                    0.0,
                    0.0,
                    markerSnapshotMeasurementWidthPoints,
                    markerSnapshotMeasurementHeightPoints,
                ),
            )
        }
        parentViewController.addChildViewController(snapshotViewController)
        parentViewController.view.addSubview(snapshotViewController.view)
        parentViewController.view.sendSubviewToBack(snapshotViewController.view)
        snapshotViewController.didMoveToParentViewController(parentViewController)
    }

    suspend fun render(
        density: androidx.compose.ui.unit.Density,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        content: @Composable () -> Unit,
    ): PlatformMarkerComposableImage {
        densityState.value = density
        layoutDirectionState.value = layoutDirection
        measuredContentSizeState.value = IntSize.Zero
        contentState.value = content

        prepareMeasurementFrame()
        val measuredContentSize = awaitMeasuredContentSize()
            ?: return transparentMarkerPlaceholder

        lastMeasuredContentSize = measuredContentSize
        val screenScale = UIScreen.mainScreen.scale
        val contentWidthPoints = measuredContentSize.width.toDouble() / screenScale
        val contentHeightPoints = measuredContentSize.height.toDouble() / screenScale
        val captureWidthPoints = contentWidthPoints + markerSnapshotCaptureWidthSlackPoints
        val captureHeightPoints = contentHeightPoints + markerSnapshotCaptureHeightSlackPoints

        snapshotViewController.view.setFrame(
            CGRectMake(
                0.0,
                0.0,
                captureWidthPoints,
                captureHeightPoints,
            ),
        )

        repeat(markerRenderRetryFrames) {
            withFrameNanos { }
            snapshotViewController.view.setNeedsLayout()
            snapshotViewController.view.layoutIfNeeded()
        }

        val capturedImage = snapshotViewController.view.captureToUIImage()
        if (capturedImage.size.useContents { width <= 1.0 || height <= 1.0 }) {
            return transparentMarkerPlaceholder
        }

        return imageFactory(capturedImage, captureWidthPoints, captureHeightPoints)
    }

    fun dispose() {
        snapshotViewController.view.removeFromSuperview()
        snapshotViewController.removeFromParentViewController()
    }

    private fun prepareMeasurementFrame() {
        val screenScale = UIScreen.mainScreen.scale
        val lastWidthPoints = if (lastMeasuredContentSize.width > 1) {
            lastMeasuredContentSize.width.toDouble() / screenScale
        } else {
            0.0
        }
        val lastHeightPoints = if (lastMeasuredContentSize.height > 1) {
            lastMeasuredContentSize.height.toDouble() / screenScale
        } else {
            0.0
        }
        snapshotViewController.view.setFrame(
            CGRectMake(
                0.0,
                0.0,
                maxOf(markerSnapshotMeasurementWidthPoints, lastWidthPoints),
                maxOf(markerSnapshotMeasurementHeightPoints, lastHeightPoints),
            ),
        )
    }

    private suspend fun awaitMeasuredContentSize(): IntSize? {
        repeat(markerMeasureTimeoutFrames) {
            withFrameNanos { }
            snapshotViewController.view.setNeedsLayout()
            snapshotViewController.view.layoutIfNeeded()
            val measuredSize = measuredContentSizeState.value
            if (measuredSize.width > 1 && measuredSize.height > 1) {
                return measuredSize
            }
        }
        return null
    }
}

private class MarkerSnapshotHostPool(
    parentViewController: UIViewController,
    imageFactory: (UIImage, Double, Double) -> PlatformMarkerComposableImage,
) {
    private val hosts = List(markerRenderConcurrency) {
        MarkerSnapshotHost(
            parentViewController = parentViewController,
            imageFactory = imageFactory,
        )
    }
    private var nextHostIndex = 0

    suspend fun render(
        density: androidx.compose.ui.unit.Density,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        content: @Composable () -> Unit,
    ): PlatformMarkerComposableImage {
        val host = hosts[nextHostIndex]
        nextHostIndex = (nextHostIndex + 1) % hosts.size
        return host.render(
            density = density,
            layoutDirection = layoutDirection,
            content = content,
        )
    }

    fun dispose() {
        hosts.forEach(MarkerSnapshotHost::dispose)
    }
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
        this.position = position.toMarkerNativeLatLng()
        iconImage = icon.nativeImage
        width = icon.widthPoints
        height = icon.heightPoints
        this.captionText = ""
        this.alpha = 1.0
        applyMarkerCommonStyle(handle, style, onClick)
    }
}

private fun UIView.captureToUIImage(): UIImage {
    val targetSize = bounds.useContents { CGSizeMake(size.width, size.height) }
    UIGraphicsBeginImageContextWithOptions(targetSize, false, UIScreen.mainScreen.scale)
    return try {
        drawViewHierarchyInRect(rect = bounds, afterScreenUpdates = true)
        UIGraphicsGetImageFromCurrentImageContext()
            ?: throw IllegalStateException("MarkerComposable UIView를 캡처하지 못했습니다.")
    } finally {
        UIGraphicsEndImageContext()
    }
}

private fun NMFOverlay.applyMarkerCommonStyle(
    handle: PlatformMapHandle,
    style: OverlayStyle,
    onClick: () -> Boolean,
) {
    userInfo = style.tag?.let { mapOf("tag" to it) } ?: emptyMap<Any?, Any?>()
    hidden = !style.visible
    minZoom = style.minZoom
    isMinZoomInclusive = style.minZoomInclusive
    maxZoom = style.maxZoom
    isMaxZoomInclusive = style.maxZoomInclusive
    zIndex = style.zIndex.toLong()
    globalZIndex = style.globalZIndex.toLong()
    touchHandler = { _ -> onClick() }
    mapView = handle.nativeMap
}

private fun LatLng.toMarkerNativeLatLng() = cocoapods.NMapsMap.NMGLatLng.latLngWithLat(latitude, longitude)
