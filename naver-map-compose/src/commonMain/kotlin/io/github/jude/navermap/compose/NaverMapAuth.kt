package io.github.hyungju.navermap.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class NaverMapAuthOptions(
    val ncpKeyId: String,
) {
    init {
        require(ncpKeyId.isNotBlank()) { "ncpKeyId must not be blank." }
    }
}

private val LocalNaverMapAuthOptions = staticCompositionLocalOf<NaverMapAuthOptions?> { null }

@Composable
fun NaverMapAuthProvider(
    ncpKeyId: String,
    content: @Composable () -> Unit,
) {
    NaverMapAuthProvider(
        authOptions = NaverMapAuthOptions(ncpKeyId = ncpKeyId),
        content = content,
    )
}

@Composable
fun NaverMapAuthProvider(
    authOptions: NaverMapAuthOptions,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalNaverMapAuthOptions provides authOptions,
        content = content,
    )
}

@Composable
internal fun currentNaverMapAuthOptions(
    explicitAuthOptions: NaverMapAuthOptions?,
): NaverMapAuthOptions {
    return explicitAuthOptions ?: LocalNaverMapAuthOptions.current
        ?: error("NaverMap requires NaverMapAuthProvider or authOptions.")
}
