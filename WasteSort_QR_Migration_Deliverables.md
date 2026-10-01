# WasteSort – Umstellung auf QR-Code-Stimulus-Lookup

Dokumentation gemäß Anfrage (Abschnitte A–G). Alle Änderungen wurden am bestehenden Projekt vorgenommen, nichts wurde von Grund auf neu gebaut.

---

## Nachtrag: Bugfix – bereits gedruckte Setcards wurden nicht erkannt

**Ursache:** Die 20 bereits vorhandenen Setcard-PNGs (`Setcard_01_....png` … `Setcard_20_....png`) wurden mit einem früheren Skript erzeugt, das **keine** `stimulusId` in den QR-Code kodiert, sondern ein Metadaten-Format:

```
Nr: 9; Set: 1; Gegenstand: Apfelrest
```

Der Scanner erwartete aber exakt `BG-WASTE-09`. Ich habe alle 20 Setcards mit `pyzbar`/`zbar` dekodiert, um das tatsächliche Format zu verifizieren, und `StimulusRepository.resolveFromQrPayload()` ergänzt: Diese Funktion akzeptiert jetzt **beide** Formate — zuerst wird die neue `stimulusId` exakt geprüft, andernfalls wird aus `"Nr: <n>"` die Item-Nummer extrahiert und darüber aufgelöst. So müssen die bereits gedruckten Karten **nicht neu produziert werden**. `MainViewModel.lookupStimulus()` nutzt jetzt diesen robusteren Resolver; ein neuer Unit-Test (`resolveFromQrPayload legacy format works for all 20 real setcard payloads`) prüft das anhand der tatsächlich aus allen 20 PNGs dekodierten Rohdaten.

**Nebenbefund (nicht behoben, nur zur Info):** Die "Gegenstand"-Texte in den Legacy-QR-Codes enthalten kaputte Umlaute (z. B. "Gemüseschale" wird als Mojibake kodiert) — vermutlich ein Encoding-Bug im ursprünglichen Setcard-Skript (doppelte/falsche UTF-8-Kodierung). Das beeinträchtigt die App-Funktion nicht (nur die Nummer wird ausgewertet), könnte aber an anderer Stelle auffallen, falls der rohe QR-Text jemals angezeigt wird.

---

## A. Implementierungs-Zusammenfassung

**Was geändert wurde:**

Die App verwendete zuvor eine Kamera-Aufnahme mit anschließender (fest hinterlegter Mock-)Klassifikation eines von 20 vordefinierten Items. Diese Live-Erkennung wurde vollständig durch einen **QR-Code-Lookup** ersetzt: Jede der 20 physischen Stimulus-Karten trägt einen QR-Code mit ausschließlich einer stabilen ID (`BG-WASTE-01` … `BG-WASTE-20`). Der QR-Code klassifiziert nichts – er ist nur ein Schlüssel in eine zentrale, fest hinterlegte Tabelle (`StimulusRepository.kt`).

**Wiederverwendete Teile (unverändert oder nur minimal angepasst):**
- `BinSelectionScreen.kt` – Tonnenauswahl für das Kind, komplett unverändert übernommen.
- `StudyLogger.kt` – CSV-Logging-Mechanismus (Datei-Handling, FileProvider-Sharing) wiederverwendet, nur das Zeilenschema erweitert.
- `SummaryScreen.kt` / `ExportScreen.kt` – Struktur unverändert, nur Feldnamen an das neue Schema angepasst.
- `MainViewModel.kt`, `StudySessionManager.kt` – Architektur (ViewModel hält State, SessionManager verwaltet Trials) beibehalten, Kernlogik von "Foto → Mock-Klassifikation" auf "QR → Repository-Lookup" umgestellt.
- Die komplette **TrashAI/Roboflow-Live-Anbindung** (`RoboflowTrashAiClient.kt`, `TrashAiClient.kt`, `MockClassifier.kt`, `WasteClassifier.kt`, `BergischGladbachMapper.kt`) bleibt unverändert im Projekt erhalten, wird aber vom aktiven Studien-Workflow nicht mehr aufgerufen (siehe `StudyConfig.STUDY_MODE = "qr_predefined"` in `Models.kt`).

