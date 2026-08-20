package com.nprime.vault.ui.lock

import android.app.WallpaperManager
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import com.nprime.vault.ui.components.PasswordField
import com.nprime.vault.ui.theme.ErrorRed
import com.nprime.vault.ui.theme.Gray400
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun LockScreen(
    state: LockUiState,
    onSubmit: (String) -> Unit
) {
    when {
        state.isBlockingInput -> {
            // Transparent touch sink — blocks all input while file deletion finishes
            Box(modifier = Modifier.fillMaxSize())
        }
        state.isWiping -> {
            DuressLoadingScreen(progress = state.wipeProgress)
        }
        else -> {
            NormalLockScreen(state = state, onSubmit = onSubmit)
        }
    }
}

// ── Duress loading screen ─────────────────────────────────────────────────────

@Composable
private fun DuressLoadingScreen(progress: Float) {
    val context = LocalContext.current

    val wallpaperBitmap: ImageBitmap? = remember {
        runCatching {
            val drawable = WallpaperManager.getInstance(context).drawable
            (drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()
        }.getOrNull()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400),
        label = "wipe_progress"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred background — wallpaper if available, otherwise near-black
        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(24.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0A0A))
            )
        }

        // Dark overlay to dim the background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
        )

        // Center content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PulsingDots()

            // Progress bar + percentage
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    trackColor = Color(0xFF3A3A3A)
                )
            }

            Text(
                text = "Loading....",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun PulsingDots() {
    val transition = rememberInfiniteTransition(label = "dots")

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(4) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.25f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0.25f at 0 using LinearEasing
                        1f    at 250 using LinearEasing
                        0.25f at 600 using LinearEasing
                        0.25f at 1200 using LinearEasing
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * 150)
                ),
                label = "dot_alpha_$index"
            )
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .alpha(alpha)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

// ── Normal lock screen ────────────────────────────────────────────────────────

@Composable
private fun NormalLockScreen(
    state: LockUiState,
    onSubmit: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = LocalDateTime.now()
            nowMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(state.isError) {
        if (state.isError) {
            delay(700)
            password = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                style = MaterialTheme.typography.titleMedium,
                color = Gray400
            )

            Spacer(modifier = Modifier.height(56.dp))

            val isLockedOut = state.lockoutUntil > nowMs
            if (isLockedOut) {
                val seconds = ((state.lockoutUntil - nowMs) / 1000).coerceAtLeast(1)
                Text(
                    text = "Too many attempts — try again in ${seconds}s",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            PasswordField(
                value = password,
                onValueChange = { if (!isLockedOut) password = it },
                onSubmit = { if (!isLockedOut) onSubmit(password) },
                label = "Password",
                isError = state.isError,
                autoFocus = true
            )
        }
    }
}
