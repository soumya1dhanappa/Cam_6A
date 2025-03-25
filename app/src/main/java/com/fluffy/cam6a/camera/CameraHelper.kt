package com.fluffy.cam6a.camera

import android.R.attr.id
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.camera2.*
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import kotlin.math.max
import kotlin.math.min

class CameraHelper(private val context: Context, private val textureView: TextureView) {
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var zoomLevel = 1.0f
    private var minExposure = 0
    private var maxExposure = 0
    private var currentExposure = 0
    private var cameraId: String = ""
    private var isFrontCamera = false // Default to back camera

    init {
        setupCamera()
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
                Log.e("CameraHelper", "Camera permission not granted")
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
                    Log.e("CameraHelper", "Camera error: $error")
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e("CameraHelper", "Failed to open camera: ${e.message}")
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
                    Log.e("CameraHelper", "Failed to configure camera preview")
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e("CameraHelper", "Failed to create camera preview session: ${e.message}")
        }
    }

    fun captureBitmap(): Bitmap? {
        return textureView.bitmap
    }

    fun applyZoom(zoomLevel: Float) {
        this.zoomLevel = zoomLevel.coerceIn(1.0f, 3.0f) // Limit zoom between 1x and 3x
        updatePreview()
    }

    fun adjustExposure(increase: Boolean) {
        currentExposure = if (increase) min(currentExposure + 1, maxExposure) else max(currentExposure - 1, minExposure)
        updatePreview()
    }

    private fun updatePreview() {
        captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, getZoomRect())
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, currentExposure)
        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
    }

    fun switchCamera() {
        cameraDevice?.close()
        isFrontCamera = !isFrontCamera
        cameraId = getCameraId(isFrontCamera)
        openCamera()
    }

    private fun getCameraId(front: Boolean): String {
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if ((front && facing == CameraCharacteristics.LENS_FACING_FRONT) ||
                (!front && facing == CameraCharacteristics.LENS_FACING_BACK)) {
                return id
            }
        }
        return cameraManager.cameraIdList[id]
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
}
