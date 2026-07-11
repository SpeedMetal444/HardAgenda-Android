package com.hardagenda.app.ui.tabs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hardagenda.app.R
import com.hardagenda.app.ui.theme.*

@Composable
fun AcercaDeTab() {
    val context = LocalContext.current
    val logoAvailable = remember {
        try {
            context.resources.getDrawable(R.drawable.logo_default_large, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "HardAgenda",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = GreenDark
        )
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayText
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sistema de gestion de turnos basado en Kotlin y PostgreSQL.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Desarrollado por:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Abel Godoy",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Contacto:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:abelgodoy.1802@gmail.com?subject=REPORTE%20-%20HardAgenda%20Android%20V1.0.0")
            }
            context.startActivity(intent)
        }) {
            Text("abelgodoy.1802@gmail.com", color = LinkBlue)
        }
        Text(
            text = "+54 3795 320959",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Soporte:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = {
            val body = "REPORTAR PROBLEMA - HardAgenda Android V1.0.0\n\n" +
                    "--- Descripcion del problema ---\n" +
                    "(Describe que hiciste y que esperabas que pasara)\n\n" +
                    "--- Pasos para reproducir ---\n" +
                    "1. \n2. \n3. \n\n" +
                    "--- Comportamiento esperado ---\n\n" +
                    "--- Comportamiento actual ---\n\n" +
                    "--- Datos del sistema ---\n" +
                    "SO: Android\n"
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:abelgodoy.1802@gmail.com?subject=${Uri.encode("REPORTE - HardAgenda Android V1.0.0")}&body=${Uri.encode(body)}")
            }
            context.startActivity(intent)
        }) {
            Text("Reportar un problema", color = LinkBlue)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (logoAvailable) {
                Image(
                    painter = painterResource(id = R.drawable.logo_default_large),
                    contentDescription = "Logo HardAgenda",
                    modifier = Modifier.size(200.dp)
                )
            } else {
                Text(
                    text = "HardAgenda",
                    style = MaterialTheme.typography.displayLarge,
                    color = GreenDark.copy(alpha = 0.3f)
                )
            }
        }
    }
}
