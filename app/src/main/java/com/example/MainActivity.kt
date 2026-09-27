package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.example.ui.AishaHomeScreen
import com.example.ui.theme.AishaTheme
import com.example.viewmodel.AishaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AishaViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.onMicTapped(hasRecordAudioPermission = true) {}
        } else {
            viewModel.onPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AishaTheme {
                val assistantState by viewModel.assistantState.collectAsState()
                val aiStatus by viewModel.aiStatus.collectAsState()
                val messages by viewModel.messages.collectAsState()
                val rmsdB by viewModel.rmsdB.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                // Listen for snackbar events
                LaunchedEffect(Unit) {
                    viewModel.snackbarMessages.collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                AishaHomeScreen(
                    assistantState = assistantState,
                    aiStatus = aiStatus,
                    messages = messages,
                    rmsdB = rmsdB,
                    snackbarHostState = snackbarHostState,
                    onMicClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        viewModel.onMicTapped(
                            hasRecordAudioPermission = hasPermission,
                            onRequestPermission = {
                                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        )
                    },
                    onSendMessage = { text ->
                        viewModel.processInput(text)
                    },
                    onQuickCommand = { cmd ->
                        viewModel.processInput(cmd)
                    }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.onActivityStop()
    }
}
