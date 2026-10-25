package com.parcel.presentation

import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.parcel.resources.*
import org.jetbrains.compose.resources.Font

val Orange = Color(0xFFF2A541)
val Teal: Color @Composable get() = MaterialTheme.colorScheme.primary
val Ivory: Color @Composable get() = MaterialTheme.colorScheme.background
val Ink: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val ParcelSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val ParcelOutline: Color @Composable get() = MaterialTheme.colorScheme.outline
val OnAccent: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
internal val LocalParcelDark = staticCompositionLocalOf { true }
val Peach: Color @Composable get() = if (LocalParcelDark.current) Color(0xFF402D24) else Color(0xFFFFDFC9)
val Lavender: Color @Composable get() = if (LocalParcelDark.current) Color(0xFF2A2543) else Color(0xFFE9E0FF)
val Mint: Color @Composable get() = if (LocalParcelDark.current) Color(0xFF18382F) else Color(0xFFDDF3E7)
val Pink: Color @Composable get() = if (LocalParcelDark.current) Color(0xFF3D2635) else Color(0xFFFFDCE7)

@Composable
fun ParcelTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val family = FontFamily(Font(Res.font.albert_sans, FontWeight.Normal))
    fun style(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = family, fontWeight = weight, fontSize = size.sp, lineHeight = (size * 1.4).sp,
    )
    val scheme = if (dark) darkColorScheme(
            primary = Orange, onPrimary = Color(0xFF2A1A00), primaryContainer = Color(0xFF49371F),
            onPrimaryContainer = Color(0xFFFFDDB0), secondary = Color(0xFF7EA6FF),
            onSecondary = Color(0xFF102344), secondaryContainer = Color(0xFF283655),
            onSecondaryContainer = Color(0xFFEEF1F7), background = Color(0xFF0A0F1C), onBackground = Color(0xFFEEF1F7),
            surface = Color(0xFF151C30), onSurface = Color(0xFFEEF1F7), onSurfaceVariant = Color(0xFF9AA3B8),
            surfaceVariant = Color(0xFF1D2641), outline = Color(0xFF64718C),
            outlineVariant = Color(0xFF263050), surfaceDim = Color(0xFF0A0F1C),
            surfaceBright = Color(0xFF303A50), surfaceContainerLowest = Color(0xFF0A0F1C),
            surfaceContainerLow = Color(0xFF151C30), surfaceContainer = Color(0xFF192238),
            surfaceContainerHigh = Color(0xFF202B43), surfaceContainerHighest = Color(0xFF28334D),
            error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        ) else lightColorScheme(
            primary = Color(0xFF14213D), onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE2B6), onPrimaryContainer = Color(0xFF14213D),
            secondary = Color(0xFF2F5B9D), secondaryContainer = Color(0xFFDCE6F2),
            onSecondaryContainer = Color(0xFF14213D), background = Color(0xFFE9EDE7), onBackground = Color(0xFF14213D),
            surface = Color.White, onSurface = Color(0xFF14213D), onSurfaceVariant = Color(0xFF5B6479),
            surfaceVariant = Color(0xFFE2E6E0), outline = Color(0xFF75827F), outlineVariant = Color(0xFFD1D8D1),
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF3F5F1),
            surfaceContainer = Color(0xFFEAEFEA), surfaceContainerHigh = Color(0xFFE2E8E2), surfaceContainerHighest = Color(0xFFDCE3DC),
        )
    PlatformAppearance(dark)
    CompositionLocalProvider(LocalParcelDark provides dark) {
    MaterialTheme(colorScheme = scheme,
        shapes = Shapes(small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(36.dp)),
        typography = Typography(
            headlineLarge = style(32, FontWeight.Bold), headlineMedium = style(28, FontWeight.Bold),
            headlineSmall = style(24, FontWeight.Bold), titleLarge = style(22, FontWeight.Bold),
            titleMedium = style(16, FontWeight.SemiBold), titleSmall = style(14, FontWeight.SemiBold),
            bodyLarge = style(16), bodyMedium = style(14), bodySmall = style(12),
            labelLarge = style(14, FontWeight.SemiBold), labelMedium = style(12, FontWeight.Medium),
            labelSmall = style(11, FontWeight.Medium),
        ), content = content,
    )
}

}
