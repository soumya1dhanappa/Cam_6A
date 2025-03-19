package com.fluffy.cam6a.ui.components

import android.content.Context
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.fluffy.cam6a.camera.CameraHelper
import kotlinx.coroutines.launch

/**
 * A reusable Composable camera preview component for both photo and video screens
 */
@Composable
fun CameraPreview(
    cameraHelper: CameraHelper,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    isVideoMode: Boolean = false,
    modifier: Modifier = Modifier,
    onCameraReady: () -> Unit = {}
) {
    val TAG = "CameraPreview"
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var cameraInitialized by remember { mutableStateOf(false) }

    // Create a PreviewView for camera display
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Initialize camera
    LaunchedEffect(lensFacing, isVideoMode) {
        try {
            // Initialize camera provider
            val cameraProvider = cameraHelper.initializeCamera(lifecycleOwner)

            // Setup camera based on mode
            if (isVideoMode) {
                setupVideoPreview(
                    context,
                    lifecycleOwner,
                    cameraProvider,
                    cameraHelper,
                    previewView
                )
            } else {
                setupPhotoPreview(
                    context,
                    lifecycleOwner,
                    cameraProvider,
                    cameraHelper,
                    previewView
                )
            }

            cameraInitialized = true
            onCameraReady()
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed", e)
        }
    }

    // Dispose of camera resources when leaving composition
    DisposableEffect(lifecycleOwner) {
        onDispose {
            coroutineScope.launch {
                try {
                    previewUseCase?.setSurfaceProvider(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error cleaning up camera", e)
                }
            }
        }
    }

    // Camera preview UI
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Setup the camera preview for photo mode
 */
private suspend fun setupPhotoPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraProvider: ProcessCameraProvider,
    cameraHelper: CameraHelper,
    previewView: PreviewView
) {
    try {
        // Start camera preview
        cameraHelper.startCameraPreview(
            lifecycleOwner,
            previewView.surfaceProvider
        )
    } catch (e: Exception) {
        Log.e("CameraPreview", "Photo preview setup failed", e)
    }
}

/**
 * Setup the camera preview for video mode
 */
private fun setupVideoPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraProvider: ProcessCameraProvider,
    cameraHelper: CameraHelper,
    previewView: PreviewView
) {
    try {
        // Setup video capture
        cameraHelper.setupVideoCapture(
            lifecycleOwner,
            previewView.surfaceProvider
        )
    } catch (e: Exception) {
        Log.e("CameraPreview", "Video preview setup failed", e)
    }
}

/**
 * A composable that provides a camera preview with filter analysis
 */
@Composable
fun CameraPreviewWithAnalysis(
    cameraHelper: CameraHelper,
    analyzerCallback: (androidx.camera.core.ImageProxy) -> Unit,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    modifier: Modifier = Modifier,
    onCameraReady: () -> Unit = {}
) {
    val TAG = "CameraPreviewWithAnalysis"
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var cameraInitialized by remember { mutableStateOf(false) }

    // Create a PreviewView for camera display
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Setup image analyzer
    val imageAnalyzer = remember {
        androidx.camera.core.ImageAnalysis.Analyzer { imageProxy ->
            analyzerCallback(imageProxy)
            imageProxy.close()
        }
    }

    // Initialize camera with analysis
    LaunchedEffect(lensFacing) {
        try {
            // Initialize camera provider
            val cameraProvider = cameraHelper.initializeCamera(lifecycleOwner)

            // Start camera preview with analyzer
            cameraHelper.startCameraPreview(
                lifecycleOwner,
                previewView.surfaceProvider,
                imageAnalyzer
            )

            cameraInitialized = true
            onCameraReady()
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed", e)
        }
    }

    // Dispose of camera resources when leaving composition
    DisposableEffect(lifecycleOwner) {
        onDispose {
            coroutineScope.launch {
                try {
                    previewUseCase?.setSurfaceProvider(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error cleaning up camera", e)
                }
            }
        }
    }

    // Camera preview UI
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}