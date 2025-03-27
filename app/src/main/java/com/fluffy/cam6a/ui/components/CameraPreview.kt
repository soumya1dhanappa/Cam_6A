package com.fluffy.cam6a.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fluffy.cam6a.filters.FiltersViewModel
import com.fluffy.cam6a.photo.PhotoViewModel
import com.fluffy.cam6a.video.VideoViewModel

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    photoViewModel: PhotoViewModel,
    filtersViewModel: FiltersViewModel,
    videoViewModel: VideoViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val updatedPhotoViewModel by rememberUpdatedState(photoViewModel)
    val selectedFilter by filtersViewModel.currentFilterFunction.observeAsState()


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("CameraPreview", "Lifecycle RESUME: Opening camera")
                    updatedPhotoViewModel.openCamera()
                    videoViewModel.openCamera()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("CameraPreview", "Lifecycle PAUSE: Releasing resources")
                    videoViewModel.releaseAll()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFDEE1FA))
            .padding(2.dp)
            .clip(RoundedCornerShape(16.dp)),
        factory = { ctx ->
            FrameLayout(ctx).apply {
                val textureView = TextureView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                val imageView = ImageView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }

                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        Log.d("CameraPreview", "Surface available: Setting textureView")
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            updatedPhotoViewModel.setTextureView(textureView)
                            videoViewModel.initializeTextureView(textureView)
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {}

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                        textureView.bitmap?.let { originalBitmap ->
                            val filteredBitmap = selectedFilter?.invoke(originalBitmap) ?: originalBitmap
                            imageView.post { imageView.setImageBitmap(filteredBitmap) }
                        }
                    }
                }
                addView(textureView)
                addView(imageView)
            }
        },
        update = {
            // Update logic if needed
        }
    )
}