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
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.fluffy.cam6a.ui.components.CameraPreview

@Composable
fun VideoScreen(navController: NavController) {
    val context = LocalContext.current.applicationContext as Application
    val videoViewModel: VideoViewModel = viewModel(factory = VideoViewModelFactory(context))

    val captureSuccess by videoViewModel.captureSuccess.observeAsState(initial = false)
    val recentVideos by videoViewModel.recentVideos.observeAsState(initial = emptyList())

    // Fetch recent videos when screen loads
    LaunchedEffect(Unit) {
        videoViewModel.fetchRecentVideos()
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
                .padding(16.dp)
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(), viewModel = videoViewModel,
                photoViewModel = TODO()
            )
        }

        // Recent Videos Row (Displays last 5 recorded videos)
        RecentVideosRow(recentVideos)

        // Bottom UI (Gallery, Record, Switch Camera)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Gallery Button (Opens Recent Videos)
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                intent.type = "video/*"
                galleryLauncher.launch(intent)
            }) {
                Icon(imageVector = Icons.Default.Collections, contentDescription = "Gallery", tint = Color.White)
            }

            // Record Button
            Button(onClick = { videoViewModel.startVideoRecording() }, modifier = Modifier.size(72.dp)) {
                Icon(imageVector = Icons.Default.Videocam, contentDescription = "Record", tint = Color.Black)
            }

            // Stop Recording Button (Optional)
            Button(onClick = { videoViewModel.stopVideoRecording() }, modifier = Modifier.size(72.dp)) {
                Text("Stop")
            }

            // Switch Camera Button
            IconButton(onClick = { videoViewModel.switchCamera() }) {
                Icon(imageVector = Icons.Default.SwitchCamera, contentDescription = "Switch Camera", tint = Color.White)
            }
        }
    }

    if (captureSuccess) {
        Toast.makeText(context, "Video saved to Gallery!", Toast.LENGTH_SHORT).show()
        videoViewModel.resetCaptureSuccess()
    }
}

/** Displays recent videos in a horizontal row */
@Composable
fun RecentVideosRow(videos: List<Uri>) {
    if (videos.isNotEmpty()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(videos) { videoUri ->
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
}