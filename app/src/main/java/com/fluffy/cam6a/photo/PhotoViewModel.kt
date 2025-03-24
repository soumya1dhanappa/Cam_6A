package com.fluffy.cam6a.photo
import android.app.Application

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.TextureView
import androidx.lifecycle.*
import com.fluffy.cam6a.camera.CameraHelper
import com.fluffy.cam6a.filters.FiltersViewModel
import com.fluffy.cam6a.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private var textureViewState: TextureView? = null
    private var cameraHelper: CameraHelper? = null
    private val context: Context = getApplication<Application>().applicationContext

    private val fileHelper = FileHelper(context) // Initialize FileHelper

    private val _captureSuccess = MutableLiveData<Boolean>()
    val captureSuccess: LiveData<Boolean> get() = _captureSuccess

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> get() = _selectedImageUri

    private val _recentImages = MutableLiveData<List<Uri>>()
    val recentImages: LiveData<List<Uri>> get() = _recentImages

    private val _zoomLevel = MutableLiveData(1.0f) // Default zoom level
    val zoomLevel: LiveData<Float> = _zoomLevel

    /** Stores the selected image URI */
    fun setSelectedImage(uri: Uri?) {
        _selectedImageUri.postValue(uri)
    }

    /** Initializes TextureView and CameraHelper */
    fun setTextureView(textureView: TextureView) {
        textureViewState = textureView
        if (cameraHelper == null) {
            cameraHelper = CameraHelper(context, textureView)
        }
        openCamera()
    }

    /** Opens the camera */
    fun openCamera() {
        viewModelScope.launch(Dispatchers.Main) {
            cameraHelper?.openCamera()
            delay(500) // ✅ Small delay to allow camera to initialize
            cameraHelper?.applyZoomToCamera(_zoomLevel.value ?: 1.0f) // ✅ Apply zoom after opening camera
        }
    }


    /** Captures an image and saves it */
    fun captureImage(filtersViewModel: FiltersViewModel) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap: Bitmap? = cameraHelper?.captureBitmap()
                if (bitmap == null) {
                    logError("Failed to capture image - bitmap is null")
                    _captureSuccess.postValue(false)
                    return@launch
                }

                // Apply the selected filter from FiltersViewModel
                val filteredBitmap = filtersViewModel.applySelectedFilter(bitmap)

                val savedUri =
                    fileHelper.saveImageToGallery(filteredBitmap) // Save the filtered image
                if (savedUri != null) {
                    setSelectedImage(savedUri)
                    _captureSuccess.postValue(true)
                    Log.d(TAG, "Filtered image saved successfully: $savedUri")

                    // Refresh recent images after saving

                } else {
                    logError("Failed to save image")
                    _captureSuccess.postValue(false)
                }
            } catch (e: Exception) {
                logError("Error capturing image: ${e.localizedMessage}")
                _captureSuccess.postValue(false)
            }
        }
    }

    /** Logs errors with Log.e */
    private fun logError(message: String) {
        Log.e(TAG, "PhotoViewModel Error: $message")
    }

    /** Fetches recent images from the gallery */


    /**  Reset capture success flag */
    fun resetCaptureSuccess() {
        _captureSuccess.postValue(false)
    }

    /**  Switches between front and back cameras */
    fun switchCamera() {
        viewModelScope.launch(Dispatchers.Main) {
            cameraHelper?.switchCamera() ?: logError("CameraHelper not initialized")
        }
    }


    fun setZoom(scaleFactor: Float) {
        val currentZoom = _zoomLevel.value ?: 1.0f
        val newZoom = (currentZoom * scaleFactor).coerceIn(1.0f, 5.0f) // Prevent over-zooming

        viewModelScope.launch(Dispatchers.Main) {
            if (cameraHelper?.cameraDevice == null) { // ✅ Check if camera is open
                logError("Cannot apply zoom: Camera is not open")
                return@launch
            }

            _zoomLevel.postValue(newZoom)
            cameraHelper?.applyZoomToCamera(newZoom) // ✅ Ensure zoom is applied only when camera is open
        }
    }



    companion object {
        const val TAG = "PhotoViewModel"
    }
}
class PhotoViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhotoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}