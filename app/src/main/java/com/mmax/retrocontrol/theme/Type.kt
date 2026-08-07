@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.mmax.retrocontrol.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.mmax.retrocontrol.R

private val GoogleSansFlexWeights =
  listOf(
    FontWeight.Thin,
    FontWeight.ExtraLight,
    FontWeight.Light,
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
    FontWeight.Black,
  )

private fun googleSansFlexFont(weight: FontWeight, roundness: Float) =
  Font(
    resId = R.font.google_sans_flex_subset,
    weight = weight,
    variationSettings =
      FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("ROND", roundness),
      ),
  )

/** The bundled variable font, mapped across every CSS/Compose weight. */
val GoogleSansFlexFamily =
  FontFamily(
    *GoogleSansFlexWeights.map { weight -> googleSansFlexFont(weight, roundness = 0f) }.toTypedArray()
  )

/** Rounded face for headings, with the same complete weight mapping. */
val RoundedGoogleSansFlexFamily =
  FontFamily(
    *GoogleSansFlexWeights.map { weight -> googleSansFlexFont(weight, roundness = 100f) }.toTypedArray()
  )

private val MaterialTypography = Typography()

val Typography =
  Typography(
    displayLarge = MaterialTypography.displayLarge.copy(fontFamily = RoundedGoogleSansFlexFamily),
    displayMedium = MaterialTypography.displayMedium.copy(fontFamily = RoundedGoogleSansFlexFamily),
    displaySmall = MaterialTypography.displaySmall.copy(fontFamily = RoundedGoogleSansFlexFamily),
    headlineLarge = MaterialTypography.headlineLarge.copy(fontFamily = RoundedGoogleSansFlexFamily),
    headlineMedium = MaterialTypography.headlineMedium.copy(fontFamily = RoundedGoogleSansFlexFamily),
    headlineSmall = MaterialTypography.headlineSmall.copy(fontFamily = RoundedGoogleSansFlexFamily),
    titleLarge = MaterialTypography.titleLarge.copy(fontFamily = RoundedGoogleSansFlexFamily),
    titleMedium = MaterialTypography.titleMedium.copy(fontFamily = RoundedGoogleSansFlexFamily),
    titleSmall = MaterialTypography.titleSmall.copy(fontFamily = RoundedGoogleSansFlexFamily),
    bodyLarge = MaterialTypography.bodyLarge.copy(fontFamily = GoogleSansFlexFamily),
    bodyMedium = MaterialTypography.bodyMedium.copy(fontFamily = GoogleSansFlexFamily),
    bodySmall = MaterialTypography.bodySmall.copy(fontFamily = GoogleSansFlexFamily),
    labelLarge = MaterialTypography.labelLarge.copy(fontFamily = GoogleSansFlexFamily),
    labelMedium = MaterialTypography.labelMedium.copy(fontFamily = GoogleSansFlexFamily),
    labelSmall = MaterialTypography.labelSmall.copy(fontFamily = GoogleSansFlexFamily),
    displayLargeEmphasized = MaterialTypography.displayLargeEmphasized.copy(
      fontFamily = RoundedGoogleSansFlexFamily,
    ),
    displayMediumEmphasized = MaterialTypography.displayMediumEmphasized.copy(
      fontFamily = RoundedGoogleSansFlexFamily,
    ),
    displaySmallEmphasized = MaterialTypography.displaySmallEmphasized.copy(
      fontFamily = RoundedGoogleSansFlexFamily,
    ),
    headlineLargeEmphasized = MaterialTypography.headlineLargeEmphasized.copy(
      fontFamily = RoundedGoogleSansFlexFamily,
    ),
    headlineMediumEmphasized = MaterialTypography.headlineMediumEmphasized.copy(
      fontFamily = RoundedGoogleSansFlexFamily,
    ),
    headlineSmallEmphasized = MaterialTypography.headlineSmallEmphasized.copy(
      fontFamily = RoundedGoogleSansFlexFamily,
    ),
    titleLargeEmphasized = MaterialTypography.titleLargeEmphasized.copy(
      fontFamily = RoundedGoogleSansFlexFamily,
    ),
    titleMediumEmphasized = MaterialTypography.titleMediumEmphasized.copy(
      fontFamily = RoundedGoogleSansFlexFamily,
    ),
    titleSmallEmphasized = MaterialTypography.titleSmallEmphasized.copy(
      fontFamily = RoundedGoogleSansFlexFamily,
    ),
    bodyLargeEmphasized = MaterialTypography.bodyLargeEmphasized.copy(
      fontFamily = GoogleSansFlexFamily,
    ),
    bodyMediumEmphasized = MaterialTypography.bodyMediumEmphasized.copy(
      fontFamily = GoogleSansFlexFamily,
    ),
    bodySmallEmphasized = MaterialTypography.bodySmallEmphasized.copy(
      fontFamily = GoogleSansFlexFamily,
    ),
    labelLargeEmphasized = MaterialTypography.labelLargeEmphasized.copy(
      fontFamily = GoogleSansFlexFamily,
    ),
    labelMediumEmphasized = MaterialTypography.labelMediumEmphasized.copy(
      fontFamily = GoogleSansFlexFamily,
    ),
    labelSmallEmphasized = MaterialTypography.labelSmallEmphasized.copy(
      fontFamily = GoogleSansFlexFamily,
    ),
  )
