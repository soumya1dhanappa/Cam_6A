package com.fluffy.cam6a.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.fluffy.cam6a.camera.CameraHelper
import com.fluffy.cam6a.filters.FiltersViewModel
import com.fluffy.cam6a.filters.applyEclipseFilter
import com.fluffy.cam6a.filters.applyGrayscaleFilter
import com.fluffy.cam6a.filters.applySepiaFilter

@Composable
fun FilterBar(filtersViewModel: FiltersViewModel, cameraHelper: CameraHelper) {
    var expanded by remember { mutableStateOf(false) }
    val zoomLevel by filtersViewModel.zoomLevel.observeAsState(1.0f)

    Column(
        modifier = Modifier
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { /* Handle Back Navigation */ }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back Icon", tint = Color.White)
                    }

                    if (expanded) {
                        IconButton(onClick = { filtersViewModel.adjustExposure(filtersViewModel.exposureLevel.value + 0.1f) }) {
                            Icon(imageVector = Icons.Default.WbSunny, contentDescription = "Exposure Icon", tint = Color.White)
                        }
                        IconButton(onClick = {
                            filtersViewModel.adjustZoom(true)
                            cameraHelper.applyZoomToCamera(zoomLevel + 0.5f) // ✅ Zoom applied via CameraHelper
                        }) {
                            Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom In Icon", tint = Color.White)
                        }
                        IconButton(onClick = {
                            filtersViewModel.adjustZoom(false)
                            cameraHelper.applyZoomToCamera(zoomLevel - 0.5f) // ✅ Zoom applied via CameraHelper
                        }) {
                            Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Zoom Out Icon", tint = Color.White)
                        }
                    }

                    var flashOn by remember { mutableStateOf(true) }
                    IconButton(onClick = { flashOn = !flashOn }) {
                        Icon(imageVector = if (flashOn) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = "Flash Icon", tint = Color.White.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Dropdown Icon", tint = Color.White.copy(alpha = 0.6f))
                    }
                }

                if (expanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        listOf("Normal", "Grayscale", "Sepia", "Eclipse").forEach { filter ->
                            Button(
                                onClick = {
                                    val selectedFilter = when (filter) {
                                        "Normal" -> { bitmap: Bitmap -> bitmap }
                                        "Grayscale" -> ::applyGrayscaleFilter
                                        "Sepia" -> ::applySepiaFilter
                                        "Eclipse" -> ::applyEclipseFilter
                                        else -> { bitmap: Bitmap -> bitmap }
                                    }
                                    filtersViewModel.setFilter(selectedFilter, filter)
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                                modifier = Modifier
                                    .size(60.dp)
                                    .border(3.dp, Color.White, CircleShape)
                            ) {
                                Text(text = filter, fontSize = 10.sp, color = Color.White, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
fun PreviewFilterScreen() {
    val dummyCameraHelper = CameraHelper(context = LocalContext.current, textureView = TextureView(
        LocalContext.current))
    val dummyViewModel = FiltersViewModel(dummyCameraHelper) // ✅ Pass CameraHelper here
    FilterBar(filtersViewModel = dummyViewModel, cameraHelper = dummyCameraHelper)
}
