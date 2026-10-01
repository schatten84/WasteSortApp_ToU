package de.tou.wastesort

import de.tou.wastesort.data.InterfaceVariant
import de.tou.wastesort.data.StimulusRepository
import de.tou.wastesort.data.StudyLogger
import de.tou.wastesort.data.WasteFraction
import de.tou.wastesort.study.StudySessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Weitere Unit-Tests: Logging-Zeilenformat + UI-Aequivalenz zwischen
 * Variante A und B + Session-/Ground-Truth-Verhalten.
 */
class StudyLoggerAndSessionTest {

    // ── Logging ──────────────────────────────────────────────────────────────

    @Test
    fun `a successful stimulus interaction creates the expected log row`() {
        val item = StimulusRepository.findByStimulusId("BG-WASTE-09")!! // Apfelrest, Bio, korrekt, sicher
        val manager = StudySessionManager(InterfaceVariant.VERSION_A, "P-TEST01")
        manager.startTrial()
        val trial = manager.recordDecision(item, WasteFraction.BIO)

        val row = StudyLogger.trialToCsvRow(trial)
        val fields = row.split(",")

        assertEquals("P-TEST01", fields[0])                 // participant_id
        assertEquals(trial.sessionId, fields[1])             // session_id
        assertEquals(trial.trialId, fields[2])                // trial_id
        assertEquals(trial.timestamp, fields[3])              // timestamp
        assertEquals("VERSION_A", fields[4])                  // ui_variant
        assertEquals("BG-WASTE-09", fields[5])                // stimulus_id
        assertEquals("9", fields[6])                          // item_number
        assertTrue(fields[7].contains("Apfelrest"))           // item_name_de
        assertEquals("BIO", fields[8])                        // ground_truth_category
        assertEquals("BIO", fields[9])                        // prototype_response
        assertEquals("false", fields[11])                     // unclear_flag
        assertEquals("BIO", fields[12])                       // participant_response
        assertEquals("true", fields[13])                      // is_correct
    }

    @Test
    fun `isCorrect compares against ground truth, not against a possibly wrong prototype response`() {
        // BG-WASTE-04 ist absichtlich falsch klassifiziert: Prototyp sagt
        // Gelbe Tonne, Ground Truth ist Restmuell.
        val item = StimulusRepository.findByStimulusId("BG-WASTE-04")!!
        val manager = StudySessionManager(InterfaceVariant.VERSION_B, "P-TEST02")
        manager.startTrial()

        // Kind waehlt die tatsaechlich richtige Tonne (Restmuell), NICHT die
        // (falsche) Prototyp-Empfehlung (Gelbe Tonne).
        val trial = manager.recordDecision(item, WasteFraction.RESTMUELL)

        assertEquals(WasteFraction.GELBE_TONNE, trial.prototypeFraction)
        assertEquals(WasteFraction.RESTMUELL, trial.groundTruthFraction)
        assertEquals(true, trial.isCorrect) // richtig sortiert trotz falscher KI-Empfehlung
    }

    @Test
    fun `unclear flag on an item produces the correct trial metadata for both variants`() {
        val item = StimulusRepository.findByStimulusId("BG-WASTE-19")!! // unclearFlag = true
        assertTrue(item.unclearFlag)

        listOf(InterfaceVariant.VERSION_A, InterfaceVariant.VERSION_B).forEach { variant ->
            val manager = StudySessionManager(variant, "P-TEST03")
            manager.startTrial()
            val trial = manager.recordDecision(item, item.groundTruthFraction)
            assertTrue(trial.unclearFlag)
            assertEquals(item.confidence, trial.confidence)
        }
    }

    // ── UI-Aequivalenz (A und B erhalten dasselbe Antwort-Objekt) ──────────────

    @Test
    fun `variant A and variant B receive an identical underlying response object`() {
        val item = StimulusRepository.findByStimulusId("BG-WASTE-15")!! // absichtliche Fehlklassifikation
        val responseForA = item.toResponse()
        val responseForB = item.toResponse()

        assertEquals(responseForA, responseForB)
        assertEquals(responseForA.stimulusId, responseForB.stimulusId)
        assertEquals(responseForA.prototypeFraction, responseForB.prototypeFraction)
        assertEquals(responseForA.confidence, responseForB.confidence)
        assertEquals(responseForA.unclear, responseForB.unclear)
    }

    // ── Session-Verhalten ────────────────────────────────────────────────────

    @Test
    fun `switching variant mid-session preserves session and participant id`() {
        val manager = StudySessionManager(InterfaceVariant.VERSION_A, "P-TEST04")
        val sessionIdBefore = manager.session.sessionId
        manager.switchVariant(InterfaceVariant.VERSION_B)
        assertEquals(sessionIdBefore, manager.session.sessionId)
        assertEquals("P-TEST04", manager.session.participantId)
        assertEquals(InterfaceVariant.VERSION_B, manager.session.interfaceVariant)
    }

    @Test
    fun `reset creates a fresh session with a new session id`() {
        val manager = StudySessionManager(InterfaceVariant.VERSION_A, "P-TEST05")
        val item = StimulusRepository.findByStimulusId("BG-WASTE-01")!!
        manager.startTrial()
        manager.recordDecision(item, item.groundTruthFraction)
        assertEquals(1, manager.trialCount)

        val oldSessionId = manager.session.sessionId
        manager.reset(InterfaceVariant.VERSION_A, "P-TEST05")

        assertFalse(manager.session.sessionId == oldSessionId)
        assertEquals(0, manager.trialCount)
    }
}
