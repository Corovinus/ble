package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myapplication.Controller.States
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var bleOpener: BleDoorOpener
    private var status by mutableStateOf("Ready")

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun hasBlePermissions() =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasBlePermissions()) performState(lastRequestedState, lastActionName)
    }

    private var lastRequestedState: States = States.DoorLockOpen
    private var lastActionName: String = ""

    private fun performState(state: States, actionName: String) {
        lastRequestedState = state
        lastActionName = actionName
        if (!hasBlePermissions()) {
            permLauncher.launch(requiredPermissions)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) { status = "$actionName: connecting…" }
            val success = bleOpener.sendState(state)
            withContext(Dispatchers.Main) {
                status = if (success) "$actionName: OK" else "$actionName: Error"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleOpener = BleDoorOpener(this)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Door & Light Control", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(24.dp))
                        Row {
                            Button(onClick = { performState(States.DoorLockOpen, "Open Door") }) {
                                Text("Open Door")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { performState(States.DoorLockClose, "Close Door") }) {
                                Text("Close Door")
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row {
                            Button(onClick = { performState(States.LightOn, "Light On") }) {
                                Text("Light On")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { performState(States.LightOff, "Light Off") }) {
                                Text("Light Off")
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Status: $status")
                    }
                }
            }
        }
    }
}