**Wie das QR-Scanning funktioniert:**
CameraX (bereits im Projekt vorhanden) liefert einen Live-Kamera-Feed; ein `ImageAnalysis`-Use-Case mit einem ML-Kit-`BarcodeScanner` (`QrAnalyzer.kt`) prüft jedes Frame auf QR-Codes. Der erkannte Rohtext wird gegen `StimulusRepository.findByStimulusId()` geprüft. Bei Erfolg: kurzes "Karte erkannt ✓"-Feedback (650 ms), dann Weiterleitung zum Ergebnis-Screen. Bei unbekanntem Code: Fehlermeldung, automatischer Reset nach 2,2 s.

**Wo die 20-Item-Zuordnung liegt:**
Zentral und ausschließlich in `app/src/main/java/de/tou/wastesort/data/StimulusRepository.kt` (eine Kotlin-Objektliste von `StimulusItem`). Keine Streuung über UI-Komponenten.

---

## B. Geänderte/neue Dateien

| Datei | Status | Zweck |
|---|---|---|
| `data/Models.kt` | geändert | `StudyConfig` (STUDY_MODE-Flag), `StimulusItem`, `StimulusResponse` (geteiltes Objekt für A/B), erweitertes `StudyTrial`/`StudySession` (participantId, stimulusId, itemNumber, groundTruthFraction, prototypeFraction, unclearFlag) |
| `data/StimulusRepository.kt` | **neu** (ersetzt `study/ThesisTestItems.kt`) | Zentrales 20-Item-Dataset inkl. Ground-Truth/Prototyp-Trennung, absichtlicher Fehlklassifikationen und Unclear-Fällen |
| `data/StudyLogger.kt` | geändert | CSV-Schema erweitert um participant_id, stimulus_id, item_number, ground_truth_category, unclear_flag; `trialToCsvRow` auf `internal` für Testbarkeit |
| `study/StudySessionManager.kt` | geändert | Kein Auto-Zähler mehr; nimmt beliebig gescannte `StimulusItem`s entgegen, vergleicht `isCorrect` gegen Ground Truth |
| `study/ThesisTestItems.kt` | **gelöscht** | Ersetzt durch `data/StimulusRepository.kt` |
| `qr/QrAnalyzer.kt` | **neu** | ML-Kit-Barcode-Scanner als CameraX-`ImageAnalysis.Analyzer` |
| `ui/screens/ScanScreen.kt` | **neu** (ersetzt `CaptureScreen.kt` + `TrialScreen.kt`) | Kamera-Vorschau, Live-QR-Erkennung, Erfolgs-/Fehler-Feedback, manueller Eingabe-Fallback für Tests |
| `ui/screens/CaptureScreen.kt`, `TrialScreen.kt` | **gelöscht** | Foto-Aufnahme-Workflow entfällt vollständig |
| `ui/screens/ResultScreenA.kt` | geändert | Rendert jetzt `StimulusResponse` statt `MappingResult`+Foto; kein Foto-Thumbnail mehr |
| `ui/screens/ResultScreenB.kt` | geändert | Rendert `StimulusResponse`; kindgerechte Confidence-Aussage (✓/?) statt nur Prozentzahl; expliziter Unclear-Zustand |
| `ui/screens/DebugScreen.kt` | **neu** | Testleiter-Ansicht aller 20 Mappings, nicht Teil der Teilnehmer-Navigation |
| `ui/screens/StartScreen.kt` | geändert | Teilnehmer-Code-Feld hinzugefügt; Long-Press auf App-Titel öffnet DebugScreen; Infotext auf QR-Workflow angepasst |
| `ui/screens/SummaryScreen.kt` | geändert | Feldnamen an neues `StudyTrial`-Schema angepasst, Teilnehmer-Code in der Zusammenfassung angezeigt |
| `ui/navigation/AppNavigation.kt` | geändert | Routen `CAPTURE`/`TRIAL` → `SCAN`; neue Route `DEBUG` |
| `viewmodel/MainViewModel.kt` | geändert | `lookupStimulus()`, `onStimulusFound()`, `setParticipantId()` statt Foto-/Klassifikations-Logik |
| `res/xml/file_paths.xml` | geändert | `camera_images`-Cache-Pfad entfernt (kein Foto-Capture mehr) |
| `AndroidManifest.xml` | geändert | Kommentare aktualisiert (Kamera jetzt für QR-Scan, nicht Fotoaufnahme) |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | geändert | ML-Kit-Barcode-Scanning-Dependency, JUnit-Test-Dependency ergänzt |
| `src/test/.../StimulusRepositoryTest.kt` | **neu** | Unit-Tests: Mapping, Determinismus, unbekannter QR, Confidence, Unclear-Mix |
| `src/test/.../StudyLoggerAndSessionTest.kt` | **neu** | Unit-Tests: CSV-Zeilenformat, Ground-Truth-Vergleich, UI-Äquivalenz A/B, Session-Verhalten |
| `tools/generate_qr_codes.py` | **neu** | Python-Skript zur QR-Code-Generierung (20 PNGs + Übersichtsblatt) |
| `tools/qr_codes/BG-WASTE-01.png` … `-20.png` | **neu** | Generierte, druckfertige QR-Codes je Stimulus-Item |
| `tools/qr_codes/overview_sheet.png` | **neu** | Druckfertiges Übersichtsblatt aller 20 Codes |

