package com.fluffy.cam6a.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.fluffy.cam6a.filters.FiltersViewModel
import com.fluffy.cam6a.photo.PhotoViewModel
import com.fluffy.cam6a.photo.PhotoViewModelFactory


@Composable
fun CameraPreview(modifier: Modifier = Modifier,
                  photoViewModel: PhotoViewModel,
                  filtersViewModel: FiltersViewModel // Pass FiltersViewModel to CameraPreview
){
    val context = LocalContext.current
    val updatedViewModel by rememberUpdatedState(photoViewModel)  // Ensures latest ViewModel is used

    val selectedFilter by filtersViewModel.selectedFilter.collectAsState()

    val scaleGestureDetector = remember {
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {

                photoViewModel.setZoom(detector.scaleFactor)// ✅ Uses ViewModel zoom method // ✅ Apply zoom using ViewModel
                return true
            }
        })
    }
    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFDEE1FA)) // Light blue background
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp)), // Rounded corners for camera preview
        factory = { ctx ->
            FrameLayout(ctx).apply {
                val textureView = TextureView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                setOnTouchListener { _, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    true
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
                        surface: android.graphics.SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            updatedViewModel.setTextureView(textureView)
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: android.graphics.SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {}

                    override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean =
                        true

                    override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
                        textureView.bitmap?.let { originalBitmap ->
                            val filteredBitmap = selectedFilter(originalBitmap) // ✅ Apply filter
                            imageView.post { imageView.setImageBitmap(filteredBitmap) } // ✅ Update ImageView
                        }
                    }
                }

                // ✅ Ensure textureView and imageView are not null before adding
                addView(textureView)
                addView(imageView)
            }
        }
    )
}
