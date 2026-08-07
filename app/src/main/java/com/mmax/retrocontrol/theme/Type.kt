@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.mmax.retrocontrol.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface as ComposeTypeface
import androidx.compose.ui.unit.sp
import java.io.File

val SystemGoogleSansFamily = try {
  val fontFile = File("/product/fonts/GoogleSansFlex-Regular.ttf")
  if (fontFile.exists() && fontFile.canRead()) {
    val typeface = Typeface.createFromFile(fontFile)
    FontFamily(ComposeTypeface(typeface))
  } else {
    FontFamily.Default
  }
} catch (_: Exception) {
  FontFamily.Default
}

val GoogleSansHeadlineFamily = try {
  val fontFile = File("/product/fonts/GoogleSansFlex-Regular.ttf")
  if (fontFile.exists()) {
    FontFamily(
      Font(
        file = fontFile,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
          FontVariation.weight(400),
          FontVariation.Setting("opsz", 32f),
        ),
      )
    )
  } else {
    FontFamily.Default
  }
} catch (_: Exception) {
  FontFamily.Default
}

val Typography =
  Typography(
    displayMedium =
      TextStyle(
        fontFamily = GoogleSansHeadlineFamily,
        fontSize = 32.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = SystemGoogleSansFamily,
        fontSize = 22.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = SystemGoogleSansFamily,
        fontSize = 16.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = SystemGoogleSansFamily,
        fontSize = 14.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = SystemGoogleSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      )
  )
