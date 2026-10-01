package de.tou.wastesort.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tou.wastesort.data.InterfaceVariant
import de.tou.wastesort.data.StimulusResponse

/**
 * Version B – Kindgerechte visuelle Version (ChildResultView), Zielgruppe
 * ca. 6-jaehrige Kinder.
 *
 * Rendert dasselbe StimulusResponse-Objekt wie ResultScreenA, nur anders
 * dargestellt (siehe Spezifikation Punkt 9/17/18). Redundante Codierung
 * (Farbe + Icon + Text + Piktogramm, nicht nur Farbe - Punkt 12).
 * Confidence wird als kindgerechte Sterne-Bewertung (1-5 Sterne) kommuniziert,
 * der zugrunde liegende numerische Wert bleibt erhalten und wird klein
 * mitangezeigt (Punkt 10).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreenB(
    response:        StimulusResponse,
    onVariantChange: (InterfaceVariant) -> Unit,
    onProceed:       () -> Unit
) {
    val fraction = response.prototypeFraction
    val binColor = fraction.toComposeColor()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ergebnis – Version B") },
                actions = {
                    VariantToggle(
                        currentVariant  = InterfaceVariant.VERSION_B,
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
                .fillMaxSize()
                .background((if (response.unclear) Color(0xFF9E9E9E) else binColor).copy(alpha = 0.08f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Item-Name gross und einfach
                Text(
                    text       = response.itemNameDe,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                )

                Spacer(Modifier.height(16.dp))

                if (response.unclear) {
                    // ── UNSICHERHEITS-ZUSTAND (Punkt 11) ─────────────────────────
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9E9E9E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🤔", fontSize = 72.sp)
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text       = "Ich bin nicht sicher.",
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign  = TextAlign.Center,
                        color      = Color(0xFF616161)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text       = "Frag einen Erwachsenen! 🙋",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center,
                        color      = Color(0xFF616161)
                    )
                } else {
                    // ── Grosses Tonnen-Icon (Hauptelement, Punkt 9) ──────────────
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(binColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(fraction.emoji, fontSize = 72.sp)
                    }

                    Spacer(Modifier.height(20.dp))

                    // Kindgerechtes Text-Label (max. 6 Woerter, redundant zu Farbe/Icon)
                    Text(
                        text       = fraction.childLabel,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign  = TextAlign.Center,
                        color      = binColor,
                        lineHeight = 32.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    // Tonnen-Name als zusaetzliches Text-Signal (nicht nur Farbe, Punkt 12)
                    Surface(
                        shape  = RoundedCornerShape(24.dp),
                        color  = binColor,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text       = fraction.shortName,
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            modifier   = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Kindgerechte Confidence-Anzeige (Punkt 10) ───────────────────
                ChildConfidenceIndicator(confidence = response.confidence, unclear = response.unclear, binColor = binColor)
            }

            // Grosser Weiter-Button (kindgerecht)
            Button(
                onClick  = onProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .height(72.dp),
                shape  = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (response.unclear) Color(0xFF757575) else binColor
                )
            ) {
                Text(
                    text       = "Ich hab's! Weiter ➡",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
            }
        }
    }
}

// ── Kindgerechte Confidence-Anzeige ───────────────────────────────────────────
//
// Uebersetzt den intern gleichbleibenden numerischen Confidence-Wert in eine
// von einem 6-jaehrigen Kind verstehbare Aussage: eine Sterne-Bewertung
// (1-5 Sterne) statt einer abstrakten Prozentzahl (Design-Entscheidung
// bestaetigt). Der numerische Wert bleibt zusaetzlich sichtbar (klein), aber
// visuell nachrangig - siehe Spezifikation Punkt 10: "retain the actual
// numeric confidence internally; display the same underlying value in both
// conditions".

@Composable
private fun ChildConfidenceIndicator(confidence: Float, unclear: Boolean, binColor: Color) {
    // Confidence 0.0-1.0 → 1 bis 5 gefuellte Sterne (min. 1, damit nie
    // "0 Sterne" wirkt - der Text darunter macht die Unsicherheit trotzdem klar).
    val filledStars = (confidence * 5f).toInt().coerceIn(1, 5)
    val starColor = if (unclear) Color(0xFF9E9E9E) else binColor

    val caption = when {
        unclear            -> "Ich bin mir nicht sicher."
        confidence >= 0.8f -> "Ich bin mir ziemlich sicher!"
        confidence >= 0.6f -> "Ich glaube, das stimmt."
        else               -> "Ich bin mir nicht sicher."
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text     = "Wie sicher ist die App?",
            fontSize = 13.sp,
            color    = MaterialTheme.colorScheme.onSurface.copy(0.55f)
        )
        Spacer(Modifier.height(4.dp))
        Row {
            repeat(5) { index ->
                Text(
                    text     = if (index < filledStars) "⭐" else "☆",
                    fontSize = 28.sp,
                    color    = starColor
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(caption, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(
            text     = "${(confidence * 100).toInt()} %",
            fontSize = 12.sp,
            color    = MaterialTheme.colorScheme.onSurface.copy(0.45f)
        )
    }
}
