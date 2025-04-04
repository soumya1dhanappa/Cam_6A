package com.fluffy.cam6a

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fluffy.cam6a.ui.theme.Cam6ATheme
import com.fluffy.cam6a.ui.SplashScreen
import com.fluffy.cam6a.utils.PermissionHelper
import com.fluffy.cam6a.photo.PhotoScreen  // FIXED IMPORT
import com.fluffy.cam6a.video.VideoScreen  // FIXED IMPORT
import com.fluffy.cam6a.filters.FiltersViewModel  // Added import for ViewModel
import com.fluffy.cam6a.photo.PhotoViewModel
import com.fluffy.cam6a.photo.PhotoViewModelFactory
import com.fluffy.cam6a.video.VideoViewModel

class MainActivity : ComponentActivity() {
    private lateinit var permissionHelper: PermissionHelper

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Permissions denied. Cannot use camera!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionHelper = PermissionHelper(this)
        permissionHelper.checkAndRequestPermissions(requestPermissionLauncher)

        setContent {
            Cam6ATheme {
                val navController = rememberNavController()
                var showMainScreen by remember { mutableStateOf(false) }

                if (showMainScreen) {
                    AppNavigation(navController, this@MainActivity)
                } else {
                    SplashScreen { showMainScreen = true }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController, context: ComponentActivity) {
    val filtersViewModel: FiltersViewModel = viewModel()
    val videoViewModel: VideoViewModel=viewModel ()
    val photoViewModel: PhotoViewModel = viewModel(
        factory = PhotoViewModelFactory(context.application)
    )

    NavHost(navController = navController, startDestination = "mainScreen") {
        composable("mainScreen") { MainScreen(navController) }
        composable("photoScreen") {
            PhotoScreen(
                navController = navController,
                filtersViewModel = filtersViewModel,
                photoViewModel = photoViewModel,
                onBack = { navController.popBackStack() },
                videoViewModel = VideoViewModel
            )
        }
        composable("videoScreen") {
            val videoViewModel: VideoViewModel = viewModel()
            VideoScreen(navController, videoViewModel)
        }

    }
}

@Composable
fun MainScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Cam6A",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Button(
            onClick = { navController.navigate("photoScreen") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(text = "Capture Photo")
        }

        Button(
            onClick = { navController.navigate("videoScreen") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(text = "Capture Video")
        }
    }
}