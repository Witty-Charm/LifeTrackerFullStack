package com.lifetracker.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import com.lifetracker.mobile.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage  = "com.google.android.gms",
    certificates     = R.array.com_google_android_gms_fonts_certs
)

val NunitoFont = GoogleFont("Nunito")

val NunitoFontFamily = FontFamily(
    Font(googleFont = NunitoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = NunitoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = NunitoFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = NunitoFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = NunitoFont, fontProvider = provider, weight = FontWeight.ExtraBold)
)

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp
    )
)