Nicht angetastet: `BinSelectionScreen.kt`, `Theme.kt`, `MainActivity.kt`, `mapping/BergischGladbachMapper.kt`, `classifier/*.kt`, Gradle-Wrapper, Launcher-Icons.

---

## C. Vollständige 20-Item-Mapping-Tabelle

**Quelle:** Die offizielle "Sortierübersicht" der Stadt Bergisch Gladbach, die du zwischenzeitlich bereitgestellt hast. Ich habe alle 20 Ground-Truth-Zuordnungen dagegen abgeglichen (siehe Verifikations-Kommentar am Kopf von `StimulusRepository.kt`). Legende: `✓ Beleg` = wortgleicher oder direkt vergleichbarer Eintrag im Dokument gefunden; `– Konvention` = kein exakter Beleg im Dokument, Zuordnung folgt etablierter Abfalltrennungs-Konvention (keine Widersprüche gefunden).

| ID | Stimulus-Item | Zustand | BG Ground Truth | Beleg | Prototyp-Antwort | Confidence | Unclear |
|---|---|---|---|---|---|---|---|
| BG-WASTE-01 | Zerbrochene Keramiktasse | zerbrochen, scharfkantig | Restmülltonne | ✓ "Keramiktopf"/"Porzellangeschirr" | Restmülltonne | 88% | Nein |
| BG-WASTE-02 | Gebrauchtes Klebeband | gebraucht, an Papprolle | Restmülltonne | ✓ "Tesafilmroller" (naher Beleg) | Restmülltonne | 79% | Nein |
| BG-WASTE-03 | Windel | unbenutzt (Demo-Objekt) | Restmülltonne | – Konvention | Restmülltonne | 91% | Nein |
| BG-WASTE-04 | Zerkratzte CD/DVD | zerkratzt, ohne Hülle | Restmülltonne | – Konvention | **Gelbe Tonne ⚠** | 68% | Nein |
| BG-WASTE-05 | Zeitung | trocken, gefaltet | Papiertonne | ✓ "Zeitschrift, Zeitung" | Papiertonne | 93% | Nein |
| BG-WASTE-06 | Flachgefaltete Kartonage | evtl. Klebefolienreste | Papiertonne | ✓ Analogie "beschichtete Pappe → Gelbe Tonne" | Papiertonne | **55% ⚠** | **Ja** |
| BG-WASTE-07 | Papierumschlag | leer, ohne Sichtfenster | Papiertonne | ✓ "Versandumschlag" | Papiertonne | 90% | Nein |
| BG-WASTE-08 | Papiertragetasche | unbeschichtet | Papiertonne | ✓ "Einkaufstüte (Papier)" | Papiertonne | 82% | Nein |
| BG-WASTE-09 | Apfelrest | frisch, unverpackt | Biotonne | ✓ "Apfelrest" (wortgleich) | Biotonne | 94% | Nein |
| BG-WASTE-10 | Kaffeesatz mit Papierfilter | feucht, unbeschichteter Filter | Biotonne | ✓ "Kaffeefilter" + "Kaffeesatz" | Biotonne | 85% | Nein |
| BG-WASTE-11 | Gemüseschale | frisch, unverpackt | Biotonne | ✓ "Gemüseabfälle" | Biotonne | 89% | Nein |
| BG-WASTE-12 | Eierschale | leer, zerbrochen | Biotonne | ✓ "Eierschale" (wortgleich) | Biotonne | 92% | Nein |
| BG-WASTE-13 | Joghurtbecher | leer, Grüner Punkt | Gelbe Tonne | ✓ "Joghurtbecher (leer)" (wortgleich) | Gelbe Tonne | 86% | Nein |
| BG-WASTE-14 | Alufolie | zerknüllt, ohne Speisereste | Gelbe Tonne | ✓ "Alufolie" (wortgleich) | Gelbe Tonne | 78% | Nein |
| BG-WASTE-15 | Getränkekarton (Tetra Pak) | leer, Verbundmaterial | Gelbe Tonne | ✓ "Apfelsaftkarton"/"Milchkarton"/"Saftkarton" | **Papiertonne ⚠** | 71% | Nein |
| BG-WASTE-16 | Shampooflasche (Kunststoff) | leer | Gelbe Tonne | ✓ "Shampooflasche (Plastik)" | Gelbe Tonne | 90% | Nein |
| BG-WASTE-17 | Grüne Weinflasche | leer, Metallverschluss entfernt | Altglas Grün | ✓ "Getränkeeinwegflasche (Glas)" + Farbregel | Altglas Grün | 91% | Nein |
| BG-WASTE-18 | Braune Arzneimittelflasche (Glas) *(rev.)* | leer, ohne Deckel/Etikett | Altglas Braun | ✓ "Arzneimittelflasche (Glas)" | Altglas Braun | 83% | Nein |
| BG-WASTE-19 | Klare Aperini-Flasche *(rev.)* | leer, Kronkorken entfernt | Altglas Weiß | – Konvention (Farbregel Grün/Braun/Weiß bestätigt) | Altglas Weiß | **52% ⚠** | **Ja** |
| BG-WASTE-20 | Blaue Mineralwasserflasche | leer, Glasflasche (kein PET) | Altglas Grün | ✓ **wortgleich**: "Blaues Glas kommt in den Grünglascontainer." | Altglas Grün | **48% ⚠** | **Ja** |

