package com.hardagenda.app.ui.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hardagenda.app.data.TurnoRepository
import com.hardagenda.app.data.model.Turno
import com.hardagenda.app.ui.components.ConfirmDialog
import com.hardagenda.app.ui.components.ErrorDialog
import com.hardagenda.app.ui.theme.*
import com.hardagenda.app.util.PrefsManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodosTurnosTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var buscarDni by remember { mutableStateOf("") }
    var buscarNombre by remember { mutableStateOf("") }
    var buscarApellido by remember { mutableStateOf("") }
    var turnos by remember { mutableStateOf<List<Turno>>(emptyList()) }
    var allTurnos by remember { mutableStateOf<List<Turno>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var turnoDetalle by remember { mutableStateOf<Turno?>(null) }
    var turnoEditar by remember { mutableStateOf<Turno?>(null) }
    var turnoEliminar by remember { mutableStateOf<Turno?>(null) }

    fun cargarTurnos() {
        scope.launch {
            isLoading = true
            val result = TurnoRepository.obtenerTodosLosTurnos()
            result.onSuccess {
                allTurnos = it
                turnos = it
            }
            result.onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { cargarTurnos() }

    if (errorMessage != null) {
        ErrorDialog(message = errorMessage!!) { errorMessage = null }
    }

    turnoDetalle?.let { TurnoDetalleDialog(turno = it) { turnoDetalle = null } }

    turnoEliminar?.let { turno ->
        ConfirmDialog(
            title = "Confirmar eliminacion",
            message = "Eliminar el turno de ${turno.nombre} ${turno.apellido}?",
            onConfirm = {
                scope.launch {
                    val result = TurnoRepository.eliminarTurno(turno.id)
                    result.onSuccess {
                        TurnoRepository.registrarCambio(
                            "turnos", null, "Baja",
                            "${turno.nombre} ${turno.apellido} (DNI: ${turno.dni})",
                            PrefsManager.usuarioActual(context),
                            turno.dni, turno.nombre, turno.apellido
                        )
                        cargarTurnos()
                    }
                    result.onFailure { errorMessage = it.message }
                }
                turnoEliminar = null
            },
            onDismiss = { turnoEliminar = null }
        )
    }

    turnoEditar?.let { turno ->
        EditarTurnoDialog(
            turno = turno,
            onDismiss = { turnoEditar = null },
            onSaved = { cargarTurnos(); turnoEditar = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Buscar turnos:",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GreenDark,
            modifier = Modifier.padding(16.dp)
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SearchField("DNI:", buscarDni, { buscarDni = it })
            SearchField("Nombre:", buscarNombre, { buscarNombre = it })
            SearchField("Apellido:", buscarApellido, { buscarApellido = it })

            Row(modifier = Modifier.padding(top = 4.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val result = TurnoRepository.buscarTurnos(
                                buscarDni.ifBlank { null },
                                buscarNombre.ifBlank { null },
                                buscarApellido.ifBlank { null }
                            )
                            result.onSuccess { turnos = it }
                            result.onFailure { errorMessage = it.message }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Buscar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        buscarDni = ""; buscarNombre = ""; buscarApellido = ""
                        turnos = allTurnos
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Limpiar")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenDark)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderCell("ID", Modifier.weight(0.8f))
                        HeaderCell("Nombre", Modifier.weight(1.2f))
                        HeaderCell("Apellido", Modifier.weight(1.2f))
                        HeaderCell("DNI", Modifier.weight(1f))
                        HeaderCell("Fecha", Modifier.weight(1f))
                        HeaderCell("Hora", Modifier.weight(0.7f))
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                items(turnos) { turno ->
                    TurnoRow(
                        turno = turno,
                        onClick = { turnoDetalle = turno },
                        onCopyDni = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("DNI", turno.dni))
                            Toast.makeText(context, "DNI copiado", Toast.LENGTH_SHORT).show()
                        },
                        onCopyName = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Nombre", "${turno.nombre} ${turno.apellido}"))
                            Toast.makeText(context, "Nombre copiado", Toast.LENGTH_SHORT).show()
                        },
                        onEdit = { turnoEditar = turno },
                        onDelete = { turnoEliminar = turno }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Turnos: ${turnos.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayText
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TurnoRow(
    turno: Turno,
    onClick: () -> Unit,
    onCopyDni: () -> Unit,
    onCopyName: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DataCell(turno.id.toString().padStart(8, '0'), Modifier.weight(0.8f))
            DataCell(turno.nombre, Modifier.weight(1.2f))
            DataCell(turno.apellido, Modifier.weight(1.2f))
            DataCell(turno.dni, Modifier.weight(1f))
            DataCell(turno.fecha?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "", Modifier.weight(1f))
            DataCell(turno.hora?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "", Modifier.weight(0.7f))
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Copiar DNI") },
                onClick = { onCopyDni(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Copiar nombre completo") },
                onClick = { onCopyName(); showMenu = false }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Ver detalle") },
                onClick = { onClick(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Editar turno") },
                onClick = { onEdit(); showMenu = false }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Eliminar turno", color = ErrorColor) },
                onClick = { onDelete(); showMenu = false }
            )
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = GreenDark,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun DataCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun SearchField(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(72.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun TurnoDetalleDialog(turno: Turno, onDismiss: () -> Unit) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalle del turno") },
        text = {
            Column {
                DetailRow("Nombre", turno.nombre)
                DetailRow("Apellido", turno.apellido)
                DetailRow("DNI", turno.dni)
                DetailRow("Obra Social", turno.obraSocial ?: "N/A")
                DetailRow("Fecha", turno.fecha?.format(dateFormatter) ?: "")
                DetailRow("Hora", turno.hora?.format(timeFormatter) ?: "")
                DetailRow("Estado", turno.estado)
                DetailRow("Motivo", turno.motivoConsulta ?: "N/A")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = GreenDark) }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = GrayText
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarTurnoDialog(
    turno: Turno,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nombre by remember { mutableStateOf(turno.nombre) }
    var apellido by remember { mutableStateOf(turno.apellido) }
    var dni by remember { mutableStateOf(turno.dni) }
    var obraSocial by remember { mutableStateOf(turno.obraSocial ?: "") }
    var motivo by remember { mutableStateOf(turno.motivoConsulta ?: "") }
    var selectedDate by remember { mutableStateOf(turno.fecha ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(turno.hora?.hour ?: 9) }
    var minute by remember { mutableIntStateOf(turno.hora?.minute ?: 0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (errorMessage != null) {
        ErrorDialog(message = errorMessage!!) { errorMessage = null }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar turno") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                EditField("Nombre:", nombre, { nombre = it })
                EditField("Apellido:", apellido, { apellido = it })
                EditField("DNI:", dni, { dni = it })
                EditField("Obra Social:", obraSocial, { obraSocial = it })
                EditField("Motivo:", motivo, { motivo = it })

                Spacer(modifier = Modifier.height(8.dp))

                Text("Fecha:", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text("Hora:", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hour.toString().padStart(2, '0'),
                        onValueChange = { h -> h.toIntOrNull()?.let { if (it in 0..23) hour = it } },
                        modifier = Modifier.width(60.dp), singleLine = true
                    )
                    Text(" : ", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = minute.toString().padStart(2, '0'),
                        onValueChange = { m -> m.toIntOrNull()?.let { if (it in 0..59) minute = it } },
                        modifier = Modifier.width(60.dp), singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isBlank() || apellido.isBlank() || dni.isBlank()) {
                    errorMessage = "Nombre, apellido y DNI son obligatorios"
                    return@TextButton
                }
                scope.launch {
                    val fechaHora = LocalDateTime.of(selectedDate, java.time.LocalTime.of(hour, minute))
                    val result = TurnoRepository.editarTurno(
                        turno.id, nombre.trim(), apellido.trim(), dni.trim(),
                        obraSocial.trim().ifBlank { null },
                        motivo.trim().ifBlank { null },
                        selectedDate, fechaHora
                    )
                    result.onSuccess {
                        TurnoRepository.registrarCambio(
                            "turnos", turno.id, "Modificado",
                            "${nombre.trim()} ${apellido.trim()} (DNI: ${dni.trim()}) - ${fechaHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}",
                            PrefsManager.usuarioActual(context),
                            dni.trim(), nombre.trim(), apellido.trim()
                        )
                        onSaved()
                    }
                    result.onFailure { errorMessage = it.message }
                }
            }) {
                Text("Guardar", color = GreenDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = GrayText) }
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
                }) { Text("OK", color = GreenDark) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar", color = GrayText) }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun EditField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
