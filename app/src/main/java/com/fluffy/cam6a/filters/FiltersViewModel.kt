package com.fluffy.cam6a.filters

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class FiltersViewModel(application: Application) : AndroidViewModel(application) {
    // Current filter type and function
    private val _currentFilterType = MutableLiveData<FilterType>(FilterType.NONE)
    val currentFilterType: LiveData<FilterType> = _currentFilterType

    private val _currentFilterFunction = MutableLiveData<(Bitmap) -> Bitmap>({ it })
    val currentFilterFunction: LiveData<(Bitmap) -> Bitmap> = _currentFilterFunction

    // Camera controls
    private val _zoomLevel = MutableLiveData(1.0f)
    val zoomLevel: LiveData<Float> = _zoomLevel

    private val _exposureLevel = MutableLiveData(1.0f)
    val exposureLevel: LiveData<Float> = _exposureLevel

    // Filter application
    fun setFilter(filterFunction: (Bitmap) -> Bitmap, filterName: String) {
        val type = when (filterName) {
            "Normal" -> FilterType.NONE
            "Grayscale" -> FilterType.GRAYSCALE
            "Sepia" -> FilterType.SEPIA
            "Eclipse" -> FilterType.ECLIPSE
            else -> FilterType.NONE
        }
        _currentFilterType.value = type
        _currentFilterFunction.value = filterFunction
        Log.d("FiltersViewModel", "Filter set to: $filterName")
    }

    fun applyFilterToBitmap(bitmap: Bitmap): Bitmap {
        return _currentFilterFunction.value?.invoke(bitmap) ?: bitmap
    }

    // Camera controls
    fun adjustZoom(zoomIn: Boolean) {
        _zoomLevel.value = (_zoomLevel.value ?: 1.0f).let {
            if (zoomIn) (it + 0.5f).coerceAtMost(5.0f)
            else (it - 0.5f).coerceAtLeast(1.0f)
        }
    }

    fun adjustExposure(level: Float) {
        _exposureLevel.value = level.coerceIn(0.5f, 2.0f)
    }
}