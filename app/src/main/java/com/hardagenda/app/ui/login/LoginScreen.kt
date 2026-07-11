package com.hardagenda.app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hardagenda.app.data.ApiClient
import com.hardagenda.app.data.TurnoRepository
import com.hardagenda.app.ui.components.ErrorDialog
import com.hardagenda.app.ui.components.InfoDialog
import com.hardagenda.app.ui.theme.GreenDark
import com.hardagenda.app.ui.theme.GrayText
import com.hardagenda.app.util.PrefsManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverIp by remember { mutableStateOf("") }
    var serverPort by remember { mutableStateOf("8080") }
    var dbName by remember { mutableStateOf("hardagenda_db") }
    var dbUser by remember { mutableStateOf("postgres") }
    var dbPass by remember { mutableStateOf("") }
    var createDb by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (PrefsManager.hasSavedConfig(context)) {
            val cfg = PrefsManager.loadConfig(context)
            serverIp = cfg.ip
            serverPort = cfg.port
            dbName = cfg.dbName
            dbUser = cfg.dbUser
            dbPass = cfg.dbPass
        }
    }

    if (errorMessage != null) {
        ErrorDialog(message = errorMessage!!) { errorMessage = null }
    }
    if (infoMessage != null) {
        InfoDialog(title = "HardAgenda", message = infoMessage!!) { infoMessage = null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "HardAgenda",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = GreenDark,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Configuracion del servidor",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayText,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("IP del servidor:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = serverIp,
            onValueChange = { serverIp = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("192.168.0.82") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Puerto:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = serverPort,
            onValueChange = { serverPort = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("8080") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Nombre de la base de datos:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = dbName,
            onValueChange = { dbName = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("hardagenda_db") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Usuario de PostgreSQL:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = dbUser,
            onValueChange = { dbUser = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("postgres") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Contrasena de PostgreSQL:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = dbPass,
            onValueChange = { dbPass = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            placeholder = { Text("********") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        CheckboxRow("Crear base de datos", createDb) { createDb = it }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        isLoading = true
                        val ip = serverIp.trim()
                        if (ip.isBlank()) {
                            errorMessage = "Ingresa la IP del servidor"
                            isLoading = false
                            return@launch
                        }

                        ApiClient.configure(ip, serverPort, dbUser, dbPass, dbName)

                        val testResult = ApiClient.testConnection(dbName = "postgres")
                        if (testResult.isFailure) {
                            errorMessage = testResult.exceptionOrNull()?.message ?: "No se pudo conectar al servidor"
                            isLoading = false
                            return@launch
                        }

                        if (createDb) {
                            val result = TurnoRepository.crearBaseDeDatos()
                            result.onSuccess { infoMessage = it }
                            result.onFailure { errorMessage = it.message; isLoading = false; return@launch }
                        }

                        val connectResult = ApiClient.testConnection()
                        if (connectResult.isFailure) {
                            errorMessage = "No se pudo conectar a la base de datos '${dbName}'. Verifica que exista o marca 'Crear base de datos'."
                            isLoading = false
                            return@launch
                        }

                        val tablasResult = TurnoRepository.crearTablas()
                        tablasResult.onFailure { errorMessage = it.message; isLoading = false; return@launch }

                        PrefsManager.saveConfig(context, ip, serverPort, dbName.trim(), dbUser.trim(), dbPass)
                        isLoading = false
                        onLoginSuccess()
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message ?: e.javaClass.simpleName}"
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isLoading && serverIp.isNotBlank() && dbUser.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Iniciar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CheckboxRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
