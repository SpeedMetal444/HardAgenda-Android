package com.hardagenda.app.data.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Turno(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val obraSocial: String?,
    val motivoConsulta: String?,
    val fecha: LocalDate?,
    val hora: LocalDateTime?,
    val estado: String,
    val usuario: String?
)

data class HistorialCambio(
    val id: Int,
    val tabla: String,
    val registroId: Int?,
    val accion: String,
    val detalle: String?,
    val usuario: String?,
    val dni: String?,
    val nombre: String?,
    val apellido: String?,
    val fecha: LocalDateTime?
)
