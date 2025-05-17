package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private lateinit var bleOpener: BleDoorOpener

    // 1) Состояние статуса здесь
    private var status by mutableStateOf("Ready")

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun hasBlePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }


    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (hasBlePermissions()) {
            openDoorImpl()
        } else {
            // Можно показывать тост, что без прав не получится
        }
    }

    private fun openDoor() {
        if (hasBlePermissions()) {
            openDoorImpl()
        } else {
            permLauncher.launch(requiredPermissions)
        }
    }

    private fun openDoorImpl() {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("BLE", "Starting connectAndOpen()")
            val success = bleOpener.connectAndOpen()
            Log.d("BLE", "Finished connectAndOpen(): $success")

            // 2) Обновляем наше Activity-состояние на главном потоке
            withContext(Dispatchers.Main) {
                status = if (success) "Дверь открыта" else "Ошибка открытия"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleOpener = BleDoorOpener(this)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    // 3) Передаём status в экран
                    DoorControlScreen(
                        status = status,
                        onOpenDoor = { openDoor() }
                    )
                }
            }
        }
    }
}

@Composable
fun DoorControlScreen(
    status: String,                // получили из Activity
    onOpenDoor: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Door Control", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onOpenDoor) {
            Text("Open Door")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Status: $status")   // просто читаем параметр
    }
}
