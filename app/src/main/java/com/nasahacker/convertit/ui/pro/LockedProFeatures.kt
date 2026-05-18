@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)

package com.nasahacker.convertit.ui.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nasahacker.convertit.R

/**
 * Shared model + chip used by both [ProScreen] and the in-app
 * [com.nasahacker.convertit.ui.component.ProUpgradeDialog]. Keeping this
 * single source of truth means both surfaces always display the same icon
 * + label + colour pairing for each locked feature.
 */
internal data class LockedFeature(
    val icon: ImageVector,
    val labelRes: Int,
    val container: Color,
    val onContainer: Color,
)

@Composable
internal fun lockedFeatures(): List<LockedFeature> {
    val cs = MaterialTheme.colorScheme
    return listOf(
        LockedFeature(Icons.Filled.Image, R.string.pro_locked_feature_1, cs.primaryContainer, cs.onPrimaryContainer),
        LockedFeature(Icons.Filled.Videocam, R.string.pro_locked_feature_2, cs.tertiaryContainer, cs.onTertiaryContainer),
        LockedFeature(Icons.Filled.HighQuality, R.string.pro_locked_feature_3, cs.secondaryContainer, cs.onSecondaryContainer),
        LockedFeature(Icons.Filled.ContentCut, R.string.pro_locked_feature_4, cs.primaryContainer, cs.onPrimaryContainer),
        LockedFeature(Icons.AutoMirrored.Filled.LibraryBooks, R.string.pro_locked_feature_5, cs.tertiaryContainer, cs.onTertiaryContainer),
        LockedFeature(Icons.Filled.AutoAwesome, R.string.pro_locked_feature_6, cs.secondaryContainer, cs.onSecondaryContainer),
        LockedFeature(Icons.Filled.SurroundSound, R.string.pro_locked_feature_7, cs.primaryContainer, cs.onPrimaryContainer),
        LockedFeature(Icons.Filled.PrivacyTip, R.string.pro_locked_feature_8, cs.tertiaryContainer, cs.onTertiaryContainer),
        LockedFeature(Icons.Filled.VolumeOff, R.string.pro_locked_feature_9, cs.secondaryContainer, cs.onSecondaryContainer),
        LockedFeature(Icons.Filled.Bookmark, R.string.pro_locked_feature_10, cs.primaryContainer, cs.onPrimaryContainer),
        LockedFeature(Icons.Filled.Gif, R.string.pro_locked_feature_11, cs.tertiaryContainer, cs.onTertiaryContainer),
        LockedFeature(Icons.Filled.Compress, R.string.pro_locked_feature_12, cs.secondaryContainer, cs.onSecondaryContainer),
    )
}

/**
 * Single locked-feature tile. Fixed [chipHeight] keeps every tile visually
 * uniform regardless of how many lines its label runs to; longer labels are
 * ellipsised so nothing breaks the grid.
 */
@Composable
internal fun FeatureChip(
    feature: LockedFeature,
    modifier: Modifier = Modifier,
    chipHeight: Dp = 104.dp,
) {
    Surface(
        modifier = modifier.height(chipHeight),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialShapes.Cookie7Sided.toShape())
                    .background(feature.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = feature.onContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = stringResource(feature.labelRes),
                style = MaterialTheme.typography.bodySmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Full grid of locked Pro features for [ProScreen] and [com.nasahacker.convertit.ui.component.ProUpgradeDialog].
 * Always reflects every entry in [lockedFeatures] (including video compression and future items).
 */
@Composable
fun LockedFeaturesGrid(modifier: Modifier = Modifier) {
    val features = lockedFeatures()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.pro_locked_features_title),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2,
        ) {
            features.forEach { feature ->
                FeatureChip(
                    feature = feature,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
