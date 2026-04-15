package io.github.jude.navermap.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
class CameraPositionState(
    position: CameraPosition = NaverMapConstants.DefaultCameraPosition,
) {
    var isMoving by mutableStateOf(false)
        internal set

    var cameraUpdateReason by mutableStateOf(CameraUpdateReason.NO_MOVEMENT_YET)
        internal set

    var locationTrackingMode by mutableStateOf(LocationTrackingMode.None)
        internal set

    internal var rawPosition by mutableStateOf(position)

    var position: CameraPosition
        get() = rawPosition
        set(value) {
            rawPosition = value
            moveCamera?.invoke(value)
        }

    private var moveCamera: ((CameraPosition) -> Unit)? = null

    internal fun bind(moveCamera: ((CameraPosition) -> Unit)?) {
        this.moveCamera = moveCamera
        moveCamera?.invoke(rawPosition)
    }

    internal fun updateFromMap(
        position: CameraPosition,
        isMoving: Boolean = this.isMoving,
        cameraUpdateReason: CameraUpdateReason = this.cameraUpdateReason,
        locationTrackingMode: LocationTrackingMode = this.locationTrackingMode,
    ) {
        rawPosition = position
        this.isMoving = isMoving
        this.cameraUpdateReason = cameraUpdateReason
        this.locationTrackingMode = locationTrackingMode
    }

    companion object {
        val Saver = listSaver<CameraPositionState, Double>(
            save = { state ->
                listOf(
                    state.position.target.latitude,
                    state.position.target.longitude,
                    state.position.zoom,
                    state.position.tilt,
                    state.position.bearing,
                )
            },
            restore = { restored ->
                CameraPositionState(
                    CameraPosition(
                        target = LatLng(
                            latitude = restored[0],
                            longitude = restored[1],
                        ),
                        zoom = restored[2],
                        tilt = restored[3],
                        bearing = restored[4],
                    ),
                )
            },
        )
    }
}

@Composable
inline fun rememberCameraPositionState(
    crossinline init: CameraPositionState.() -> Unit = {},
): CameraPositionState = rememberSaveable(saver = CameraPositionState.Saver) {
    CameraPositionState().apply(init)
}

@Deprecated(
    message = "CameraPositionState를 사용하세요.",
    replaceWith = ReplaceWith("CameraPositionState"),
)
typealias NaverMapCameraState = CameraPositionState

@Deprecated(
    message = "rememberCameraPositionState를 사용하세요.",
    replaceWith = ReplaceWith("rememberCameraPositionState { position = initialPosition }"),
)
@Composable
fun rememberNaverMapCameraState(
    initialPosition: CameraPosition = NaverMapConstants.DefaultCameraPosition,
): NaverMapCameraState = rememberCameraPositionState {
    position = initialPosition
}
