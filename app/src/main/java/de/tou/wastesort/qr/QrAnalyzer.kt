package de.tou.wastesort.qr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX-ImageAnalysis.Analyzer, der jedes Kameraframe auf QR-Codes prueft.
 *
 * onQrDetected wird auf jedem erkannten QR-Code-Text aufgerufen (Rohtext,
 * z.B. "BG-WASTE-09"). Der Aufrufer ist fuer Debouncing zustaendig (siehe
 * ScanScreen.kt), damit ein einmal erkannter Code nicht mehrfach pro Sekunde
 * denselben Callback ausloest.
 */
class QrAnalyzer(
    private val onQrDetected: (String) -> Unit
) : androidx.camera.core.ImageAnalysis.Analyzer {

    // Nur QR-Codes scannen (keine anderen Barcode-Formate noetig)
    private val scanner = BarcodeScanning.getClient(
        com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let { value ->
                    onQrDetected(value)
                }
            }
            .addOnCompleteListener {
                // Frame IMMER schliessen, sonst blockiert die Kamera-Pipeline
                imageProxy.close()
            }
    }
}
