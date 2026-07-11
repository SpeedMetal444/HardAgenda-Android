package com.hardagenda.app.util

import android.content.Context
import android.content.SharedPreferences

object PrefsManager {
    private const val PREFS_NAME = "hardagenda_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_DB_NAME = "db_name"
    private const val KEY_DB_USER = "db_user"
    private const val KEY_DB_PASS = "db_pass"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveConfig(context: Context, serverUrl: String, dbName: String, dbUser: String, dbPass: String) {
        prefs(context).edit().apply {
            putString(KEY_SERVER_URL, serverUrl)
            putString(KEY_DB_NAME, dbName)
            putString(KEY_DB_USER, dbUser)
            putString(KEY_DB_PASS, dbPass)
            apply()
        }
    }

    fun loadConfig(context: Context): Config {
        val p = prefs(context)
        return Config(
            serverUrl = p.getString(KEY_SERVER_URL, "") ?: "",
            dbName = p.getString(KEY_DB_NAME, "hardagenda_db") ?: "hardagenda_db",
            dbUser = p.getString(KEY_DB_USER, "postgres") ?: "postgres",
            dbPass = p.getString(KEY_DB_PASS, "") ?: ""
        )
    }

    data class Config(
        val serverUrl: String,
        val dbName: String,
        val dbUser: String,
        val dbPass: String
    )

    fun hasSavedConfig(context: Context): Boolean {
        return (prefs(context).getString(KEY_SERVER_URL, "") ?: "").isNotBlank()
    }

    fun clearSession(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun usuarioActual(context: Context): String {
        return prefs(context).getString(KEY_DB_NAME, "desconocido") ?: "desconocido"
    }
}
