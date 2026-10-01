package de.tou.wastesort.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime

/**
 * Schreibt Studiendaten lokal als CSV auf das Geraet.
 * Keine personenbezogenen Daten (participantId ist ein Pseudonym-Code, kein
 * echter Name), keine Cloud, kein Netzwerk.
 *
 * Dateipfad: /data/data/de.tou.wastesort/files/study_data/session_<id>.csv
 *
 * Enthaltene Spalten (siehe Spezifikation Punkt 16):
 * participant_id, session_id, timestamp, ui_variant, stimulus_id, item_number,
 * ground_truth_category, prototype_response, confidence, unclear_flag,
 * participant_response, response_time (+ trial_id, is_correct als zusaetzliche,
 * aus den Kernfeldern abgeleitete Spalten fuer die Auswertung).
 */
object StudyLogger {

    private const val CSV_HEADER =
        "participant_id,session_id,trial_id,timestamp,ui_variant," +
        "stimulus_id,item_number,item_name_de," +
        "ground_truth_category,prototype_response,confidence,unclear_flag," +
        "participant_response,is_correct,response_time"

    // ── CSV-Datei fuer eine Session anlegen ───────────────────────────────────

    fun getOrCreateCsvFile(context: Context, sessionId: String): File {
        val dir = File(context.filesDir, "study_data").apply { mkdirs() }
        return File(dir, "session_${sessionId.take(8)}.csv").also { file ->
            if (!file.exists()) {
                file.createNewFile()
                file.writeText(CSV_HEADER + "\n")
            }
        }
    }

    // ── Einzelnen Trial anheften ──────────────────────────────────────────────

    fun appendTrial(context: Context, trial: StudyTrial) {
        val file = getOrCreateCsvFile(context, trial.sessionId)
        FileWriter(file, /* append = */ true).use { writer ->
            writer.appendLine(trialToCsvRow(trial))
        }
    }

    // ── Gesamte Session auf einmal schreiben ──────────────────────────────────

    fun writeSession(context: Context, session: StudySession) {
        val file = getOrCreateCsvFile(context, session.sessionId)
        FileWriter(file, false).use { writer ->
            writer.appendLine(CSV_HEADER)
            session.trials.forEach { writer.appendLine(trialToCsvRow(it)) }
        }
    }

    // ── CSV-Zeile aus Trial ───────────────────────────────────────────────────
    //
    // internal statt private, damit JVM-Unit-Tests (app/src/test/...) das
    // exakte CSV-Zeilenformat unabhaengig von Android Context pruefen koennen.

    internal fun trialToCsvRow(t: StudyTrial): String {
        return listOf(
            t.participantId.sanitizeCsv(),
            t.sessionId,
            t.trialId,
            t.timestamp,
            t.interfaceVariant.name,
            t.stimulusId,
            t.itemNumber.toString(),
            t.itemNameDe.sanitizeCsv(),
            t.groundTruthFraction.name,
            t.prototypeFraction.name,
            "%.4f".format(t.confidence),
            t.unclearFlag.toString(),
            t.participantResponse?.name ?: "NOT_ANSWERED",
            t.isCorrect?.toString() ?: "NOT_ANSWERED",
            "%.3f".format(t.responseTimeSeconds)
        ).joinToString(",")
    }

    private fun String.sanitizeCsv(): String =
        this.replace(",", ";").replace("\n", " ").replace("\"", "'")

    // ── URI fuer Android Share-Intent ─────────────────────────────────────────

    fun getCsvShareUri(context: Context, sessionId: String): Uri {
        val file = getOrCreateCsvFile(context, sessionId)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    // ── Intent zum Teilen per E-Mail, Drive etc. ──────────────────────────────

    fun buildShareIntent(context: Context, sessionId: String): Intent {
        val uri = getCsvShareUri(context, sessionId)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "WasteSort Thesis – Session $sessionId")
            putExtra(Intent.EXTRA_TEXT,
                "Studiendaten aus der WasteSort-Thesis-App.\n" +
                "Session: $sessionId\n" +
                "Exportiert: ${LocalDateTime.now()}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // ── Alle CSV-Dateien auflisten ────────────────────────────────────────────

    fun listAllCsvFiles(context: Context): List<File> {
        val dir = File(context.filesDir, "study_data")
        return dir.listFiles { f -> f.extension == "csv" }?.toList() ?: emptyList()
    }

    // ── Alle Sessions loeschen (Datenschutz / Reset) ──────────────────────────

    fun deleteAllData(context: Context) {
        val dir = File(context.filesDir, "study_data")
        dir.listFiles()?.forEach { it.delete() }
    }
}
