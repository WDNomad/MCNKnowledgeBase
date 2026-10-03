package ru.mcn.knowledgebase.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = McnBlue, onPrimary = Color.White,
    primaryContainer = Color(0xFFE2F0FF), onPrimaryContainer = Color(0xFF104D80),
    secondary = McnTeal, onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF4F1), onSecondaryContainer = Color(0xFF075F60),
    tertiary = McnDeepBlue, onTertiary = Color.White,
    background = Color(0xFFF4F7FB), onBackground = Color(0xFF172C43),
    surface = Color.White, onSurface = Color(0xFF172C43),
    surfaceVariant = Color(0xFFEAF0F7), onSurfaceVariant = Color(0xFF53667A),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F9FC),
    surfaceContainer = Color(0xFFEDF3F9), surfaceContainerHigh = Color(0xFFE6EEF6),
    surfaceContainerHighest = Color(0xFFDEE8F2),
    outline = Color(0xFF73859A), outlineVariant = Color(0xFFDCE5EE),
    surfaceTint = McnBlue
)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF86C8FF), onPrimary = Color(0xFF00365E),
    primaryContainer = Color(0xFF183D5C), onPrimaryContainer = Color(0xFFD3EAFE),
    secondary = Color(0xFF7AD9D0), onSecondary = Color(0xFF003B3B),
    secondaryContainer = Color(0xFF164344), onSecondaryContainer = Color(0xFFB9F2EA),
    tertiary = Color(0xFFB2D4F9), onTertiary = Color(0xFF15374E),
    background = Color(0xFF101B29), onBackground = Color(0xFFE4EDF7),
    surface = Color(0xFF192838), onSurface = Color(0xFFE4EDF7),
    surfaceVariant = Color(0xFF24364A), onSurfaceVariant = Color(0xFFB2C2D4),
    surfaceContainerLowest = Color(0xFF0D1723), surfaceContainerLow = Color(0xFF142130),
    surfaceContainer = Color(0xFF1B2B3E), surfaceContainerHigh = Color(0xFF23354A),
    surfaceContainerHighest = Color(0xFF2E4156),
    outline = Color(0xFF8093A9), outlineVariant = Color(0xFF31455B),
    surfaceTint = Color(0xFF86C8FF)
)
@Composable
fun MCNKnowledgeBaseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
