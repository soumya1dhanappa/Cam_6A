package com.fluffy.cam6a

import PhotoScreen
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bumptech.glide.Glide
import com.fluffy.cam6a.ui.theme.Cam6ATheme
import com.fluffy.cam6a.ui.SplashScreen
import com.fluffy.cam6a.utils.FileHelper
import com.fluffy.cam6a.utils.PermissionHelper
import com.fluffy.cam6a.video.VideoScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        composable("photoScreen") { PhotoScreen(navController) }
        composable("videoScreen") { VideoScreen(navController) }
    }
}

@Composable
fun MainScreen(navController: NavController) {
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF87CEEB),
            Color(0xFF9370DB)
        )
    )

    val imageList = listOf(
        R.drawable.bu_dinosaur,
        R.drawable.ferrari_img,
        R.drawable.hplaptop_img,
        R.drawable.hpprinter_img
    )

    var currentImageIndex by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = true) {
        coroutineScope.launch {
            while (true) {
                delay(2000)
                currentImageIndex = (currentImageIndex + 1) % imageList.size
            }
        }
    }

    var isPhotoMode by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Photo",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(0.8f)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray.copy(alpha = 0.2f))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable {
                        isPhotoMode = true
                        navController.navigate("photoScreen") {
                            popUpTo("mainScreen") { inclusive = true }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageList[currentImageIndex]),
                    contentDescription = "Sliding image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(66.dp))

            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .width(328.dp)
                    .height(82.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(74.dp)
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hp_logo),
                        contentDescription = "HP Logo",
                        modifier = Modifier
                            .size(74.dp) // Set explicit size
                            .padding(8.dp)
                    )
                }


                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                        .size(60.dp)
                        .background(Color.White)

                        .padding(2.dp)
                        .clickable {
                            isPhotoMode = true
                            navController.navigate("photoScreen") {
                                popUpTo("mainScreen") { inclusive = true }
                            }
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.toggle_camera),
                        contentDescription = "Camera",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .size(60.dp)
                        .background(
                            if (!isPhotoMode) Color(0xFF0096D6).copy(alpha = 0.2f) else Color.Transparent,
                            CircleShape
                        )
                        .padding(2.dp)
                        .clickable {
                            isPhotoMode = false
                            navController.navigate("videoScreen") {
                                popUpTo("mainScreen") { inclusive = true }
                            }
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.toggle_video),
                        contentDescription = "Video",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(66.dp))

            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(0.8f)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray.copy(alpha = 0.2f))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable {
                        isPhotoMode = false
                        navController.navigate("videoScreen") {
                            popUpTo("mainScreen") { inclusive = true }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            Glide.with(context)
                                .asGif()
                                .load(R.drawable.dino_rungif)
                                .into(this)
                        }
                    }
                )

            }

            Text(
                text = "Video",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
