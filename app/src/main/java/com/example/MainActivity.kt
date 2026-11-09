package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.data.repository.AiMockGeneratorRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.ExamRepository
import com.example.data.repository.MockRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.screens.auth.SignInScreen
import com.example.ui.theme.FutureAirmenTheme
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory App Check configuration with launch intent before UI/Firebase
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            FutureAirmenTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val authRepository = remember { AuthRepository(context) }
    val mockRepository = remember { MockRepository(context) }
    val examRepository = remember { ExamRepository(context) }
    val aiRepository = remember { AiMockGeneratorRepository() }

    val currentUser by authRepository.currentUser.collectAsState()

    // Attempt silent auto-sign in on startup if previous session exists
    LaunchedEffect(Unit) {
        authRepository.silentAutoSignIn(scope)
    }

    if (currentUser == null) {
        SignInScreen(
            authRepository = authRepository,
            onAuthSuccess = {
                // AuthStateListener will update currentUser automatically
            }
        )
    } else {
        AppNavigation(
            authRepository = authRepository,
            mockRepository = mockRepository,
            examRepository = examRepository,
            aiRepository = aiRepository
        )
    }
}
