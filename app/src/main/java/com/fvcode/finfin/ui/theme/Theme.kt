package com.fvcode.finfin.ui.theme

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
import com.fvcode.finfin.core.datastore.FinfinPreferences

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Slate900,
    surface = Slate800,
    surfaceContainerLowest = Slate800,
    surfaceContainerLow = Slate900,
    outline = Slate700,
    outlineVariant = Slate700,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    // Cores do web: fundo #f1f5f9, cards #FFF, bordas #e2e8f0.
    background = Slate100,
    surface = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    outline = Slate200,
    outlineVariant = Slate200,
    inverseSurface = Marinho,
    inverseOnSurface = Color.White,
)

/**
 * Tema com `claro|escuro|sistema` (`docs/specs/09-componentes.md`, `tema.tsx`).
 * `dynamicColor` desligado por padrão para manter a paleta do web.
 */
@Composable
fun FinfinTheme(
    tema: String = FinfinPreferences.TEMA_SISTEMA,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val sistemaEscuro = isSystemInDarkTheme()
    val darkTheme = when (tema) {
        FinfinPreferences.TEMA_CLARO -> false
        FinfinPreferences.TEMA_ESCURO -> true
        else -> sistemaEscuro
    }
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
