package de.tou.wastesort.classifier

import android.net.Uri
import de.tou.wastesort.data.TrashAiResult
import kotlinx.coroutines.delay

/**
 * Mock-Classifier fuer Offline-Entwicklung/Tests ohne Netzwerkzugriff.
 * Gibt fuer jedes Foto ein festes, plausibles TrashAI-Ergebnis zurueck.
 *
 * Aktuell wird im Studienbetrieb (STUDY_MODE = "qr_predefined") weder dieser
 * Classifier noch der echte RoboflowTrashAiClient verwendet: die Klassifikation
 * kommt ausschliesslich aus dem QR-Code-Lookup gegen die 20 fest hinterlegten
 * Items in data/StimulusRepository.kt (echte TrashAI-Ergebnisse waren zu
 * ungenau fuer die Thesis-Auswertung). Dieses File bleibt als Referenz/fuer
 * moegliche spaetere Live-Tests erhalten - siehe WasteClassifier-Interface.
 */
class MockClassifier : WasteClassifier {

    // Simuliert Netzwerklatenz (realistisch fuer Benutzertests)
    private val simulatedDelayMs = 800L

    override suspend fun classify(imageUri: Uri): TrashAiResult {
        delay(simulatedDelayMs)
        return TrashAiResult(
            rawLabel   = "plastic",
            confidence = 0.72f,
            allLabels  = mapOf("plastic" to 0.72f, "bottle" to 0.15f, "container" to 0.13f)
        )
    }
}
