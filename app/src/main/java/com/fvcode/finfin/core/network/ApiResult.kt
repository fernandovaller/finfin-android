package com.fvcode.finfin.core.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.HttpException
import java.io.IOException

/** Resultado padrao das chamadas, espelha o tratamento de `api.ts:85-107`. */
sealed interface ApiResult<out T> {
    data class Ok<T>(val dados: T) : ApiResult<T>
    data class Erro(val mensagem: String, val codigo: Int? = null) : ApiResult<Nothing>
}

object ApiErros {
    const val SESSAO_EXPIRADA = "Sessão expirada. Entre novamente."
}

private data class CorpoErro(val message: Any?)

suspend fun <T> aoResultado(bloco: suspend () -> T, semToken: Boolean): ApiResult<T> {
    return try {
        ApiResult.Ok(bloco())
    } catch (e: HttpException) {
        val msg = extrairMensagem(e) ?: "Erro ${e.code()}"
        if (e.code() == 401 && !semToken) {
            ApiResult.Erro(ApiErros.SESSAO_EXPIRADA, 401)
        } else {
            ApiResult.Erro(msg, e.code())
        }
    } catch (e: IOException) {
        ApiResult.Erro("Falha de rede. Verifique a conexão.")
    }
}

private fun extrairMensagem(e: HttpException): String? {
    return try {
        val corpo = e.response()?.errorBody()?.string() ?: return null
        val parsed = Gson().fromJson(corpo, CorpoErro::class.java)
        when (val m = parsed.message) {
            is String -> m
            is List<*> -> m.joinToString("; ")
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

data class MensagemOk(
    @SerializedName("ok") val ok: Boolean = true,
)
