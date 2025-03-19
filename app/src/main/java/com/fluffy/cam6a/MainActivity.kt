package com.fluffy.cam6a

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
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
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.fluffy.cam6a.ui.theme.Cam6ATheme
import com.fluffy.cam6a.ui.SplashScreen
import com.fluffy.cam6a.photo.PhotoScreen
import com.fluffy.cam6a.video.VideoScreen
import com.fluffy.cam6a.utils.PermissionHelper
import com.fluffy.cam6a.utils.FileHelper

class MainActivity : ComponentActivity() {
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var fileHelper: FileHelper

    // Request multiple permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize helpers
        permissionHelper = PermissionHelper(this)
        fileHelper = FileHelper(this)

        // Check permissions at startup
        permissionHelper.checkAndRequestPermissions(requestPermissionLauncher)

        setContent {
            Cam6ATheme {
                val navController = rememberNavController()
                var showMainScreen by remember { mutableStateOf(false) }

                if (showMainScreen) {
                    AppNavigation(navController)
                } else {
                    SplashScreen { showMainScreen = true }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "mainScreen") {
        composable("mainScreen") { MainScreen(navController) }
        composable("photoScreen") { PhotoScreen() }
        composable("videoScreen") { VideoScreen() }
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
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(text = "Capture Photo")
        }

        Button(
            onClick = { navController.navigate("videoScreen") },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(text = "Capture Video")
        }
    }
}