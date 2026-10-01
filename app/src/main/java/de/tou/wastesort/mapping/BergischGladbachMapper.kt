package de.tou.wastesort.mapping

import de.tou.wastesort.data.MappingResult
import de.tou.wastesort.data.TrashAiResult
import de.tou.wastesort.data.WasteFraction

/**
 * Mapping-Layer: TrashAI-Output-Label → Bergisch Gladbacher Abfallfraktion
 *
 * Bergisch Gladbach definiert 5 Fraktionen gemaess Abfallentsorgungssatzung
 * (Stand Dezember 2025) und Sortierhinweise (Stadt BG, 2025b).
 *
 * Erweiterung: Neue Eintraege einfach in die mappingTable eintragen.
 * Ambiguitaeten werden im ambiguityNote-Feld dokumentiert.
 */
object BergischGladbachMapper {

    /** Confidence unter diesem Schwellenwert → UNCLEAR */
    private const val CONFIDENCE_THRESHOLD = 0.6f

    /**
     * Mapping-Tabelle: normiertes TrashAI-Label → (Fraktion, Ambiguitaetshinweis)
     *
     * Labels werden vor dem Lookup lowercase + trim normiert.
     * Reihenfolge spielt keine Rolle; exakter Match hat Vorrang vor partiellem.
     */
    private val mappingTable: Map<String, Pair<WasteFraction, String?>> = mapOf(

        // ── RESTMUELL ─────────────────────────────────────────────────────────
        "ceramic"         to (WasteFraction.RESTMUELL to null),
        "broken ceramic"  to (WasteFraction.RESTMUELL to null),
        "porcelain"       to (WasteFraction.RESTMUELL to null),
        "tape"            to (WasteFraction.RESTMUELL to null),
        "adhesive tape"   to (WasteFraction.RESTMUELL to null),
        "nappy"           to (WasteFraction.RESTMUELL to null),
        "diaper"          to (WasteFraction.RESTMUELL to null),
        "cd"              to (WasteFraction.RESTMUELL to "CDs/DVDs gehoeren nicht in die Gelbe Tonne"),
        "dvd"             to (WasteFraction.RESTMUELL to "CDs/DVDs gehoeren nicht in die Gelbe Tonne"),
        "cigarette"       to (WasteFraction.RESTMUELL to null),
        "tissue"          to (WasteFraction.RESTMUELL to null),
        "rubber"          to (WasteFraction.RESTMUELL to null),
        "styrofoam"       to (WasteFraction.RESTMUELL to "Transportverpackung aus Styropor → Gelbe Tonne; Sonstiges → Restmuell"),
        "mixed waste"     to (WasteFraction.RESTMUELL to null),
        "broken glass"    to (WasteFraction.RESTMUELL to "Scherben sicher verpacken, dann Restmuell"),
        "mirror"          to (WasteFraction.RESTMUELL to "Spiegel nicht in Glascontainer"),
        "hygiene"         to (WasteFraction.RESTMUELL to null),
        "napkin"          to (WasteFraction.RESTMUELL to null),
        "paper cup"       to (WasteFraction.RESTMUELL to "Kaffeebecher mit Kunststoffbeschichtung → Restmuell"),

        // ── PAPIERTONNE / BLAUE TONNE ─────────────────────────────────────────
        "paper"           to (WasteFraction.PAPIER to null),
        "newspaper"       to (WasteFraction.PAPIER to null),
        "cardboard"       to (WasteFraction.PAPIER to "Nur ohne Plastikbeschichtung oder Verbundmaterial"),
        "magazine"        to (WasteFraction.PAPIER to null),
        "book"            to (WasteFraction.PAPIER to null),
        "envelope"        to (WasteFraction.PAPIER to null),
        "paper bag"       to (WasteFraction.PAPIER to null),
        "flyer"           to (WasteFraction.PAPIER to null),
        "carton box"      to (WasteFraction.PAPIER to "Nur Kartonagen ohne Kunststoffbeschichtung"),

        // ── BIOTONNE ──────────────────────────────────────────────────────────
        "food"            to (WasteFraction.BIO to null),
        "food waste"      to (WasteFraction.BIO to null),
        "organic"         to (WasteFraction.BIO to null),
        "fruit"           to (WasteFraction.BIO to null),
        "vegetable"       to (WasteFraction.BIO to null),
        "peel"            to (WasteFraction.BIO to null),
        "apple core"      to (WasteFraction.BIO to null),
        "core"            to (WasteFraction.BIO to null),
        "coffee grounds"  to (WasteFraction.BIO to null),
        "coffee filter"   to (WasteFraction.BIO to null),
        "eggshell"        to (WasteFraction.BIO to null),
        "garden waste"    to (WasteFraction.BIO to null),
        "leaves"          to (WasteFraction.BIO to null),
        "plant"           to (WasteFraction.BIO to null),
        "bread"           to (WasteFraction.BIO to null),
        "meat"            to (WasteFraction.BIO to null),

        // ── GELBE TONNE / GELBER SACK ─────────────────────────────────────────
        "plastic"         to (WasteFraction.GELBE_TONNE to "Nur Kunststoffverpackungen mit Gruenem Punkt"),
        "plastic bottle"  to (WasteFraction.GELBE_TONNE to "Nur Verpackungen; Kappe kann dranbleiben"),
        "plastic bag"     to (WasteFraction.GELBE_TONNE to "Nur Verpackungsfolien mit Gruenem Punkt"),
        "can"             to (WasteFraction.GELBE_TONNE to null),
        "tin can"         to (WasteFraction.GELBE_TONNE to null),
        "metal"           to (WasteFraction.GELBE_TONNE to "Nur metallische Verpackungen"),
        "aluminium"       to (WasteFraction.GELBE_TONNE to null),
        "aluminum foil"   to (WasteFraction.GELBE_TONNE to null),
        "foil"            to (WasteFraction.GELBE_TONNE to null),
        "carton"          to (WasteFraction.GELBE_TONNE to "Verbundverpackungen wie Tetra Pak → Gelbe Tonne"),
        "tetra pak"       to (WasteFraction.GELBE_TONNE to null),
        "wrapper"         to (WasteFraction.GELBE_TONNE to "Nur Verpackungsfolien"),
        "bottle cap"      to (WasteFraction.GELBE_TONNE to null),
        "yoghurt"         to (WasteFraction.GELBE_TONNE to null),
        "yogurt container" to (WasteFraction.GELBE_TONNE to null),
        "shampoo bottle"  to (WasteFraction.GELBE_TONNE to null),
        "packaging"       to (WasteFraction.GELBE_TONNE to "Nur mit Gruenem Punkt; verunreinigt → Restmuell"),

        // ── GLASCONTAINER ─────────────────────────────────────────────────────
        "glass bottle"    to (WasteFraction.GLAS_GRUEN to "Farbe beachten: Gruen/Braun/Weiss"),
        "wine bottle"     to (WasteFraction.GLAS_GRUEN to "Gruenes Glas → Gruenglascontainer"),
        "green bottle"    to (WasteFraction.GLAS_GRUEN to null),
        "beer bottle"     to (WasteFraction.GLAS_BRAUN to "Braunes Glas → Braunglascontainer"),
        "brown bottle"    to (WasteFraction.GLAS_BRAUN to null),
        "jam jar"         to (WasteFraction.GLAS_WEISS to "Deckel aus Metall → Gelbe Tonne; Keramikdeckel → Restmuell"),
        "jar"             to (WasteFraction.GLAS_WEISS to "Deckel separat entsorgen"),
        "white bottle"    to (WasteFraction.GLAS_WEISS to null),
        "clear bottle"    to (WasteFraction.GLAS_WEISS to null),
        "mineral water"   to (WasteFraction.GLAS_WEISS to "Glasflasche: Weissglascontainer; PET-Flasche: Pfand oder Gelbe Tonne"),
        "blue glass"      to (WasteFraction.GLAS_GRUEN to "Blaues Glas zaehlt in Bergisch Gladbach zum Gruenglas"),
        "glass"           to (WasteFraction.GLAS_GRUEN to "Glasfarbe beachten; blaues Glas → Gruenglascontainer")
    )

