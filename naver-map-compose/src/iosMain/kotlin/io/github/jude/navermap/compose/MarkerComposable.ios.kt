@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package io.github.hyungju.navermap.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.ComposeUIViewController
import cocoapods.NMapsMap.NMFOverlay
import cocoapods.NMapsMap.NMFOverlayImage
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.impl.use
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.drawViewHierarchyInRect
import platform.UIKit.removeFromParentViewController
import platform.posix.memcpy
import kotlin.native.concurrent.ThreadLocal

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

@ThreadLocal
private object MarkerComposableImageIds {
    var value: Long = 0
}

internal actual class PlatformMarkerComposableImage(
    val nativeImage: NMFOverlayImage,
    val widthPoints: Double,
    val heightPoints: Double,
    actual val isReady: Boolean,
)

@Composable
internal actual fun rememberPlatformMarkerComposableImage(
    density: androidx.compose.ui.unit.Density,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    content: @Composable () -> Unit,
): PlatformMarkerComposableImage {
    val parentViewController = LocalUIViewController.current
    val currentContent by rememberUpdatedState(content)
    val renderedImage = remember {
        mutableStateOf(transparentMarkerPlaceholder)
    }

    LaunchedEffect(
        parentViewController,
        density.density,
        density.fontScale,
        layoutDirection,
    ) {
        repeat(2) {
            withFrameNanos { }
        }

        renderedImage.value = renderMarkerComposableToImage(
            parentViewController = parentViewController,
            density = density,
            layoutDirection = layoutDirection,
            content = currentContent,
        )
    }

    return renderedImage.value
}

private fun createPlatformMarkerComposableImage(image: UIImage): PlatformMarkerComposableImage {
    val imageId = ++MarkerComposableImageIds.value
    val imageSize = image.size.useContents { this }
    return PlatformMarkerComposableImage(
        nativeImage = NMFOverlayImage.overlayImageWithImage(
            image,
            reuseIdentifier = "marker-composable-$imageId",
        ),
        widthPoints = imageSize.width,
        heightPoints = imageSize.height,
        isReady = true,
    )
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

@OptIn(ExperimentalComposeUiApi::class)
private suspend fun renderMarkerComposableToImage(
    parentViewController: UIViewController,
    density: androidx.compose.ui.unit.Density,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    content: @Composable () -> Unit,
): PlatformMarkerComposableImage {
    val snapshotViewController = ComposeUIViewController(
        configure = {
            opaque = false
            enforceStrictPlistSanityCheck = false
        },
    ) {
        CompositionLocalProvider(
            LocalDensity provides density,
            LocalLayoutDirection provides layoutDirection,
        ) {
            content()
        }
    }

    return try {
        val offscreenY = parentViewController.view.bounds.useContents { size.height + 32.0 }
        snapshotViewController.view.apply {
            backgroundColor = UIColor.clearColor
            alpha = 1.0
            setFrame(CGRectMake(0.0, offscreenY, 512.0, 256.0))
        }
        parentViewController.addChildViewController(snapshotViewController)
        parentViewController.view.addSubview(snapshotViewController.view)
        snapshotViewController.didMoveToParentViewController(parentViewController)

        repeat(4) {
            withFrameNanos { }
            snapshotViewController.view.layoutIfNeeded()
        }

        val capturedBitmap = snapshotViewController.view
            .captureToImageBitmap()
            .cropTransparentBounds()
        require(capturedBitmap.width > 0 && capturedBitmap.height > 0) {
            "MarkerComposable 콘텐츠의 너비와 높이는 0보다 커야 합니다."
        }
        val uiImage = capturedBitmap.toUIImage()
        createPlatformMarkerComposableImage(uiImage)
    } finally {
        snapshotViewController.view.removeFromSuperview()
        snapshotViewController.removeFromParentViewController()
    }
}

private fun UIView.captureToImageBitmap(): ImageBitmap {
    val targetSize = bounds.useContents { CGSizeMake(size.width, size.height) }
    UIGraphicsBeginImageContextWithOptions(targetSize, false, UIScreen.mainScreen.scale)
    drawViewHierarchyInRect(bounds, true)
    val uiImage = UIGraphicsGetImageFromCurrentImageContext()
        ?: throw IllegalStateException("MarkerComposable UIView를 캡처하지 못했습니다.")
    val pngData = UIImagePNGRepresentation(uiImage)
        ?: throw IllegalStateException("MarkerComposable UIView를 PNG로 변환하지 못했습니다.")
    UIGraphicsEndImageContext()

    val length = pngData.length.toInt()
    val bytes = ByteArray(length)
    bytes.usePinned { dst ->
        memcpy(dst.addressOf(0), pngData.bytes, length.convert())
    }

    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}

private fun ImageBitmap.cropTransparentBounds(): ImageBitmap {
    val pixels = toPixelMap()
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (pixels[x, y].alpha > 0f) {
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
        }
    }

    if (maxX < minX || maxY < minY) {
        return ImageBitmap(1, 1)
    }

    val croppedWidth = maxX - minX + 1
    val croppedHeight = maxY - minY + 1
    val croppedBitmap = ImageBitmap(croppedWidth, croppedHeight)
    val canvas = Canvas(croppedBitmap)
    canvas.drawImageRect(
        image = this,
        srcOffset = IntOffset(minX, minY),
        srcSize = IntSize(croppedWidth, croppedHeight),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(croppedWidth, croppedHeight),
        paint = Paint(),
    )
    return croppedBitmap
}

private fun ImageBitmap.toUIImage(): UIImage {
    return Image.makeFromBitmap(asSkiaBitmap()).use { image ->
        val data = image.encodeToData(EncodedImageFormat.PNG)
            ?: throw IllegalStateException("MarkerComposable 이미지를 PNG로 인코딩하지 못했습니다.")
        val bytes = data.bytes
        val nsData = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        UIImage.imageWithData(nsData, UIScreen.mainScreen.scale)
            ?: throw IllegalStateException("MarkerComposable UIImage를 생성하지 못했습니다.")
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
