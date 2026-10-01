package de.tou.wastesort.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import de.tou.wastesort.data.InterfaceVariant
import de.tou.wastesort.data.StimulusItem
import de.tou.wastesort.data.StimulusRepository
import de.tou.wastesort.data.StimulusResponse
import de.tou.wastesort.data.StudyLogger
import de.tou.wastesort.data.StudySession
import de.tou.wastesort.data.StudyTrial
import de.tou.wastesort.data.WasteFraction
import de.tou.wastesort.study.StudySessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ─────────────────────────────────────────────────────────────────────────────
//
// STUDY_MODE = "qr_predefined" (siehe StudyConfig in Models.kt):
// Die Klassifikation kommt ausschliesslich aus StimulusRepository (20 fest
// hinterlegte Items), niemals aus einer Live-KI-Anfrage. Der QR-Code ist nur
// ein Schluessel - siehe Spezifikation Punkt 2-4 fuer die Begruendung.
//
// Architektur (Punkt 17-18 der Spezifikation):
//   ScanScreen (QR) → lookupStimulus() → StimulusItem → toResponse()
//     → StimulusResponse (identisch fuer Variante A und B)
//     → ResultScreenA(response) / ResultScreenB(response) rendern dasselbe
//       Objekt nur unterschiedlich. Keine separate Klassifikationslogik pro UI.
//
// ─────────────────────────────────────────────────────────────────────────────

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // ── Interface-Variante (jederzeit umschaltbar) ────────────────────────────

    private val _currentVariant = MutableStateFlow(InterfaceVariant.VERSION_A)
    val currentVariant: StateFlow<InterfaceVariant> = _currentVariant.asStateFlow()

    // ── Teilnehmer-/Session-Verwaltung ────────────────────────────────────────

    private var participantId: String = generateParticipantId()
    private var sessionManager = StudySessionManager(_currentVariant.value, participantId)

    val currentSession: StudySession get() = sessionManager.session
    val trialCount: Int get() = sessionManager.trialCount
    val totalItems: Int get() = sessionManager.totalItems
    val scannedStimulusIds: Set<String> get() = sessionManager.scannedStimulusIds

    // ── Aktuell aufgeloestes Stimulus-Item + geteiltes Antwort-Objekt ──────────

    private val _currentItem = MutableStateFlow<StimulusItem?>(null)
    val currentItem: StateFlow<StimulusItem?> = _currentItem.asStateFlow()

    private val _currentResponse = MutableStateFlow<StimulusResponse?>(null)
    val currentResponse: StateFlow<StimulusResponse?> = _currentResponse.asStateFlow()

    // ── Letzter Trial ─────────────────────────────────────────────────────────

    private val _lastTrial = MutableStateFlow<StudyTrial?>(null)
    val lastTrial: StateFlow<StudyTrial?> = _lastTrial.asStateFlow()

    // ── Aktionen ──────────────────────────────────────────────────────────────

    fun switchVariant(variant: InterfaceVariant) {
        _currentVariant.value = variant
        sessionManager.switchVariant(variant)
    }

    fun setParticipantId(id: String) {
        participantId = id.ifBlank { generateParticipantId() }
    }

    /**
     * Lookup-Funktion fuer einen gescannten QR-Rohwert.
     * Akzeptiert sowohl das aktuelle stimulusId-Format als auch das
     * Legacy-Format der bereits gedruckten Setcards (siehe
     * StimulusRepository.resolveFromQrPayload fuer Details).
     * Fuehrt selbst KEINE Zustandsaenderung durch (rein lesend), damit
     * ScanScreen unabhaengig vom ViewModel-Zustand pruefen kann.
     */
    fun lookupStimulus(qrRawValue: String): StimulusItem? =
        StimulusRepository.resolveFromQrPayload(qrRawValue)

    /** Wird aufgerufen, sobald ein gueltiges Stimulus-Item aufgeloest wurde */
    fun onStimulusFound(item: StimulusItem) {
        _currentItem.value = item
        _currentResponse.value = item.toResponse()
        sessionManager.startTrial()
    }

    /** Entscheidung des Kindes speichern (Ground-Truth-Vergleich, siehe StudySessionManager) */
    fun recordDecision(selectedFraction: WasteFraction) {
        val item = _currentItem.value ?: return
        val trial = sessionManager.recordDecision(item, selectedFraction)
        _lastTrial.value = trial
        StudyLogger.appendTrial(getApplication(), trial)
        _currentItem.value = null
        _currentResponse.value = null
    }

    fun startNewSession() {
        sessionManager.reset(_currentVariant.value, participantId)
        _currentItem.value = null
        _currentResponse.value = null
        _lastTrial.value = null
    }

    fun getShareIntent() = StudyLogger.buildShareIntent(
        getApplication(), currentSession.sessionId
    )

    fun deleteAllData() = StudyLogger.deleteAllData(getApplication())

    private fun generateParticipantId(): String =
        "P-${java.util.UUID.randomUUID().toString().take(6).uppercase()}"
}
