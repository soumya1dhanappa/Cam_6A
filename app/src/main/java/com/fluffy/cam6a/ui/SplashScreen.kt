package com.fluffy.cam6a.ui



import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000)
        showSplash = false
        onSplashFinished()
    }

    if (showSplash) {
        SplashScreenContent()
    }
}

@Composable
fun SplashScreenContent() {
    var alpha by remember { mutableStateOf(0f) }
    val animatedAlpha = animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 3000)
    )
    alpha = animatedAlpha.value

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Cam6A",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Blue,
            modifier = Modifier.alpha(alpha)
        )
    }
}
