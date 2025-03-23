package com.fluffy.cam6a.video

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.fluffy.cam6a.ui.components.CameraPreview
import java.util.Locale

@Composable
fun VideoScreen(navController: NavController, viewModel: VideoViewModel = viewModel()) {
    val context = LocalContext.current.applicationContext as Application

    val recordingSuccess by viewModel.recordingSuccess.observeAsState(initial = false)
    val recentVideos by viewModel.recentVideos.observeAsState(initial = emptyList())
    val isRecording by viewModel.isRecording.observeAsState(initial = false)
    val recordingTime by viewModel.recordingTime.observeAsState(initial = 0)

    // Fetch recent videos when screen loads
    LaunchedEffect(Unit) {
        viewModel.fetchRecentVideos()
    }

    // ActivityResultLauncher for opening gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val videoUri = result.data?.data
            videoUri?.let {
                Toast.makeText(context, "Video Selected: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFA5AFF3))
    ) {
        // Camera Preview Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            CameraPreview(modifier = Modifier.fillMaxSize(), videoViewModel = viewModel)
        }

        // Recent Videos Row (Displays last 5 recorded videos)
        if (recentVideos.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentVideos) { videoUri ->
                    AsyncImage(
                        model = videoUri,
                        contentDescription = "Recent Video",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable { /* Future: Open video fullscreen */ }
                    )
                }
            }
        }

        // Bottom UI (Gallery, Record, Switch Camera)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Button (Opens Recent Videos)
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
                    type = "video/*"
                }
                galleryLauncher.launch(intent)
            }) {
                Icon(imageVector = Icons.Default.Collections, contentDescription = "Gallery", tint = Color.White)
            }

            // Record Button (Toggle Start/Stop Recording)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color.Red else Color.White)
                    .clickable {
                        if (isRecording) {
                            viewModel.stopRecording()
                        } else {
                            viewModel.startRecording()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    Text(
                        text = formatTime(recordingTime),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Record",
                        tint = Color.Black
                    )
                }
            }

            // Switch Camera Button
            IconButton(onClick = { viewModel.switchCamera() }) {
                Icon(imageVector = Icons.Default.SwitchCamera, contentDescription = "Switch Camera", tint = Color.White)
            }
        }
    }

    if (recordingSuccess) {
        Toast.makeText(context, "Video saved to Gallery!", Toast.LENGTH_SHORT).show()
        viewModel.resetRecordingSuccess()
    }
}

/** Formats the recording time (in seconds) into a string (MM:SS) */
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
}