package de.tou.wastesort.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tou.wastesort.data.InterfaceVariant
import de.tou.wastesort.data.WasteFraction

/**
 * BinSelectionScreen: Das Kind waehlt die Tonne, in die es das Item wirft.
 * Version A: Textliste mit Tonsymbolen.
 * Version B: Grosses, farbiges Grid mit Emojis.
 *
 * Die Auswahl loest die Datenerfassung (recordDecision) aus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinSelectionScreen(
    currentVariant:     InterfaceVariant,
    onVariantChange:    (InterfaceVariant) -> Unit,
    onFractionSelected: (WasteFraction) -> Unit
) {
    // Nur die 5 Hauptfraktionen (UNCLEAR ist keine waehlbare Antwort)
    val selectableFractions = listOf(
        WasteFraction.RESTMUELL,
        WasteFraction.PAPIER,
        WasteFraction.BIO,
        WasteFraction.GELBE_TONNE,
        WasteFraction.GLAS_GRUEN,
        WasteFraction.GLAS_BRAUN,
        WasteFraction.GLAS_WEISS
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("In welche Tonne?") },
                actions = {
                    VariantToggle(
                        currentVariant  = currentVariant,
                        onVariantChange = onVariantChange,
                        modifier        = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentVariant == InterfaceVariant.VERSION_B) {
                // ── VERSION B: Grosses visuelles Grid ────────────────────────
                Text(
                    text       = "Wo kommt das hin? 🤔",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectableFractions) { fraction ->
                        BinTileVersionB(
                            fraction  = fraction,
                            onClick   = { onFractionSelected(fraction) }
                        )
                    }
                }
            } else {
                // ── VERSION A: Textliste ──────────────────────────────────────
                Text(
                    text       = "Bitte waehlen Sie die korrekte Abfallfraktion:",
                    fontSize   = 16.sp,
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                selectableFractions.forEach { fraction ->
                    BinRowVersionA(
                        fraction = fraction,
                        onClick  = { onFractionSelected(fraction) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── Version B: Grosse farbige Kachel ──────────────────────────────────────────

@Composable
private fun BinTileVersionB(fraction: WasteFraction, onClick: () -> Unit) {
    val binColor = fraction.toComposeColor()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(binColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(fraction.emoji, fontSize = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text       = fraction.shortName,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// ── Version A: Einfache Text-Zeile ────────────────────────────────────────────

@Composable
private fun BinRowVersionA(fraction: WasteFraction, onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = fraction.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                text     = fraction.displayName,
                fontSize = 15.sp,
                color    = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
