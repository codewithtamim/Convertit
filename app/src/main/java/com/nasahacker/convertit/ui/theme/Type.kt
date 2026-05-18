package com.nasahacker.convertit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.nasahacker.convertit.R

val provider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

// Using "Outfit" for a more expressive, modern look
val displayFontFamily =
    FontFamily(
        Font(
            googleFont = GoogleFont("Outfit"),
            fontProvider = provider,
        ),
    )

val bodyFontFamily =
    FontFamily(
        Font(
            googleFont = GoogleFont("Inter"),
            fontProvider = provider,
        ),
    )

val baseline = Typography()

val AppTypography =
    Typography(
        displayLarge = baseline.displayLarge.copy(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = baseline.displayMedium.copy(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.25).sp
        ),
        displaySmall = baseline.displaySmall.copy(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.SemiBold
        ),
        headlineLarge = baseline.headlineLarge.copy(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.25).sp
        ),
        headlineMedium = baseline.headlineMedium.copy(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.SemiBold
        ),
        headlineSmall = baseline.headlineSmall.copy(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.SemiBold
        ),
        titleLarge = baseline.titleLarge.copy(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.SemiBold
        ),
        titleMedium = baseline.titleMedium.copy(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.Medium
        ),
        titleSmall = baseline.titleSmall.copy(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.Medium
        ),
        bodyLarge = baseline.bodyLarge.copy(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Normal,
            lineHeight = 24.sp
        ),
        bodyMedium = baseline.bodyMedium.copy(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp
        ),
        bodySmall = baseline.bodySmall.copy(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Normal,
            lineHeight = baseline.bodyMedium.lineHeight
        ),
        labelLarge = baseline.labelLarge.copy(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Medium
        ),
        labelMedium = baseline.labelMedium.copy(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Medium
        ),
        labelSmall = baseline.labelSmall.copy(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Medium
        ),
    )
