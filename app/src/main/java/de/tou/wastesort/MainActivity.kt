package de.tou.wastesort

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.tou.wastesort.ui.navigation.AppNavigation
import de.tou.wastesort.ui.theme.WasteSortTheme

/**
 * Einzige Activity der App. Kein Login, kein Participant-Setup.
 * Navigation und State werden komplett durch AppNavigation + MainViewModel verwaltet.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WasteSortTheme {
                AppNavigation()
            }
        }
    }
}
