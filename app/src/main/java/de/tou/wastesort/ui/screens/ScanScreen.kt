package de.tou.wastesort.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import de.tou.wastesort.data.InterfaceVariant
import de.tou.wastesort.data.StimulusItem
import de.tou.wastesort.qr.QrAnalyzer
import java.util.concurrent.Executors

/**
 * ScanScreen: Kamera-Vorschau + Live-QR-Erkennung fuer die 20 physischen
 * Stimulus-Karten. Ersetzt die frueheren CaptureScreen/TrialScreen (Live-Foto
 * an TrashAI) vollstaendig - der QR-Code klassifiziert nichts, er ist nur ein
 * Schluessel in StimulusRepository (siehe Spezifikation Punkt 2-4).
 *
 * Ablauf: Kamera oeffnet automatisch → QR erkannt → kurzes "Karte erkannt ✓"
 * Feedback → Weiterleitung zum Ergebnis-Screen. Nach einer Erkennung wird die
 * Analyse pausiert (kein Mehrfach-Trigger waehrend der Bestaetigungsanzeige).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    trialCount:        Int,
    totalItems:        Int,
    scannedStimulusIds: Set<String>,
    currentVariant:    InterfaceVariant,
    onVariantChange:   (InterfaceVariant) -> Unit,
    onLookup:          (String) -> StimulusItem?,
    onStimulusFound:   (StimulusItem) -> Unit,
    onFinishSession:   () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // scanning = false waehrend das Erfolgs-/Fehler-Feedback angezeigt wird,
    // damit derselbe QR-Code nicht mehrfach hintereinander ausgeloest wird.
    var scanning by remember { mutableStateOf(true) }
    var feedbackState by remember { mutableStateOf<ScanFeedback?>(null) }
    var manualEntryVisible by remember { mutableStateOf(false) }
    var manualEntryText by remember { mutableStateOf("") }

    fun handleDetectedCode(rawValue: String) {
        if (!scanning) return
        scanning = false
        val item = onLookup(rawValue)
        if (item != null) {
            feedbackState = ScanFeedback.Found(item)
        } else {
            feedbackState = ScanFeedback.Unknown(rawValue)
        }
    }

    // Erfolgs-Feedback kurz anzeigen, dann zum Ergebnis-Screen weiterleiten
    LaunchedEffect(feedbackState) {
        when (val fb = feedbackState) {
            is ScanFeedback.Found -> {
                kotlinx.coroutines.delay(650L)
                onStimulusFound(fb.item)
            }
            is ScanFeedback.Unknown -> {
                kotlinx.coroutines.delay(2200L)
                feedbackState = null
                scanning = true
            }
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Karte ${(trialCount + 1).coerceAtMost(totalItems)} von $totalItems") },
                actions = {
                    VariantToggle(
                        currentVariant  = currentVariant,
                        onVariantChange = onVariantChange,
                        modifier        = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (hasPermission) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val executor = Executors.newSingleThreadExecutor()

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val analysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    val mainExecutor = ContextCompat.getMainExecutor(context)
                                    it.setAnalyzer(executor, QrAnalyzer { rawValue ->
                                        // Callback kommt aus Hintergrund-Thread -> zuverlaessig auf Main posten
                                        mainExecutor.execute { handleDetectedCode(rawValue) }
                                    })
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis
                                )
                            } catch (_: Exception) {
                                // Kamera evtl. bereits gebunden/nicht verfuegbar -> UI zeigt weiterhin Hinweistext
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                // Sucher-Rahmen zur Orientierung
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(240.dp)
                        .border(3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📷", fontSize = 56.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Kamera-Berechtigung wird fuer den QR-Scan benoetigt.",
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Berechtigung erteilen")
                    }
                }
            }

            // ── Hinweistext oben ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text     = "Zeige den QR-Code der Stimulus-Karte in den Rahmen",
                    color    = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            // ── Feedback-Overlay ────────────────────────────────────────────────
            feedbackState?.let { fb ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(shape = RoundedCornerShape(20.dp)) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (fb) {
                                is ScanFeedback.Found -> {
                                    Text("✅", fontSize = 48.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Karte erkannt", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                is ScanFeedback.Unknown -> {
                                    if (currentVariant == InterfaceVariant.VERSION_B) {
                                        Text("😕", fontSize = 48.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Kenn ich nicht!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Probier eine andere Karte", fontSize = 14.sp, textAlign = TextAlign.Center)
                                    } else {
                                        Text("⚠️", fontSize = 40.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Unbekannte Stimulus-Karte", fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Dieser QR-Code gehoert nicht zum Studien-Set. Bitte eine andere Karte scannen.",
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Sitzung-beenden-Button + dezenter Testleiter-Bereich ─────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (manualEntryVisible) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Testleiter: Stimulus-ID manuell eingeben", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = manualEntryText,
                                    onValueChange = { manualEntryText = it },
                                    placeholder = { Text("BG-WASTE-09") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = {
                                    if (manualEntryText.isNotBlank()) {
                                        handleDetectedCode(manualEntryText.trim())
                                        manualEntryText = ""
                                    }
                                }) { Text("Los") }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick  = onFinishSession,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Sitzung beenden", fontSize = 14.sp)
                    }
                    TextButton(
                        onClick = { manualEntryVisible = !manualEntryVisible },
                        colors  = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(0.7f))
                    ) {
                        Text("⚙", fontSize = 18.sp)
                    }
                }

                if (trialCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text     = "$trialCount von $totalItems Karten gescannt",
                        fontSize = 12.sp,
                        color    = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

private sealed class ScanFeedback {
    data class Found(val item: StimulusItem) : ScanFeedback()
    data class Unknown(val rawValue: String) : ScanFeedback()
}
