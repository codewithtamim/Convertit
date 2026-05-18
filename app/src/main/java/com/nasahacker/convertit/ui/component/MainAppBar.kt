@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.nasahacker.convertit.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nasahacker.convertit.R
import kotlinx.coroutines.launch

/**
 * Convertit Android app
 *
 * Created by Tamim Hossain.
 * Copyright (c) 2025 The Byte Array LTD.
 *
 * @author Tamim Hossain
 * @company The Byte Array LTD
 * @year 2025
 */

@Composable
fun MainAppBar(
    onNavigateToPro: () -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
        },
        actions = {
            PremiumCrownBadge(
                onClick = onNavigateToPro,
                contentDescription = stringResource(R.string.pro_screen_appbar_title),
            )
            IconButton(onClick = onNavigateToAbout) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.label_about),
                )
            }
        },
        // Trim the default 64dp container to 48dp so the bar hugs the status
        // bar inset instead of leaving a chunky empty band above the title.
        expandedHeight = CompactTopAppBarHeight,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

internal val CompactTopAppBarHeight: Dp = 48.dp

/**
 * Animated "premium" badge that replaces the old Lottie crown asset.
 *
 * Layered look:
 *  - A [MaterialShapes.Cookie9Sided] silhouette filled with a warm gold,
 *    slowly rotating + pulsing (mirrors `ShapeBadge` so it feels native to
 *    the rest of the expressive palette).
 *  - A [Icons.Filled.WorkspacePremium] crown icon centered on top in a
 *    deep amber, kept stationary so the symbol reads cleanly while the
 *    badge spins behind it.
 */
@Composable
private fun PremiumCrownBadge(
    onClick: () -> Unit,
    contentDescription: String?,
) {
    // Gold pair tuned to read on both light and dark surfaces. We hard-code
    // these instead of pulling from the colorScheme because "premium = gold"
    // is the whole point of the badge — Monet/dynamic colors would dilute
    // the cue.
    val goldContainer = Color(0xFFFFC857)
    val goldOnContainer = Color(0xFF4A3700)

    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        // Continuous rotation — full revolution every 4s with eased motion.
        launch {
            while (true) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
                )
            }
        }
        // Soft heartbeat — 0.92 <-> 1.0 every 1.4s, ping-ponged.
        launch {
            var down = true
            while (true) {
                val target = if (down) 0.92f else 1f
                down = !down
                scale.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer { rotationZ = rotation.value }
                .scale(scale.value)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(goldContainer),
        )
        Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = contentDescription,
            tint = goldOnContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}
