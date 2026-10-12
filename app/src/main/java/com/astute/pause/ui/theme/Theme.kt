package com.astute.pause.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(
    primary = Color(0xFF3E6B5A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC3E8D7),
    onPrimaryContainer = Color(0xFF002117),
    tertiaryContainer = Color(0xFFFBE3C4),
    onTertiaryContainer = Color(0xFF2B1700),
    background = Color(0xFFFBF8F3),
    surface = Color(0xFFFBF8F3),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA7CCBB),
    onPrimary = Color(0xFF0E3729),
    primaryContainer = Color(0xFF26503F),
    onPrimaryContainer = Color(0xFFC3E8D7),
    tertiaryContainer = Color(0xFF5A4122),
    onTertiaryContainer = Color(0xFFFBE3C4),
)

@Composable
fun PauseTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
