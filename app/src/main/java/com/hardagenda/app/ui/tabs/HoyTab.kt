package com.hardagenda.app.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hardagenda.app.data.TurnoRepository
import com.hardagenda.app.data.model.Turno
import com.hardagenda.app.ui.components.ConfirmDialog
import com.hardagenda.app.ui.components.ErrorDialog
import com.hardagenda.app.ui.components.TurnoAtendidoCard
import com.hardagenda.app.ui.components.TurnoCard
import com.hardagenda.app.ui.theme.*
import com.hardagenda.app.util.PrefsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HoyTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var turnos by remember { mutableStateOf<List<Turno>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showReprogramarDialog by remember { mutableStateOf<Turno?>(null) }
    var showConfirmAvanzar by remember { mutableStateOf<Turno?>(null) }

    val pendientes = turnos.filter { it.estado == "pendiente" }
    val atendidos = turnos.filter { it.estado == "atendido" }

    fun cargarTurnos() {
        scope.launch {
            isLoading = true
            val result = TurnoRepository.obtenerTurnosDelDia()
            result.onSuccess { turnos = it }
            result.onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        cargarTurnos()
        while (true) {
            delay(60000)
            cargarTurnos()
        }
    }

    if (errorMessage != null) {
        ErrorDialog(message = errorMessage!!) { errorMessage = null }
    }

    showConfirmAvanzar?.let { turno ->
        ConfirmDialog(
            title = "Avanzar turno",
            message = "Marcar como atendido a ${turno.nombre} ${turno.apellido}?",
            onConfirm = {
                scope.launch {
                    val result = TurnoRepository.avanzarTurno(turno.id)
                    result.onSuccess {
                        TurnoRepository.registrarCambio(
                            "turnos", turno.id, "Atendido",
                            "${turno.nombre} ${turno.apellido} (DNI: ${turno.dni})",
                            PrefsManager.usuarioActual(context),
                            turno.dni, turno.nombre, turno.apellido
                        )
                        cargarTurnos()
                    }
                    result.onFailure { errorMessage = it.message }
                }
                showConfirmAvanzar = null
            },
            onDismiss = { showConfirmAvanzar = null }
        )
    }

    showReprogramarDialog?.let { turno ->
        ReprogramarDialog(
            turno = turno,
            onDismiss = { showReprogramarDialog = null },
            onConfirm = { nuevaFecha, nuevaHora ->
                scope.launch {
                    val result = TurnoRepository.reprogramarTurno(turno.id, nuevaFecha, nuevaHora)
                    result.onSuccess {
                        TurnoRepository.registrarCambio(
                            "turnos", turno.id, "Reprogramado",
                            "${turno.nombre} ${turno.apellido} (DNI: ${turno.dni}) -> ${nuevaFecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} ${nuevaHora.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                            PrefsManager.usuarioActual(context),
                            turno.dni, turno.nombre, turno.apellido
                        )
                        cargarTurnos()
                    }
                    result.onFailure { errorMessage = it.message }
                }
                showReprogramarDialog = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Turnos del dia - ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GreenDark,
            modifier = Modifier.padding(16.dp)
        )

        if (isLoading && turnos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenDark)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (pendientes.isEmpty() && atendidos.isEmpty()) {
                    item {
                        Text(
                            text = "No hay turnos para el dia de hoy",
                            style = MaterialTheme.typography.bodyLarge,
                            color = GrayText,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                items(pendientes) { turno ->
                    val isActual = turno == pendientes.firstOrNull()
                    TurnoCard(
                        turno = turno,
                        isActual = isActual,
                        onClick = { showConfirmAvanzar = turno },
                        onLongClick = { showReprogramarDialog = turno }
                    )
                }

                if (atendidos.isNotEmpty()) {
                    item {
                        Text(
                            text = "--- Ya atendidos ---",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrayLight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }
                    items(atendidos) { turno ->
                        TurnoAtendidoCard(turno = turno)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Turnos: ${turnos.size} total  |  ${pendientes.size} pendiente(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayText,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { cargarTurnos() }) {
                    Text("Refrescar", color = GreenDark)
                }
            }

            Button(
                onClick = {
                    if (pendientes.isNotEmpty()) {
                        showConfirmAvanzar = pendientes.first()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                enabled = pendientes.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
            ) {
                Text("Siguiente turno", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReprogramarDialog(
    turno: Turno,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDateTime) -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reprogramar turno") },
        text = {
            Column {
                Text(
                    text = "Turno de: ${turno.nombre} ${turno.apellido}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Nueva fecha:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Nueva hora:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hour.toString().padStart(2, '0'),
                        onValueChange = { h ->
                            h.toIntOrNull()?.let { if (it in 0..23) hour = it }
                        },
                        modifier = Modifier.width(60.dp),
                        singleLine = true
                    )
                    Text(" : ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = minute.toString().padStart(2, '0'),
                        onValueChange = { m ->
                            m.toIntOrNull()?.let { if (it in 0..59) minute = it }
                        },
                        modifier = Modifier.width(60.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hora = LocalDateTime.of(selectedDate, java.time.LocalTime.of(hour, minute))
                onConfirm(selectedDate, hora)
            }) {
                Text("Reprogramar", color = GreenDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = GrayText)
            }
        }
    )

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
                }) {
                    Text("OK", color = GreenDark)
                }
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
