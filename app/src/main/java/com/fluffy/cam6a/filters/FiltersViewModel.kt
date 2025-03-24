package com.fluffy.cam6a.filters

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.*
import com.fluffy.cam6a.camera.CameraHelper
import com.fluffy.cam6a.photo.PhotoViewModel.Companion.TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FiltersViewModel(private val cameraHelper: CameraHelper) : ViewModel() {

    private val _originalImage = MutableStateFlow<Bitmap?>(null)
    val originalImage: StateFlow<Bitmap?> = _originalImage

    private val _filteredImage = MutableStateFlow<Bitmap?>(null)
    val filteredImage: StateFlow<Bitmap?> = _filteredImage

    private val _flashState = MutableStateFlow(false)
    val flashState: StateFlow<Boolean> = _flashState

    private val _selectedFilter = MutableStateFlow<(Bitmap) -> Bitmap> { it }
    val selectedFilter: StateFlow<(Bitmap) -> Bitmap> = _selectedFilter

    private val _exposureLevel = MutableStateFlow(1.0f)
    val exposureLevel: StateFlow<Float> = _exposureLevel

    private val _selectedFilterName = MutableStateFlow("Normal")
    val selectedFilterName: StateFlow<String> = _selectedFilterName

    private val _zoomLevel = MutableLiveData(1.0f)
    val zoomLevel: LiveData<Float> = _zoomLevel

    fun setOriginalImage(bitmap: Bitmap) {
        _originalImage.value = bitmap
        _filteredImage.value = applyCurrentFilter(bitmap)
    }

    fun applySelectedFilter(bitmap: Bitmap): Bitmap {
        return _selectedFilter.value.invoke(bitmap)
    }

    private fun applyCurrentFilter(bitmap: Bitmap): Bitmap {
        return _selectedFilter.value(bitmap)
    }

    fun setFilter(filter: (Bitmap) -> Bitmap, filterName: String) {
        _selectedFilter.value = filter
        _selectedFilterName.value = filterName
        _originalImage.value?.let {
            _filteredImage.value = applyCurrentFilter(it)
        }
    }

    fun toggleFlash() {
        _flashState.update { !it }
    }

    private val _currentBitmap = MutableLiveData<Bitmap?>()
    val currentBitmap: LiveData<Bitmap?> get() = _currentBitmap

    fun adjustZoom(increase: Boolean) {
        val currentZoom = _zoomLevel.value ?: 1.0f
        val newZoom = if (increase) {
            (currentZoom + 0.5f).coerceIn(1.0f, 5.0f) // Max zoom 5.0x
        } else {
            (currentZoom - 0.5f).coerceIn(1.0f, 5.0f) // Min zoom 1.0x
        }
        _zoomLevel.value = newZoom

        cameraHelper?.let {
            if (it.cameraDevice != null) { // Ensure camera is open before applying zoom
                Log.d(TAG, "Applying Zoom: $newZoom on Camera ID: ${it.cameraId}")
                it.applyZoomToCamera(newZoom)
            } else {
                Log.e(TAG, "Cannot apply zoom: Camera is not open")
            }
        }
    }



    fun setBitmap(bitmap: Bitmap) {
        _currentBitmap.postValue(bitmap)
    }

    fun adjustExposure(level: Float) {
        _exposureLevel.value = level.coerceIn(0.5f, 2.0f)
    }

    private fun applyZoom(bitmap: Bitmap, zoomFactor: Float): Bitmap {
        return bitmap
    }
}

class FiltersViewModelFactory(private val cameraHelper: CameraHelper) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FiltersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FiltersViewModel(cameraHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}