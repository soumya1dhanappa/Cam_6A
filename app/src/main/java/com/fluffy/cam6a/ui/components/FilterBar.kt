package com.fluffy.cam6a.ui.components

import android.graphics.Bitmap
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fluffy.cam6a.camera.CameraHelper
import com.fluffy.cam6a.filters.FiltersViewModel
import com.fluffy.cam6a.filters.applyEclipseFilter
import com.fluffy.cam6a.filters.applyGrayscaleFilter
import com.fluffy.cam6a.filters.applySepiaFilter
import com.fluffy.cam6a.utils.FileHelper


@Composable
fun FilterBar(

    filtersViewModel: FiltersViewModel,  // ✅ Pass FiltersViewModel as parameter
    cameraHelper: CameraHelper,

    onBack: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(true) }
    val zoomLevel by filtersViewModel.zoomLevel.observeAsState(1.0f)
    val context = LocalContext.current


    Column(
        modifier = Modifier

            .background(Color.Transparent)
            .padding(16.dp),
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
                verticalArrangement = Arrangement.Top,
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back Icon",
                            tint = Color.White
                        )
                    }

                    if (expanded) {
                        IconButton(onClick = {
                            val newExposure = (filtersViewModel.exposureLevel.value ?: 1.0f) + 0.1f
                            filtersViewModel.adjustExposure(newExposure)
                        }) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Exposure",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = {
                            val newZoom = (zoomLevel + 0.5f).coerceAtMost(5.0f)
                            filtersViewModel.adjustZoom(true)
                            cameraHelper.applyZoom(newZoom)
                        }) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom In",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = {
                            val newZoom = (zoomLevel - 0.5f).coerceAtLeast(1.0f)
                            filtersViewModel.adjustZoom(false)
                            cameraHelper.applyZoom(newZoom)
                        }) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Zoom Out",
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(onClick = { flashOn = !flashOn }) {
                        Icon(
                            imageVector = if (flashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash Icon",
                            tint = if (flashOn) Color.Yellow else Color.White.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown Icon",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                if (expanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Normal", "Grayscale", "Sepia", "Eclipse").forEach { filter ->
                            Button(
                                onClick = {
                                    val filterFunction: (Bitmap) -> Bitmap = when (filter) {
                                        "Normal" -> { bitmap -> bitmap }
                                        "Grayscale" -> ::applyGrayscaleFilter
                                        "Sepia" -> ::applySepiaFilter
                                        "Eclipse" -> ::applyEclipseFilter
                                        else -> { bitmap -> bitmap }
                                    }
                                    filtersViewModel.setFilter(filterFunction, filter)
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), // ✅ Fixed deprecated property
                                modifier = Modifier
                                    .size(70.dp)
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

@Preview
@Composable
fun PreviewFilterBar() {
    val context = LocalContext.current
    val mockFiltersViewModel: FiltersViewModel = viewModel()
    val fileHelper = FileHelper(context)
    val mockCameraHelper = CameraHelper(
        context = context,
        textureView = TextureView(context), // Might cause issues in Compose preview
        fileHelper = fileHelper
    )

    FilterBar(

        filtersViewModel = mockFiltersViewModel,
        cameraHelper = mockCameraHelper,

        onBack = {}
    )
}