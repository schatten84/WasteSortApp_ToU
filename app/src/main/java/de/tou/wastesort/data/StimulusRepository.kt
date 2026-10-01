package de.tou.wastesort.data

/**
 * Zentrales Dataset der 20 Studien-Stimulus-Items (eine einzige Quelle der
 * Wahrheit - siehe StudyConfig.STUDY_MODE = "qr_predefined").
 *
 * Jede physische Stimulus-Karte traegt einen QR-Code mit exakt einem dieser
 * stimulusId-Werte (Format "BG-WASTE-01".."BG-WASTE-20"). Der QR-Code
 * klassifiziert NICHTS selbst - er ist nur ein Schluessel fuer diese Tabelle.
 *
 * QUELLENVERIFIKATION: Alle groundTruthFraction-Werte wurden gegen die vom
 * Nutzer bereitgestellte offizielle "Sortierübersicht" (Bergisch-Gladbach-
 * Abfalltrennungs-PDF) abgeglichen. Direkt bestaetigte Treffer (exakter oder
 * naher Begriff im Original-Dokument): Keramiktopf/Porzellangeschirr →
 * Restmuell (Item 1), Tesafilmroller → Restmuell (Item 2, stuetzt Item-2-
 * Klebeband-Zuordnung), Zeitschrift/Zeitung → Papiertonne (5), Versandumschlag
 * → Papiertonne (7), Einkaufstuete (Papier) vs. (Plastik) → unterschiedliche
 * Tonnen (8), Apfelrest → Biotonne wortgleich (9), Kaffeefilter + Kaffeesatz →
 * Biotonne (10), Gemueseabfaelle → Biotonne (11), Eierschale → Biotonne
 * wortgleich (12), Joghurtbecher (leer) → Gelbe Tonne wortgleich (13),
 * Alufolie → Gelbe Tonne wortgleich (14), Apfelsaftkarton/Milchkarton/
 * Saftkarton → Gelbe Tonne, NICHT Papiertonne (stuetzt Item 15),
 * Tiefkuehlkostschachtel (beschichtete Pappe) → Gelbe Tonne (stuetzt die
 * Unclear-Einstufung von Item 6: beschichtete/kunststoffkaschierte Kartonage
 * gehoert NICHT in die Papiertonne), Shampooflasche (Plastik) → Gelbe Tonne
 * vs. Shampooflasche (Glas) → Glascontainer (stuetzt die Materialpraezisierung
 * bei Item 16), Arzneimittelflasche (Glas) → Glascontainer vs. (Kunststoff) →
 * Gelbe Tonne (stuetzt die Materialpraezisierung bei Item 18), sowie die
 * explizite Aussage im Dokument "Blaues Glas kommt in den Grünglascontainer"
 * (bestaetigt Item 20 vollstaendig - zuvor als unverifizierte Annahme
 * geflaggt). Fuer Items 2, 3, 4 existiert kein exakter Wortlaut im Dokument;
 * die Zuordnung folgt etablierter Konvention (siehe Item-Kommentare unten).
 *
 * groundTruthFraction vs. prototypeFraction:
 * Bei den meisten Items sind beide identisch. Bei ZWEI Items weicht die
 * Prototyp-Empfehlung ABSICHTLICH von der Ground Truth ab (simulierte
 * TrashAI-Fehlklassifikation, wie im Proposal als realistisches KI-Verhalten
 * vorgesehen). Bei DREI weiteren Items liegt die Confidence unter der
 * 60%-Schwelle und unclearFlag = true (simulierte Unsicherheit). Die
 * uebrigen 15 Items haben eine korrekte, sichere Prototyp-Empfehlung.
 *
 * Aktualisierung Item 18/19 gemaess Appendix-K-Revision: Item 18 ist eine
 * braune Arzneimittelflasche AUS GLAS (nicht mehr Bierflasche; Materialangabe
 * praezisiert, da laut Sortierübersicht die Kunststoff-Variante in die Gelbe
 * Tonne gehoert), Item 19 eine klare Aperini-Flasche (nicht mehr
 * Marmeladenglas) - passend zu den bereits vorhandenen Setcards
 * Setcard_18_braunes_glas.png / Setcard_19_klares_glas.png.
 */
object StimulusRepository {

