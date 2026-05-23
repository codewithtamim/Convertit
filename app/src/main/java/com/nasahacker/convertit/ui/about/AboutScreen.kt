@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
)

package com.nasahacker.convertit.ui.about

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.nasahacker.convertit.BuildConfig
import com.nasahacker.convertit.R
import com.nasahacker.convertit.domain.model.ConvertitDarkPreview
import com.nasahacker.convertit.domain.model.ConvertitLightPreview
import com.nasahacker.convertit.ui.component.CompactTopAppBarHeight
import com.nasahacker.convertit.ui.component.expressive.AppHandlesChip
import com.nasahacker.convertit.ui.component.expressive.ShapeBadge
import com.nasahacker.convertit.ui.component.expressive.SineWaveShape
import com.nasahacker.convertit.ui.component.expressive.WaveEdge
import com.nasahacker.convertit.util.AppConfig
import com.nasahacker.convertit.util.IntentLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val intentLauncher = remember(context) { IntentLauncher(context as Activity) }
    val listState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    // Standard small TopAppBar title — TopAppBar defaults to
                    // titleLarge, which is the right weight for a small bar.
                    // titleLargeEmphasized used to make it feel oversized.
                    Text(text = stringResource(R.string.label_about))
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.label_back),
                            )
                        }
                    }
                },
                expandedHeight = CompactTopAppBarHeight,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = innerPadding,
        ) {
            item { HeroSection() }

            item {
                LinkChips(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .animateItem(),
                    onTelegram = { intentLauncher.openLink(AppConfig.TELEGRAM_CHANNEL) },
                    onDiscord = { intentLauncher.openLink(AppConfig.DISCORD_CHANNEL) },
                    onGitHub = { intentLauncher.openLink(AppConfig.GITHUB_REPO) },
                    onCopyVersion = {
                        copyVersionToClipboard(context, BuildConfig.VERSION_NAME)
                    },
                )
            }

            item {
                DeveloperSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .animateItem(),
                    onDeveloperClick = { intentLauncher.openLink(AppConfig.GITHUB_PROFILE) },
                    onModeratorClick = { intentLauncher.openLink(AppConfig.GITHUB_PROFILE_MOD) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.label_version_text, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun HeroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ShapeBadge(
            badgeSize = 130.dp,
            iconSize = 64.dp,
        ) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp),
            )
        }

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLargeEmphasized.copy(
                letterSpacing = 0.025.em,
            ),
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = stringResource(R.string.label_about_app),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun LinkChips(
    modifier: Modifier = Modifier,
    onTelegram: () -> Unit,
    onDiscord: () -> Unit,
    onGitHub: () -> Unit,
    onCopyVersion: () -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        AppHandlesChip(
            title = stringResource(R.string.label_telegram),
            description = stringResource(R.string.label_about_chip_channel),
            icon = Icons.Filled.Send,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onTelegram,
        )
        AppHandlesChip(
            title = stringResource(R.string.label_discord),
            description = stringResource(R.string.label_about_chip_community),
            icon = Icons.Filled.Forum,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onDiscord,
        )
        AppHandlesChip(
            title = stringResource(R.string.label_github),
            description = stringResource(R.string.label_about_chip_repository),
            iconPainter = painterResource(R.drawable.github_ic),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onGitHub,
        )
        AppHandlesChip(
            title = BuildConfig.VERSION_NAME,
            description = stringResource(R.string.label_about_chip_version),
            icon = Icons.Filled.Tag,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onCopyVersion,
        )
    }
}

/**
 * Copies the app version to the system clipboard.
 *
 * Android 13 (API 33) and newer surface a system-level "copied to
 * clipboard" confirmation, so we suppress our toast on those versions to
 * avoid double feedback.
 */
private fun copyVersionToClipboard(context: Context, version: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Convertit version", version))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast
            .makeText(
                context,
                context.getString(R.string.label_version_copied, version),
                Toast.LENGTH_SHORT,
            ).show()
    }
}

@Composable
private fun DeveloperSection(
    modifier: Modifier = Modifier,
    onDeveloperClick: () -> Unit = {},
    onModeratorClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(
                SineWaveShape(
                    amplitude = 10f,
                    frequency = 5f,
                    edge = WaveEdge.Both,
                ),
            )
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.label_about_developer),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 4.dp),
        )

        DeveloperRow(name = stringResource(R.string.label_dev), onClick = onDeveloperClick)
        DeveloperRow(name = stringResource(R.string.label_mod), onClick = onModeratorClick)
    }
}

@Composable
private fun DeveloperRow(name: String, onClick: () -> Unit = {}) {
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
        )
    }
}

@ConvertitLightPreview
@ConvertitDarkPreview
@Composable
fun PreviewAboutScreen() {
    AboutScreen()
}
