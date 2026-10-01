package de.tou.wastesort.classifier

import android.net.Uri
import de.tou.wastesort.data.TrashAiResult

// ─── Gemeinsames Interface ────────────────────────────────────────────────────
// Sowohl MockClassifier als auch echter Roboflow/TrashAI-Client implementieren
// dieses Interface. Um zwischen ihnen zu wechseln: im ViewModel austauschen.

interface WasteClassifier {
    /**
     * Klassifiziert ein per Kamera aufgenommenes Foto.
     * Wird fuer jedes Item im Studienablauf aufgerufen.
     */
    suspend fun classify(imageUri: Uri): TrashAiResult
}
