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
import com.fluffy.cam6a.ui.components.FilterType
import com.fluffy.cam6a.utils.FileHelper
import kotlinx.coroutines.*

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private var textureViewState: TextureView? = null
    var cameraHelper: CameraHelper? = null
    private val context: Context = getApplication<Application>().applicationContext

    private val fileHelper = FileHelper(context)



    private val _captureSuccess = MutableLiveData<Boolean>()
    val captureSuccess: LiveData<Boolean> get() = _captureSuccess

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> get() = _selectedImageUri

    private val _recentImages = MutableLiveData<List<Uri>>()
    val recentImages: LiveData<List<Uri>> get() = _recentImages

    private val _selectedFilter = MutableLiveData<FilterType>().apply { value = FilterType.NONE }
    val selectedFilter: LiveData<FilterType> get() = _selectedFilter

    private val _filteredBitmap = MutableLiveData<Bitmap?>()
    val filteredBitmap: LiveData<Bitmap?> = _filteredBitmap
    private var originalBitmap: Bitmap? = null

    /** Stores the selected image URI */
    fun setSelectedImage(uri: Uri?) {
        _selectedImageUri.postValue(uri)
    }

    /** Updates the selected filter */
    fun setFilter(filterType: FilterType) {
        _selectedFilter.postValue(filterType)
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
            cameraHelper?.openCamera() ?: logError("CameraHelper not initialized")
        }
    }

    /** Captures an image with the applied filter and saves it */
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
                val filteredBitmap = filtersViewModel.applyFilterToBitmap(bitmap)

                val savedUri = fileHelper.saveImageToGallery(filteredBitmap) // Save the filtered image
                if (savedUri != null) {
                    setSelectedImage(savedUri)
                    _captureSuccess.postValue(true)
                    Log.d(TAG, "Filtered image saved successfully: $savedUri")

                    // Refresh recent images after saving
                    fetchRecentImages()
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

    private fun logError(message: String) {
        Log.e(TAG, "PhotoViewModel Error: $message")
    }

    /** Fetches recent images from the gallery */
    fun fetchRecentImages() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imagesList = mutableListOf<Uri>()
                val projection = arrayOf(MediaStore.Images.Media._ID)

                val query = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )

                query?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        imagesList.add(uri)
                    }
                }

                _recentImages.postValue(imagesList.take(5)) // Fetches only the latest 5 images
                Log.d(TAG, "Fetched ${imagesList.size} recent images")
            } catch (e: Exception) {
                logError("Error fetching recent images: ${e.localizedMessage}")
            }
        }
    }

    /** Resets capture success flag */
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

class PhotoViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhotoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}