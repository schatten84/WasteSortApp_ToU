package de.tou.wastesort.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Basis-Farben ──────────────────────────────────────────────────────────────
val PrimaryGreen   = Color(0xFF2E7D32)
val OnPrimary      = Color.White
val SecondaryBlue  = Color(0xFF1565C0)
val Background     = Color(0xFFF5F5F5)
val Surface        = Color.White
val OnBackground   = Color(0xFF212121)

// ── Tonnen-Farben (Bergisch Gladbach) ─────────────────────────────────────────
val BinGrey        = Color(0xFF616161)   // Restmuell
val BinBlue        = Color(0xFF1565C0)   // Papiertonne
val BinBrown       = Color(0xFF4E342E)   // Biotonne
val BinYellow      = Color(0xFFF9A825)   // Gelbe Tonne
val BinGreenGlass  = Color(0xFF2E7D32)   // Gruenglas
val BinBrownGlass  = Color(0xFF6D4C41)   // Braunglas
val BinWhiteGlass  = Color(0xFF90A4AE)   // Weissglas
val BinUnclear     = Color(0xFF9E9E9E)   // Unklar

private val LightColors = lightColorScheme(
    primary         = PrimaryGreen,
    onPrimary       = OnPrimary,
    secondary       = SecondaryBlue,
    background      = Background,
    surface         = Surface,
    onBackground    = OnBackground,
    onSurface       = OnBackground
)

@Composable
fun WasteSortTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content     = content
    )
}
