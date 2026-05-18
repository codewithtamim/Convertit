@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)

package com.nasahacker.convertit.ui.component

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.nasahacker.convertit.R
import com.nasahacker.convertit.ui.component.expressive.ShapeBadge
import com.nasahacker.convertit.ui.component.expressive.SineWaveShape
import com.nasahacker.convertit.ui.component.expressive.WaveEdge
import com.nasahacker.convertit.ui.pro.LockedFeaturesGrid

/**
 * Convertit Android app
 * Copyright (c) 2025 The Byte Array LTD.
 *
 * Bottom sheet shown when the user taps a locked Pro feature. Mirrors the
 * full [com.nasahacker.convertit.ui.pro.ProScreen] design language so the
 * dialog feels like a compressed Pro screen, not a separate UI:
 *  - spinning gold cookie [ShapeBadge] hero with the Pro launcher art
 *  - emphasized headline + subhead
 *  - "this feature" callout chip naming the gated feature
 *  - 2-column locked-features grid (shared [com.nasahacker.convertit.ui.pro.LockedFeaturesGrid])
 *  - wavy [SineWaveShape] value-prop strip
 *  - emphasized CTA + "Not now" footer
 */
@Composable
fun ProUpgradeDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    featureName: String = "this feature",
) {
    if (!showDialog) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scroll = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroBadge()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.pro_upgrade_dialog_title),
                style = MaterialTheme.typography.headlineMediumEmphasized,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.pro_subheadline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            FeatureCallout(featureName = featureName)

            Spacer(modifier = Modifier.height(24.dp))

            LockedFeaturesGrid()

            Spacer(modifier = Modifier.height(24.dp))

            WavyValuePropStrip()

            Spacer(modifier = Modifier.height(20.dp))

            CallToAction(
                onPrimary = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        context.getString(R.string.premium_play_store_url).toUri(),
                    )
                    context.startActivity(intent)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Hero
// ---------------------------------------------------------------------------

@Composable
private fun HeroBadge() {
    // Same construction as ProScreen.HeroSection but ~20dp smaller so the
    // dialog opens with the badge fully on-screen on shorter phones. Gold
    // is hard-coded — Monet/dynamic colours would dilute the "premium" cue.
    ShapeBadge(
        badgeSize = 132.dp,
        iconSize = 92.dp,
        shape = MaterialShapes.Cookie9Sided.toShape(),
        containerColor = Color(0xFFFFC857),
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(MaterialShapes.Cookie7Sided.toShape()),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pro_launcher),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// "This feature" callout
// ---------------------------------------------------------------------------

@Composable
private fun FeatureCallout(featureName: String) {
    Surface(
        shape = MaterialShapes.Cookie7Sided.toShape(),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = featureName,
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Wavy value-prop strip
// ---------------------------------------------------------------------------

@Composable
private fun WavyValuePropStrip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(SineWaveShape(amplitude = 8f, frequency = 1.5f, edge = WaveEdge.Both))
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pro_deal_text),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// CTA
// ---------------------------------------------------------------------------

@Composable
private fun CallToAction(
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            // ButtonDefaults.shapes() returns the morphing shape pair that
            // makes the button squeeze on press — signature M3 Expressive.
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(R.string.pro_cta_button),
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.pro_price_note),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = stringResource(R.string.label_not_now),
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
