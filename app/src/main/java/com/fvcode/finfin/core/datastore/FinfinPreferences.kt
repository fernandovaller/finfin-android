package com.fvcode.finfin.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Equivalente Android de `frontend/src/api.ts:70-82` + `tema.tsx`.
 * Chaves: finfin_token, finfin_tema, finfin_largura.
 */
@Singleton
class FinfinPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val token: Flow<String?> = dataStore.data.map { it[KEY_TOKEN] }
    val tema: Flow<String> = dataStore.data.map { it[KEY_TEMA] ?: TEMA_SISTEMA }
    val largura: Flow<String> = dataStore.data.map { it[KEY_LARGURA] ?: LARGURA_FLUIDA }

    suspend fun salvarToken(token: String?) {
        dataStore.edit {
            if (token == null) it.remove(KEY_TOKEN) else it[KEY_TOKEN] = token
        }
    }

    suspend fun salvarTema(tema: String) {
        dataStore.edit { it[KEY_TEMA] = tema }
    }

    suspend fun salvarLargura(largura: String) {
        dataStore.edit { it[KEY_LARGURA] = largura }
    }

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("finfin_token")
        private val KEY_TEMA = stringPreferencesKey("finfin_tema")
        private val KEY_LARGURA = stringPreferencesKey("finfin_largura")

        const val TEMA_CLARO = "claro"
        const val TEMA_ESCURO = "escuro"
        const val TEMA_SISTEMA = "sistema"
        const val LARGURA_FLUIDA = "fluida"
        const val LARGURA_FIXA = "fixa"
    }
}
