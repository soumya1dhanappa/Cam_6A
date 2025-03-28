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
import com.fluffy.cam6a.filters.FiltersViewModel
import com.fluffy.cam6a.photo.PhotoViewModel
import com.fluffy.cam6a.ui.components.CameraPreview
import com.fluffy.cam6a.ui.components.FilterBar
import java.util.Locale

@Composable
fun VideoScreen(
 navController: NavController,
 videoViewModel: VideoViewModel,
 filtersViewModel: FiltersViewModel = viewModel(),
 onBack: () -> Unit = { navController.popBackStack() }
) {
 val context = LocalContext.current.applicationContext as Application

 val recordingSuccess by videoViewModel.recordingSuccess.observeAsState(initial = false)
 val recentVideos by videoViewModel.recentVideos.observeAsState(initial = emptyList())
 val isRecording by videoViewModel.isRecording.observeAsState(initial = false)
 val recordingTime by videoViewModel.recordingTime.observeAsState(initial = 0)

 val photoViewModel: PhotoViewModel = viewModel()

 // Fetch recent videos when screen loads
 LaunchedEffect(Unit) {
  videoViewModel.fetchRecentVideos()
 }

 // Gallery launcher to select video from gallery
 val galleryLauncher = rememberLauncherForActivityResult(
  contract = ActivityResultContracts.StartActivityForResult()
 ) { result ->
  if (result.resultCode == Activity.RESULT_OK) {
   val videoUri = result.data?.data
   videoUri?.let {
    Toast.makeText(context, "Video Selected: $it", Toast.LENGTH_SHORT).show()
    // Process the selected video if needed
   }
  }
 }

 Column(
  modifier = Modifier
   .fillMaxSize()
   .background(Color(0xFFA5AFF3))
 ) {
  // FilterBar UI Above CameraPreview
  videoViewModel.cameraHelper?.let { cameraHelper ->
   FilterBar(
    filtersViewModel = filtersViewModel,
    cameraHelper = cameraHelper,
    onBack = onBack
   )
  }

  // Camera Preview with applied filter
  Box(
   modifier = Modifier
    .fillMaxWidth()
    .weight(1f)
    .padding(8.dp)
  ) {
   CameraPreview(
    modifier = Modifier.fillMaxSize(),
    photoViewModel = photoViewModel,
    filtersViewModel = filtersViewModel,
    videoViewModel = videoViewModel
   )
  }

  // Recent Videos Row
  RecentVideosRow(recentVideos)

  // Bottom UI (Gallery, Record, Switch Camera)
  Row(
   modifier = Modifier
    .fillMaxWidth()
    .padding(16.dp),
   horizontalArrangement = Arrangement.SpaceAround,
   verticalAlignment = Alignment.CenterVertically
  ) {
   // Gallery Button
   IconButton(onClick = {
    val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
    intent.type = "video/*"
    galleryLauncher.launch(intent)
   }) {
    Icon(imageVector = Icons.Default.Collections, contentDescription = "Gallery", tint = Color.White)
   }

   // Record Button
   Button(
    onClick = {
     if (!isRecording) {
      videoViewModel.startRecording(filtersViewModel)
     } else {
      videoViewModel.stopRecording(filtersViewModel)
     }
    },
    modifier = Modifier.size(80.dp),
    colors = ButtonDefaults.buttonColors(
     containerColor = if (isRecording) Color.Red else Color.White,
     contentColor = if (isRecording) Color.White else Color.Black
    )
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
   IconButton(onClick = { videoViewModel.switchCamera() }) {
    Icon(imageVector = Icons.Default.SwitchCamera, contentDescription = "Switch Camera", tint = Color.White)
   }
  }
 }

 // Show toast when recording is successful
 if (recordingSuccess) {
  Toast.makeText(context, "Video saved to Gallery!", Toast.LENGTH_SHORT).show()
  videoViewModel.resetRecordingSuccess()
 }
}

/** Displays recent videos in a horizontal row */
@Composable
fun RecentVideosRow(videos: List<Uri>) {
 if (videos.isNotEmpty()) {
  LazyRow(
   modifier = Modifier
    .fillMaxWidth()
    .padding(5.dp),
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

/** Formats the recording time (in seconds) into a string (MM:SS) */
private fun formatTime(seconds: Int): String {
 val minutes = seconds / 60
 val secs = seconds % 60
 return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
}