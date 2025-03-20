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
import android.os.*
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat

class CameraHelper(private val context: Context, private val textureView: TextureView) {

    private var cameraId: String = ""
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

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

    companion object {
        private const val TAG = "CameraHelper"
    }
}
