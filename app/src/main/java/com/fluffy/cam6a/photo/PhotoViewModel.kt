package com.fluffy.cam6a.photo

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.TextureView
import androidx.lifecycle.*
import com.fluffy.cam6a.camera.CameraHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private var textureViewState: TextureView? = null
    private var cameraHelper: CameraHelper? = null
    private val context: Context = getApplication<Application>().applicationContext

    private val _captureSuccess = MutableLiveData<Boolean>()
    val captureSuccess: LiveData<Boolean> get() = _captureSuccess

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> get() = _selectedImageUri

    private val _recentImages = MutableLiveData<List<Uri>>()
    val recentImages: LiveData<List<Uri>> get() = _recentImages

    /** ✅ Stores the selected image URI */
    fun setSelectedImage(uri: Uri?) {
        _selectedImageUri.postValue(uri)
    }

    /** ✅ Initializes TextureView and CameraHelper */
    fun setTextureView(textureView: TextureView) {
        textureViewState = textureView
        if (cameraHelper == null) {
            cameraHelper = CameraHelper(context, textureView)
        }
        openCamera()
    }

    /** ✅ Opens the camera */
    fun openCamera() {
        viewModelScope.launch(Dispatchers.Main) {
            cameraHelper?.openCamera() ?: logError("CameraHelper not initialized")
        }
    }

    /** ✅ Captures an image and saves it */
    fun captureImage() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap: Bitmap? = cameraHelper?.captureBitmap()
                if (bitmap == null) {
                    logError("❌ Failed to capture image - bitmap is null")
                    _captureSuccess.postValue(false)
                    return@launch
                }

                val savedUri = saveImageToGallery(bitmap)
                if (savedUri != null) {
                    setSelectedImage(savedUri)
                    _captureSuccess.postValue(true)
                    Log.d(TAG, "✅ Image saved successfully: $savedUri")

                    // Refresh recent images after saving
                    fetchRecentImages()
                } else {
                    logError("❌ Failed to save image")
                    _captureSuccess.postValue(false)
                }
            } catch (e: Exception) {
                logError("❌ Error capturing image: ${e.localizedMessage}")
                _captureSuccess.postValue(false)
            }
        }
    }

    /** ✅ Saves the captured image to the gallery */
    private fun saveImageToGallery(bitmap: Bitmap): Uri? {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyCameraApp")
        }

        val resolver: ContentResolver = context.contentResolver
        val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        return try {
            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                Log.d(TAG, "✅ Image successfully written to $uri")
                uri
            }
        } catch (e: IOException) {
            logError("❌ Error saving image: ${e.localizedMessage}")
            null
        }
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
                Log.d(TAG, "✅ Fetched ${imagesList.size} recent images")
            } catch (e: Exception) {
                logError("❌ Error fetching recent images: ${e.localizedMessage}")
            }
        }
    }

    /** ✅ Reset capture success flag */
    fun resetCaptureSuccess() {
        _captureSuccess.postValue(false)
    }

    /** ✅ Switches between front and back cameras */
    fun switchCamera() {
        viewModelScope.launch(Dispatchers.Main) {
            cameraHelper?.switchCamera() ?: logError("CameraHelper not initialized")
        }
    }

    /** ✅ Logs errors with Log.e */
    private fun logError(message: String) {
        Log.e(TAG, "❌ PhotoViewModel Error: $message")
    }

    companion object {
        private const val TAG = "PhotoViewModel"
    }
}

/** ✅ Factory class for creating PhotoViewModel with Application context */
class PhotoViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhotoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
