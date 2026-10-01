package de.tou.wastesort.classifier

import android.content.Context
import android.net.Uri
import de.tou.wastesort.data.TrashAiResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Echter TrashAI-Client fuer die spaetere Integration.
 *
 * Setup:
 *  1. TrashAI lokal via Docker starten:
 *       docker run -p 5150:5150 -it code4sac/trashai:latest
 *  2. baseUrl auf die lokale IP setzen, z.B. "http://192.168.1.42:5150"
 *  3. In MainViewModel den MockClassifier durch diesen Client ersetzen.
 *
 * API-Endpunkt (TrashAI v1):
 *   POST /api/image/upload
 *   Body: multipart/form-data, Feld "image"
 *   Response: { "label": "...", "confidence": 0.85, ... }
 */
class TrashAiClient(
    private val context: Context,
    private val baseUrl: String = "http://10.0.2.2:5150"  // 10.0.2.2 = localhost im Emulator
) : WasteClassifier {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun classify(imageUri: Uri): TrashAiResult {
        return try {
            val stream = context.contentResolver.openInputStream(imageUri)
                ?: return fallback("Bild konnte nicht geoeffnet werden")
            val bytes = stream.readBytes()
            stream.close()

            val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "image.jpg", requestBody)
                .build()

            val request = Request.Builder()
                .url("$baseUrl/api/image/upload")
                .post(multipart)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return fallback("HTTP ${response.code}")

            val body = response.body?.string() ?: return fallback("Leere Antwort")
            parseResponse(body)

        } catch (e: Exception) {
            fallback("Fehler: ${e.message}")
        }
    }

    // ── JSON-Antwort parsen ───────────────────────────────────────────────────
    // TrashAI-Response-Format kann variieren – hier Standard-Parsing.
    // Ggf. an den tatsaechlichen API-Output anpassen.

    private fun parseResponse(json: String): TrashAiResult {
        return try {
            val obj = JSONObject(json)
            val label      = obj.optString("label", obj.optString("class", "unknown"))
            val confidence = obj.optDouble("confidence", obj.optDouble("score", 0.5)).toFloat()
            val allLabels  = mutableMapOf<String, Float>()

            // Falls TrashAI ein "predictions"-Array liefert
            if (obj.has("predictions")) {
                val preds = obj.getJSONArray("predictions")
                for (i in 0 until preds.length()) {
                    val pred = preds.getJSONObject(i)
                    allLabels[pred.optString("label")] = pred.optDouble("confidence").toFloat()
                }
            }

            TrashAiResult(label, confidence, allLabels)
        } catch (e: Exception) {
            fallback("JSON-Parse-Fehler: ${e.message}")
        }
    }

    private fun fallback(reason: String): TrashAiResult =
        TrashAiResult(rawLabel = "unknown", confidence = 0.0f,
            allLabels = mapOf("error" to 0f, "reason" to 0f))
}
