package com.example.myplants.presentation.addEditPlant

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.sharp.Lens
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


@Composable
fun CameraView(
    onImageCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
    initialLensFacing: Int = CameraSelector.LENS_FACING_BACK,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableIntStateOf(initialLensFacing) }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                LifecycleCameraController.IMAGE_CAPTURE or LifecycleCameraController.VIDEO_CAPTURE or LifecycleCameraController.IMAGE_ANALYSIS // enable if needed
            )
            cameraSelector = CameraSelector.Builder().requireLensFacing(initialLensFacing).build()
        }
    }

    val onCaptured by rememberUpdatedState(onImageCaptured)
    val onClosed by rememberUpdatedState(onClose)

    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    LaunchedEffect(controller) {
        withFrameNanos {
            previewView.controller = controller
        }
    }

    LaunchedEffect(lensFacing) {
        controller.cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }

    suspend fun takePhotoToMediaStore(
        context: Context,
        controller: LifecycleCameraController,
        folderName: String = "MyPlants"
    ): Result<Uri> = suspendCancellableCoroutine { cont ->

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val output = ImageCapture.OutputFileOptions
            .Builder(context.contentResolver, collection, values)
            .build()

        controller.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {

                override fun onError(exc: ImageCaptureException) {
                    if (cont.isActive) cont.resume(Result.failure(exc), onCancellation = null)
                }

                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    val uri = result.savedUri
                    if (uri != null && cont.isActive) {
                        cont.resume(Result.success(uri), onCancellation = null)
                    } else if (cont.isActive) {
                        cont.resume(
                            Result.failure(IllegalStateException("No savedUri returned")),
                            onCancellation = null
                        )
                    }
                }
            }
        )
    }

    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView }, modifier = Modifier.fillMaxSize()
        )

        AttachCameraGestures(previewView = previewView, controller = controller)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 24.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.SwitchCamera,
                contentDescription = "Switch Camera",
                tint = Color.White,
                modifier = Modifier
                    .size(50.dp)
                    .clickable {
                        lensFacing =
                            if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT
                            else
                                CameraSelector.LENS_FACING_BACK
                    }
            )

            Icon(
                imageVector = Icons.Sharp.Lens,
                contentDescription = "Take picture",
                tint = Color.White,
                modifier = Modifier
                    .size(56.dp)
                    .border(1.dp, Color.White, CircleShape)
                    .clickable {
                        scope.launch {
                            val res = takePhotoToMediaStore(context, controller)

                            if (res.isSuccess) {
                                onCaptured(res.getOrThrow())
                            } else {
                                val err = res.exceptionOrNull()

                                println("err: $err")
                                // TODO show snackbar/toast with err?.message
                            }
                        }
                    })

            Icon(
                imageVector = Icons.Filled.Cancel,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier
                    .size(55.dp)
                    .clickable { onClosed() })
        }
    }
}

/** Tap‑to‑focus + pinch‑to‑zoom for CameraX PreviewView with LifecycleCameraController */
@Composable
fun AttachCameraGestures(
    previewView: PreviewView,
    controller: LifecycleCameraController
) {
    DisposableEffect(previewView, controller) {
        // --- Pinch to zoom ---
        val scaleDetector = ScaleGestureDetector(
            previewView.context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val current = controller.zoomState.value?.zoomRatio ?: 1f
                    val next = (current * detector.scaleFactor).coerceIn(
                        controller.zoomState.value?.minZoomRatio ?: 1f,
                        controller.zoomState.value?.maxZoomRatio ?: 10f
                    )
                    controller.cameraControl?.setZoomRatio(next)
                    return true
                }
            })

        val tapDetector = GestureDetector(
            previewView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    val factory = previewView.meteringPointFactory
                    val afPoint = factory.createPoint(e.x, e.y)
                    val action = FocusMeteringAction.Builder(afPoint, FocusMeteringAction.FLAG_AF)
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()
                    controller.cameraControl?.startFocusAndMetering(action)
                    return true
                }
            })

        val listener = { _: Any, event: MotionEvent ->
            var handled = scaleDetector.onTouchEvent(event)
            handled = tapDetector.onTouchEvent(event) || handled
            handled
        }

        previewView.setOnTouchListener { v, ev -> listener(v, ev) }
        onDispose { previewView.setOnTouchListener(null) }
    }
}