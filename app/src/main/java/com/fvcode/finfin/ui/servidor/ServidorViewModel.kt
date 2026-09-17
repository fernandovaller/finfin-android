package com.fvcode.finfin.ui.servidor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvcode.finfin.BuildConfig
import com.fvcode.finfin.core.datastore.FinfinPreferences
import com.fvcode.finfin.core.network.UrlNormalizer
import com.fvcode.finfin.data.remote.FinfinApi
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ServidorUiState(
    val efetiva: String = BuildConfig.BASE_URL,
    val personalizada: Boolean = false,
    val campo: String = "",
    val testando: Boolean = false,
    val salvando: Boolean = false,
    val resultado: String? = null,
    val erroCampo: String? = null,
    val trechoTrocado: Boolean = false,
)

@HiltViewModel
class ServidorViewModel @Inject constructor(
    private val prefs: FinfinPreferences,
) : ViewModel() {

    private val _estado = MutableStateFlow(ServidorUiState())
    val estado: StateFlow<ServidorUiState> = _estado

    init {
        viewModelScope.launch {
            prefs.baseUrl.collect { salva ->
                val efetiva = UrlNormalizer.efetiva(salva, BuildConfig.BASE_URL)
                _estado.value = _estado.value.copy(
                    efetiva = efetiva,
                    personalizada = !salva.isNullOrBlank(),
                    campo = _estado.value.campo.ifBlank { efetiva },
                )
            }
        }
    }

    fun aoDigitar(texto: String) {
        _estado.value = _estado.value.copy(campo = texto, erroCampo = null, resultado = null)
    }

    /** Testa a URL do campo via GET /api/saude sem salvar nem exigir login. */
    fun testar() {
        val normalizada = try {
            UrlNormalizer.normalizar(_estado.value.campo)
        } catch (e: IllegalArgumentException) {
            _estado.value = _estado.value.copy(erroCampo = e.message)
            return
        }
        _estado.value = _estado.value.copy(testando = true, resultado = null, erroCampo = null)
        viewModelScope.launch {
            val ini = System.currentTimeMillis()
            try {
                val saude = withContext(Dispatchers.IO) { sondarSaude(normalizada) }
                val ms = System.currentTimeMillis() - ini
                _estado.value = if (saude.ok && saude.app == "finfin") {
                    _estado.value.copy(
                        testando = false,
                        resultado = "OK ${ms}ms • app=${saude.app} v${saude.versao ?: "?"}",
                    )
                } else {
                    _estado.value.copy(
                        testando = false,
                        erroCampo = "Respondeu, mas não parece o FinFin (ok=${saude.ok}).",
                    )
                }
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    testando = false,
                    erroCampo = mensagemAmigavel(e),
                )
            }
        }
    }

    /**
     * Salva a URL (após normalizar + sondar). Trocar de servidor invalida o
     * token do backend anterior. Retorna true se salvou.
     */
    fun salvar(aoTrocou: () -> Unit = {}) {
        val normalizada = try {
            UrlNormalizer.normalizar(_estado.value.campo)
        } catch (e: IllegalArgumentException) {
            _estado.value = _estado.value.copy(erroCampo = e.message)
            return
        }
        _estado.value = _estado.value.copy(salvando = true, erroCampo = null, resultado = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { sondarSaude(normalizada) }
                val anterior = UrlNormalizer.efetiva(prefs.baseUrl.first(), BuildConfig.BASE_URL)
                prefs.salvarBaseUrl(normalizada)
                if (anterior != normalizada) {
                    // Token do backend antigo não vale no novo.
                    prefs.salvarToken(null)
                    _estado.value = _estado.value.copy(
                        salvando = false,
                        resultado = "Servidor salvo. Entre novamente.",
                        trechoTrocado = true,
                    )
                    aoTrocou()
                } else {
                    _estado.value = _estado.value.copy(
                        salvando = false,
                        resultado = "Já apontado para este servidor.",
                    )
                }
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    salvando = false,
                    erroCampo = "Não salvei: ${mensagemAmigavel(e)}",
                )
            }
        }
    }

    fun restaurarPadrao(aoTrocou: () -> Unit = {}) {
        viewModelScope.launch {
            prefs.limparBaseUrl()
            prefs.salvarToken(null)
            _estado.value = _estado.value.copy(
                campo = BuildConfig.BASE_URL,
                resultado = "Voltou ao padrão de build. Entre novamente.",
                trechoTrocado = true,
            )
            aoTrocou()
        }
    }

    fun consumirTroca() {
        _estado.value = _estado.value.copy(trechoTrocado = false)
    }

    private suspend fun sondarSaude(baseUrl: String): com.fvcode.finfin.data.model.SaudeResposta {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .addInterceptor(log)
            .build()
        val api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
            .create(FinfinApi::class.java)
        return api.saude()
    }

    private fun mensagemAmigavel(e: Exception): String {
        val m = e.message ?: ""
        return when {
            e is java.net.UnknownHostException -> "Host não encontrado. Confira IP/DNS."
            e is java.net.ConnectException -> "Conexão recusada. Backend fora do ar ou porta errada?"
            e is java.net.SocketTimeoutException -> "Tempo esgotado. Backend lento ou URL errada."
            e is retrofit2.HttpException && e.code() == 429 ->
                "Muitas tentativas (429 rate-limit). Aguarde ~1 min e tente de novo."
            e is retrofit2.HttpException -> "HTTP ${e.code()}. É o backend do FinFin?"
            m.contains("Cleartext", true) -> "HTTP bloqueado. Use https ou libere no networkSecurityConfig."
            else -> "Falha de rede. Verifique a URL e a conexão."
        }
    }
}
