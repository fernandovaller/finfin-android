package com.fvcode.finfin.ui.relatorios

import com.fvcode.finfin.data.model.Conta
import com.fvcode.finfin.data.model.Despesa
import com.fvcode.finfin.data.model.Receita
import com.fvcode.finfin.ui.home.FatiaCategoria
import com.fvcode.finfin.ui.home.PontoMensal
import java.time.LocalDate
import java.time.YearMonth

/** Presets de período (espelham `Relatorios.tsx`). */
object Presets {
    const val MES = "mes"
    const val SEIS_M = "6m"
    const val DOZE_M = "12m"
    const val ANO = "ano"
    const val INTERVALO = "intervalo"
}

data class FiltrosRelatorio(
    val preset: String = Presets.MES,
    val mes: String = YearMonth.now().toString(),
    val ano: Int = YearMonth.now().year,
    val ini: String = LocalDate.now().withDayOfMonth(1).toString(),
    val fim: String = LocalDate.now().toString(),
    val contaId: Int? = null,
    val catReceita: String = "",
    val catDespesa: String = "",
    val forma: String = "",
    val busca: String = "",
    val minTxt: String = "",
    val maxTxt: String = "",
)

data class Intervalo(val ini: String, val fim: String)

data class ItemValor(
    val titulo: String,
    val detalhe: String,
    val valor: Double,
)

data class ContaAgregada(
    val conta: Conta,
    val receitas: Double,
    val despesas: Double,
)

data class ResultadoRelatorio(
    val receitas: List<Receita>,
    val despesas: List<Despesa>,
    val totalReceitas: Double,
    val totalDespesas: Double,
    val ticketReceita: Double,
    val ticketDespesa: Double,
    val porMes: List<PontoMensal>,
    val acumulado: Double,
    val porCategoria: List<FatiaCategoria>,
    val porForma: List<Pair<String, Double>>,
    val porConta: List<ContaAgregada>,
    val topDespesas: List<ItemValor>,
)

/** `parseValorBR`: `.` milhar, `,` decimal. */
fun parseValorBR(txt: String): Double? {
    val t = txt.trim()
    if (t.isEmpty()) return null
    return t.replace(".", "").replace(",", ".").toDoubleOrNull()
}

