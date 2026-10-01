package de.tou.wastesort.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.tou.wastesort.data.InterfaceVariant
import de.tou.wastesort.ui.screens.BinSelectionScreen
import de.tou.wastesort.ui.screens.DebugScreen
import de.tou.wastesort.ui.screens.ExportScreen
import de.tou.wastesort.ui.screens.ResultScreenA
import de.tou.wastesort.ui.screens.ResultScreenB
import de.tou.wastesort.ui.screens.ScanScreen
import de.tou.wastesort.ui.screens.StartScreen
import de.tou.wastesort.ui.screens.SummaryScreen
import de.tou.wastesort.viewmodel.MainViewModel

// ── Routen-Konstanten ─────────────────────────────────────────────────────────

object Routes {
    const val START          = "start"
    const val SCAN           = "scan"
    const val RESULT         = "result"
    const val BIN_SELECTION  = "bin_selection"
    const val SUMMARY        = "summary"
    const val EXPORT         = "export"
    const val DEBUG          = "debug"
}

// ── Navigations-Graph ─────────────────────────────────────────────────────────

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel()
) {
    val variant by viewModel.currentVariant.collectAsState()
    val currentResponse by viewModel.currentResponse.collectAsState()

    NavHost(navController = navController, startDestination = Routes.START) {

        // ── StartScreen ───────────────────────────────────────────────────────
        composable(Routes.START) {
            StartScreen(
                currentVariant   = variant,
                onVariantChange  = viewModel::switchVariant,
                onParticipantIdChange = viewModel::setParticipantId,
                onStart = {
                    viewModel.startNewSession()
                    navController.navigate(Routes.SCAN)
                },
                onOpenDebug = { navController.navigate(Routes.DEBUG) }
            )
        }

        // ── DebugScreen (Testleiter-Verifikation, nicht Teil der Teilnehmer-UI) ─
        composable(Routes.DEBUG) {
            DebugScreen(onBack = { navController.popBackStack() })
        }

        // ── ScanScreen (QR-Code scannen) ─────────────────────────────────────
        composable(Routes.SCAN) {
            ScanScreen(
                trialCount         = viewModel.trialCount,
                totalItems         = viewModel.totalItems,
                scannedStimulusIds = viewModel.scannedStimulusIds,
                currentVariant     = variant,
                onVariantChange    = viewModel::switchVariant,
                onLookup           = viewModel::lookupStimulus,
                onStimulusFound    = { item ->
                    viewModel.onStimulusFound(item)
                    navController.navigate(Routes.RESULT)
                },
                onFinishSession    = { navController.navigate(Routes.SUMMARY) }
            )
        }

        // ── ResultScreen (A oder B je nach Variante) ──────────────────────────
        composable(Routes.RESULT) {
            currentResponse?.let { response ->
                if (variant == InterfaceVariant.VERSION_A) {
                    ResultScreenA(
                        response        = response,
                        onVariantChange = viewModel::switchVariant,
                        onProceed       = { navController.navigate(Routes.BIN_SELECTION) }
                    )
                } else {
                    ResultScreenB(
                        response        = response,
                        onVariantChange = viewModel::switchVariant,
                        onProceed       = { navController.navigate(Routes.BIN_SELECTION) }
                    )
                }
            }
        }

        // ── BinSelectionScreen (Kind waehlt Tonne) ────────────────────────────
        composable(Routes.BIN_SELECTION) {
            BinSelectionScreen(
                currentVariant  = variant,
                onVariantChange = viewModel::switchVariant,
                onFractionSelected = { fraction ->
                    viewModel.recordDecision(fraction)
                    navController.navigate(Routes.SCAN) {
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                }
            )
        }

        // ── SummaryScreen ─────────────────────────────────────────────────────
        composable(Routes.SUMMARY) {
            SummaryScreen(
                session         = viewModel.currentSession,
                onExport        = { navController.navigate(Routes.EXPORT) },
                onNewSession    = {
                    viewModel.startNewSession()
                    navController.navigate(Routes.START) {
                        popUpTo(Routes.START) { inclusive = true }
                    }
                }
            )
        }

        // ── ExportScreen ──────────────────────────────────────────────────────
        composable(Routes.EXPORT) {
            ExportScreen(
                session         = viewModel.currentSession,
                onShare         = { viewModel.getShareIntent() },
                onBack          = { navController.popBackStack() }
            )
        }
    }
}
