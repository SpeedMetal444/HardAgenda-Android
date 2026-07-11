package com.hardagenda.app.data

import com.hardagenda.app.data.model.HistorialCambio
import com.hardagenda.app.data.model.Turno
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TurnoRepository {

    private fun mapToTurno(m: Map<String, Any?>): Turno {
        return Turno(
            id = (m["id"] as? Number)?.toInt() ?: 0,
            nombre = m["nombre"] as? String ?: "",
            apellido = m["apellido"] as? String ?: "",
            dni = m["dni"] as? String ?: "",
            obraSocial = m["obra_social"] as? String,
            motivoConsulta = m["motivo_consulta"] as? String,
            fecha = parseDate(m["fecha"] as? String),
            hora = parseDateTime(m["hora"] as? String),
            estado = m["estado"] as? String ?: "pendiente",
            usuario = m["usuario"] as? String
        )
    }

    private fun mapToHistorial(m: Map<String, Any?>): HistorialCambio {
        return HistorialCambio(
            id = (m["id"] as? Number)?.toInt() ?: 0,
            tabla = m["tabla"] as? String ?: "",
            registroId = (m["registro_id"] as? Number)?.toInt(),
            accion = m["accion"] as? String ?: "",
            detalle = m["detalle"] as? String,
            usuario = m["usuario"] as? String,
            dni = m["dni"] as? String,
            nombre = m["nombre"] as? String,
            apellido = m["apellido"] as? String,
            fecha = parseDateTime(m["fecha"] as? String)
        )
    }

    private fun parseDate(s: String?): LocalDate? {
        s ?: return null
        return try {
            LocalDate.parse(s.substring(0, 10))
        } catch (_: Exception) { null }
    }

    private fun parseDateTime(s: String?): LocalDateTime? {
        s ?: return null
        return try {
            LocalDateTime.parse(s.replace("T", " ").substring(0, 19))
        } catch (_: Exception) {
            try { LocalDateTime.parse(s) } catch (_: Exception) { null }
        }
    }

    private fun fmtDate(d: LocalDate?): String? = d?.format(DateTimeFormatter.ISO_LOCAL_DATE)
    private fun fmtDateTime(dt: LocalDateTime?): String? = dt?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    suspend fun crearBaseDeDatos() = ApiClient.crearBaseDeDatos()
    suspend fun crearTablas() = ApiClient.crearTablas()

    suspend fun obtenerTurnosDelDia(): Result<List<Turno>> {
        return ApiClient.obtenerTurnosDelDia().map { list -> list.map { mapToTurno(it) } }
    }

    suspend fun obtenerTodosLosTurnos(): Result<List<Turno>> {
        return ApiClient.obtenerTodosLosTurnos().map { list -> list.map { mapToTurno(it) } }
    }

    suspend fun buscarTurnos(dni: String?, nombre: String?, apellido: String?): Result<List<Turno>> {
        return ApiClient.buscarTurnos(dni, nombre, apellido).map { list -> list.map { mapToTurno(it) } }
    }

    suspend fun agregarTurno(
        nombre: String, apellido: String, dni: String,
        obraSocial: String?, motivoConsulta: String?,
        usuario: String?, fecha: LocalDate?, hora: LocalDateTime?
    ): Result<Unit> {
        return ApiClient.registrarTurno(
            nombre, apellido, dni, obraSocial, motivoConsulta,
            fmtDate(fecha), fmtDateTime(hora), usuario
        )
    }

    suspend fun avanzarTurno(turnoId: Int) = ApiClient.avanzarTurno(turnoId)

    suspend fun editarTurno(
        turnoId: Int, nombre: String, apellido: String, dni: String,
        obraSocial: String?, motivoConsulta: String?,
        fecha: LocalDate, hora: LocalDateTime
    ) = ApiClient.editarTurno(turnoId, nombre, apellido, dni, obraSocial, motivoConsulta, fmtDate(fecha), fmtDateTime(hora))

    suspend fun reprogramarTurno(turnoId: Int, nuevaFecha: LocalDate, nuevaHora: LocalDateTime) =
        ApiClient.reprogramarTurno(turnoId, fmtDate(nuevaFecha)!!, fmtDateTime(nuevaHora)!!)

    suspend fun eliminarTurno(turnoId: Int) = ApiClient.eliminarTurno(turnoId)

    suspend fun registrarCambio(
        tabla: String, registroId: Int?, accion: String, detalle: String,
        usuario: String?, dni: String?, nombre: String?, apellido: String?
    ) = ApiClient.registrarCambio(tabla, registroId, accion, detalle, usuario, dni, nombre, apellido)

    suspend fun obtenerHistorial(): Result<List<HistorialCambio>> {
        return ApiClient.obtenerHistorial().map { list -> list.map { mapToHistorial(it) } }
    }
}
