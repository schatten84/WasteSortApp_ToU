package de.tou.wastesort.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tou.wastesort.data.StudySession

// ═══════════════════════════════════════════════════════════════════════════════
// SummaryScreen
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    session:       StudySession,
    onExport:      () -> Unit,
    onNewSession:  () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Ergebnisse der Sitzung") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Gesamtergebnis ─────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(20.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Sitzung abgeschlossen! 🎉", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))

                        Text(
                            text       = "${session.correctCount} / ${session.totalAnswered}",
                            fontSize   = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                        Text("richtig sortiert", fontSize = 16.sp)

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text     = "Trefferquote: ${"%.0f".format(session.accuracyPercent)} %",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text   = "Ø Entscheidungszeit: ${"%.1f".format(session.avgDecisionTimeSeconds)} s",
                            fontSize = 14.sp,
                            color  = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text   = "Interface: ${session.interfaceVariant.label}",
                            fontSize = 13.sp,
                            color  = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.6f)
                        )
                        Text(
                            text   = "Teilnehmer: ${session.participantId}  •  Session: ${session.sessionId.take(8)}",
                            fontSize = 11.sp,
                            color  = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.4f)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Detailergebnisse:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
            }

            // ── Einzelne Trials ────────────────────────────────────────────────
            items(session.trials) { trial ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (trial.isCorrect == true) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (trial.isCorrect == true) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${trial.itemNumber.toString().padStart(2, '0')} – ${trial.itemNameDe}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Ausgewaehlt: ${trial.participantResponse?.shortName ?: "-"}  " +
                                "| Empfehlung: ${trial.prototypeFraction.shortName} (${"%.0f".format(trial.confidence * 100)} %)" +
                                (if (trial.unclearFlag) " ⚠" else "") +
                                "  | ${trial.responseTimeSeconds.toInt()} s",
                                fontSize = 11.sp,
                                color    = MaterialTheme.colorScheme.onSurface.copy(0.55f)
                            )
                        }
                    }
                }
            }

            // ── Buttons ────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick  = onExport,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("CSV exportieren / teilen", fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick  = onNewSession,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Neue Sitzung starten", fontSize = 16.sp)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ExportScreen
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    session:  StudySession,
    onShare:  () -> Intent,
    onBack:   () -> Unit
) {
    val context = LocalContext.current
    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* optional: nothing to handle */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CSV exportieren") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("← ", fontSize = 20.sp)
                    }
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
            verticalArrangement = Arrangement.Center
        ) {
            Text("📊", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text       = "Studiendaten exportieren",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "Session: ${session.sessionId.take(8)}\n${session.totalAnswered} Trials protokolliert",
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurface.copy(0.6f)
            )
            Spacer(Modifier.height(32.dp))

            // CSV-Spalten anzeigen
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("CSV-Spalten:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    listOf(
                        "participant_id", "session_id", "trial_id", "timestamp",
                        "ui_variant", "stimulus_id", "item_number", "item_name_de",
                        "ground_truth_category", "prototype_response", "confidence",
                        "unclear_flag", "participant_response", "is_correct", "response_time"
                    ).forEach {
                        Text("• $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick  = {
                    val intent = onShare()
                    shareLauncher.launch(Intent.createChooser(intent, "CSV teilen via ..."))
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Text("CSV jetzt teilen / exportieren", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text     = "Die Datei wird per E-Mail, Google Drive oder einem anderen App geteilt.\nKeine Daten werden automatisch uebertragen.",
                fontSize  = 12.sp,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurface.copy(0.45f)
            )
        }
    }
}
