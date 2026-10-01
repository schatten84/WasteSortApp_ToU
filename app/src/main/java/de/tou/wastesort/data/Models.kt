package de.tou.wastesort.data

import androidx.compose.ui.graphics.Color
import java.time.LocalDateTime
import java.util.UUID

// ─── Studien-Modus-Konfiguration ───────────────────────────────────────────────
//
// STUDY_MODE = "qr_predefined": produktiver Studienbetrieb. Klassifikation
// kommt ausschliesslich aus der fest hinterlegten 20-Item-Stimulus-Tabelle
// (StimulusRepository), niemals aus einer Live-KI-Anfrage.
//
// Die echte TrashAI/Roboflow-Anbindung (classifier/RoboflowTrashAiClient.kt)
// bleibt im Projekt erhalten fuer Entwicklung/Dokumentation, wird aber vom
// Studien-Workflow (MainViewModel, ScanScreen) nicht mehr aufgerufen.

object StudyConfig {
    const val STUDY_MODE = "qr_predefined"

    /** Confidence-Schwelle: nur informativ/dokumentarisch (unclearFlag ist pro Item fest hinterlegt) */
    const val CONFIDENCE_THRESHOLD = 0.6f

    /** Praefix-Format der gueltigen QR-Stimulus-IDs dieser Studie */
    const val STIMULUS_ID_PATTERN = "BG-WASTE-%02d"
}

// ─── Interface-Variante ───────────────────────────────────────────────────────

enum class InterfaceVariant(val label: String) {
    VERSION_A("Version A – Text"),
    VERSION_B("Version B – Kindgerecht")
}

// ─── Bergisch Gladbacher Abfallfraktionen ─────────────────────────────────────

enum class WasteFraction(
    val displayName: String,
    val shortName: String,
    val colorHex: Long,
    val emoji: String,
    val childLabel: String          // max. 6 Woerter fuer Version B
) {
    RESTMUELL(
        displayName  = "Restmuelltonne",
        shortName    = "Restmuell",
        colorHex     = 0xFF616161,
        emoji        = "\uD83D\uDDD1\uFE0F",   // 🗑️
        childLabel   = "Geht in die graue Tonne!"
    ),
    PAPIER(
        displayName  = "Papiertonne",
        shortName    = "Papiertonne",
        colorHex     = 0xFF1565C0,
        emoji        = "\uD83D\uDCC4",          // 📄
        childLabel   = "Ab in die blaue Tonne!"
    ),
    BIO(
        displayName  = "Biotonne",
        shortName    = "Biotonne",
        colorHex     = 0xFF4E342E,
        emoji        = "\uD83C\uDF3F",          // 🌿
        childLabel   = "Rein in die braune Tonne!"
    ),
    GELBE_TONNE(
        displayName  = "Gelbe Tonne / Gelber Sack",
        shortName    = "Gelbe Tonne",
        colorHex     = 0xFFF9A825,
        emoji        = "\u267B\uFE0F",          // ♻️
        childLabel   = "Das kommt in die gelbe Tonne!"
    ),
    GLAS_GRUEN(
        displayName  = "Altglascontainer (Gruen)",
        shortName    = "Gruenglas",
        colorHex     = 0xFF2E7D32,
        emoji        = "\uD83C\uDF7E",          // 🍾
        childLabel   = "In den gruenen Glascontainer!"
    ),
    GLAS_BRAUN(
        displayName  = "Altglascontainer (Braun)",
        shortName    = "Braunglas",
        colorHex     = 0xFF6D4C41,
        emoji        = "\uD83C\uDF7A",          // 🍺
        childLabel   = "In den braunen Glascontainer!"
    ),
    GLAS_WEISS(
        displayName  = "Altglascontainer (Weiss)",
        shortName    = "Weissglas",
        colorHex     = 0xFFB0BEC5,
        emoji        = "\uD83E\uDEA9",          // 🫙
        childLabel   = "In den weissen Glascontainer!"
    ),
    // UNCLEAR wird im QR-Studien-Workflow nicht als eigene Fraktion pro Item
    // verwendet (dort gibt es stattdessen StimulusItem.unclearFlag). Bleibt
    // im Enum erhalten, da die dev-mode-only BergischGladbachMapper.kt
    // (Live-TrashAI-Anbindung) diesen Wert weiterhin zurueckgibt.
    UNCLEAR(
        displayName  = "Unklar – bitte Sortierhinweise pruefen",
        shortName    = "Unklar",
        colorHex     = 0xFF9E9E9E,
        emoji        = "\u2753",                // ❓
        childLabel   = "Bitte frag einen Erwachsenen!"
    );

    fun toComposeColor(): Color = Color(colorHex)
}

