package com.example.carbotapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    // Launcher para abrir el MasterChooser y recibir el master URI
    private val masterChooserLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val masterUri: String? = data?.getStringExtra("ROS_MASTER_URI")
                if (masterUri != null) {
                    RosManager.connect(this, masterUri)
                    Toast.makeText(this, "Conectado a $masterUri", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Sin ROS_MASTER_URI", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun openMasterChooser() {
        val intent = Intent(this, org.ros.android.MasterChooser::class.java)
        masterChooserLauncher.launch(intent)
    }

    private fun disconnectRos() {
        RosManager.disconnect(this)
        Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ControlScreen(
                    onConnect = { openMasterChooser() },
                    onDisconnect = { disconnectRos() },
                    onPublish = { lin, ang -> RosManager.setVelocity(lin, ang) }
                )
            }
        }
    }
}

/* ---------------- UI ---------------- */

@Composable
fun ControlScreen(
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onPublish: (Double, Double) -> Unit
) {
    var connected by remember { mutableStateOf(false) }
    var lin by remember { mutableStateOf(0f) }   // -1..+1
    var ang by remember { mutableStateOf(0f) }   // -1..+1

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onConnect(); connected = true },
                enabled = !connected,
                modifier = Modifier.weight(1f)
            ) { Text("Conectar (MasterChooser)") }

            OutlinedButton(
                onClick = { onDisconnect(); connected = false },
                enabled = connected,
                modifier = Modifier.weight(1f)
            ) { Text("Desconectar") }
        }

        Spacer(Modifier.height(16.dp))
        Text("Aceleración (linear.x) = %.2f".format(lin))
        Slider(
            value = lin,
            onValueChange = {
                lin = it
                if (connected) onPublish(lin.toDouble(), ang.toDouble())
            },
            valueRange = -1f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text("Giro (angular.z) = %.2f".format(ang))
        Slider(
            value = ang,
            onValueChange = {
                ang = it
                if (connected) onPublish(lin.toDouble(), ang.toDouble())
            },
            valueRange = -1f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                lin = 0f; ang = 0f
                if (connected) onPublish(0.0, 0.0)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("E-STOP") }
    }
}