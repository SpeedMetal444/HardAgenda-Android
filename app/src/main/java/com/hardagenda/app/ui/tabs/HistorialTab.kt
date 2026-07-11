package com.hardagenda.app.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hardagenda.app.data.TurnoRepository
import com.hardagenda.app.data.model.HistorialCambio
import com.hardagenda.app.ui.components.ErrorDialog
import com.hardagenda.app.ui.theme.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun HistorialTab() {
    val scope = rememberCoroutineScope()
    var registros by remember { mutableStateOf<List<HistorialCambio>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun cargarHistorial() {
        scope.launch {
            isLoading = true
            val result = TurnoRepository.obtenerHistorial()
            result.onSuccess { registros = it }
            result.onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { cargarHistorial() }

    if (errorMessage != null) {
        ErrorDialog(message = errorMessage!!) { errorMessage = null }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenDark)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (registros.isEmpty()) {
                    item {
                        Text(
                            text = "No se registran cambios al dia de la fecha",
                            style = MaterialTheme.typography.bodyLarge,
                            color = GrayText,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderCell("Fecha", Modifier.weight(1.2f))
                        HeaderCell("Accion", Modifier.weight(1f))
                        HeaderCell("Detalle", Modifier.weight(1.5f))
                        HeaderCell("DNI", Modifier.weight(0.8f))
                        HeaderCell("Nombre", Modifier.weight(1.2f))
                        HeaderCell("Usuario", Modifier.weight(1f))
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                items(registros) { r ->
                    val fechaStr = r.fecha?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: ""
                    val nombreCompleto = "${r.nombre ?: ""} ${r.apellido ?: ""}".trim()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        DataCellSmall(fechaStr, Modifier.weight(1.2f))
                        DataCellSmall(r.accion, Modifier.weight(1f))
                        DataCellSmall(r.detalle ?: "", Modifier.weight(1.5f))
                        DataCellSmall(r.dni ?: "", Modifier.weight(0.8f))
                        DataCellSmall(nombreCompleto, Modifier.weight(1.2f))
                        DataCellSmall(r.usuario ?: "", Modifier.weight(1f))
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            Button(
                onClick = { cargarHistorial() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
            ) {
                Text("Refrescar historial", fontWeight = FontWeight.Bold)
            }
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
private fun DataCellSmall(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
