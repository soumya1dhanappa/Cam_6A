package com.fluffy.cam6a.ui.components

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fluffy.cam6a.photo.PhotoViewModel
import com.fluffy.cam6a.video.VideoViewModel

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    photoViewModel: PhotoViewModel? = null,
    videoViewModel: VideoViewModel? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val textureView = remember { TextureView(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("CameraPreview", "Lifecycle RESUME: Opening camera")
                    photoViewModel?.openCamera()
                    videoViewModel?.openCamera()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("CameraPreview", "Lifecycle PAUSE: Releasing resources")
                    videoViewModel?.releaseAll()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { textureView.apply {
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    Log.d("CameraPreview", "Surface available: Setting textureView")
                    photoViewModel?.setTextureView(this@apply)
                    videoViewModel?.initializeTextureView(this@apply)
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
        }},
        modifier = modifier
    )
}