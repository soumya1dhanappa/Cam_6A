package com.fluffy.cam6a.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat

class CameraHelper(private val context: Context, private val textureView: TextureView) {

    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var zoomLevel: Float = 1.0f // Default zoom level
    private var _cameraId: String = ""
    var cameraId: String
        get() = _cameraId
        private set(value) { _cameraId = value }
    internal var cameraDevice: CameraDevice? = null
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    init {
        startBackgroundThread()
        cameraId = getBackCameraId()
        Log.d(TAG, "Camera initialized with ID: $cameraId")
    }

    private fun getBackCameraId(): String {
        return cameraManager.cameraIdList.find {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
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
        if (cameraDevice != null) {
            Log.d(TAG, "Camera already open")
            return
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
            }
        }, backgroundHandler)
    }


    private fun createCameraPreviewSession() {
        val surfaceTexture = textureView.surfaceTexture ?: return
        surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
        val previewSurface = Surface(surfaceTexture)

        captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder?.addTarget(previewSurface)

        cameraDevice?.createCaptureSession(
            listOf(previewSurface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureSession?.setRepeatingRequest(captureRequestBuilder!!.build(), null, backgroundHandler)
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Session configuration failed")
                }
            }, backgroundHandler
        )
    }

    fun applyZoomToCamera(zoomFactor: Float) {
        if (cameraDevice == null || captureSession == null) {
            Log.e(TAG, "Cannot apply zoom: Camera is not open")
            return
        }

        try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return
            val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
            val newZoom = zoomFactor.coerceIn(1.0f, maxZoom)

            val cropWidth = (rect.width() / newZoom).toInt()
            val cropHeight = (rect.height() / newZoom).toInt()
            val zoomRect = Rect(
                rect.centerX() - cropWidth / 2,
                rect.centerY() - cropHeight / 2,
                rect.centerX() + cropWidth / 2,
                rect.centerY() + cropHeight / 2
            )

            captureRequestBuilder?.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
            captureSession?.setRepeatingRequest(captureRequestBuilder!!.build(), null, backgroundHandler)
            Log.d(TAG, "Zoom applied: $newZoom")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply zoom: ${e.localizedMessage}")
        }
    }

    fun captureBitmap(): Bitmap? {
        return if (textureView.isAvailable) {
            textureView.bitmap
        } else {
            Log.e(TAG, "TextureView is not available")
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
        if (cameraId.isEmpty()) {
            Log.e(TAG, "Failed to switch camera: No valid camera ID found")
            return
        }

        Log.d(TAG, "Switching to Camera ID: $cameraId")
        closeCamera()
        openCamera()
        zoomLevel = 1.0f // Reset zoom on switch
    }

    companion object {
        private const val TAG = "CameraHelper"
    }
}