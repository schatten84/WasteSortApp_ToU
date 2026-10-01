package de.tou.wastesort.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tou.wastesort.data.StimulusRepository

/**
 * DebugScreen: Testleiter-/Entwickler-Ansicht, um alle 20 Stimulus-Mappings
 * zu verifizieren, OHNE jede physische Karte einzeln scannen zu muessen
 * (Spezifikation Punkt 20).
 *
 * WICHTIG: Diese Ansicht ist bewusst NICHT ueber die normale
 * Studien-Navigation erreichbar, sondern nur ueber einen versteckten
 * Long-Press auf den App-Titel im StartScreen - siehe StartScreen.kt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Testleiter-Debug – Alle 20 Mappings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurueck")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text     = "STUDY_MODE = qr_predefined – Nur diese Umgebung ist im Studienbetrieb aktiv.",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
                Spacer(Modifier.height(4.dp))
            }

            items(StimulusRepository.items) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (!item.isPrototypeCorrect || item.unclearFlag)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text       = "${item.itemNumber.toString().padStart(2, '0')}",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 15.sp,
                                modifier   = Modifier
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(item.nameDe, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("QR-Payload: ${item.stimulusId}", fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        Text("Zustand: ${item.assumedCondition}", fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Ground Truth: ${item.groundTruthFraction.shortName}   →   Prototyp-Antwort: ${item.prototypeFraction.shortName}",
                            fontSize = 12.sp,
                            fontWeight = if (!item.isPrototypeCorrect) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "Konfidenz: ${(item.confidence * 100).toInt()}%" +
                                    if (item.unclearFlag) "  ⚠ UNCLEAR" else "",
                            fontSize = 12.sp,
                            fontWeight = if (item.unclearFlag) FontWeight.Bold else FontWeight.Normal
                        )
                        if (!item.isPrototypeCorrect) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "⚠ Absichtliche Fehlklassifikation (Ground Truth ≠ Prototyp-Antwort)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        item.ambiguityNote?.let {
                            Spacer(Modifier.height(4.dp))
                            Text("Hinweis: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = "Gesamt: ${StimulusRepository.items.size} Items  •  " +
                            "Fehlklassifikation: ${StimulusRepository.items.count { !it.isPrototypeCorrect }}  •  " +
                            "Unclear: ${StimulusRepository.items.count { it.unclearFlag }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
