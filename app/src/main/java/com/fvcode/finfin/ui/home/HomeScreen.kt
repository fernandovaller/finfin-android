package com.fvcode.finfin.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.core.util.emReais
import com.fvcode.finfin.core.util.formatarData
import com.fvcode.finfin.core.util.mesLabel
import com.fvcode.finfin.ui.components.FiltroConta
import com.fvcode.finfin.ui.components.MesNav
import com.fvcode.finfin.ui.components.SecaoTitulo

/** Seções: `MesNav`, filtro conta, `ResumoMes`, Contas, barras, donut, recentes. */
@Composable
fun HomeScreen(
    vm: HomeViewModel = hiltViewModel(),
    aoSessaoExpirada: () -> Unit = {},
) {
    val estado by vm.estado.collectAsState()

    if (estado.sessaoExpirada) {
        vm.consumirSessaoExpirada()
        aoSessaoExpirada()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MesNav(
            mes = estado.mes,
            aoAnterior = { vm.mudarMes(-1) },
            aoProximo = { vm.mudarMes(1) },
            aoHoje = { vm.irParaHoje() },
        )

        FiltroConta(
            contas = estado.contas.map { it.id to (it.nome + if (it.principal) " ★" else "") },
            selecionada = estado.contaFiltro,
            aoTrocar = { vm.trocarConta(it) },
        )

        estado.erro?.let { msg ->
            Card { 
                Column(Modifier.padding(12.dp)) {
                    Text(msg, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.recarregar() }) { Text("Tentar de novo") }
                }
            }
        }

        ResumoMes(
            carregando = estado.carregando,
            receitas = estado.resumo?.totalReceitas ?: 0.0,
            despesas = estado.resumo?.totalDespesas ?: 0.0,
            saldo = estado.resumo?.saldo ?: 0.0,
            qtdReceitas = estado.qtdReceitas,
            qtdDespesas = estado.qtdDespesas,
            mes = estado.mes,
        )

        SecaoTitulo("Contas")
        if (estado.contas.isEmpty()) {
            Text("Nenhuma conta.", style = MaterialTheme.typography.bodyMedium)
        } else {
            estado.contas.forEach { conta ->
                val saldo = estado.saldosPorConta[conta.id] ?: conta.saldoInicial
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            (conta.icone.ifBlank { "💰" }) + "  " + conta.nome,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        if (conta.principal) {
                            AssistChip(onClick = {}, label = { Text("Principal") })
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (estado.carregando) "..." else saldo.emReais(),
                            fontWeight = FontWeight.SemiBold,
                            color = if (saldo >= 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        SecaoTitulo("Evolução (6 meses)")
        GraficoBarrasMensal(estado.evolucao)

        SecaoTitulo("Despesas por categoria")
        GraficoDonut(estado.donut, estado.totalDonut)

        SecaoTitulo("Atividade recente")
        if (estado.recentes.isEmpty()) {
            Text("Sem lançamentos no mês.", style = MaterialTheme.typography.bodyMedium)
        } else {
            estado.recentes.forEachIndexed { i, item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (item.tipo == "receita") "+" else "−",
                        color = if (item.tipo == "receita") Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(20.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(item.descricao, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(
                            "${item.categoria} • ${formatarData(item.data)}" +
                                (vm.contaPorId(item.contaId).let { if (it.isBlank()) "" else " • $it" }),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        item.valor.emReais(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (i < estado.recentes.lastIndex) HorizontalDivider()
            }
        }
    }
}

/** `ResumoMes`: 3 cards `{resumo, carregando, mes, qtd}`. */
@Composable
private fun ResumoMes(
    carregando: Boolean,
    receitas: Double,
    despesas: Double,
    saldo: Double,
    qtdReceitas: Int,
    qtdDespesas: Int,
    mes: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        CartaoResumo("Receitas", receitas, carregando, "$qtdReceitas lanç.", Modifier.weight(1f))
        CartaoResumo("Despesas", despesas, carregando, "$qtdDespesas lanç.", Modifier.weight(1f))
        CartaoResumo(
            "Saldo",
            saldo,
            carregando,
            mesLabel(mes).take(3),
            Modifier.weight(1f),
            destaque = true,
        )
    }
}

@Composable
private fun CartaoResumo(
    titulo: String,
    valor: Double,
    carregando: Boolean,
    detalhe: String,
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium)
            Text(
                if (carregando) "..." else valor.emReais(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (destaque) {
                    if (valor >= 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                } else MaterialTheme.colorScheme.onSurface,
            )
            Text(detalhe, style = MaterialTheme.typography.bodySmall)
        }
    }
}
