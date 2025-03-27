package com.fluffy.cam6a.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import com.fluffy.cam6a.utils.FileHelper
import java.io.File
import kotlin.math.max
import kotlin.math.min

class CameraHelper(
    private val context: Context,
    private val textureView: TextureView,
    private val fileHelper: FileHelper // Corrected direct assignment
) {
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var zoomLevel = 1.0f
    private var minExposure = 0
    private var maxExposure = 0
    private var currentExposure = 0
    private var cameraId: String = ""
    private var isFrontCamera = false
    private lateinit var captureSession: CameraCaptureSession

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = Handler(Looper.getMainLooper())

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var recordingFile: File? = null

    private val TAG = "CameraHelper" // Corrected TAG usage

    init {
        startBackgroundThread()
        cameraId = getCameraId(isFrontCamera)
        setupCamera()
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

    private fun setupCamera() {
        cameraId = getCameraId(isFrontCamera)
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        minExposure = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)?.lower ?: 0
        maxExposure = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)?.upper ?: 0
    }

    fun openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted")
                return
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCameraPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    Log.e(TAG, "Camera error: $error")
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to open camera: ${e.message}")
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val surfaceTexture = textureView.surfaceTexture ?: return
            surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
            val surface = Surface(surfaceTexture)

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)

            cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    updatePreview()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Failed to configure camera preview")
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create camera preview session: ${e.message}")
        }
    }

    fun captureBitmap(): Bitmap? {
        return textureView.bitmap
    }

    fun applyZoom(zoomLevel: Float) {
        this.zoomLevel = zoomLevel.coerceIn(1.0f, 3.0f)
        updatePreview()
    }

    fun adjustExposure(increase: Boolean) {
        currentExposure = if (increase) min(currentExposure + 1, maxExposure) else max(currentExposure - 1, minExposure)
        updatePreview()
    }

    private fun updatePreview() {
        if (!::cameraCaptureSession.isInitialized) {
            Log.e(TAG, "CameraCaptureSession is not initialized")
            return
        }
        captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, getZoomRect())
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, currentExposure)
        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
    }

    fun switchCamera() {
        if (::cameraDevice.isInitialized) {
            cameraDevice.close()
        } else {
            Log.e(TAG, "Camera device is not initialized! Skipping close.")
        }

        isFrontCamera = !isFrontCamera
        cameraId = getCameraId(isFrontCamera)

        try {
            openCamera() // Ensure openCamera() is properly called
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera after switch: ${e.message}")
        }
    }



    private fun getCameraId(front: Boolean): String {
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if ((front && facing == CameraCharacteristics.LENS_FACING_FRONT) ||
                (!front && facing == CameraCharacteristics.LENS_FACING_BACK)
            ) {
                return id
            }
        }
        return cameraManager.cameraIdList.firstOrNull() ?: throw IllegalStateException("No camera found")
    }

    private fun getZoomRect(): Rect {
        val sensorSize = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            ?: return Rect()
        val centerX = sensorSize.width() / 2
        val centerY = sensorSize.height() / 2
        val deltaX = (sensorSize.width() / zoomLevel).toInt() / 2
        val deltaY = (sensorSize.height() / zoomLevel).toInt() / 2
        return Rect(centerX - deltaX, centerY - deltaY, centerX + deltaX, centerY + deltaY)
    }

    fun startRecording() {
        if (isRecording) return

        try {
            recordingFile = fileHelper.createVideoFile()
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(recordingFile!!.absolutePath)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoSize(textureView.width, textureView.height)
                setVideoFrameRate(30)
                setOrientationHint(90)
                prepare()
            }

            val previewSurface = Surface(textureView.surfaceTexture)
            val recorderSurface = mediaRecorder!!.surface

            cameraDevice.createCaptureSession(
                listOf(previewSurface, recorderSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        val recordRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                            addTarget(previewSurface)
                            addTarget(recorderSurface)
                        }.build()

                        session.setRepeatingRequest(recordRequest, null, backgroundHandler)
                        mediaRecorder?.start()
                        isRecording = true
                        Log.d(TAG, "Recording started")
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to configure recording session")
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording: ${e.message}")
        }
    }

    fun stopRecording(): Uri? {
        return try {
            if (isRecording) {
                isRecording = false
                mediaRecorder?.apply {
                    stop()
                    reset()
                    release()
                }
                mediaRecorder = null
                recordingFile?.let { fileHelper.notifyMediaScanner(it) }
                Uri.fromFile(recordingFile)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}")
            null
        }
    }

    fun closeCamera() {
        try {
            if (::cameraCaptureSession.isInitialized) cameraCaptureSession.close()
            if (::cameraDevice.isInitialized) cameraDevice.close()
            Log.d(TAG, "Camera closed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera: ${e.message}")
        }
    }


}
