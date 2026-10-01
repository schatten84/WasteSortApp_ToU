package de.tou.wastesort.classifier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import de.tou.wastesort.data.TrashAiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Echter Roboflow-Client fuer das TrashAI-Modell (trash-ai/4).
 *
 * API-Details (Roboflow Serverless Inference):
 *   URL:      https://serverless.roboflow.com/trash-ai/4
 *   API-Key:  2EYPda52JyzvQjBnKYkz
 *   Methode:  POST
 *   Body:     Base64-kodiertes JPEG (Content-Type: application/x-www-form-urlencoded)
 *   Antwort:  JSON mit "predictions"-Array
 *
 * Hinweis fuer die Thesis:
 *   Fuer Test-Items ohne echtes Foto wird der MockClassifier verwendet.
 *   Fuer Kamera-/Galerie-Bilder wird dieser Client aufgerufen.
 *   Der API-Key ist fuer den Forschungsprototyp akzeptabel;
 *   in einer Produktions-App wuerde er in BuildConfig oder einem gesicherten
 *   Backend-Proxy gespeichert werden.
 */
class RoboflowTrashAiClient(
    private val context: Context
) : WasteClassifier {

    companion object {
        private const val TAG        = "RoboflowClient"
        private const val API_URL    = "https://serverless.roboflow.com"
        private const val MODEL_ID   = "trash-ai/4"
        private const val API_KEY    = "2EYPda52JyzvQjBnKYkz"
        private const val MAX_SIZE   = 640   // px – Roboflow empfiehlt <= 640 x 640
        private const val JPEG_QUAL  = 85    // Kompression: Qualitaet vs. Upload-Groesse
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Klassifikation per URI (Kamera oder Galerie) ──────────────────────────

    override suspend fun classify(imageUri: Uri): TrashAiResult = withContext(Dispatchers.IO) {
        try {
            val bytes = prepareImage(imageUri)
            callRoboflow(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "classify() Fehler: ${e.message}", e)
            TrashAiResult(rawLabel = "unknown", confidence = 0f)
        }
    }

    // ── Bild vorbereiten: skalieren + als JPEG in Base64 umwandeln ────────────

    private fun prepareImage(uri: Uri): ByteArray {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Bildstream konnte nicht geoeffnet werden: $uri")

        val original = BitmapFactory.decodeStream(stream)
        stream.close()

        val scaled = scaleBitmap(original, MAX_SIZE)
        return bitmapToJpeg(scaled)
    }

    private fun scaleBitmap(bmp: Bitmap, maxPx: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        if (w <= maxPx && h <= maxPx) return bmp
        val ratio = maxPx.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bmp, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    private fun bitmapToJpeg(bmp: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUAL, out)
        return out.toByteArray()
    }

    // ── Roboflow-API aufrufen ─────────────────────────────────────────────────

    private fun callRoboflow(imageBytes: ByteArray): TrashAiResult {
        val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val body = base64.toRequestBody(
            "application/x-www-form-urlencoded".toMediaTypeOrNull()
        )
        val request = Request.Builder()
            .url("$API_URL/$MODEL_ID?api_key=$API_KEY")
            .post(body)
            .build()

        val response = http.newCall(request).execute()

        if (!response.isSuccessful) {
            Log.w(TAG, "HTTP ${response.code}: ${response.message}")
            return TrashAiResult(rawLabel = "unknown", confidence = 0f)
        }

        val json = response.body?.string()
            ?: return TrashAiResult(rawLabel = "unknown", confidence = 0f)

        Log.d(TAG, "Roboflow-Antwort: $json")
        return parseResponse(json)
    }

    // ── JSON-Antwort parsen ───────────────────────────────────────────────────
    //
    // Roboflow-Antwortformat (Klassifikation, trash-ai/4):
    // {
    //   "time": 0.045,
    //   "image": { "width": 640, "height": 480 },
    //   "predictions": [
    //     { "class": "plastic",   "confidence": 0.8723 },
    //     { "class": "paper",     "confidence": 0.0654 },
    //     { "class": "cardboard", "confidence": 0.0312 }
    //   ]
    // }
    //
    // Falls das Modell Object Detection liefert (mit x/y/width/height):
    // Das Array wird analog verarbeitet – hoeChste Confidence gewinnt.

    private fun parseResponse(json: String): TrashAiResult {
        return try {
            val root = JSONObject(json)
            val preds = root.optJSONArray("predictions")
                ?: return TrashAiResult(rawLabel = "unknown", confidence = 0f)

            val allLabels = mutableMapOf<String, Float>()
            var topLabel  = "unknown"
            var topConf   = 0f

            for (i in 0 until preds.length()) {
                val pred = preds.getJSONObject(i)
                val cls  = pred.optString("class", "unknown")
                val conf = pred.optDouble("confidence", 0.0).toFloat()
                allLabels[cls] = conf
                if (conf > topConf) {
                    topConf   = conf
                    topLabel  = cls
                }
            }

            Log.d(TAG, "Top-Label: $topLabel (${"%.0f".format(topConf * 100)} %)")
            TrashAiResult(
                rawLabel   = topLabel,
                confidence = topConf,
                allLabels  = allLabels
            )

        } catch (e: Exception) {
            Log.e(TAG, "JSON-Parse-Fehler: ${e.message}")
            TrashAiResult(rawLabel = "unknown", confidence = 0f)
        }
    }
}
