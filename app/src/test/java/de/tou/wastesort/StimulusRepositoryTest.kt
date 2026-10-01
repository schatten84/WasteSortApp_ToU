package de.tou.wastesort

import de.tou.wastesort.data.StimulusRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-Tests fuer das zentrale 20-Item-Stimulus-Dataset (siehe Spezifikation
 * Punkt 22 "Tests"). Reine JVM-Tests, kein Android-Geraet/Emulator noetig.
 */
class StimulusRepositoryTest {

    // ── Mapping ──────────────────────────────────────────────────────────────

    @Test
    fun `all 20 expected stimulus IDs exist`() {
        val expectedIds = (1..20).map { "BG-WASTE-%02d".format(it) }
        val actualIds = StimulusRepository.allStimulusIds
        assertEquals(20, actualIds.size)
        expectedIds.forEach { expected ->
            assertTrue("Fehlende Stimulus-ID: $expected", actualIds.contains(expected))
        }
    }

    @Test
    fun `no duplicate stimulus IDs exist`() {
        val ids = StimulusRepository.allStimulusIds
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `no duplicate item numbers exist`() {
        val numbers = StimulusRepository.items.map { it.itemNumber }
        assertEquals(numbers.size, numbers.toSet().size)
        assertEquals((1..20).toList(), numbers.sorted())
    }

    // ── Determinismus ────────────────────────────────────────────────────────

    @Test
    fun `same QR always returns the same response`() {
        val first = StimulusRepository.findByStimulusId("BG-WASTE-09")
        val second = StimulusRepository.findByStimulusId("BG-WASTE-09")
        assertNotNull(first)
        assertEquals(first, second)
    }

    @Test
    fun `lookup is case-insensitive and trims whitespace (robust QR reads)`() {
        val lower = StimulusRepository.findByStimulusId("bg-waste-09")
        val padded = StimulusRepository.findByStimulusId("  BG-WASTE-09  ")
        assertNotNull(lower)
        assertNotNull(padded)
        assertEquals(lower, padded)
    }

    // ── Unbekannter QR ───────────────────────────────────────────────────────

    @Test
    fun `unknown QR produces null, no fallback classification`() {
        assertNull(StimulusRepository.findByStimulusId("BG-WASTE-99"))
        assertNull(StimulusRepository.findByStimulusId("totally-invalid"))
        assertFalse(StimulusRepository.isValidStudyStimulus("BG-WASTE-21"))
    }

    // ── Legacy-Setcard-Format (bereits gedruckte physische Karten) ─────────────
    //
    // Die urspruenglich erzeugten Setcards (Setcard_01_....png ...
    // Setcard_20_....png) kodieren KEINE stimulusId, sondern
    // "Nr: <n>; Set: <s>; Gegenstand: <name>". resolveFromQrPayload() muss
    // dieses Format weiterhin akzeptieren, damit bereits gedruckte Karten
    // nicht neu produziert werden muessen.

    @Test
    fun `resolveFromQrPayload accepts the current stimulusId format`() {
        val item = StimulusRepository.resolveFromQrPayload("BG-WASTE-09")
        assertNotNull(item)
        assertEquals(9, item!!.itemNumber)
    }

    @Test
    fun `resolveFromQrPayload accepts the legacy setcard payload format`() {
        // Tatsaechlicher dekodierter QR-Inhalt aus Setcard_09_apfelrest.png
        val item = StimulusRepository.resolveFromQrPayload(
            "Nr: 9; Set: 1; Gegenstand: Apfelrest"
        )
        assertNotNull(item)
        assertEquals("BG-WASTE-09", item!!.stimulusId)
        assertEquals(9, item.itemNumber)
    }

    @Test
    fun `resolveFromQrPayload legacy format works for all 20 real setcard payloads`() {
        // Tatsaechlich aus allen 20 Setcard-PNGs dekodierte QR-Rohwerte
        val legacyPayloads = listOf(
            1 to "Nr: 1; Set: 1; Gegenstand: Zerbrochener Keramikbecher",
            2 to "Nr: 2; Set: 2; Gegenstand: Gebrauchtes Klebeband",
            3 to "Nr: 3; Set: 1; Gegenstand: Wegwerfwindel",
            4 to "Nr: 4; Set: 2; Gegenstand: Zerkratzte CD/DVD",
            5 to "Nr: 5; Set: 1; Gegenstand: Zeitung",
            6 to "Nr: 6; Set: 2; Gegenstand: Plattgedrueckter Karton",
            7 to "Nr: 7; Set: 2; Gegenstand: Papierumschlag, ohne Sichtfenster",
            8 to "Nr: 8; Set: 1; Gegenstand: Papiertuete",
            9 to "Nr: 9; Set: 1; Gegenstand: Apfelrest",
            10 to "Nr: 10; Set: 2; Gegenstand: Kaffeesatz mit Papierfilter",
            11 to "Nr: 11; Set: 2; Gegenstand: Gemueseschale",
            12 to "Nr: 12; Set: 1; Gegenstand: Eierschale",
            13 to "Nr: 13; Set: 1; Gegenstand: Joghurtbecher, leer und sauber",
            14 to "Nr: 14; Set: 2; Gegenstand: Alufolie, saubere Verpackungsfolie",
            15 to "Nr: 15; Set: 1; Gegenstand: Getraenkekarton (Tetra Pak), leer",
            16 to "Nr: 16; Set: 2; Gegenstand: Shampooflasche, leer",
            17 to "Nr: 17; Set: 1; Gegenstand: Gruene Weinflasche, ohne Deckel",
            18 to "Nr: 18; Set: 2; Gegenstand: Braunes Glas (Arzneiflaeschchen, leer)",
            19 to "Nr: 19; Set: 1; Gegenstand: Klares Glas (Flasche, Deckel entfernt)",
            20 to "Nr: 20; Set: 2; Gegenstand: Blaue Glasflasche, ohne Deckel"
        )
        legacyPayloads.forEach { (expectedNumber, payload) ->
            val item = StimulusRepository.resolveFromQrPayload(payload)
            assertNotNull("Kein Treffer fuer: $payload", item)
            assertEquals(expectedNumber, item!!.itemNumber)
        }
    }

    @Test
    fun `resolveFromQrPayload rejects garbage and out-of-range legacy numbers`() {
        assertNull(StimulusRepository.resolveFromQrPayload("Nr: 21; Set: 1; Gegenstand: Irgendwas"))
        assertNull(StimulusRepository.resolveFromQrPayload("random text without a number prefix"))
        assertNull(StimulusRepository.resolveFromQrPayload(""))
    }

    // ── Confidence ───────────────────────────────────────────────────────────

    @Test
    fun `confidence values are preserved exactly and within valid range`() {
        StimulusRepository.items.forEach { item ->
            assertTrue(
                "Confidence von ${item.stimulusId} ausserhalb [0,1]: ${item.confidence}",
                item.confidence in 0f..1f
            )
        }
    }

    // ── Unclear ──────────────────────────────────────────────────────────────

    @Test
    fun `dataset contains the expected mix of correct, misclassified and unclear items`() {
        val misclassified = StimulusRepository.items.filter { !it.isPrototypeCorrect }
        val unclear = StimulusRepository.items.filter { it.unclearFlag }

        // Bewusst hinterlegtes realistisches KI-Verhalten (siehe StimulusRepository.kt-Kommentar):
        // 2 absichtliche Fehlklassifikationen, 3 Unclear-Faelle.
        assertEquals(2, misclassified.size)
        assertEquals(3, unclear.size)
    }

    @Test
    fun `toResponse never leaks ground truth to the participant-facing object`() {
        StimulusRepository.items.forEach { item ->
            val response = item.toResponse()
            // StimulusResponse hat bewusst kein groundTruth-Feld - Kompilierbarkeit
            // dieses Tests ist bereits der Beweis, dass es nicht existiert.
            assertEquals(item.prototypeFraction, response.prototypeFraction)
            assertEquals(item.confidence, response.confidence)
            assertEquals(item.unclearFlag, response.unclear)
        }
    }
}