    val items: List<StimulusItem> = listOf(

        // ── RESTMUELLTONNE (Items 1-4) ───────────────────────────────────────
        StimulusItem(
            stimulusId          = "BG-WASTE-01",
            itemNumber          = 1,
            nameDe              = "Zerbrochene Keramiktasse",
            nameEn              = "Broken ceramic mug",
            assumedCondition    = "zerbrochen, scharfkantige Scherben",
            groundTruthFraction = WasteFraction.RESTMUELL,
            prototypeFraction   = WasteFraction.RESTMUELL,
            confidence          = 0.88f,
            unclearFlag         = false
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-02",
            itemNumber          = 2,
            nameDe              = "Gebrauchtes Klebeband",
            nameEn              = "Used adhesive tape",
            assumedCondition    = "gebraucht, an kleiner Papprolle haftend",
            groundTruthFraction = WasteFraction.RESTMUELL,
            prototypeFraction   = WasteFraction.RESTMUELL,
            confidence          = 0.79f,
            unclearFlag         = false
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-03",
            itemNumber          = 3,
            nameDe              = "Windel",
            nameEn              = "Disposable diaper",
            assumedCondition    = "unbenutzt (Demonstrationsobjekt)",
            groundTruthFraction = WasteFraction.RESTMUELL,
            prototypeFraction   = WasteFraction.RESTMUELL,
            confidence          = 0.91f,
            unclearFlag         = false
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-04",
            itemNumber          = 4,
            nameDe              = "Zerkratzte CD/DVD",
            nameEn              = "Scratched CD/DVD",
            assumedCondition    = "zerkratzt, ohne Huelle",
            groundTruthFraction = WasteFraction.RESTMUELL,
            // ABSICHTLICHE FEHLKLASSIFIKATION: haeufige reale KI-Verwechslung,
            // da CDs wie Kunststoffverpackung aussehen.
            prototypeFraction   = WasteFraction.GELBE_TONNE,
            confidence          = 0.68f,
            unclearFlag         = false,
            ambiguityNote       = "CDs/DVDs gehoeren nicht in die Gelbe Tonne, auch wenn sie aus Kunststoff bestehen"
        ),

        // ── PAPIERTONNE (Items 5-8) ──────────────────────────────────────────
        StimulusItem(
            stimulusId          = "BG-WASTE-05",
            itemNumber          = 5,
            nameDe              = "Zeitung",
            nameEn              = "Folded newspaper",
            assumedCondition    = "trocken, gefaltet",
            groundTruthFraction = WasteFraction.PAPIER,
            prototypeFraction   = WasteFraction.PAPIER,
            confidence          = 0.93f,
            unclearFlag         = false
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-06",
            itemNumber          = 6,
            nameDe              = "Flachgefaltete Kartonage",
            nameEn              = "Flattened cardboard box",
            assumedCondition    = "flachgefaltet, evtl. Klebefolienreste",
            groundTruthFraction = WasteFraction.PAPIER,
            prototypeFraction   = WasteFraction.PAPIER,
            // NIEDRIGE CONFIDENCE / UNCLEAR: Kunststoffbeschichtung ist per Foto
            // nicht zuverlaessig erkennbar - realistische KI-Unsicherheit.
            confidence          = 0.55f,
            unclearFlag         = true,
            ambiguityNote       = "Nur ohne Plastikbeschichtung oder Verbundmaterial in die Papiertonne - laut Sortierübersicht gehört z.B. beschichtete Pappe (Tiefkühlkostschachtel) in die Gelbe Tonne"
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-07",
            itemNumber          = 7,
            nameDe              = "Papierumschlag",
            nameEn              = "Paper envelope",
            assumedCondition    = "leer, ohne Sichtfenster",
            groundTruthFraction = WasteFraction.PAPIER,
            prototypeFraction   = WasteFraction.PAPIER,
            confidence          = 0.90f,
            unclearFlag         = false
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-08",
            itemNumber          = 8,
            nameDe              = "Papiertragetasche",
            nameEn              = "Paper shopping bag",
            assumedCondition    = "unbeschichtet, ohne Kunststoffgriffe",
            groundTruthFraction = WasteFraction.PAPIER,
            prototypeFraction   = WasteFraction.PAPIER,
            confidence          = 0.82f,
            unclearFlag         = false
        ),

        // ── BIOTONNE (Items 9-12) ────────────────────────────────────────────
        StimulusItem(
            stimulusId          = "BG-WASTE-09",
            itemNumber          = 9,
            nameDe              = "Apfelrest",
            nameEn              = "Apple core",
            assumedCondition    = "frisch, unverpackt",
            groundTruthFraction = WasteFraction.BIO,
            prototypeFraction   = WasteFraction.BIO,
            confidence          = 0.94f,
            unclearFlag         = false
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-10",
            itemNumber          = 10,
            nameDe              = "Kaffeesatz mit Papierfilter",
            nameEn              = "Coffee grounds with paper filter",
            assumedCondition    = "feucht, Filter aus unbeschichtetem Papier",
            groundTruthFraction = WasteFraction.BIO,
            prototypeFraction   = WasteFraction.BIO,
            confidence          = 0.85f,
            unclearFlag         = false
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-11",
            itemNumber          = 11,
            nameDe              = "Gemueseschale",
            nameEn              = "Vegetable peel",
            assumedCondition    = "frisch, unverpackt",
            groundTruthFraction = WasteFraction.BIO,
            prototypeFraction   = WasteFraction.BIO,
            confidence          = 0.89f,
            unclearFlag         = false
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-12",
            itemNumber          = 12,
            nameDe              = "Eierschale",
            nameEn              = "Eggshell",
            assumedCondition    = "leer, zerbrochen",
            groundTruthFraction = WasteFraction.BIO,
            prototypeFraction   = WasteFraction.BIO,
            confidence          = 0.92f,
            unclearFlag         = false
        ),

        // ── GELBE TONNE / GELBER SACK (Items 13-16) ──────────────────────────
        StimulusItem(
            stimulusId          = "BG-WASTE-13",
            itemNumber          = 13,
            nameDe              = "Joghurtbecher",
            nameEn              = "Plastic yoghurt pot",
            assumedCondition    = "leer, ausgespuelt, mit Gruenem Punkt",
            groundTruthFraction = WasteFraction.GELBE_TONNE,
            prototypeFraction   = WasteFraction.GELBE_TONNE,
            confidence          = 0.86f,
            unclearFlag         = false
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-14",
            itemNumber          = 14,
            nameDe              = "Alufolie",
            nameEn              = "Aluminium foil",
            assumedCondition    = "zerknuellt, ohne Speisereste",
            groundTruthFraction = WasteFraction.GELBE_TONNE,
            prototypeFraction   = WasteFraction.GELBE_TONNE,
            confidence          = 0.78f,
            unclearFlag         = false
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-15",
            itemNumber          = 15,
            nameDe              = "Getraenkekarton (Tetra Pak)",
            nameEn              = "Drinks carton (Tetra Pak)",
            assumedCondition    = "leer, ausgespuelt, Verbundmaterial",
            groundTruthFraction = WasteFraction.GELBE_TONNE,
            prototypeFraction   = WasteFraction.GELBE_TONNE,
            confidence          = 0.71f,
            unclearFlag         = false,
            ambiguityNote       = "Verbundverpackungen wie Tetra Pak gehoeren trotz Kartonoptik in die Gelbe Tonne (laut Sortierübersicht ebenso Apfelsaftkarton, Milchkarton, Saftkarton - nicht Papiertonne)"
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-16",
            itemNumber          = 16,
            nameDe              = "Shampooflasche (Kunststoff)",
            nameEn              = "Plastic shampoo bottle",
            assumedCondition    = "Kunststoffflasche, leer, Verschluss kann dranbleiben",
            groundTruthFraction = WasteFraction.GELBE_TONNE,
            prototypeFraction   = WasteFraction.GELBE_TONNE,
            confidence          = 0.90f,
            unclearFlag         = false,
            ambiguityNote       = "Nur die Kunststoff-Variante gehoert in die Gelbe Tonne - laut Sortierübersicht gehoert eine Shampooflasche aus Glas stattdessen in den Glascontainer"
        ),

        // ── GLASCONTAINER NACH FARBE (Items 17-20) ───────────────────────────
        StimulusItem(
            stimulusId          = "BG-WASTE-17",
            itemNumber          = 17,
            nameDe              = "Gruene Weinflasche",
            nameEn              = "Green wine bottle",
            assumedCondition    = "leer, Metallverschluss entfernt",
            groundTruthFraction = WasteFraction.GLAS_GRUEN,
            prototypeFraction   = WasteFraction.GLAS_GRUEN,
            confidence          = 0.91f,
            unclearFlag         = false,
            ambiguityNote       = "Gruenes Glas → Gruenglascontainer"
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-18",
            itemNumber          = 18,
            nameDe              = "Braune Arzneimittelflasche (Glas)",
            nameEn              = "Brown pharmaceutical bottle (glass)",
            assumedCondition    = "Glasflasche, leer, kein Deckel, kein Etikett",
            groundTruthFraction = WasteFraction.GLAS_BRAUN,
            prototypeFraction   = WasteFraction.GLAS_BRAUN,
            confidence          = 0.83f,
            unclearFlag         = false,
            ambiguityNote       = "Braunes Glas → Braunglascontainer (Arzneimittelreste vorher entfernen). Nur die Glas-Variante - laut Sortierübersicht gehoert eine Arzneimittelflasche aus Kunststoff stattdessen in die Gelbe Tonne"
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-19",
            itemNumber          = 19,
            nameDe              = "Klare Aperini-Flasche",
            nameEn              = "Clear glass Aperini bottle",
            assumedCondition    = "leer, Kronkorken entfernt",
            groundTruthFraction = WasteFraction.GLAS_WEISS,
            prototypeFraction   = WasteFraction.GLAS_WEISS,
            // NIEDRIGE CONFIDENCE / UNCLEAR: klares/helles Glas wird von
            // Bilderkennung haeufiger mit Kunststoff verwechselt.
            confidence          = 0.52f,
            unclearFlag         = true,
            ambiguityNote       = "Klares Glas → Weissglascontainer; Metallverschluss separat in die Gelbe Tonne"
        ),
        StimulusItem(
            stimulusId          = "BG-WASTE-20",
            itemNumber          = 20,
            nameDe              = "Blaue Mineralwasserflasche",
            nameEn              = "Blue mineral water bottle",
            assumedCondition    = "leer, Glasflasche (kein PET)",
            groundTruthFraction = WasteFraction.GLAS_GRUEN,
            prototypeFraction   = WasteFraction.GLAS_GRUEN,
            // NIEDRIGE CONFIDENCE / UNCLEAR: Farbzuordnung von blauem Glas ist
            // fuer Bilderkennung besonders ambig (koennte als Weissglas erkannt werden).
            confidence          = 0.48f,
            unclearFlag         = true,
            ambiguityNote       = "Laut offizieller Sortierübersicht: \"Blaues Glas kommt in den Grünglascontainer.\" (bestaetigt, keine Annahme)"
        )
    )

