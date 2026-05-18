@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.nasahacker.convertit.ui.component.expressive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nasahacker.convertit.ui.component.CompactTopAppBarHeight

/**
 * A reusable scaffold for detail screens (About, Pro upsell, etc.).
 *
 * Uses a small pinned [TopAppBar] — short detail screens looked broken with
 * [androidx.compose.material3.LargeTopAppBar] because the collapsing area
 * blended with the dark hero badge underneath, making it look like the bar
 * had swollen to fill half the screen. A pinned bar is unambiguous.
 *
 * The [TopAppBarScrollBehavior] is still passed to [content] so consumers can
 * keep wiring `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`
 * (no-op for pinned, but future-proof).
 */
@Composable
fun ExpressiveScaffold(
    topBarTitle: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    content: @Composable (innerPadding: PaddingValues, scrollBehavior: TopAppBarScrollBehavior) -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = backContentDescription,
                            )
                        }
                    }
                },
                expandedHeight = CompactTopAppBarHeight,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        content(innerPadding, scrollBehavior)
    }
}
