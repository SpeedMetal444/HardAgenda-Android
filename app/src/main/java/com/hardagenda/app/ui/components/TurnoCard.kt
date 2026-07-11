package com.hardagenda.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hardagenda.app.data.model.Turno
import com.hardagenda.app.ui.theme.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TurnoCard(
    turno: Turno,
    isActual: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    val bgColor = if (isActual) GreenHighlight else Surface
    val textColor = if (isActual) GreenDark else OnSurface
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActual) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (isActual) Modifier.background(GreenHighlight)
                    else Modifier
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isActual) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GreenMedium)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${turno.nombre} ${turno.apellido}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActual) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "DNI: ${turno.dni}${turno.obraSocial?.let { "  |  Obra Social: $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActual) GreenDark.copy(alpha = 0.7f) else GrayText
                )
            }

            turno.hora?.let { h ->
                Text(
                    text = h.format(timeFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isActual) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActual) GreenDark else GrayText
                )
            }
        }
    }
}

@Composable
fun TurnoAtendidoCard(turno: Turno) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${turno.nombre} ${turno.apellido}",
                    style = MaterialTheme.typography.titleMedium,
                    color = GrayLight
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "DNI: ${turno.dni}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayLight
                )
            }
            turno.hora?.let { h ->
                Text(
                    text = h.format(timeFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayLight
                )
            }
        }
    }
}
