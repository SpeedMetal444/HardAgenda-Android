package com.hardagenda.app.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hardagenda.app.data.TurnoRepository
import com.hardagenda.app.ui.components.ErrorDialog
import com.hardagenda.app.ui.components.InfoDialog
import com.hardagenda.app.ui.theme.GreenDark
import com.hardagenda.app.ui.theme.GrayText
import com.hardagenda.app.util.PrefsManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoTurnoTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var obraSocial by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(LocalTime.now().hour) }
    var minute by remember { mutableIntStateOf(LocalTime.now().minute) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    if (errorMessage != null) {
        ErrorDialog(message = errorMessage!!) { errorMessage = null }
    }
    if (infoMessage != null) {
        InfoDialog(title = "Registrado", message = infoMessage!!) {
            infoMessage = null
            nombre = ""
            apellido = ""
            dni = ""
            obraSocial = ""
            motivo = ""
            selectedDate = LocalDate.now()
            hour = LocalTime.now().hour
            minute = LocalTime.now().minute
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        FormField("Nombre:", nombre, { nombre = it }, "Ingrese nombre")
        FormField("Apellido:", apellido, { apellido = it }, "Ingrese apellido")
        FormField("DNI:", dni, { dni = it }, "Ingrese DNI")
        FormField("Obra Social:", obraSocial, { obraSocial = it }, "Ingrese obra social")
        FormField("Motivo de consulta:", motivo, { motivo = it }, "Ingrese motivo de consulta")

        Spacer(modifier = Modifier.height(8.dp))

        Text("Fecha del turno:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(onClick = { showDatePicker = true }) {
            Text(selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Hora del turno:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = hour.toString().padStart(2, '0'),
                onValueChange = { h -> h.toIntOrNull()?.let { if (it in 0..23) hour = it } },
                modifier = Modifier.width(60.dp),
                singleLine = true
            )
            Text(" : ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = minute.toString().padStart(2, '0'),
                onValueChange = { m -> m.toIntOrNull()?.let { if (it in 0..59) minute = it } },
                modifier = Modifier.width(60.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (nombre.isBlank() || apellido.isBlank() || dni.isBlank()) {
                    errorMessage = "Nombre, apellido y DNI son obligatorios"
                    return@Button
                }
                scope.launch {
                    val fechaHora = LocalDateTime.of(selectedDate, LocalTime.of(hour, minute))
                    val usuario = PrefsManager.usuarioActual(context)
                    val result = TurnoRepository.agregarTurno(
                        nombre.trim(), apellido.trim(), dni.trim(),
                        obraSocial.trim().ifBlank { null },
                        motivo.trim().ifBlank { null },
                        usuario, selectedDate, fechaHora
                    )
                    result.onSuccess {
                        TurnoRepository.registrarCambio(
                            "turnos", null, "Alta",
                            "${nombre.trim()} ${apellido.trim()} (DNI: ${dni.trim()}) - ${fechaHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}",
                            usuario, dni.trim(), nombre.trim(), apellido.trim()
                        )
                        infoMessage = "Turno registrado con exito"
                    }
                    result.onFailure { errorMessage = it.message }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
        ) {
            Text("Registrar turno", fontWeight = FontWeight.Bold)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = LocalDate.ofEpochDay(it / 86400000L)
                    }
                    showDatePicker = false
                }) { Text("OK", color = GreenDark) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = GrayText)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder) }
        )
    }
}
