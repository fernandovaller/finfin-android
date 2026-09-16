package com.fvcode.finfin.core.util

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

/** Item parseado (espelha `OfxItem` de `frontend/src/ofx.ts`). */
data class OfxItem(
    val fitid: String?,
    val data: String, // YYYY-MM-DD
    val valor: Double, // > 0, 2 casas
    val tipo: String, // receita|despesa
    val descricao: String,
)

private fun tag(bloco: String, nome: String): String? {
    val m = Regex("<$nome>([^<\\r\\n]*)", RegexOption.IGNORE_CASE).find(bloco)
    return m?.groupValues?.get(1)?.trim()?.ifBlank { null }
}

/**
 * Parser OFX (`ofx.ts:21-53`): exige `<OFX>`; split por `<STMTTRN>`;
 * `DTPOSTED` → `YYYY-MM-DD`; `TRNAMT` (`,` → `.`), pula NaN/0;
 * sinal → tipo; `MEMO || NAME || 'Lançamento OFX'` (slice 200);
 * dedup `FITID` intra-arquivo; sort por data.
 */
fun parseOfx(texto: String): List<OfxItem> {
    if (!texto.contains("<OFX>", ignoreCase = true)) {
        throw IllegalArgumentException("Arquivo inválido: tag <OFX> não encontrada.")
    }
    val vistos = mutableSetOf<String>()
    return texto.split(Regex("(?i)<STMTTRN[>\\s]")).drop(1).mapNotNull { bloco ->
        val digitos = tag(bloco, "DTPOSTED")?.filter(Char::isDigit)?.take(8) ?: return@mapNotNull null
        if (digitos.length < 8) return@mapNotNull null
        val data = "${digitos.substring(0, 4)}-${digitos.substring(4, 6)}-${digitos.substring(6, 8)}"
        try {
            LocalDate.parse(data)
        } catch (_: Exception) {
            return@mapNotNull null
        }
        val bruto = tag(bloco, "TRNAMT")?.replace(",", ".")?.toDoubleOrNull() ?: return@mapNotNull null
        if (bruto == 0.0) return@mapNotNull null
        val valor = (abs(bruto) * 100).roundToLong() / 100.0
        if (valor <= 0) return@mapNotNull null
        val fitid = tag(bloco, "FITID")?.take(100)
        if (fitid != null && !vistos.add(fitid)) return@mapNotNull null // repetido no arquivo
        OfxItem(
            fitid = fitid,
            data = data,
            valor = valor,
            tipo = if (bruto < 0) "despesa" else "receita",
            descricao = (tag(bloco, "MEMO") ?: tag(bloco, "NAME") ?: "Lançamento OFX").take(200),
        )
    }.sortedBy { it.data }
}
