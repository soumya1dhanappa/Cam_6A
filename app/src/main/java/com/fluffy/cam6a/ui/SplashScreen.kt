package com.fluffy.cam6a.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fluffy.cam6a.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(4000)
        showSplash = false
        onSplashFinished()
    }

    if (showSplash) {
        SplashScreenContent()
    }
}

@Composable
fun SplashScreenContent() {
    val letters = listOf("C", "A", "M", "6", "A")
    val shuffledLetters = letters.shuffled() // Start with jumbled order

    val colors = listOf(
        Color.White,  // White
        Color(0xFF4361EE), // Blue
        Color.White,  // White
        Color(0xFF4361EE), // Blue
        Color.White  // White
    )

    val letterDelays = listOf(0L, 300L, 600L, 900L, 1200L)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFA7C7E7),
                        Color(0xFF85A9F6),
                        Color(0xFFC6B7F2)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative Background Shapes
        Image(
            painter = painterResource(id = R.drawable.purple),
            contentDescription = "Purple Shape",
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopStart)
                .offset(x = (-30).dp, y = 15.dp)
                .rotate(-10f)
        )

        Image(
            painter = painterResource(id = R.drawable.darkblue),
            contentDescription = "Dark Blue Shape",
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-50).dp, y = 0.dp)
                .rotate(15f)
        )

        Image(
            painter = painterResource(id = R.drawable.blue),
            contentDescription = "Blue Shape",
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = 60.dp)
                .rotate(-8f)
        )

        Image(
            painter = painterResource(id = R.drawable.purple1),
            contentDescription = "Purple1 Shape",
            modifier = Modifier
                .size(125.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 40.dp, y = 60.dp)
                .rotate(12f)
        )

        Image(
            painter = painterResource(id = R.drawable.blue1),
            contentDescription = "Blue1 Shape",
            modifier = Modifier
                .size(145.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-25).dp, y = (-20).dp)
                .rotate(18f)
        )

        // Animated Jumbled Text Effect
        Row {
            letters.forEachIndexed { index, letter ->
                var displayedLetter by remember { mutableStateOf(shuffledLetters[index]) } // Start jumbled

                val animationProgress = remember { Animatable(-100f) }
                val alpha = remember { Animatable(0f) }

                LaunchedEffect(Unit) {
                    delay(letterDelays[index])
                    animationProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
                    )
                    alpha.animateTo(1f, animationSpec = tween(500))

                    // Rearrange letters to correct order after delay
                    delay(1000)
                    displayedLetter = letter
                }

                Text(
                    text = displayedLetter,
                    fontSize = 55.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors[index],
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(4.dp)
                        .offset(y = animationProgress.value.dp)
                        .alpha(alpha.value)
                )
            }
        }
    }
}
