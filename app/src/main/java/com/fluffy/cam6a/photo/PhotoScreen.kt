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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.SwitchCamera
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
import com.fluffy.cam6a.photo.PhotoViewModel
import com.fluffy.cam6a.photo.PhotoViewModelFactory
import com.fluffy.cam6a.ui.components.CameraPreview
import com.fluffy.cam6a.ui.components.FilterBar
import com.fluffy.cam6a.filters.FiltersViewModel
import com.fluffy.cam6a.camera.CameraHelper
import com.fluffy.cam6a.filters.FiltersViewModelFactory

@Composable
fun PhotoScreen(navController: NavController,cameraHelper: CameraHelper) { // ✅ Pass CameraHelper
    val context = LocalContext.current.applicationContext as Application
    val photoViewModel: PhotoViewModel = viewModel(factory = PhotoViewModelFactory(context))
    val filtersViewModel: FiltersViewModel = viewModel(factory = FiltersViewModelFactory(cameraHelper))


    val captureSuccess by photoViewModel.captureSuccess.observeAsState(initial = false)
    val recentImages by photoViewModel.recentImages.observeAsState(initial = emptyList())



    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                Toast.makeText(context, "Image Selected: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFA5AFF3))
    ) {
        FilterBar(filtersViewModel, cameraHelper ) // ✅ Pass CameraHelper
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(1.dp)
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                photoViewModel = photoViewModel,
                filtersViewModel = filtersViewModel
            )
        }

        RecentImagesRow(recentImages)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                galleryLauncher.launch(intent)
            }) {
                Icon(imageVector = Icons.Default.Collections, contentDescription = "Gallery", tint = Color.White)
            }

            Button(onClick = { photoViewModel.captureImage(filtersViewModel) }, modifier = Modifier.size(72.dp)) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color.Black)
            }

            IconButton(onClick = { photoViewModel.switchCamera() }) {
                Icon(imageVector = Icons.Default.SwitchCamera, contentDescription = "Switch Camera", tint = Color.White)
            }
        }
    }

    if (captureSuccess) {
        Toast.makeText(context, "Photo saved to Gallery!", Toast.LENGTH_SHORT).show()
        photoViewModel.resetCaptureSuccess()
    }
}

@Composable
fun RecentImagesRow(images: List<Uri>) {
    if (images.isNotEmpty()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images) { imageUri ->
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Recent Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable { /* Future: Open image fullscreen */ }
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
        }
    }
}
