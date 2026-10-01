package de.tou.wastesort.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tou.wastesort.data.InterfaceVariant
import de.tou.wastesort.data.StimulusResponse

/**
 * Version A – Adult-oriented, textbasierte Kontrollversion (AdultResultView).
 *
 * Rendert dasselbe StimulusResponse-Objekt wie ResultScreenB, nur anders
 * dargestellt (siehe Spezifikation Punkt 8/17/18). Enthaelt KEINE eigene
 * Klassifikationslogik.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreenA(
    response:        StimulusResponse,
    onVariantChange: (InterfaceVariant) -> Unit,
    onProceed:       () -> Unit
) {
    val fraction = response.prototypeFraction

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ergebnis – Version A") },
                actions = {
                    VariantToggle(
                        currentVariant  = InterfaceVariant.VERSION_A,
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
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Abfallgegenstand", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                        Spacer(Modifier.height(2.dp))
                        Text(response.itemNameDe, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (response.unclear) {
                    // ── Unsicherheits-Zustand (Punkt 11) ─────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                "Empfehlung mit niedriger Konfidenz",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Konfidenz: ${(response.confidence * 100).toInt()}%",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Bitte überprüfe die Entsorgungskategorie, bevor du den Gegenstand wegwirfst.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(0.85f)
                            )
                        }
                    }
                } else {
                    // ── Regulaere Empfehlung ──────────────────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                "Empfohlene Entsorgung",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.55f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text       = fraction.displayName,
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Konfidenz",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.55f)
                            )
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { response.confidence },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${(response.confidence * 100).toInt()}%",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            response.ambiguityNote?.let { note ->
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text  = note,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.65f)
                                )
                            }
                        }
                    }
                }
            }

            // Weiter-Button
            Button(
                onClick  = onProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Meine Entscheidung treffen", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