`⚠` = absichtliche Fehlklassifikation (Prototyp ≠ Ground Truth) bzw. Unclear-Fall (Confidence < 60%).

**Wichtige Materialpräzisierung nach Quellenprüfung:** Die Sortierübersicht listet für mehrere Produkttypen unterschiedliche Fraktionen je nach Material (z. B. "Shampooflasche (Plastik)" → Gelbe Tonne vs. "Shampooflasche (Glas)" → Glascontainer; "Arzneimittelflasche (Kunststoff)" → Gelbe Tonne vs. "Arzneimittelflasche (Glas)" → Glascontainer). Ich habe deshalb Item 16 zu **"Shampooflasche (Kunststoff)"** und Item 18 zu **"Braune Arzneimittelflasche (Glas)"** präzisiert (nur Namens-/Zustandsfeld, IDs und Fraktionen unverändert), um jede Verwechslungsgefahr bei der Setcard-Erstellung auszuschließen.

**Bewusst eingebautes Fehlerverhalten** (2 Fehlklassifikationen + 3 Unclear-Fälle, wie in deiner Methodik dokumentiert):
- **BG-WASTE-04** (CD/DVD): realistische Verwechslung mit Kunststoffverpackung.
- **BG-WASTE-15** (Tetra Pak): realistische Verwechslung mit reiner Kartonage – jetzt zusätzlich durch die Analogie-Beispiele aus der Sortierübersicht gestützt.
- **BG-WASTE-06, 19, 20**: Confidence < 60% – simulierte Erkennungsunsicherheit bei beschichtetem Karton bzw. hellem/blauem Glas.

