package com.fluffy.cam6a.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import com.fluffy.cam6a.utils.FileHelper
import java.io.IOException
import java.util.*

class CameraHelper(private val context: Context, private val textureView: TextureView) {

    private var cameraId: String = ""
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    // Video recording components
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var videoFilePath: String = ""
    private val fileHelper = FileHelper(context)

    init {
        startBackgroundThread()
        cameraId = getBackCameraId()
        Log.d(TAG, "Camera initialized with ID: $cameraId")
    }

    private fun getBackCameraId(): String {
        return cameraManager.cameraIdList.find { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: ""
    }

    private fun getFrontCameraId(): String {
        return cameraManager.cameraIdList.find { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        } ?: ""
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackgroundThread").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        backgroundThread?.join()
        backgroundThread = null
        backgroundHandler = null
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        if (cameraId.isEmpty() || ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted or invalid camera ID")
            return
        }

        if (textureView.isAvailable) {
            startCameraSession()
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    startCameraSession()
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = false
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
        }
    }

    private fun startCameraSession() {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCameraPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error opening camera: ${e.message}")
        }
    }

    private fun createCameraPreviewSession() {
        val surfaceTexture = textureView.surfaceTexture ?: return
        surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
        val previewSurface = Surface(surfaceTexture)

        if (imageReader == null) {
            imageReader = ImageReader.newInstance(textureView.width, textureView.height, ImageFormat.JPEG, 1)
        }

        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder?.addTarget(previewSurface)

        try {
            cameraDevice?.createCaptureSession(
                listOf(previewSurface, imageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        captureRequestBuilder?.build()?.let {
                            captureSession?.setRepeatingRequest(it, null, backgroundHandler)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Session configuration failed")
                    }
                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error configuring session: ${e.message}")
        }
    }

    /** Captures a Bitmap from Camera */
    fun captureBitmap(): Bitmap? {
        return try {
            if (!textureView.isAvailable) {
                Log.e(TAG, "TextureView is not available")
                null
            } else {
                textureView.bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, " Error capturing bitmap: ${e.localizedMessage}")
            null
        }
    }

    fun closeCamera() {
        stopBackgroundThread()
        cameraDevice?.close()
        cameraDevice = null
        captureSession?.close()
        captureSession = null
        imageReader?.close()
        imageReader = null
    }

    fun switchCamera() {
        cameraId = if (cameraId == getBackCameraId()) getFrontCameraId() else getBackCameraId()
        closeCamera()
        openCamera()
    }

    /** Sets up MediaRecorder for video capture */
    private fun setupMediaRecorder(): MediaRecorder {
        val rotation = 0 // You might want to get this from the display rotation

        return MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            // Create a new file for the video
            videoFilePath = fileHelper.createVideoFile().absolutePath
            setOutputFile(videoFilePath)

            setVideoEncodingBitRate(10_000_000)
            setVideoFrameRate(30)

            // Get optimal video size
            val videoSize = getOptimalVideoSize()
            setVideoSize(videoSize.width, videoSize.height)

            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOrientationHint(rotation)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(TAG, "Error preparing MediaRecorder: ${e.message}")
                throw e
            }
        }
    }

    /** Gets optimal video size based on camera characteristics */
    private fun getOptimalVideoSize(): Size {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        // Get available sizes for MediaRecorder output
        val videoSizes = streamConfigMap?.getOutputSizes(MediaRecorder::class.java) ?: arrayOf()

        // Default to 1080p or the highest available resolution below that
        val targetResolution = Size(1920, 1080)

        return if (videoSizes.isNotEmpty()) {
            // Find size that is closest to 1080p without exceeding it
            videoSizes.filter { it.height <= targetResolution.height && it.width <= targetResolution.width }
                .maxByOrNull { it.width * it.height } ?: videoSizes[0]
        } else {
            Size(1280, 720) // Fallback to 720p
        }
    }

    /** Starts video recording and returns the URI of the saved file */
    fun startVideoRecording(): Uri? {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return null
        }

        try {
            // Close any existing preview session
            captureSession?.close()
            captureSession = null

            // Initialize MediaRecorder
            mediaRecorder = setupMediaRecorder()

            // Create surfaces for both preview and recording
            val surfaceTexture = textureView.surfaceTexture ?: return null
            surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
            val previewSurface = Surface(surfaceTexture)
            val recorderSurface = mediaRecorder!!.surface

            // Set up a new capture session with both surfaces
            val surfaces = listOf(previewSurface, recorderSurface)

            // Create capture request for video recording
            val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            captureRequestBuilder?.addTarget(previewSurface)
            captureRequestBuilder?.addTarget(recorderSurface)

            cameraDevice?.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureRequestBuilder?.build()?.let {
                        captureSession?.setRepeatingRequest(it, null, backgroundHandler)

                        // Start recording
                        mediaRecorder?.start()
                        isRecording = true
                        Log.d(TAG, "Started recording to $videoFilePath")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Failed to configure capture session for video recording")
                }
            }, backgroundHandler)

            // Return the URI for the video file
            return Uri.parse("file://$videoFilePath")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting video recording: ${e.message}")
            releaseMediaRecorder()
            return null
        }
    }

    /** Stops video recording */
    fun stopVideoRecording() {
        if (!isRecording) {
            Log.w(TAG, "Not recording")
            return
        }

        try {
            // Stop recording
            mediaRecorder?.stop()
            Log.d(TAG, "Stopped recording")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}")
        } finally {
            releaseMediaRecorder()
            // Restart the preview
            startCameraSession()
        }
    }

    /** Releases MediaRecorder resources */
    private fun releaseMediaRecorder() {
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null
        isRecording = false
    }

    companion object {
        private const val TAG = "CameraHelper"
    }
}