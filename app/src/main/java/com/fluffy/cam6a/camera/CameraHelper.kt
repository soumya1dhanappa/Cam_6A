package com.fluffy.cam6a.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Helper class for camera operations that can be shared between photo and video screens
 */
class CameraHelper(private val context: Context) {
    private val TAG = "CameraHelper"

    // Camera properties
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null
    private var activeRecording: Recording? = null

    // Camera state
    private val _flashEnabled = MutableStateFlow(false)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled

    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled

    private val _cameraLensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val cameraLensFacing: StateFlow<Int> = _cameraLensFacing

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    /**
     * Initialize the camera with lifecycle owner
     */
    suspend fun initializeCamera(lifecycleOwner: LifecycleOwner): ProcessCameraProvider {
        return suspendCoroutine { continuation ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider
                    continuation.resume(provider)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize camera", e)
                    continuation.resumeWithException(e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    /**
     * Start camera preview
     */
    fun startCameraPreview(
        lifecycleOwner: LifecycleOwner,
        previewSurfaceProvider: Preview.SurfaceProvider,
        imageAnalysisAnalyzer: ImageAnalysis.Analyzer? = null
    ) {
        val cameraProvider = cameraProvider ?: return

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()

            // Camera selector
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(_cameraLensFacing.value)
                .build()

            // Preview use case
            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(previewSurfaceProvider)
                }

            // ImageCapture use case
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setFlashMode(if (_flashEnabled.value) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                .build()

            // Optional image analysis
            imageAnalysisAnalyzer?.let {
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor, it)
                    }
            }

            // Bind use cases
            val useCases = mutableListOf<androidx.camera.core.UseCase>().apply {
                add(preview!!)
                add(imageCapture!!)
                imageAnalysis?.let { add(it) }
            }

            // Connect camera to use cases
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )

            // Update torch state if available
            camera?.cameraInfo?.torchState?.observe(lifecycleOwner) { torchState ->
                _torchEnabled.value = torchState == androidx.camera.core.TorchState.ON
            }

        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    /**
     * Set up video capture
     */
    fun setupVideoCapture(lifecycleOwner: LifecycleOwner, previewSurfaceProvider: Preview.SurfaceProvider) {
        val cameraProvider = cameraProvider ?: return

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()

            // Camera selector
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(_cameraLensFacing.value)
                .build()

            // Setup preview
            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(previewSurfaceProvider)
                }

            // Setup video capture with quality selector
            val qualitySelector = QualitySelector.fromOrderedList(
                listOf(Quality.FHD, Quality.HD, Quality.SD),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )

            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            // Bind use cases
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )

            // Update torch state if available
            camera?.cameraInfo?.torchState?.observe(lifecycleOwner) { torchState ->
                _torchEnabled.value = torchState == androidx.camera.core.TorchState.ON
            }

        } catch (e: Exception) {
            Log.e(TAG, "Video capture setup failed", e)
        }
    }

    /**
     * Take a photo and save to a file
     * @param outputFile The file to save the photo to
     * @param onPhotoCaptured Callback with success/failure result
     */
    fun takePhoto(
        outputFile: File,
        onPhotoCaptured: (success: Boolean, error: String?, savedUri: android.net.Uri?) -> Unit
    ) {
        val imageCapture = imageCapture ?: run {
            onPhotoCaptured(false, "Camera not initialized", null)
            return
        }

        // Setup output options
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        // Take the picture
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    Log.d(TAG, "Photo saved: $savedUri")
                    onPhotoCaptured(true, null, savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    onPhotoCaptured(false, exception.message, null)
                }
            }
        )
    }

    /**
     * Start video recording
     * @param outputFile The file to save the video to
     * @param onVideoRecordEvent Callback with video recording events
     */
    fun startVideoRecording(
        outputFile: File,
        onVideoRecordEvent: (VideoRecordEvent) -> Unit
    ) {
        if (_isRecording.value) return

        val videoCapture = videoCapture ?: run {
            Log.e(TAG, "Video capture not initialized")
            return
        }

        // Setup output options
        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        // Start recording
        activeRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .apply {
                if (PermissionHelper(context).hasAudioPermission()) {
                    withAudioEnabled()
                }
            }
            .start(cameraExecutor) { event ->
                onVideoRecordEvent(event)

                if (event is VideoRecordEvent.Finalize) {
                    _isRecording.value = false
                }
            }

        _isRecording.value = true
    }

    /**
     * Stop video recording
     */
    fun stopVideoRecording() {
        if (!_isRecording.value) return

        activeRecording?.stop()
        activeRecording = null
    }

    /**
     * Toggle camera flash mode
     */
    fun toggleFlash() {
        _flashEnabled.value = !_flashEnabled.value
        imageCapture?.flashMode = if (_flashEnabled.value) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
    }

    /**
     * Toggle camera torch mode
     */
    fun toggleTorch() {
        camera?.cameraControl?.enableTorch(!_torchEnabled.value)
    }

    /**
     * Switch between front and back cameras
     */
    fun toggleCamera(lifecycleOwner: LifecycleOwner, previewSurfaceProvider: Preview.SurfaceProvider) {
        _cameraLensFacing.value = if (_cameraLensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        // Restart camera with new lens facing
        startCameraPreview(lifecycleOwner, previewSurfaceProvider)
    }

    /**
     * Get image analysis use case setup for filtering
     */
    fun getImageAnalysis(): ImageAnalysis {
        if (imageAnalysis == null) {
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
        }
        return imageAnalysis!!
    }

    /**
     * Get the correct rotation for the current device orientation
     */
    @SuppressLint("RestrictedApi")
    fun getImageRotationDegrees(rotation: Int): Int {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.first()
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

        return when (rotation) {
            Surface.ROTATION_0 -> sensorOrientation
            Surface.ROTATION_90 -> (sensorOrientation + 270) % 360
            Surface.ROTATION_180 -> (sensorOrientation + 180) % 360
            Surface.ROTATION_270 -> (sensorOrientation + 90) % 360
            else -> sensorOrientation
        }
    }

    /**
     * Helper class for camera permissions
     */
    inner class PermissionHelper(private val context: Context) {
        fun hasAudioPermission(): Boolean {
            return com.fluffy.cam6a.utils.PermissionHelper.hasAudioPermission(context)
        }
    }

    /**
     * Convert ImageProxy to Bitmap for filtering
     */
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val yuvImage = android.graphics.YuvImage(
            bytes,
            android.graphics.ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )

        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
            100,
            out
        )

        val imageBytes = out.toByteArray()
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        // Apply rotation if needed
        val matrix = Matrix()
        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        return Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width, bitmap.height,
            matrix,
            true
        )
    }

    /**
     * Release camera resources
     */
    fun shutdown() {
        try {
            cameraExecutor.shutdown()
            activeRecording?.stop()
            activeRecording = null
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down camera", e)
        }
    }

    /**
     * Get available camera resolutions
     */
    fun getAvailableCameraResolutions(): List<Size> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.first()
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)

        val streamConfigurationMap = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        ) ?: return emptyList()

        return streamConfigurationMap.getOutputSizes(android.graphics.ImageFormat.JPEG).toList()
    }
}