package ca.georgebrown.studygroupfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ca.georgebrown.studygroupfinder.ui.navigation.AppNavHost
import ca.georgebrown.studygroupfinder.ui.theme.StudyGroupTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // the root Scaffold pads content away from the status and nav bars
        val container = (application as StudyGroupApp).container
        setContent {
            StudyGroupTheme {
                AppNavHost(container = container)
            }
        }
    }
}
