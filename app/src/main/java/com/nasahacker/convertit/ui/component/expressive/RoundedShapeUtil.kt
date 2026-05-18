package com.nasahacker.convertit.ui.component.expressive

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Connected rounded-corner shapes for vertical lists. Adjacent items appear
 * "joined": first rounds the top, last rounds the bottom, middles are flat-ish.
 * Mirrors the look used across aShellYou's settings/about screens.
 */
object RoundedShapeUtil {
    val SINGLE = RoundedCornerShape(20.dp)

    val FIRST = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 6.dp,
        bottomEnd = 6.dp,
    )

    val MIDDLE = RoundedCornerShape(
        topStart = 6.dp,
        topEnd = 6.dp,
        bottomStart = 6.dp,
        bottomEnd = 6.dp,
    )

    val LAST = RoundedCornerShape(
        topStart = 6.dp,
        topEnd = 6.dp,
        bottomStart = 20.dp,
        bottomEnd = 20.dp,
    )

    fun getRoundedShape(index: Int, size: Int): RoundedCornerShape = when {
        size <= 1 -> SINGLE
        index == 0 -> FIRST
        index == size - 1 -> LAST
        else -> MIDDLE
    }
}
