package com.fluffy.cam6a.filters

import android.graphics.*

fun applyGrayscaleFilter(original: Bitmap): Bitmap {
    val grayscaleBitmap = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(grayscaleBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(original, 0f, 0f, paint)
    return grayscaleBitmap
}

fun applySepiaFilter(original: Bitmap): Bitmap {
    val sepiaBitmap = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(sepiaBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setScale(1f, 0.95f, 0.82f, 1f)
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(original, 0f, 0f, paint)
    return sepiaBitmap
}

fun applyEclipseFilter(original: Bitmap): Bitmap {
    val eclipseBitmap = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(eclipseBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix()

    colorMatrix.set(
        floatArrayOf(
            0.6f, 0.0f, 0.4f, 0f, 50f,
            0.0f, 0.6f, 0.4f, 0f, 30f,
            0.3f, 0.0f, 1.0f, 0f, 80f,
            0.0f, 0.0f, 0.0f, 1f, 0f
        )
    )

    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(original, 0f, 0f, paint)
    return eclipseBitmap
}

fun adjustBrightness(original: Bitmap, factor: Float): Bitmap {
    val brightnessBitmap = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(brightnessBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setScale(factor, factor, factor, 1f)
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(original, 0f, 0f, paint)
    return brightnessBitmap
}