// ─── Stimulus-Item (zentrales Dataset der 20 fest hinterlegten Studien-Items) ──
//
// WICHTIG: groundTruthFraction und prototypeFraction sind bewusst getrennte
// Felder. Sie sind bei den meisten Items identisch, bei einigen Items jedoch
// ABSICHTLICH unterschiedlich (simulierte Fehlklassifikation) oder mit
// confidence < STUDY_CONFIDENCE_THRESHOLD und unclearFlag = true versehen
// (simulierte Unsicherheit) - siehe StimulusRepository.kt fuer die konkrete
// Zuordnung und Begruendung pro Item.
//
// - groundTruthFraction: die nach Bergisch-Gladbach-Sortierhinweisen TATSAECHLICH
//   korrekte Fraktion. Wird NIE auf dem ResultScreen angezeigt (sonst waere die
//   Aufgabe fuer die Familie trivial) - nur intern fuer isCorrect verwendet.
// - prototypeFraction: die vom (fest hinterlegten) Prototyp EMPFOHLENE Fraktion.
//   Das ist es, was tatsaechlich auf dem ResultScreen erscheint.
// - confidence: fest hinterlegter Confidence-Wert des Prototyps fuer diese Empfehlung.
// - unclearFlag: wenn true, zeigt die App einen Unsicherheits-Hinweis statt
//   einer klaren Empfehlung (siehe ResultScreenA/B).

data class StimulusItem(
    val stimulusId: String,              // z.B. "BG-WASTE-09" (identisch zum QR-Payload)
    val itemNumber: Int,                  // 1..20
    val nameDe: String,
    val nameEn: String,
    val assumedCondition: String,         // z.B. "gebrochen, scharfe Kanten" - kann Sortierung beeinflussen
    val groundTruthFraction: WasteFraction,
    val prototypeFraction: WasteFraction,
    val confidence: Float,
    val unclearFlag: Boolean,
    val ambiguityNote: String? = null
) {
    val isPrototypeCorrect: Boolean get() = prototypeFraction == groundTruthFraction

    fun toResponse(): StimulusResponse = StimulusResponse(
        stimulusId       = stimulusId,
        itemNameDe        = nameDe,
        itemNameEn        = nameEn,
        prototypeFraction = prototypeFraction,
        confidence        = confidence,
        unclear           = unclearFlag,
        ambiguityNote     = ambiguityNote
    )
}

// ─── Geteiltes Antwort-Objekt fuer Variante A und B ────────────────────────────
//
// AdultResultView (ResultScreenA) und ChildResultView (ResultScreenB) rendern
// GENAU dasselbe StimulusResponse-Objekt, nur unterschiedlich dargestellt.
// Es gibt KEINE separate Klassifikationslogik in den beiden UI-Varianten.
// Enthaelt bewusst NICHT die groundTruthFraction - die App zeigt der Familie
// niemals die "richtige Antwort" an, nur die Prototyp-Empfehlung.

data class StimulusResponse(
    val stimulusId: String,
    val itemNameDe: String,
    val itemNameEn: String,
    val prototypeFraction: WasteFraction,
    val confidence: Float,
    val unclear: Boolean,
    val ambiguityNote: String? = null
)

// ─── Studien-Daten ────────────────────────────────────────────────────────────
//
// isCorrect vergleicht die Tonnenwahl des Kindes (participantResponse) mit der
// echten Ground Truth des gescannten Items (StimulusItem.groundTruthFraction),
// NICHT mit der Prototyp-Empfehlung. So bleibt messbar, ob Kinder trotz
// gelegentlich falscher/unsicherer Prototyp-Empfehlungen richtig sortieren.

data class StudyTrial(
    val trialId: String = UUID.randomUUID().toString(),
    val participantId: String,
    val sessionId: String,
    val interfaceVariant: InterfaceVariant,
    val stimulusId: String,
    val itemNumber: Int,
    val itemNameDe: String,
    val groundTruthFraction: WasteFraction,
    val prototypeFraction: WasteFraction,
    val confidence: Float,
    val unclearFlag: Boolean,
    val participantResponse: WasteFraction? = null,
    val isCorrect: Boolean? = null,
    val responseTimeSeconds: Float = 0f,
    val timestamp: String = LocalDateTime.now().toString()
)

data class StudySession(
    val sessionId: String = UUID.randomUUID().toString(),
    val participantId: String = "P-${UUID.randomUUID().toString().take(6).uppercase()}",
    val startTime: String = LocalDateTime.now().toString(),
    val interfaceVariant: InterfaceVariant,
    val trials: MutableList<StudyTrial> = mutableListOf()
) {
    val correctCount: Int
        get() = trials.count { it.isCorrect == true }
    val totalAnswered: Int
        get() = trials.count { it.isCorrect != null }
    val accuracyPercent: Float
        get() = if (totalAnswered == 0) 0f else (correctCount.toFloat() / totalAnswered) * 100f
    val avgDecisionTimeSeconds: Float
        get() = if (trials.isEmpty()) 0f else trials.map { it.responseTimeSeconds }.average().toFloat()
    val scannedStimulusIds: Set<String>
        get() = trials.map { it.stimulusId }.toSet()
}

// ─── Legacy-Modelle (nur fuer die dev-mode-only TrashAI-Live-Anbindung) ────────
//
// Werden weiterhin von classifier/RoboflowTrashAiClient.kt, TrashAiClient.kt,
// MockClassifier.kt und mapping/BergischGladbachMapper.kt verwendet. Diese
// Dateien sind NICHT Teil des aktiven Studien-Workflows (STUDY_MODE =
// "qr_predefined"), bleiben aber fuer Entwicklung/Dokumentation erhalten.

data class TrashAiResult(
    val rawLabel: String,
    val confidence: Float,
    val allLabels: Map<String, Float> = emptyMap()
)

data class MappingResult(
    val fraction: WasteFraction,
    val confidence: Float,
    val rawLabel: String,
    val isUnclear: Boolean,
    val ambiguityNote: String? = null
)
