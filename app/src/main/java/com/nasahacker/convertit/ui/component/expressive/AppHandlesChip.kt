@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nasahacker.convertit.ui.component.expressive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Compact "handle" chip used on detail screens (About, etc.). Built on
 * [FilledTonalButton] with role-colored container/content for visual variety.
 * Shows a leading icon and a two-line title/description block.
 */
@Composable
fun AppHandlesChip(
    title: String,
    description: String,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit = {},
) {
    FilledTonalButton(
        modifier = modifier,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        onClick = onClick,
    ) {
        when {
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            iconPainter != null -> Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
        }

        if (icon != null || iconPainter != null) {
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmallEmphasized,
                color = contentColor.copy(alpha = 0.85f),
            )
        }
    }
}
