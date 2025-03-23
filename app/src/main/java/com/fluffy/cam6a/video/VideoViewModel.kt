package com.fluffy.cam6a.video

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.TextureView
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.*
import com.fluffy.cam6a.camera.CameraHelper
import com.fluffy.cam6a.utils.FileHelper
import com.fluffy.cam6a.utils.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private var textureViewState: TextureView? = null
    private var cameraHelper: CameraHelper? = null
    private val context = getApplication<Application>().applicationContext

    private val fileHelper = FileHelper(context) // FileHelper instance

    private val _recordingSuccess = MutableLiveData(false)
    val recordingSuccess: LiveData<Boolean> get() = _recordingSuccess

    private val _recentVideos = MutableLiveData<List<Uri>>()
    val recentVideos: LiveData<List<Uri>> get() = _recentVideos

    private val _permissionsGranted = MutableLiveData(false)
    val permissionsGranted: LiveData<Boolean> get() = _permissionsGranted

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> get() = _isRecording

    private val _recordingTime = MutableLiveData(0)
    val recordingTime: LiveData<Int> get() = _recordingTime

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var recordingTimer: Timer? = null

    /** Sets the permission launcher */
    fun setPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        permissionLauncher = launcher
    }

    /** Updates the permissions granted state */
    fun updatePermissionsGranted(allGranted: Boolean) {
        _permissionsGranted.value = allGranted
    }

    /** Checks if all required permissions are granted */
    fun arePermissionsGranted(context: Context): Boolean {
        return PermissionHelper(context).hasAllPermissions()
    }

    /** Requests required permissions */
    fun requestPermissions() {
        permissionLauncher?.launch(PermissionHelper.REQUIRED_PERMISSIONS)
    }

    /** Initializes the camera manager */
    fun initializeCameraManager(context: Context) {
        // Initialize camera-related components here
    }

    /** Initializes TextureView and CameraHelper */
    fun initializeTextureView(textureView: TextureView) {
        textureViewState = textureView
        if (cameraHelper == null) {
            // Pass the fileHelper instance to CameraHelper
            cameraHelper = CameraHelper(context, textureView, fileHelper)
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
    fun startRecording() {
        _isRecording.value = true
        startRecordingTimer()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cameraHelper?.startRecording()
                Log.d(TAG, "Recording started")
            } catch (e: Exception) {
                logError("Error starting recording: ${e.message}")
            }
        }
    }

    /** Stops video recording and saves the video */
    fun stopRecording() {
        _isRecording.value = false
        stopRecordingTimer()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val videoUri = cameraHelper?.stopRecording()
                if (videoUri != null) {
                    _recordingSuccess.postValue(true)
                    Log.d(TAG, "Recording stopped and saved: $videoUri")
                } else {
                    _recordingSuccess.postValue(false)
                    Log.e(TAG, "Failed to save recording")
                }
            } catch (e: Exception) {
                logError("Error stopping recording: ${e.message}")
            }
        }
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

    /** Starts the recording timer */
    private fun startRecordingTimer() {
        recordingTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    _recordingTime.postValue((_recordingTime.value ?: 0) + 1)
                }
            }, 1000, 1000) // Update every second
        }
    }

    /** Stops the recording timer */
    private fun stopRecordingTimer() {
        recordingTimer?.cancel()
        recordingTimer = null
        _recordingTime.postValue(0)
    }

    /** Switches between front and back cameras */
    fun switchCamera() {
        cameraHelper?.switchCamera()
    }

    /** Resets the recording success flag */
    fun resetRecordingSuccess() {
        _recordingSuccess.value = false
    }

    /** Releases all resources (e.g., camera) */
    fun releaseAll() {
        cameraHelper?.closeCamera()
    }

    /** Logs errors with Log.e */
    private fun logError(message: String) {
        Log.e(TAG, "VideoViewModel Error: $message")
    }

    companion object {
        private const val TAG = "VideoViewModel"
    }
}