package com.fluffy.cam6a.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class FileHelper(private val context: Context) {

    companion object {
        private const val TAG = "FileHelper"
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
        private const val IMAGE_PREFIX = "IMG_"
        private const val IMAGE_EXTENSION = ".jpg"
    }

    /** ✅ Saves image to gallery and returns the saved URI */
    fun saveImageToGallery(imageBytes: ByteArray): Uri? {
        val correctedBytes = correctImageRotation(imageBytes)
        val imageUri = getImageUri()  // ✅ Get URI before saving

        return try {
            context.contentResolver.openOutputStream(imageUri)?.use { outputStream: OutputStream ->
                outputStream.write(correctedBytes)
                outputStream.flush()
            }
            Log.d(TAG, "✅ Image successfully saved: $imageUri")
            imageUri  // ✅ Return saved URI
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving image", e)
            null
        }
    }

    /** ✅ Creates a unique Uri for saving an image */
    private fun getImageUri(): Uri {
        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
        val fileName = "$IMAGE_PREFIX$timeStamp$IMAGE_EXTENSION"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Cam6A")
            }
        }

        return context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        ) ?: throw RuntimeException("❌ Failed to create media URI")
    }

    /** ✅ Fixes image rotation before saving */
    private fun correctImageRotation(imageBytes: ByteArray): ByteArray {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            val exif = ExifInterface(ByteArrayInputStream(imageBytes))

            val rotationDegrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (rotationDegrees == 0) return imageBytes // No rotation needed

            val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)

            // Convert rotated bitmap back to ByteArray
            val outputStream = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error correcting image rotation", e)
            imageBytes // Return original if rotation fails
        }
    }

    /** ✅ Rotates a bitmap by the given degrees */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
