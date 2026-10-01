package de.tou.wastesort.study

import de.tou.wastesort.data.InterfaceVariant
import de.tou.wastesort.data.StimulusItem
import de.tou.wastesort.data.StudySession
import de.tou.wastesort.data.StudyTrial
import de.tou.wastesort.data.WasteFraction
import java.time.LocalDateTime

/**
 * Verwaltet den Zustand einer laufenden Studiensitzung im QR-Workflow.
 *
 * Es gibt keinen erzwungenen Item-Zaehler mehr - jede der 20 physischen
 * Stimulus-Karten kann in beliebiger Reihenfolge gescannt werden (die
 * Reihenfolge ergibt sich daraus, in welcher Reihenfolge der Testleiter die
 * Karten dem Kind zeigt). Der Manager verfolgt lediglich, welche
 * stimulusIds in dieser Sitzung bereits einen Trial erzeugt haben.
 */
class StudySessionManager(initialVariant: InterfaceVariant, participantId: String) {

    private var _session = StudySession(interfaceVariant = initialVariant, participantId = participantId)
    val session: StudySession get() = _session

    val trialCount: Int get() = _session.trials.size
    val totalItems: Int get() = de.tou.wastesort.data.StimulusRepository.items.size
    val scannedStimulusIds: Set<String> get() = _session.scannedStimulusIds

    // Zeitstempel fuer Entscheidungszeit-Messung
    private var trialStartTime: Long = System.currentTimeMillis()

    /** Wird aufgerufen, sobald ein QR-Code erfolgreich zu einem Item aufgeloest wurde */
    fun startTrial() {
        trialStartTime = System.currentTimeMillis()
    }

    /**
     * Wird aufgerufen, wenn das Kind eine Tonne auswaehlt.
     * isCorrect vergleicht die Kindentscheidung mit der Ground Truth des
     * gescannten Items (NICHT mit der ggf. absichtlich falschen/unsicheren
     * Prototyp-Empfehlung).
     */
    fun recordDecision(
        item: StimulusItem,
        participantResponse: WasteFraction
    ): StudyTrial {
        val responseTime = (System.currentTimeMillis() - trialStartTime) / 1000f
        val isCorrect = participantResponse == item.groundTruthFraction

        val trial = StudyTrial(
            participantId       = _session.participantId,
            sessionId            = _session.sessionId,
            interfaceVariant     = _session.interfaceVariant,
            stimulusId           = item.stimulusId,
            itemNumber           = item.itemNumber,
            itemNameDe           = item.nameDe,
            groundTruthFraction  = item.groundTruthFraction,
            prototypeFraction    = item.prototypeFraction,
            confidence           = item.confidence,
            unclearFlag          = item.unclearFlag,
            participantResponse  = participantResponse,
            isCorrect            = isCorrect,
            responseTimeSeconds  = responseTime,
            timestamp            = LocalDateTime.now().toString()
        )

        _session.trials.add(trial)
        return trial
    }

    /** Interface-Variante waehrend der Session wechseln */
    fun switchVariant(variant: InterfaceVariant) {
        _session = _session.copy(interfaceVariant = variant)
    }

    /** Neue Session starten (alle Daten zuruecksetzen) */
    fun reset(variant: InterfaceVariant, participantId: String) {
        _session = StudySession(interfaceVariant = variant, participantId = participantId)
        trialStartTime = System.currentTimeMillis()
    }
}
