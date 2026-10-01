package de.tou.wastesort.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tou.wastesort.data.InterfaceVariant

/**
 * StartScreen.
 *
 * Long-Press auf den App-Titel oeffnet den DebugScreen (Testleiter-Verifikation
 * aller 20 Mappings, siehe Spezifikation Punkt 20). Absichtlich unauffaellig,
 * damit Teilnehmer:innen ihn nicht versehentlich finden.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartScreen(
    currentVariant:    InterfaceVariant,
    onVariantChange:   (InterfaceVariant) -> Unit,
    onParticipantIdChange: (String) -> Unit,
    onStart:           () -> Unit,
    onOpenDebug:       () -> Unit
) {
    var participantIdText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Kopfbereich (Long-Press → Debug) ─────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = onOpenDebug
            )
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                text       = "♻️ WasteSort",
                fontSize   = 42.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "Thesis Evaluation App\nTomorrow University of Applied Sciences",
                fontSize  = 14.sp,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }

        // ── Infotext ──────────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Stadt Bergisch Gladbach", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = "Diese App hilft Familien beim richtigen Sortieren von Haushaltsmuell. " +
                            "Es gibt 20 Stimulus-Karten mit QR-Code - scanne den QR-Code jeder Karte, " +
                            "um die Empfehlung zu sehen.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Teilnehmer-Code ───────────────────────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Teilnehmer-Code (optional):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = participantIdText,
                onValueChange = {
                    participantIdText = it
                    onParticipantIdChange(it)
                },
                placeholder = { Text("z.B. F01 (leer = automatisch)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── Interface-Version waehlen ─────────────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Interface-Version:", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            VariantToggle(currentVariant = currentVariant, onVariantChange = onVariantChange)
        }

        // ── Start-Button ──────────────────────────────────────────────────────
        Button(
            onClick  = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Studie starten", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Wiederverwendbarer Version-Toggle ─────────────────────────────────────────

@Composable
fun VariantToggle(
    currentVariant:  InterfaceVariant,
    onVariantChange: (InterfaceVariant) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier            = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        InterfaceVariant.entries.forEach { variant ->
            val selected = variant == currentVariant
            Button(
                onClick  = { onVariantChange(variant) },
                modifier = Modifier.padding(horizontal = 6.dp),
                colors   = if (selected)
                    ButtonDefaults.buttonColors()
                else
                    ButtonDefaults.outlinedButtonColors(),
                shape    = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text       = variant.label,
                    fontSize   = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