    fun findByStimulusId(stimulusId: String): StimulusItem? =
        items.find { it.stimulusId.equals(stimulusId.trim(), ignoreCase = true) }

    /**
     * Robuster QR-Payload-Resolver: akzeptiert sowohl das aktuelle QR-Format
     * ("BG-WASTE-09") als auch das AELTERE Format der bereits gedruckten
     * Setcards aus der urspruenglichen Setcard-Erstellung
     * ("Nr: 9; Set: 1; Gegenstand: Apfelrest").
     *
     * Hintergrund: Die physischen Setcards (Setcard_01_....png ...
     * Setcard_20_....png) wurden mit einem frueheren Skript erzeugt, das
     * Metadaten statt einer stabilen stimulusId in den QR-Code kodiert hat.
     * Damit bereits gedruckte Karten nicht neu produziert werden muessen,
     * wird hier zusaetzlich die "Nr: <n>"-Praefix-Konvention geparst und
     * ueber itemNumber aufgeloest. Neue QR-Codes (z.B. aus
     * tools/generate_qr_codes.py) nutzen weiterhin ausschliesslich das
     * kurze stimulusId-Format.
     */
    private val legacyNumberRegex = Regex("""Nr\s*:\s*(\d{1,2})""", RegexOption.IGNORE_CASE)

    fun resolveFromQrPayload(rawValue: String): StimulusItem? {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return null

        // 1. Aktuelles Format: exakte stimulusId
        findByStimulusId(trimmed)?.let { return it }

        // 2. Legacy-Format der bereits gedruckten Setcards: "Nr: <n>; Set: ...; Gegenstand: ..."
        legacyNumberRegex.find(trimmed)?.groupValues?.get(1)?.toIntOrNull()?.let { number ->
            items.find { it.itemNumber == number }?.let { return it }
        }

        return null
    }

    /** Nur die 20 fest hinterlegten Studien-Items gelten als gueltig (siehe Punkt 15 der Spezifikation) */
    fun isValidStudyStimulus(rawValue: String): Boolean =
        resolveFromQrPayload(rawValue) != null

    val allStimulusIds: List<String> get() = items.map { it.stimulusId }
}
