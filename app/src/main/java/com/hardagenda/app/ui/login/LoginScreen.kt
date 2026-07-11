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
import androidx.compose.ui.text.style.TextAlign
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

    var serverUrl by remember { mutableStateOf("") }
    var dbName by remember { mutableStateOf("hardagenda_db") }
    var dbUser by remember { mutableStateOf("postgres") }
    var dbPass by remember { mutableStateOf("") }
    var createDb by remember { mutableStateOf(false) }
    var createTables by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (PrefsManager.hasSavedConfig(context)) {
            val cfg = PrefsManager.loadConfig(context)
            serverUrl = cfg.serverUrl
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

        Text("URL del servidor:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("http://192.168.0.82:8080") }
        )
        Text(
            text = "Ejecuta server.py en tu PC y pone la IP de la PC",
            style = MaterialTheme.typography.labelSmall,
            color = GrayText,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp)
        )

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
        CheckboxRow("Crear tabla", createTables) { createTables = it }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        isLoading = true
                        val url = serverUrl.trim()
                        if (url.isBlank()) {
                            errorMessage = "Ingresa la URL del servidor"
                            isLoading = false
                            return@launch
                        }

                        ApiClient.serverUrl = url
                        ApiClient.dbUser = dbUser.trim()
                        ApiClient.dbPass = dbPass
                        ApiClient.dbName = dbName.trim()

                        val testResult = ApiClient.testConnection()
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

                        if (createTables) {
                            val result = TurnoRepository.crearTablas()
                            result.onSuccess { infoMessage = it }
                            result.onFailure { errorMessage = it.message; isLoading = false; return@launch }
                        }

                        PrefsManager.saveConfig(context, url, dbName.trim(), dbUser.trim(), dbPass)
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
            enabled = !isLoading && serverUrl.isNotBlank(),
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
