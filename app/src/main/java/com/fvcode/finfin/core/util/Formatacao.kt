package com.fvcode.finfin.core.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val PT_BR = Locale("pt", "BR")
private val moeda: NumberFormat = NumberFormat.getCurrencyInstance(PT_BR)

/** Equivalente de `BRL` em `frontend/src/ui.tsx:5-27`. */
fun Double.emReais(): String = moeda.format(this)

/** `mesAtual` — `YYYY-MM` do mes corrente. */
fun mesAtual(): String = YearMonth.now().toString()

/** `mesLabel` — "setembro de 2026". */
fun mesLabel(mes: String): String {
    return try {
        val ym = YearMonth.parse(mes)
        val nome = ym.month.getDisplayName(TextStyle.FULL, PT_BR)
        "$nome de ${ym.year}"
    } catch (_: Exception) {
        mes
    }
}

/** `deslocarMes` — soma N meses preservando `YYYY-MM`. */
fun deslocarMes(mes: String, delta: Long): String {
    return try {
        YearMonth.parse(mes).plusMonths(delta).toString()
    } catch (_: Exception) {
        mesAtual()
    }
}

/** Ultimos N meses terminando em `mes` (inclusive), ordem crescente. */
fun ultimosMeses(mes: String, n: Int): List<String> {
    return try {
        val fim = YearMonth.parse(mes)
        (n - 1 downTo 0).map { fim.minusMonths(it.toLong()).toString() }
    } catch (_: Exception) {
        listOf(mes)
    }
}

/** Rotulo curto p/ eixo de grafico — "set/26". */
fun rotuloCurto(mes: String): String {
    return try {
        val ym = YearMonth.parse(mes)
        val abrev = ym.month.getDisplayName(TextStyle.SHORT, PT_BR).trimEnd('.')
        "$abrev/${ym.year.toString().takeLast(2)}"
    } catch (_: Exception) {
        mes
    }
}

/** `formatarData` — ISO `YYYY-MM-DD` vira `DD/MM`. */
fun formatarData(iso: String): String {
    return try {
        val d = LocalDate.parse(iso)
        "%02d/%02d".format(d.dayOfMonth, d.monthValue)
    } catch (_: Exception) {
        iso
    }
}