---

## D. UI-Erklärung: Variante A vs. Variante B

Beide Varianten rendern **dasselbe** `StimulusResponse`-Objekt – es gibt keine separate Klassifikationslogik in den UI-Komponenten (siehe `ResultScreenA.kt`/`ResultScreenB.kt`, beide nehmen `response: StimulusResponse` entgegen).

**Variante A (Erwachsene, textbasiert):**
- Konventionelle Kartenstruktur: "Waste item" → Item-Name, "Recommended disposal" → Fraktion, "Confidence" → Prozentzahl + Fortschrittsbalken.
- Bei `unclear = true`: eigener Hinweis-Block ("Low-confidence recommendation") statt der regulären Empfehlung, mit Aufforderung zur manuellen Prüfung.
- Neutrale Farbgebung, keine große Bin-Grafik, kompakte Textstruktur.

**Variante B (Kinder, ca. 6 Jahre):**
- Zentrales Element: großes farbiges Tonnen-Icon (160dp Kreis) mit Emoji – beantwortet sofort "Wo kommt das hin?".
- Redundante Codierung (Punkt 12 der Spezifikation): Farbe **+** Icon **+** kurzer Text-Label (max. 6 Wörter, z. B. "Ab in die blaue Tonne!") **+** zusätzliches Tonnen-Namens-Badge ("Papiertonne") – nicht nur Farbe.
- Confidence wird **nicht** als reine Prozentzahl präsentiert, sondern übersetzt: "✓ Ich bin mir ziemlich sicher!" (≥80%), "? Ich glaube, das stimmt." (60–79%), "? Ich bin mir nicht sicher." (<60%). Der numerische Wert bleibt darunter klein sichtbar (interner Wert bleibt unverändert, nur die Darstellung ändert sich – Punkt 10).
- Bei `unclear = true`: eigener, klar abweichender Zustand (graue Fläche, 🤔-Icon, "Ich bin nicht sicher. Frag einen Erwachsenen! 🙋") statt der normalen Tonnen-Darstellung.
- Großer, ganzflächiger "Weiter"-Button (72dp Höhe) statt kleiner Buttons.

Beim unbekannten QR-Code unterscheidet sich ebenfalls nur die Formulierung: Variante A zeigt eine sachliche Fehlermeldung, Variante B "Kenn ich nicht! 😕 Probier eine andere Karte" – beide Zustände werden im selben `ScanScreen.kt` gerendert, nur der Text/die Emoji-Wahl hängt von `currentVariant` ab.

---

## E. Testing

**Hinzugefügte Tests** (JVM-Unit-Tests, `app/src/test/java/de/tou/wastesort/`):

`StimulusRepositoryTest.kt`
- Alle 20 erwarteten IDs vorhanden
- Keine doppelten Stimulus-IDs / Item-Nummern
- Determinismus: derselbe QR liefert dasselbe Ergebnis
- Lookup robust gegen Groß-/Kleinschreibung und Whitespace
- Unbekannter QR liefert `null`, keine Fallback-Klassifikation
- Confidence-Werte liegen im gültigen Bereich [0,1]
- Dataset enthält exakt die erwarteten 2 Fehlklassifikationen + 3 Unclear-Fälle
- `toResponse()` leakt niemals die Ground Truth

`StudyLoggerAndSessionTest.kt`
- CSV-Zeilenformat entspricht exakt dem Spaltenschema (Feld-für-Feld-Prüfung)
- `isCorrect` vergleicht gegen Ground Truth, nicht gegen eine ggf. falsche Prototyp-Antwort (Testfall mit BG-WASTE-04)
- Unclear-Flag wird für beide Varianten korrekt ins Trial übernommen
- Variante A und B erhalten nachweislich dasselbe `StimulusResponse`-Objekt
- Variantenwechsel mitten in der Sitzung erhält Session-/Teilnehmer-ID
- Session-Reset erzeugt neue Session-ID und leert Trials

