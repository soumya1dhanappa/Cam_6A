package com.fluffy.cam6a.video

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.TextureView
import androidx.lifecycle.*
import com.fluffy.cam6a.camera.CameraHelper
import com.fluffy.cam6a.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private var textureViewState: TextureView? = null
    private var cameraHelper: CameraHelper? = null
    private val context: Context = getApplication<Application>().applicationContext

    private val fileHelper = FileHelper(context)

    private val _captureSuccess = MutableLiveData<Boolean>()
    val captureSuccess: LiveData<Boolean> get() = _captureSuccess

    private val _selectedVideoUri = MutableLiveData<Uri?>()
    val selectedVideoUri: LiveData<Uri?> get() = _selectedVideoUri

    private val _recentVideos = MutableLiveData<List<Uri>>()
    val recentVideos: LiveData<List<Uri>> get() = _recentVideos

    // Recording state
    private val _isRecording = MutableLiveData<Boolean>(false)
    val isRecording: LiveData<Boolean> get() = _isRecording

    /** Stores the selected video URI */
    fun setSelectedVideo(uri: Uri?) {
        _selectedVideoUri.postValue(uri)
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

    /** Starts video recording */
    fun captureVideo() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_isRecording.value == true) {
                // Already recording, stop it instead
                stopRecording()
                return@launch
            }

            try {
                _isRecording.postValue(true)
                val savedUri = cameraHelper?.startVideoRecording()
                if (savedUri != null) {
                    setSelectedVideo(savedUri)
                    Log.d(TAG, "Started video recording: $savedUri")
                } else {
                    logError("Failed to start video recording")
                    _isRecording.postValue(false)
                }
            } catch (e: Exception) {
                logError("Error starting video recording: ${e.localizedMessage}")
                _isRecording.postValue(false)
            }
        }
    }

    /** Stops video recording */
    fun stopRecording() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cameraHelper?.stopVideoRecording()
                _isRecording.postValue(false)
                _captureSuccess.postValue(true)
                fetchRecentVideos() // Update the gallery after recording is complete
                Log.d(TAG, "Video recording stopped successfully")
            } catch (e: Exception) {
                logError("Error stopping video recording: ${e.localizedMessage}")
                _isRecording.postValue(false)
                _captureSuccess.postValue(false)
            }
        }
    }

    /** Logs errors with Log.e */
    private fun logError(message: String) {
        Log.e(TAG, "VideoViewModel Error: $message")
    }

    /** Fetches recent videos from the gallery */
    fun fetchRecentVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val videosList = mutableListOf<Uri>()
                val projection = arrayOf(MediaStore.Video.Media._ID)
                val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC LIMIT 5"

                val query = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, sortOrder
                )

                query?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                        videosList.add(uri)
                    }
                }

                _recentVideos.postValue(videosList)
                Log.d(TAG, "Fetched ${videosList.size} recent videos")
            } catch (e: Exception) {
                logError("Error fetching recent videos: ${e.localizedMessage}")
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

    /** Properly implemented startVideoRecording method that was previously a TODO */
    fun startVideoRecording() {
        captureVideo() // Reuse the existing captureVideo method
    }

    /** Properly implemented stopVideoRecording method that was previously a TODO */
    fun stopVideoRecording() {
        stopRecording() // Reuse the existing stopRecording method
    }

    /** Clean up resources when ViewModel is cleared */
    override fun onCleared() {
        super.onCleared()
        cameraHelper?.closeCamera()
    }

    companion object {
        const val TAG = "VideoViewModel"
    }
}

class VideoViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}