    // ── Haupt-Mapping-Funktion ─────────────────────────────────────────────────

    fun map(result: TrashAiResult): MappingResult {

        // Confidence zu niedrig → UNCLEAR
        if (result.confidence < CONFIDENCE_THRESHOLD) {
            return MappingResult(
                fraction      = WasteFraction.UNCLEAR,
                confidence    = result.confidence,
                rawLabel      = result.rawLabel,
                isUnclear     = true,
                ambiguityNote = "Confidence ${(result.confidence * 100).toInt()} % < 60 % – bitte Sortierhinweise pruefen"
            )
        }

        val normalized = result.rawLabel.lowercase().trim()

        // 1. Exakter Match
        mappingTable[normalized]?.let { (fraction, note) ->
            return MappingResult(
                fraction      = fraction,
                confidence    = result.confidence,
                rawLabel      = result.rawLabel,
                isUnclear     = false,
                ambiguityNote = note
            )
        }

        // 2. Partieller Match (Label enthaelt einen bekannten Key oder umgekehrt)
        for ((key, value) in mappingTable) {
            if (normalized.contains(key) || key.contains(normalized)) {
                return MappingResult(
                    fraction      = value.first,
                    confidence    = result.confidence * 0.85f,  // leicht reduziert bei partiellem Match
                    rawLabel      = result.rawLabel,
                    isUnclear     = false,
                    ambiguityNote = value.second ?: "Partieller Treffer fuer '$key'"
                )
            }
        }

        // 3. Kein Match → UNCLEAR
        return MappingResult(
            fraction      = WasteFraction.UNCLEAR,
            confidence    = result.confidence,
            rawLabel      = result.rawLabel,
            isUnclear     = true,
            ambiguityNote = "Unbekanntes Material '${result.rawLabel}' – bitte manuell pruefen"
        )
    }

    // ── Hilfsfunktion fuer Unit-Tests ─────────────────────────────────────────

    fun mapLabel(label: String, confidence: Float = 0.9f): MappingResult =
        map(TrashAiResult(rawLabel = label, confidence = confidence))
}
