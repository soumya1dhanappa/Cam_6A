package com.fluffy.cam6a.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FileHelper(private val context: Context) {

    companion object {
        private const val TAG = "FileHelper"
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
        private const val IMAGE_PREFIX = "IMG_"
        private const val VIDEO_PREFIX = "VID_"
        private const val IMAGE_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"
    }

    /** Saves the captured image to the gallery */
    fun saveImageToGallery(bitmap: Bitmap): Uri? {
        val filename = "$IMAGE_PREFIX${System.currentTimeMillis()}$IMAGE_EXTENSION"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Cam6A")
        }

        return try {
            val resolver: ContentResolver = context.contentResolver
            val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                        throw IOException("Failed to save bitmap")
                    }
                }
                Log.d(TAG, "Image saved successfully: $uri")
                uri
            }
        } catch (e: IOException) {
            logError("Error saving image: ${e.localizedMessage}")
            null
        }
    }

    /** Saves a recorded video to the gallery */
    fun saveVideoToGallery(videoUri: Uri): Uri? {
        val filename = "$VIDEO_PREFIX${System.currentTimeMillis()}$VIDEO_EXTENSION"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Cam6A")
        }

        return try {
            val resolver: ContentResolver = context.contentResolver
            val newUri: Uri? = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

            newUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    resolver.openInputStream(videoUri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d(TAG, "Video saved successfully: $uri")
                uri
            }
        } catch (e: IOException) {
            logError("Error saving video: ${e.localizedMessage}")
            null
        }
    }

    /** Creates a unique Uri for saving an image */
    fun getImageUri(): Uri {
        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
        val fileName = "$IMAGE_PREFIX$timeStamp$IMAGE_EXTENSION"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Cam6A")
        }

        return context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        ) ?: throw RuntimeException("Failed to create media URI")
    }

    /** Creates a new image file in the app-specific directory or shared media collection */
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
        val fileName = "$IMAGE_PREFIX$timeStamp$IMAGE_EXTENSION"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Cam6A")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
            ) ?: throw RuntimeException("Failed to create media collection entry")

            File(uri.toString())
        } else {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File(storageDir, fileName).apply { parentFile?.mkdirs() }
        }
    }

    /** Creates a new video file in the app-specific directory or shared media collection */
    fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
        val fileName = "$VIDEO_PREFIX$timeStamp$VIDEO_EXTENSION"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Cam6A")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues
            ) ?: throw RuntimeException("Failed to create media collection entry")

            File(uri.toString())
        } else {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            File(storageDir, fileName).apply { parentFile?.mkdirs() }
        }
    }

    /** Notifies the media scanner about a new file */
    fun notifyMediaScanner(file: File) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("video/mp4"),
            null
        )
        Log.d(TAG, "MediaScanner notified: ${file.absolutePath}")
    }

    /** Logs errors for debugging */
    private fun logError(message: String) {
        Log.e(TAG, "FileHelper Error: $message")
    }
}
