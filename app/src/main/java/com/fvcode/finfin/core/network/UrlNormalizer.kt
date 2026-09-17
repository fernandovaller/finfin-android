package com.fvcode.finfin.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Normaliza a URL do servidor digitada pelo usuário para o formato
 * `scheme://host:porta/api/` (sempre com barra final, exigida pelo Retrofit).
 *
 * BuildConfig.BASE_URL continua existindo apenas como default de compilação;
 * o valor efetivo em runtime vive em DataStore (FinfinPreferences.baseUrl).
 */
object UrlNormalizer {

    fun normalizar(entrada: String): String {
        val limpa = entrada.trim()
        if (limpa.isEmpty()) throw IllegalArgumentException("Informe a URL do servidor.")
        // UX: aceita "192.168.0.10:3001" sem scheme.
        val comScheme = if ("://" in limpa) limpa else "http://$limpa"
        val parsed = comScheme.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("URL inválida. Ex.: http://192.168.0.10:3001")
        if (parsed.scheme != "http" && parsed.scheme != "https") {
            throw IllegalArgumentException("Use http:// ou https://.")
        }
        if (parsed.host == "localhost") {
            throw IllegalArgumentException(
                "localhost no celular aponta para o próprio aparelho. " +
                    "Use 10.0.2.2 (emulador) ou o IP da rede (ex.: 192.168.0.10).",
            )
        }
        // Força o prefixo /api/ — ignora qualquer path extra digitado.
        return "${parsed.scheme}://${parsed.host}" +
            (if (parsed.port != okhttp3.HttpUrl.defaultPort(parsed.scheme)) ":${parsed.port}" else "") +
            "/api/"
    }

    /** Valor efetivo: salvo em DataStore ou o default de build. */
    fun efetiva(salva: String?, padraoBuild: String): String =
        if (salva.isNullOrBlank()) padraoBuild else salva

    /** Versão curta para exibição ("host:porta"). */
    fun resumir(url: String): String =
        try {
            val p = url.toHttpUrlOrNull() ?: return url
            if (p.port != okhttp3.HttpUrl.defaultPort(p.scheme)) "${p.host}:${p.port}" else p.host
        } catch (_: Exception) {
            url
        }
}
