package com.nasahacker.convertit.ui.component

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

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nasahacker.convertit.R

@Composable
fun AboutAppContent(context: Context) {
    Text(
        text = context.getString(R.string.label_about_app),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(8.dp),
        textAlign = TextAlign.Center,
    )
}
