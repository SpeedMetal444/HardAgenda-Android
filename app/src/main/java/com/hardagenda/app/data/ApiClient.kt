package com.hardagenda.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    var serverUrl: String = ""
    var dbUser: String = "postgres"
    var dbPass: String = ""
    var dbName: String = "hardagenda_db"

    private fun url(path: String): URL {
        val base = serverUrl.trimEnd('/')
        return URL("$base$path")
    }

    private fun request(method: String, path: String, body: JSONObject? = null): Result<JSONObject> {
        return try {
            val conn = url(path).openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("X-DB-User", dbUser)
            conn.setRequestProperty("X-DB-Pass", dbPass)
            conn.setRequestProperty("X-DB-Name", dbName)
            conn.connectTimeout = 10000
            conn.readTimeout = 30000
            conn.doOutput = body != null

            body?.let {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(it.toString())
                    writer.flush()
                }
            }

            val inputStream = if (conn.responseCode in 200..299) {
                conn.inputStream
            } else {
                conn.errorStream
            }

            val response = inputStream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText) ?: "{}"
            val json = JSONObject(response)

            if (conn.responseCode in 200..299) {
                Result.success(json)
            } else {
                Result.failure(Exception(json.optString("message", "Error HTTP ${conn.responseCode}")))
            }
        } catch (e: Exception) {
            when {
                e.message?.contains("Connection refused") == true ->
                    Result.failure(Exception("No se pudo conectar al servidor. Verifica que server.py este corriendo en la PC."))
                e.message?.contains("timeout") == true ->
                    Result.failure(Exception("Tiempo de espera agotado. Verifica la conexion."))
                e.message?.contains("Unable to resolve") == true ->
                    Result.failure(Exception("No se pudo resolver la IP. Verifica que estes en la misma red WiFi."))
                else -> Result.failure(Exception("Error de red: ${e.message}"))
            }
        }
    }

    suspend fun ping(): Result<Boolean> = withContext(Dispatchers.IO) {
        request("GET", "/api/ping").map { true }
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        request("GET", "/api/test").map { it.optString("message", "OK") }
    }

    suspend fun crearBaseDeDatos(): Result<String> = withContext(Dispatchers.IO) {
        request("GET", "/api/crear_db").map { it.optString("message", "OK") }
    }

    suspend fun crearTablas(): Result<String> = withContext(Dispatchers.IO) {
        request("GET", "/api/crear_tablas").map { it.optString("message", "OK") }
    }

    suspend fun obtenerTurnosDelDia(): Result<List<Map<String, Any?>>> = withContext(Dispatchers.IO) {
        request("GET", "/api/turnos/hoy").map { parseTurnos(it.getJSONArray("data")) }
    }

    suspend fun obtenerTodosLosTurnos(): Result<List<Map<String, Any?>>> = withContext(Dispatchers.IO) {
        request("GET", "/api/turnos/todos").map { parseTurnos(it.getJSONArray("data")) }
    }

    suspend fun buscarTurnos(dni: String?, nombre: String?, apellido: String?): Result<List<Map<String, Any?>>> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                dni?.takeIf { it.isNotBlank() }?.let { put("dni", it) }
                nombre?.takeIf { it.isNotBlank() }?.let { put("nombre", it) }
                apellido?.takeIf { it.isNotBlank() }?.let { put("apellido", it) }
            }
            request("POST", "/api/turnos/buscar", body).map { parseTurnos(it.getJSONArray("data")) }
        }

    suspend fun registrarTurno(
        nombre: String, apellido: String, dni: String,
        obraSocial: String?, motivo: String?,
        fecha: String?, hora: String?, usuario: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("nombre", nombre)
            put("apellido", apellido)
            put("dni", dni)
            put("obra_social", obraSocial)
            put("motivo", motivo)
            put("fecha", fecha)
            put("hora", hora)
            put("usuario", usuario)
        }
        request("POST", "/api/turnos/registrar", body).map { }
    }

    suspend fun avanzarTurno(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        request("POST", "/api/turnos/avanzar", JSONObject().put("id", id)).map { }
    }

    suspend fun editarTurno(
        id: Int, nombre: String, apellido: String, dni: String,
        obraSocial: String?, motivo: String?, fecha: String?, hora: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("id", id)
            put("nombre", nombre)
            put("apellido", apellido)
            put("dni", dni)
            put("obra_social", obraSocial)
            put("motivo", motivo)
            put("fecha", fecha)
            put("hora", hora)
        }
        request("POST", "/api/turnos/editar", body).map { }
    }

    suspend fun reprogramarTurno(id: Int, fecha: String, hora: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("id", id)
                put("fecha", fecha)
                put("hora", hora)
            }
            request("POST", "/api/turnos/reprogramar", body).map { }
        }

    suspend fun eliminarTurno(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        request("POST", "/api/turnos/eliminar", JSONObject().put("id", id)).map { }
    }

    suspend fun obtenerHistorial(): Result<List<Map<String, Any?>>> = withContext(Dispatchers.IO) {
        request("GET", "/api/historial").map { parseTurnos(it.getJSONArray("data")) }
    }

    suspend fun registrarCambio(
        tabla: String?, registroId: Int?, accion: String, detalle: String?,
        usuario: String?, dni: String?, nombre: String?, apellido: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            tabla?.let { put("tabla", it) }
            registroId?.let { put("registro_id", it) }
            put("accion", accion)
            detalle?.let { put("detalle", it) }
            usuario?.let { put("usuario", it) }
            dni?.let { put("dni", it) }
            nombre?.let { put("nombre", it) }
            apellido?.let { put("apellido", it) }
        }
        request("POST", "/api/historial/registrar", body).map { }
    }

    private fun parseTurnos(arr: org.json.JSONArray): List<Map<String, Any?>> {
        val list = mutableListOf<Map<String, Any?>>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val map = mutableMapOf<String, Any?>()
            for (key in obj.keys()) {
                map[key] = if (obj.isNull(key)) null else obj.get(key)
            }
            list.add(map)
        }
        return list
    }
}
