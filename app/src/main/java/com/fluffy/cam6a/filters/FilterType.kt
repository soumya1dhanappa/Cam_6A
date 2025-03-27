package com.fluffy.cam6a.filters

enum class FilterType(val displayName: String) {
    NONE("None"),
    GRAYSCALE("Grayscale"),
    SEPIA("Sepia"),
    ECLIPSE("Eclipse"),
    COOL_TONE("Cool Tone"),
    WARM_TONE("Warm Tone");

    override fun toString(): String = displayName
}