/** Resolve o intervalo ISO do preset; null se intervalo inválido. */
fun resolverIntervalo(f: FiltrosRelatorio): Intervalo? {
    return try {
        when (f.preset) {
            Presets.MES -> {
                val ym = YearMonth.parse(f.mes)
                Intervalo(ym.atDay(1).toString(), ym.atEndOfMonth().toString())
            }
            Presets.SEIS_M -> mesesAtras(f.mes, 6)
            Presets.DOZE_M -> mesesAtras(f.mes, 12)
            Presets.ANO -> Intervalo("${f.ano}-01-01", "${f.ano}-12-31")
            else -> {
                val ini = LocalDate.parse(f.ini)
                val fim = LocalDate.parse(f.fim)
                if (fim.isBefore(ini)) null else Intervalo(ini.toString(), fim.toString())
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun mesesAtras(mes: String, n: Int): Intervalo {
    val fim = YearMonth.parse(mes)
    val inicio = fim.minusMonths((n - 1).toLong())
    return Intervalo(inicio.atDay(1).toString(), fim.atEndOfMonth().toString())
}

/** Meses `YYYY-MM` dentro do intervalo (teto 36). */
fun mesesDoIntervalo(iv: Intervalo): List<String> {
    return try {
        var m = YearMonth.from(LocalDate.parse(iv.ini))
        val fim = YearMonth.from(LocalDate.parse(iv.fim))
        val out = mutableListOf<String>()
        while (!m.isAfter(fim) && out.size < 36) {
            out.add(m.toString())
            m = m.plusMonths(1)
        }
        out
    } catch (_: Exception) {
        emptyList()
    }
}

/** Aplica todos os filtros (conta, categorias, forma, busca, min/max, período). */
fun filtrar(
    receitas: List<Receita>,
    despesas: List<Despesa>,
    f: FiltrosRelatorio,
    iv: Intervalo,
): Pair<List<Receita>, List<Despesa>> {
    val busca = f.busca.trim().lowercase()
    val min = parseValorBR(f.minTxt)
    val max = parseValorBR(f.maxTxt)
    fun ok(data: String, valor: Double, categoria: String, titulo: String, contaId: Int?, forma: String, catFiltro: String): Boolean {
        if (data < iv.ini || data > iv.fim) return false
        if (f.contaId != null && contaId != f.contaId) return false
        if (catFiltro.isNotBlank() && categoria != catFiltro) return false
        if (f.forma.isNotBlank() && forma != f.forma) return false
        if (busca.isNotBlank() && !(titulo.lowercase().contains(busca) || categoria.lowercase().contains(busca))) return false
        if (min != null && valor < min) return false
        if (max != null && valor > max) return false
        return true
    }
    return receitas.filter {
        ok(it.data, it.valor, it.categoria, it.origem, it.contaId, it.formaPagamento, f.catReceita)
    } to despesas.filter {
        ok(it.data, it.valor, it.categoria, it.descricao, it.contaId, it.formaPagamento, f.catDespesa)
    }
}

/** Agregados: totais, ticket, evolução com acumulado, top 5, por categoria/forma/conta. */
fun agregar(
    receitas: List<Receita>,
    despesas: List<Despesa>,
    contas: List<Conta>,
    corPorCategoria: Map<String, String>,
    meses: List<String>,
): ResultadoRelatorio {
    val totalRec = receitas.sumOf { it.valor }
    val totalDes = despesas.sumOf { it.valor }
    val porMes = meses.map { m ->
        PontoMensal(
            mes = m,
            receitas = receitas.filter { it.data.startsWith(m) }.sumOf { it.valor },
            despesas = despesas.filter { it.data.startsWith(m) }.sumOf { it.valor },
        )
    }
    val porCategoria = despesas.groupBy { it.categoria }
        .mapValues { (_, v) -> v.sumOf { it.valor } }
        .toList().sortedByDescending { it.second }
        .let { lista ->
            val top = lista.take(7)
            val resto = lista.drop(7).sumOf { it.second }
            top.map { (nome, total) -> FatiaCategoria(nome, corPorCategoria[nome] ?: "slate", total) } +
                (if (resto > 0) listOf(FatiaCategoria("Outras", "slate", resto)) else emptyList())
        }
    val porForma = despesas.groupBy { it.formaPagamento.ifBlank { "Não informada" } }
        .mapValues { (_, v) -> v.sumOf { it.valor } }
        .toList().sortedByDescending { it.second }
    val porConta = contas.mapNotNull { c ->
        val r = receitas.filter { it.contaId == c.id }.sumOf { it.valor }
        val d = despesas.filter { it.contaId == c.id }.sumOf { it.valor }
        if (r == 0.0 && d == 0.0) null else ContaAgregada(c, r, d)
    }.sortedByDescending { it.receitas - it.despesas }
    val topDespesas = despesas.sortedByDescending { it.valor }.take(5).map {
        ItemValor(
            titulo = it.descricao.ifBlank { it.categoria },
            detalhe = "${it.categoria} • ${it.data}",
            valor = it.valor,
        )
    }
    return ResultadoRelatorio(
        receitas = receitas,
        despesas = despesas,
        totalReceitas = totalRec,
        totalDespesas = totalDes,
        ticketReceita = if (receitas.isNotEmpty()) totalRec / receitas.size else 0.0,
        ticketDespesa = if (despesas.isNotEmpty()) totalDes / despesas.size else 0.0,
        porMes = porMes,
        acumulado = porMes.sumOf { it.receitas - it.despesas },
        porCategoria = porCategoria,
        porForma = porForma,
        porConta = porConta,
        topDespesas = topDespesas,
    )
}
