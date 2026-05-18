@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nasahacker.convertit.ui.component.expressive

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Hero "badge" used on detail screens (e.g. About). Renders a [MaterialShapes]
 * silhouette that slowly rotates and pulses, with a centered icon slot.
 */
@Composable
fun ShapeBadge(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 120.dp,
    iconSize: Dp = 60.dp,
    shape: Shape = MaterialShapes.Cookie9Sided.toShape(),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    icon: @Composable () -> Unit,
) {
    val (angle, scale) = syncedRotationAndScale()
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Only the cookie silhouette behind the icon rotates + pulses; the
        // icon slot stays stationary so its content (e.g. the Pro launcher
        // art) reads cleanly while the outline spins around it.
        Spacer(
            modifier = Modifier
                .requiredSize(badgeSize)
                .graphicsLayer { rotationZ = angle }
                .scale(scale)
                .clip(shape)
                .background(containerColor),
        )
        Box(
            modifier = Modifier.size(iconSize),
            contentAlignment = Alignment.Center,
        ) { icon() }
    }
}

@Composable
fun syncedRotationAndScale(durationMillis: Int = 3000): Pair<Float, Float> {
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            while (true) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(
                        durationMillis = durationMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
        }
        launch {
            var toSmall = true
            while (true) {
                val target = if (toSmall) 0.85f else 1f
                toSmall = !toSmall
                scale.animateTo(
                    targetValue = target,
                    animationSpec = tween(
                        durationMillis = durationMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
        }
    }

    return rotation.value to scale.value
}
