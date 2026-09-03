package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SafetyGreenPrimaryDark,
    onPrimary = SafetyGreenOnPrimaryDark,
    primaryContainer = SafetyGreenContainerDark,
    secondary = SafetyGoldSecondary,
    background = SafetyBackgroundDark,
    surface = SafetySurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = SafetyGreenPrimary,
    onPrimary = SafetyGreenOnPrimary,
    primaryContainer = SafetyGreenContainer,
    onPrimaryContainer = SafetyGreenOnContainer,
    secondary = SafetyGoldSecondary,
    onSecondary = SafetyGoldOnSecondary,
    secondaryContainer = SafetyGoldContainer,
    onSecondaryContainer = SafetyGoldOnContainer,
    background = SafetyBackgroundLight,
    surface = SafetySurfaceLight,
    surfaceVariant = SafetySurfaceVariantLight
)

@Composable
fun SafetyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    SafetyTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

