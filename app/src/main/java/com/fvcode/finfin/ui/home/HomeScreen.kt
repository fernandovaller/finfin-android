package com.fvcode.finfin.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.core.util.emReais
import com.fvcode.finfin.core.util.formatarData
import com.fvcode.finfin.core.util.mesLabel
import com.fvcode.finfin.core.util.pluralLancamentos
import com.fvcode.finfin.data.model.Conta
import com.fvcode.finfin.ui.components.FiltroConta
import com.fvcode.finfin.ui.components.MesNavEscuro
import com.fvcode.finfin.ui.components.SecaoTitulo

private val VERDE = Color(0xFF16A34A)
private val VERMELHO = Color(0xFFDC2626)

/** Home alinhada ao web: header + pill de mês, `ResumoMes`, Contas, gráficos, recentes. */
@Composable
fun HomeScreen(
    vm: HomeViewModel = hiltViewModel(),
    aoSessaoExpirada: () -> Unit = {},
    aoGerenciarContas: () -> Unit = {},
    aoNovoLancamento: () -> Unit = {},
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.inverseSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text("Home", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        MesNavEscuro(
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

        SecaoContas(
            mes = estado.mes,
            contas = estado.contas,
            carregando = estado.carregando,
            saldoDe = { estado.saldosPorConta[it] },
            mesDe = { estado.recDesMesPorConta[it] },
            aoGerenciar = aoGerenciarContas,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Receitas x Despesas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text("Últimos 6 meses", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                GraficoBarrasMensal(estado.evolucao)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Despesas por categoria · ${mesLabel(estado.mes)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text("Por categoria no mês", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                GraficoDonut(estado.donut, estado.totalDonut)
            }
        }

        SecaoAtividadeRecente(
            mes = estado.mes,
            recentes = estado.recentes,
            contaNomeDe = { vm.contaPorId(it) },
            corDeCategoria = { estado.corPorCategoria[it] },
            aoNovo = aoNovoLancamento,
        )
    }
}

/** Card "Atividade recente · mês" com `+ Novo lançamento`, como no web. */
@Composable
private fun SecaoAtividadeRecente(
    mes: String,
    recentes: List<ItemRecente>,
    contaNomeDe: (Int?) -> String,
    corDeCategoria: (String) -> String?,
    aoNovo: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Atividade recente · ${mesLabel(mes)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = aoNovo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    ),
                ) { Text("+ Novo lançamento") }
            }
            if (recentes.isEmpty()) {
                Text("Sem lançamentos no mês.", style = MaterialTheme.typography.bodyMedium)
            } else {
                recentes.forEachIndexed { i, item ->
                    val receita = item.tipo == "receita"
                    val corSinal = if (receita) VERDE else VERMELHO
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp)
                                .clip(CircleShape)
                                .background(corSinal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (receita) "+" else "−",
                                color = corSinal,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.descricao,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${formatarData(item.data)} · ${contaNomeDe(item.contaId).ifBlank { "—" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Spacer(Modifier.width(6.dp))
                                BadgeCategoria(item.categoria, corDeCategoria(item.categoria))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            item.valor.emReais(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (receita) VERDE else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (i < recentes.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

/** `BadgeCategoria` com a cor da categoria (espelha `ui.tsx:31-67`). */
@Composable
private fun BadgeCategoria(nome: String, corNome: String?) {
    val cor = corDe(corNome)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(cor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(nome, color = cor, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** `ResumoMes`: 3 cards com seta e subtítulo, como no web. */
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
    val rotuloMes = mesLabel(mes)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CartaoResumo(
            titulo = "Receitas · $rotuloMes",
            valor = receitas,
            carregando = carregando,
            detalhe = pluralLancamentos(qtdReceitas),
            corValor = VERDE,
            seta = Icons.Filled.NorthEast,
            corSeta = VERDE,
        )
        CartaoResumo(
            titulo = "Despesas · $rotuloMes",
            valor = despesas,
            carregando = carregando,
            detalhe = pluralLancamentos(qtdDespesas),
            corValor = VERMELHO,
            seta = Icons.Filled.SouthEast,
            corSeta = VERMELHO,
        )
        CartaoResumo(
            titulo = "Saldo do mês",
            valor = saldo,
            carregando = carregando,
            detalhe = if (saldo >= 0) "Receitas acima das despesas" else "Atenção — despesas acima das receitas",
            corValor = if (saldo >= 0) VERDE else VERMELHO,
            seta = Icons.Filled.SouthEast,
            corSeta = VERMELHO,
        )
    }
}

@Composable
private fun CartaoResumo(
    titulo: String,
    valor: Double,
    carregando: Boolean,
    detalhe: String,
    corValor: Color,
    seta: ImageVector,
    corSeta: Color,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (carregando) "..." else valor.emReais(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = corValor,
                )
                Spacer(Modifier.height(4.dp))
                Text(detalhe, style = MaterialTheme.typography.bodySmall)
            }
            Box(
                modifier = Modifier.size(36.dp)
                    .clip(CircleShape)
                    .background(corSeta.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(seta, contentDescription = null, tint = corSeta)
            }
        }
    }
}

/** Seção Contas com `Gerenciar` e cartões horizontais por conta. */
@Composable
private fun SecaoContas(
    mes: String,
    contas: List<Conta>,
    carregando: Boolean,
    saldoDe: (Int) -> Double?,
    mesDe: (Int) -> Pair<Double, Double>?,
    aoGerenciar: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Contas · ${mesLabel(mes)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = aoGerenciar) { Text("Gerenciar") }
            }
            if (contas.isEmpty()) {
                Text("Nenhuma conta.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    contas.forEach { conta ->
                        CartaoConta(
                            conta = conta,
                            carregando = carregando,
                            saldoAtual = saldoDe(conta.id) ?: conta.saldoInicial,
                            mes = mesDe(conta.id) ?: (0.0 to 0.0),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartaoConta(
    conta: Conta,
    carregando: Boolean,
    saldoAtual: Double,
    mes: Pair<Double, Double>,
) {
    val (recMes, desMes) = mes
    val saldoMes = recMes - desMes
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        modifier = Modifier.width(210.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${conta.icone.ifBlank { "💰" }}  ${conta.nome}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                if (carregando) "..." else saldoAtual.emReais(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (saldoAtual >= 0) VERDE else VERMELHO,
            )
            Text("saldo atual", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text(
                "+${recMes.emReais()} · −${desMes.emReais()}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "${if (saldoMes >= 0) "+" else "−"}${emReaisAbs(saldoMes)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (saldoMes >= 0) VERDE else VERMELHO,
            )
        }
    }
}

private fun emReaisAbs(valor: Double): String = kotlin.math.abs(valor).emReais()
