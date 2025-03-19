package com.fluffy.cam6a.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileHelper(private val context: Context) {

    companion object {
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
        private const val IMAGE_PREFIX = "IMG_"
        private const val VIDEO_PREFIX = "VID_"
        private const val IMAGE_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"
    }

    // Create a new image file in the app-specific directory or shared media collection
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
        val fileName = "$IMAGE_PREFIX$timeStamp$IMAGE_EXTENSION"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, use MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Cam6A")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw RuntimeException("Failed to create media collection entry")

            // Creating an empty file object to return
            // The actual content will be written to uri directly in the app
            File(uri.toString())
        } else {
            // For older Android versions
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File(storageDir, fileName).apply {
                parentFile?.mkdirs() // Ensure directory exists
            }
        }
    }

    // Create a new video file in the app-specific directory or shared media collection
    fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
        val fileName = "$VIDEO_PREFIX$timeStamp$VIDEO_EXTENSION"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, use MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Cam6A")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw RuntimeException("Failed to create media collection entry")

            // Creating an empty file object to return
            File(uri.toString())
        } else {
            // For older Android versions
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            File(storageDir, fileName).apply {
                parentFile?.mkdirs() // Ensure directory exists
            }
        }
    }

    // Get unique Uri for saving media content
    fun getImageUri(): Uri {
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
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw RuntimeException("Failed to create media uri")
    }

    // Get unique Uri for saving video content
    fun getVideoUri(): Uri {
        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
        val fileName = "$VIDEO_PREFIX$timeStamp$VIDEO_EXTENSION"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Cam6A")
            }
        }

        return context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw RuntimeException("Failed to create media uri")
    }
}