**Ausgeführt:** Ich konnte die Tests in dieser Umgebung **nicht tatsächlich mit Gradle ausführen** – hier steht kein Android SDK/Gradle-Toolchain zur Verfügung, nur eine reine Linux-Sandbox ohne `kotlinc`. Ich habe den Code stattdessen sorgfältig manuell auf Konsistenz geprüft (Signaturen, Feldreihenfolgen, Imports, Aufrufstellen) und bin zuversichtlich, dass er kompiliert – **bitte in Android Studio mit `./gradlew test` bestätigen**, bevor du dich darauf verlässt.

**Verbleibende Test-Lücken:**
- Keine Instrumented/Compose-UI-Tests (würden ein Android-Gerät/Emulator benötigen) – z. B. dass das ScanScreen-Overlay tatsächlich sichtbar wird oder dass CameraX korrekt bindet.
- Keine automatisierten Tests für den `QrAnalyzer` selbst (ML-Kit-Integration ist schwer ohne echtes Kamerabild zu testen).

---

## F. Manuelle Test-Checkliste

1. **App starten** → StartScreen sichtbar mit "♻️ WasteSort", Teilnehmer-Code-Feld, Interface-Umschalter.
2. **Variante A wählen** → "Studie starten" antippen → ScanScreen öffnet, Kamera-Berechtigung wird ggf. angefragt.
3. **Gültigen QR scannen** (z. B. `BG-WASTE-09.png` am Bildschirm zeigen oder ausgedruckt vorhalten) → "Karte erkannt ✓" erscheint kurz → automatische Weiterleitung zu ResultScreenA.
4. **Ergebnis prüfen**: Item-Name "Apfelrest", Empfehlung "Biotonne", Confidence 94% mit Balken sichtbar. Kein Foto-Thumbnail mehr vorhanden (entfällt).
5. **Ungültigen QR scannen** (z. B. `BG-WASTE-99` über manuelle Eingabe (⚙-Button) oder einen beliebigen Fremd-QR-Code) → Fehlermeldung "Unbekannte Stimulus-Karte" erscheint, App stürzt nicht ab, Scan setzt sich nach ca. 2 Sekunden automatisch zurück.
6. **Mit Variante B wiederholen**: Schritte 2–5 erneut, diesmal mit "Version B – Kindgerecht" ausgewählt.
7. **Kindgerechte Darstellung prüfen**: großes farbiges Tonnen-Icon, kurzer Text ("Rein in die braune Tonne!"), zusätzliches Namens-Badge sichtbar (nicht nur Farbe).
8. **Confidence-Darstellung prüfen**: bei BG-WASTE-09 sollte "✓ Ich bin mir ziemlich sicher!" erscheinen (94% ≥ 80%); bei BG-WASTE-19 oder -20 sollte stattdessen der Unclear-Zustand ("🤔 Ich bin nicht sicher. Frag einen Erwachsenen!") erscheinen.
9. **Unclear-Item verifizieren**: BG-WASTE-19 oder BG-WASTE-20 scannen → in Variante A erscheint "Low-confidence recommendation" statt der normalen Empfehlung; in Variante B der graue 🤔-Zustand.
10. **Logging verifizieren**: Nach mehreren Trials zur Zusammenfassung navigieren ("Sitzung beenden") → SummaryScreen zeigt Trial-Liste mit korrekten Item-Nummern/-Namen → "CSV exportieren/teilen" antippen → Datei sollte alle 15 Spalten enthalten (participant_id bis response_time) → per E-Mail/Drive teilen und Inhalt stichprobenartig gegen die Mapping-Tabelle (Abschnitt C) prüfen.

**Zusätzlich (Entwickler/Testleiter):**
- Long-Press auf "♻️ WasteSort"-Titel im StartScreen → DebugScreen öffnet sich mit allen 20 Mappings, Fehlklassifikationen/Unclear-Items sind farblich hervorgehoben.
- `tools/qr_codes/overview_sheet.png` ausdrucken und alle 20 Codes nacheinander scannen → jedes sollte zum jeweils korrekten Item aus der Tabelle in Abschnitt C führen.

---

## G. Offene Punkte / nicht erfundene Informationen

