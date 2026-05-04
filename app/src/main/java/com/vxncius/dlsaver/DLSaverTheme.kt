package com.vxncius.dlsaver

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import androidx.compose.ui.unit.sp

private val DLSaverColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFD0D0D0),
    outline = Color(0xFF5A5A5A)
)

private val googleFontProvider = Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val titleFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Special Gothic Expanded One"),
        fontProvider = googleFontProvider
    )
)

private fun TextStyle.withTitleFont(): TextStyle = copy(fontFamily = titleFontFamily)

private val DLSaverTypography = Typography().run {
    copy(
        displayLarge = displayLarge.withTitleFont(),
        displayMedium = displayMedium.withTitleFont(),
        displaySmall = displaySmall.withTitleFont(),
        headlineLarge = headlineLarge.withTitleFont(),
        headlineMedium = headlineMedium.withTitleFont(),
        headlineSmall = headlineSmall.copy(fontSize = 16.sp, lineHeight = 20.sp).withTitleFont(),
        titleLarge = titleLarge.copy(fontSize = 14.sp, lineHeight = 18.sp).withTitleFont(),
        titleMedium = titleMedium.copy(fontSize = 13.sp, lineHeight = 16.sp).withTitleFont(),
        titleSmall = titleSmall.copy(fontSize = 12.sp, lineHeight = 15.sp).withTitleFont(),
        bodyLarge = bodyLarge.copy(fontSize = 13.sp, lineHeight = 18.sp),
        bodyMedium = bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
        bodySmall = bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
        labelLarge = labelLarge.copy(fontSize = 11.sp, lineHeight = 14.sp),
        labelMedium = labelMedium.copy(fontSize = 10.sp, lineHeight = 12.sp),
        labelSmall = labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp)
    )
}

@Composable
fun DLSaverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DLSaverColors,
        typography = DLSaverTypography,
        content = content
    )
}
