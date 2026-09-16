package com.fvcode.finfin.ui.relatorios

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fvcode.finfin.core.util.emReais
import com.fvcode.finfin.core.util.rotuloCurto

/**
 * `Exportar PDF`: o web gera HTML + `window.open + print()`. No Android o
 * equivalente é imprimir o HTML via `PrintManager` (permite salvar em PDF).
 */
object ExportarRelatorio {

    fun imprimir(contexto: Context, titulo: String, html: String) {
        val impressao = contexto.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val web = WebView(contexto)
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                impressao.print(
                    titulo,
                    view.createPrintDocumentAdapter(titulo),
                    PrintAttributes.Builder().build(),
                )
            }
        }
        web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun html(descricaoPeriodo: String, r: ResultadoRelatorio): String {
        fun linha2(c1: String, c2: String) = "<tr><td>$c1</td><td style='text-align:right'>$c2</td></tr>"
        val sb = StringBuilder()
        sb.append("<html><head><meta charset='utf-8'/>")
        sb.append("<style>body{font-family:sans-serif;padding:16px}table{width:100%;border-collapse:collapse;margin:8px 0}td,th{border:1px solid #ccc;padding:6px;font-size:13px}h2{margin:16px 0 4px}</style>")
        sb.append("</head><body>")
        sb.append("<h1>FinFin — Relatório</h1><p>$descricaoPeriodo</p>")
        sb.append("<h2>Resumo</h2><table>")
        sb.append(linha2("Receitas (${r.receitas.size})", r.totalReceitas.emReais()))
        sb.append(linha2("Ticket médio receita", r.ticketReceita.emReais()))
        sb.append(linha2("Despesas (${r.despesas.size})", r.totalDespesas.emReais()))
        sb.append(linha2("Ticket médio despesa", r.ticketDespesa.emReais()))
        sb.append(linha2("<b>Saldo acumulado</b>", "<b>${r.acumulado.emReais()}</b>"))
        sb.append("</table>")
        sb.append("<h2>Evolução mensal</h2><table><tr><th>Mês</th><th>Receitas</th><th>Despesas</th><th>Saldo</th></tr>")
        r.porMes.forEach { p ->
            sb.append("<tr><td>${rotuloCurto(p.mes)}</td><td style='text-align:right'>${p.receitas.emReais()}</td><td style='text-align:right'>${p.despesas.emReais()}</td><td style='text-align:right'>${(p.receitas - p.despesas).emReais()}</td></tr>")
        }
        sb.append("</table>")
        sb.append("<h2>Despesas por categoria</h2><table>")
        r.porCategoria.forEach { sb.append(linha2(it.nome, it.total.emReais())) }
        sb.append("</table>")
        sb.append("<h2>Por forma de pagamento</h2><table>")
        r.porForma.forEach { (nome, total) -> sb.append(linha2(nome, total.emReais())) }
        sb.append("</table>")
        sb.append("<h2>Por conta</h2><table><tr><th>Conta</th><th>Receitas</th><th>Despesas</th></tr>")
        r.porConta.forEach {
            sb.append("<tr><td>${it.conta.nome}</td><td style='text-align:right'>${it.receitas.emReais()}</td><td style='text-align:right'>${it.despesas.emReais()}</td></tr>")
        }
        sb.append("</table>")
        sb.append("<h2>Top 5 despesas</h2><table>")
        r.topDespesas.forEach { sb.append(linha2("${it.titulo} <small>(${it.detalhe})</small>", it.valor.emReais())) }
        sb.append("</table></body></html>")
        return sb.toString()
    }

    fun descricaoPeriodo(f: FiltrosRelatorio, iv: Intervalo): String {
        return when (f.preset) {
            Presets.MES -> "Mês ${f.mes}"
            Presets.SEIS_M -> "Últimos 6 meses até ${f.mes}"
            Presets.DOZE_M -> "Últimos 12 meses até ${f.mes}"
            Presets.ANO -> "Ano ${f.ano}"
            else -> "Período ${iv.ini} a ${iv.fim}"
        } + listOfNotNull(
            f.contaId?.let { "conta $it" },
            f.catReceita.ifBlank { null }?.let { "cat. receita: $it" },
            f.catDespesa.ifBlank { null }?.let { "cat. despesa: $it" },
            f.forma.ifBlank { null }?.let { "forma: $it" },
            f.busca.ifBlank { null }?.let { "busca: $it" },
        ).let { extras -> if (extras.isEmpty()) "" else " • " + extras.joinToString(" • ") }
    }
}
