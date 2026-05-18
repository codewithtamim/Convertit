package com.nasahacker.convertit.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nasahacker.convertit.domain.model.BottomNavigation

/**
 * Expressive Material 3 bottom navigation.
 *
 * Switched off the legacy `NavigationBar` in favor of the M3 Expressive
 * [ShortNavigationBar], which renders the spec's pill indicator and gives us
 * a tighter container. On top of the stock component we layer the
 * "expressive" motion vocabulary:
 *
 * - **Bouncy icon swap** — [AnimatedContent] crossfades the filled/outlined
 *   pair with a spring `scaleIn` so selection lands with a soft pop instead
 *   of a hard cut.
 * - **Selection pulse** — the active icon eases up to ~1.12× via a low-
 *   stiffness spring, giving the chosen tab a gentle "lift".
 * - **Press feedback** — pressing an item momentarily scales it to 0.88×
 *   so the bar feels tactile.
 * - **Confirm haptic** — selecting a *new* tab fires a `Confirm` haptic; we
 *   intentionally skip it when re-tapping the current tab to avoid noise.
 *
 * Indicator/label colors fall back to the spec defaults
 * ([ShortNavigationBarItemDefaults.colors]), which already use
 * `secondaryContainer` for the indicator — that's the vivid pill the
 * Expressive guidelines call for, so we don't override it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomNavigationBar(navController: NavController) {
    val haptics = LocalHapticFeedback.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavigation.Home,
        BottomNavigation.Library,
    )

    ShortNavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            ExpressiveNavItem(
                isSelected = isSelected,
                label = item.label,
                selectedIcon = when (item) {
                    BottomNavigation.Home -> Icons.Filled.Home
                    BottomNavigation.Library -> Icons.Filled.Inventory2
                },
                unselectedIcon = when (item) {
                    BottomNavigation.Home -> Icons.Outlined.Home
                    BottomNavigation.Library -> Icons.Outlined.Inventory2
                },
                onClick = {
                    if (currentRoute != item.route) {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveNavItem(
    isSelected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
) {
    // Hoist the interaction source so we can drive a press-down scale below
    // *and* still hand it back to the navigation item for ripple/state
    // bookkeeping. Without hoisting we'd lose the tactile feedback.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Selection pulse: 1.0 -> 1.12 with a soft spring so the active tab
    // settles slightly larger than its neighbours.
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "selectionScale",
    )
    // Press feedback rides on top of the selection scale — combined as a
    // multiplier on the icon's scale modifier below.
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "pressScale",
    )

    // Material 3 Expressive haptic: emit a soft tick when the press lands,
    // separate from the Confirm haptic we fire on actual route change.
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }
    }

    ShortNavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        iconPosition = NavigationItemIconPosition.Top,
        interactionSource = interactionSource,
        icon = {
            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    val enter = scaleIn(
                        initialScale = 0.6f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) + fadeIn(animationSpec = tween(220))
                    val exit = scaleOut(
                        targetScale = 0.6f,
                        animationSpec = tween(120),
                    ) + fadeOut(animationSpec = tween(120))
                    enter togetherWith exit
                },
                label = "iconSwap",
            ) { selected ->
                Icon(
                    imageVector = if (selected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    modifier = Modifier
                        .scale(selectionScale * pressScale)
                        .size(24.dp),
                )
            }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMediumEmphasized,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            )
        },
        colors = ShortNavigationBarItemDefaults.colors(),
    )
}
