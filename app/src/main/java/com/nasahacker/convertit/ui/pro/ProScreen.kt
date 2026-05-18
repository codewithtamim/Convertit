@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
)

package com.nasahacker.convertit.ui.pro

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.nasahacker.convertit.R
import com.nasahacker.convertit.ui.component.expressive.ExpressiveScaffold
import com.nasahacker.convertit.ui.component.expressive.ShapeBadge
import com.nasahacker.convertit.ui.component.expressive.SineWaveShape
import com.nasahacker.convertit.ui.component.expressive.WaveEdge

/**
 * Material 3 Expressive upsell screen for the FOSS app.
 *
 * Sections (top -> bottom):
 *  1. Hero — animated [ShapeBadge] holding the Pro app launcher icon.
 *  2. Headline + subhead.
 *  3. Locked features — 2-column grid of icon-led "feature chips" backed
 *     by [MaterialShapes] cookie shapes.
 *  4. Wavy value-prop strip.
 *  5. Emphasized CTA.
 */
@Composable
fun ProScreen(onNavigateBack: (() -> Unit)? = null) {
    ExpressiveScaffold(
        topBarTitle = stringResource(R.string.pro_screen_appbar_title),
        onNavigateBack = onNavigateBack,
        backContentDescription = stringResource(R.string.label_back),
    ) { innerPadding, scrollBehavior ->
        val scroll = rememberScrollState()
        // Keep the gesture-nav inset so the CTA button never sits under the
        // home-indicator bar.
        val bottomBarInset = WindowInsets.systemBars
            .asPaddingValues()
            .calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
                .padding(bottom = bottomBarInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroSection()

            Spacer(modifier = Modifier.height(28.dp))

            LockedFeaturesGrid()

            Spacer(modifier = Modifier.height(28.dp))

            WavyValuePropStrip()

            Spacer(modifier = Modifier.height(24.dp))

            CallToAction()

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Hero
// ---------------------------------------------------------------------------

@Composable
private fun HeroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // A larger gold cookie silhouette spins + pulses behind a smaller
        // static Pro launcher icon — only the outline moves, the icon
        // content inside stays put. We use a hard-coded warm gold instead
        // of `primaryContainer` so the "premium" cue isn't washed out by
        // Monet/dynamic colors.
        ShapeBadge(
            badgeSize = 156.dp,
            iconSize = 108.dp,
            shape = MaterialShapes.Cookie9Sided.toShape(),
            containerColor = Color(0xFFFFC857),
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
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

        // Honest, conversational headline — replaces the giant duplicate of
        // the app-bar title we used to render here.
        Text(
            text = stringResource(R.string.pro_headline),
            style = MaterialTheme.typography.headlineMediumEmphasized,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.pro_subheadline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Wavy value-prop strip
// ---------------------------------------------------------------------------

@Composable
private fun WavyValuePropStrip() {
    // A short colored strip clipped to a sine-wave silhouette — pulls the
    // eye toward the CTA and adds expressive motion without animation cost.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(SineWaveShape(amplitude = 8f, frequency = 1.5f, edge = WaveEdge.Both))
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pro_deal_text),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// CTA
// ---------------------------------------------------------------------------

@Composable
private fun CallToAction() {
    val context = LocalContext.current
    val playUrl = stringResource(R.string.premium_play_store_url)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, playUrl.toUri())
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            // ButtonDefaults.shapes() returns the morphing shape pair that
            // makes the button squeeze on press — a signature M3 Expressive
            // touch.
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.pro_price_note),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
