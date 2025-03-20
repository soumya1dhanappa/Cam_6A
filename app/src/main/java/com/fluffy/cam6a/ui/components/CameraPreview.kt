package com.fluffy.cam6a.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
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
import com.fluffy.cam6a.photo.PhotoViewModel
import com.fluffy.cam6a.video.VideoViewModel

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    photoViewModel: PhotoViewModel,
    viewModel: VideoViewModel
) {
    val context = LocalContext.current
    val updatedViewModel by rememberUpdatedState(photoViewModel)  // Ensures latest ViewModel is used
    var textureView by remember { mutableStateOf<TextureView?>(null) }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFDEE1FA)) // Light blue background
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp)), // Rounded corners for camera preview
        factory = { ctx ->
            TextureView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surface: android.graphics.SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            updatedViewModel.setTextureView(this@apply) // Updated ViewModel reference
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean = true
                    override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                }
            }.also {
                textureView = it
            }
        },
        update = { view ->
            if (textureView == null) {
                textureView = view
                updatedViewModel.setTextureView(view)
            }
        }
    )
}
