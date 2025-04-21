package com.example.lidar

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Utilitário para persistir configurações do app (host, porta, reconnect delay) usando DataStore Preferences.
 * Armazena apenas a instância do DataStore (via applicationContext) para evitar leaks.
 */
private val Context.lidarDataStore: DataStore<Preferences> by preferencesDataStore(name = "lidar_settings")

object DataStoreUtil {
    private lateinit var store: DataStore<Preferences>

    private val HOST_KEY = stringPreferencesKey("host")
    private val PORT_KEY = intPreferencesKey("port")
    private val RECONNECT_DELAY_KEY = intPreferencesKey("reconnect_delay_ms")

    /** Deve ser chamado uma vez (p.ex. em Application.onCreate() ou MainActivity.onCreate()). */
    fun init(context: Context) {
        store = context.applicationContext.lidarDataStore
    }

    /**
     * Flow que emite um Triple(host, port, reconnectDelayMs) sempre que alguma configuração mudar.
     */
    val settingsFlow: Flow<Triple<String, Int, Int>> by lazy {
        store.data.map { prefs ->
            val host = prefs[HOST_KEY] ?: "127.0.0.1"
            val port = prefs[PORT_KEY] ?: 9999
            val delay = prefs[RECONNECT_DELAY_KEY] ?: 1000
            Triple(host, port, delay)
        }
    }

    /** Persiste host e porta de forma atômica. */
    suspend fun saveHostPort(host: String, port: Int) {
        store.edit { prefs ->
            prefs[HOST_KEY] = host
            prefs[PORT_KEY] = port
        }
    }

    /** Persiste reconnect delay (em ms). */
    suspend fun saveReconnectDelay(delayMs: Int) {
        store.edit { prefs ->
            prefs[RECONNECT_DELAY_KEY] = delayMs
        }
    }
}
