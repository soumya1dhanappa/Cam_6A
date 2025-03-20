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
import com.fluffy.cam6a.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private var textureViewState: TextureView? = null
    private var cameraHelper: CameraHelper? = null
    private val context: Context = getApplication<Application>().applicationContext

    private val fileHelper = FileHelper(context) //  Initialize FileHelper

    private val _captureSuccess = MutableLiveData<Boolean>()
    val captureSuccess: LiveData<Boolean> get() = _captureSuccess

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> get() = _selectedImageUri

    private val _recentImages = MutableLiveData<List<Uri>>()
    val recentImages: LiveData<List<Uri>> get() = _recentImages

    /** ViewModel Factory for instantiating PhotoViewModel */
    class PhotoViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PhotoViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PhotoViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    /**  Stores the selected image URI */
    fun setSelectedImage(uri: Uri?) {
        _selectedImageUri.postValue(uri)
    }

    /**  Initializes TextureView and CameraHelper */
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
            cameraHelper?.openCamera() ?: logError("CameraHelper not initialized")
        }
    }

    /**  Captures an image and saves it */
    fun captureImage() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap: Bitmap? = cameraHelper?.captureBitmap()
                if (bitmap == null) {
                    logError(" Failed to capture image - bitmap is null")
                    _captureSuccess.postValue(false)
                    return@launch
                }

                val savedUri = fileHelper.saveImageToGallery(bitmap) //  Call FileHelper method
                if (savedUri != null) {
                    setSelectedImage(savedUri)
                    _captureSuccess.postValue(true)
                    Log.d(TAG, " Image saved successfully: $savedUri")

                    // Refresh recent images after saving
                    fetchRecentImages()
                } else {
                    logError(" Failed to save image")
                    _captureSuccess.postValue(false)
                }
            } catch (e: Exception) {
                logError(" Error capturing image: ${e.localizedMessage}")
                _captureSuccess.postValue(false)
            }
        }
    }

    /** ✅ Logs errors with Log.e */
    private fun logError(message: String) {
        Log.e(TAG, " PhotoViewModel Error: $message")
    }

    /** ✅ Fetches recent images from the gallery */
    fun fetchRecentImages() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imagesList = mutableListOf<Uri>()
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT 5"

                val query = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, sortOrder
                )

                query?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        imagesList.add(uri)
                    }
                }

                _recentImages.postValue(imagesList)
                Log.d(TAG, " Fetched ${imagesList.size} recent images")
            } catch (e: Exception) {
                logError(" Error fetching recent images: ${e.localizedMessage}")
            }
        }
    }

    /** Reset capture success flag */
    fun resetCaptureSuccess() {
        _captureSuccess.postValue(false)
    }

    /** Switches between front and back cameras */
    fun switchCamera() {
        viewModelScope.launch(Dispatchers.Main) {
            cameraHelper?.switchCamera() ?: logError("CameraHelper not initialized")
        }
    }

    companion object {
        const val TAG = "PhotoViewModel"
    }
}