**Update:** Du hast mir zwischenzeitlich die offizielle "Sortierübersicht" zur Verfügung gestellt. Ich habe alle 20 Items dagegen abgeglichen (Details in Abschnitt C und im Verifikations-Kommentar am Kopf von `StimulusRepository.kt`). Die beiden zuvor offenen Punkte 1 sind damit **geklärt**:

- ~~BG-WASTE-20 (blaue Mineralwasserflasche)~~ → **Bestätigt**: Das Dokument sagt wortwörtlich "Blaues Glas kommt in den Grünglascontainer."
- ~~BG-WASTE-18 (Arzneimittelflasche)~~ → **Bestätigt**: "Arzneimittelflasche (Glas)" steht explizit im Glascontainer-Abschnitt. Zusätzlich musste ich präzisieren, dass es sich um die **Glas**-Variante handelt (Name jetzt "Braune Arzneimittelflasche (Glas)"), da das Dokument für die Kunststoff-Variante eine andere Tonne vorsieht (Gelbe Tonne) – reine Namensklarstellung, keine Fraktionsänderung.

Ebenfalls durch die Sortierübersicht bestätigt (vorher nur Konvention/Alltagswissen ohne Beleg): Item 1 (Keramik), 5 (Zeitung), 7 (Umschlag), 8 (Papiertüte), 9/10/11/12 (alle vier Bio-Items, teils wortgleich), 13/14 (Joghurtbecher/Alufolie, wortgleich), 15 (Tetra Pak, durch Analogie-Beispiele gestützt), 16 (Shampooflasche, mit Materialpräzisierung), 17 (Glasflasche allgemein + Farbregel).

**Weiterhin ungeklärt / bewusst nicht erfunden:**

1. **Für Items 2 (Klebeband), 3 (Windel) und 4 (CD/DVD) enthält auch die jetzt vorliegende Sortierübersicht keinen exakten Beleg.** Item 2 wird durch den nahen Beleg "Tesafilmroller" (Restmülltonne) gestützt; Items 3 und 4 bleiben auf etablierter bundesweiter Konvention (Restmüll), ohne expliziten BG-spezifischen Beleg im Dokument. Falls du eine Quelle hast, die diese explizit nennt, gerne nachreichen.
2. **Kein Wertstoffhof-Sonderfall unter den 20 Items.** Die Spezifikation erwähnt "Wertstoffhof / special disposal where applicable" als mögliche Kategorie – keines der 20 Items in der bestehenden Stimulus-Liste erforderte das, und auch die Sortierübersicht zeigt für keines der 20 Items einen Wertstoffhof-Sonderfall. Falls du das für die Studie brauchst, sag Bescheid.
3. **Formale AB/BA-Sitzungs-Gegenbalancierung nicht hart implementiert.** Dein Proposal (Kap. 5.5) sieht zwei vollständige Sitzungen pro Familie vor (eine pro Interface-Variante, Reihenfolge gegenbalanciert). Die App erlaubt aktuell jederzeit manuelles Umschalten der Variante (bestehendes Verhalten, unverändert übernommen), erzwingt aber keine Zwei-Sitzungen-Struktur oder AB/BA-Zuweisung pro Teilnehmer-Code. Das war in der vorherigen App-Version ebenfalls nicht vorhanden – ich habe es nicht neu erfunden, sondern nur den bestehenden Zustand beibehalten. Falls das für die Auswertung kritisch ist, wäre das eine separate Erweiterung.
4. **"Stimulus-Set"-Konzept (Set 1/Set 2) existierte in der Vorgängerversion nicht.** Da alle 20 Items in jeder Sitzung unabhängig von der Variante verfügbar sind (kein Subset pro Bedingung), gab es nichts zu bewahren oder zu migrieren. Falls dein Studiendesign tatsächlich zwei unterschiedliche Stimulus-Subsets vorsieht, fehlt mir diese Information komplett.
5. **QR-Fehlerkorrektur-Level H** (robust gegen Verschmutzung/Falten bei physischen Karten) wurde von mir gewählt, ohne dass du das explizit gefordert hast – reine technische Vorsichtsmaßnahme für den Druck. Falls ein anderes Format/Größe gewünscht ist, ist das in `tools/generate_qr_codes.py` leicht anpassbar (`box_size`-Parameter).
