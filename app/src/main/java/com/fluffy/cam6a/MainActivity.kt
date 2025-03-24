package com.fluffy.cam6a

import PhotoScreen
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fluffy.cam6a.ui.SplashScreen
import com.fluffy.cam6a.ui.theme.Cam6ATheme
import com.fluffy.cam6a.video.VideoScreen
import com.fluffy.cam6a.video.VideoViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: VideoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            viewModel.updatePermissionsGranted(allGranted) // Call the ViewModel method
            if (!allGranted) {
                Toast.makeText(this, "Permissions denied. Cannot use camera!", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.setPermissionLauncher(requestPermissionLauncher)

        if (!viewModel.arePermissionsGranted(this)) {
            viewModel.requestPermissions()
        } else {
            viewModel.initializeCameraManager(this)
        }

        setContent {
            Cam6ATheme {
                val navController = rememberNavController()
                var showMainScreen by remember { mutableStateOf(false) }

                if (showMainScreen) {
                    AppNavigation(navController, viewModel)
                } else {
                    SplashScreen { showMainScreen = true }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-initialize the camera when the activity resumes
        viewModel.initializeCameraManager(this)
    }

    override fun onPause() {
        super.onPause()
        // Release camera resources when the activity is paused
        viewModel.releaseAll()
    }
}

@Composable
fun AppNavigation(navController: NavHostController, viewModel: VideoViewModel) {
    NavHost(navController = navController, startDestination = "mainScreen") {
        composable("mainScreen") { MainScreen(navController) }
        composable("photoScreen") { PhotoScreen(navController) }
        composable("videoScreen") { VideoScreen(navController, viewModel) }
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