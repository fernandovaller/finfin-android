package com.fvcode.finfin.ui.relatorios

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.core.util.deslocarMes
import com.fvcode.finfin.core.util.emReais
import com.fvcode.finfin.ui.components.FinfinCard
import com.fvcode.finfin.ui.components.MesNav
import com.fvcode.finfin.ui.components.SecaoTitulo
import com.fvcode.finfin.ui.home.GraficoBarrasMensal
import com.fvcode.finfin.ui.home.GraficoDonut

/** Espelha `Relatorios.tsx`: presets, filtros, agregados, top 5, exportar PDF. */
@Composable
fun RelatoriosScreen(
    vm: RelatoriosViewModel = hiltViewModel(),
    aoSessaoExpirada: () -> Unit = {},
) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current
    val f = estado.filtros

    if (estado.sessaoExpirada) {
        vm.consumirSessaoExpirada()
        aoSessaoExpirada()
    }

    val intervalo = remember(f) { resolverIntervalo(f) }
    val resultado = remember(estado.receitas, estado.despesas, estado.contas, f, intervalo) {
        if (intervalo == null) null else {
            val (rec, des) = filtrar(estado.receitas, estado.despesas, f, intervalo)
            agregar(rec, des, estado.contas, estado.corPorCategoria, mesesDoIntervalo(intervalo))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SecaoTitulo("Relatórios")
            Spacer(Modifier.weight(1f))
            if (resultado != null && intervalo != null) {
                Button(onClick = {
                    ExportarRelatorio.imprimir(
                        contexto,
                        "finfin-relatorio",
                        ExportarRelatorio.html(ExportarRelatorio.descricaoPeriodo(f, intervalo), resultado),
                    )
                }) { Text("Exportar PDF") }
            }
        }

        // Presets: mes|6m|12m|ano|intervalo
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Presets.MES to "Mês",
                Presets.SEIS_M to "6m",
                Presets.DOZE_M to "12m",
                Presets.ANO to "Ano",
                Presets.INTERVALO to "Intervalo",
            ).forEach { (v, r) ->
                FilterChip(
                    selected = f.preset == v,
                    onClick = { vm.atualizarFiltros(f.copy(preset = v)) },
                    label = { Text(r) },
                )
            }
        }

        when (f.preset) {
            Presets.MES, Presets.SEIS_M, Presets.DOZE_M -> MesNav(
                mes = f.mes,
                aoAnterior = { vm.atualizarFiltros(f.copy(mes = deslocarMes(f.mes, -1))) },
                aoProximo = { vm.atualizarFiltros(f.copy(mes = deslocarMes(f.mes, 1))) },
                aoHoje = { vm.atualizarFiltros(f.copy(mes = java.time.YearMonth.now().toString())) },
            )
            Presets.ANO -> Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.atualizarFiltros(f.copy(ano = f.ano - 1)) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Ano anterior")
                }
                Text("${f.ano}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.atualizarFiltros(f.copy(ano = f.ano + 1)) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Próximo ano")
                }
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = f.ini,
                    onValueChange = { vm.atualizarFiltros(f.copy(ini = it)) },
                    label = { Text("Início (AAAA-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = f.fim,
                    onValueChange = { vm.atualizarFiltros(f.copy(fim = it)) },
                    label = { Text("Fim (AAAA-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
        }

        SeletorFiltro(
            rotulo = "Conta",
            valor = estado.contas.firstOrNull { it.id == f.contaId }?.nome ?: "Todas",
            opcoes = listOf("Todas") + estado.contas.map { it.nome },
            aoEscolher = { nome ->
                vm.atualizarFiltros(f.copy(contaId = estado.contas.firstOrNull { it.nome == nome }?.id))
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SeletorFiltro(
                rotulo = "Cat. receita",
                valor = f.catReceita.ifBlank { "Todas" },
                opcoes = listOf("Todas") + estado.categoriasReceita,
                aoEscolher = { vm.atualizarFiltros(f.copy(catReceita = if (it == "Todas") "" else it)) },
                modifier = Modifier.weight(1f),
            )
            SeletorFiltro(
                rotulo = "Cat. despesa",
                valor = f.catDespesa.ifBlank { "Todas" },
                opcoes = listOf("Todas") + estado.categoriasDespesa,
                aoEscolher = { vm.atualizarFiltros(f.copy(catDespesa = if (it == "Todas") "" else it)) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SeletorFiltro(
                rotulo = "Forma",
                valor = f.forma.ifBlank { "Todas" },
                opcoes = listOf("Todas") + estado.formas,
                aoEscolher = { vm.atualizarFiltros(f.copy(forma = if (it == "Todas") "" else it)) },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = f.busca,
                onValueChange = { vm.atualizarFiltros(f.copy(busca = it)) },
                label = { Text("Buscar") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = f.minTxt,
                onValueChange = { vm.atualizarFiltros(f.copy(minTxt = it)) },
                label = { Text("Mín (ex. 1.200,50)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = f.minTxt.isNotBlank() && parseValorBR(f.minTxt) == null,
            )
            OutlinedTextField(
                value = f.maxTxt,
                onValueChange = { vm.atualizarFiltros(f.copy(maxTxt = it)) },
                label = { Text("Máx") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = f.maxTxt.isNotBlank() && parseValorBR(f.maxTxt) == null,
            )
        }

        estado.erro?.let { msg ->
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text(msg, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.recarregar() }) { Text("Tentar de novo") }
                }
            }
        }

        if (estado.carregando) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (intervalo == null) {
            Text("Intervalo inválido. Confira as datas de início e fim.", color = MaterialTheme.colorScheme.error)
            return@Column
        }
        val r = resultado ?: return@Column

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CartaoTotal("Receitas", r.totalReceitas, "ticket ${r.ticketReceita.emReais()}", Modifier.weight(1f))
            CartaoTotal("Despesas", r.totalDespesas, "ticket ${r.ticketDespesa.emReais()}", Modifier.weight(1f))
        }
        CartaoTotal(
            "Saldo acumulado",
            r.acumulado,
            "${r.receitas.size} rec • ${r.despesas.size} des",
            Modifier.fillMaxWidth(),
            destaque = true,
        )

        SecaoTitulo("Evolução")
        GraficoBarrasMensal(r.porMes)

        SecaoTitulo("Despesas por categoria")
        GraficoDonut(r.porCategoria, r.totalDespesas)

        SecaoTitulo("Por forma de pagamento")
        if (r.porForma.isEmpty()) Text("Sem movimento.", style = MaterialTheme.typography.bodyMedium)
        r.porForma.forEach { (nome, total) ->
            LinhaTotal(nome, total)
        }

        SecaoTitulo("Por conta")
        if (r.porConta.isEmpty()) Text("Sem movimento.", style = MaterialTheme.typography.bodyMedium)
        r.porConta.forEach {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(it.conta.nome, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Rec ${it.receitas.emReais()} • Des ${it.despesas.emReais()} • Saldo ${(it.receitas - it.despesas).emReais()}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider()
        }

        SecaoTitulo("Top 5 despesas")
        if (r.topDespesas.isEmpty()) Text("Sem despesas.", style = MaterialTheme.typography.bodyMedium)
        r.topDespesas.forEachIndexed { i, item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("${i + 1}. ${item.titulo}", modifier = Modifier.weight(1f), maxLines = 1)
                Text(item.valor.emReais(), fontWeight = FontWeight.SemiBold)
            }
            Text(item.detalhe, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SeletorFiltro(
    rotulo: String,
    valor: String,
    opcoes: List<String>,
    aoEscolher: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var aberto by remember { mutableStateOf(false) }
    Column(modifier) {
        OutlinedButton(onClick = { aberto = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$rotulo: $valor", modifier = Modifier.weight(1f), maxLines = 1)
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { op ->
                DropdownMenuItem(text = { Text(op) }, onClick = {
                    aoEscolher(op)
                    aberto = false
                })
            }
        }
    }
}

@Composable
private fun CartaoTotal(
    titulo: String,
    valor: Double,
    detalhe: String,
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
) {
    FinfinCard(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium)
            Text(
                valor.emReais(),
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

@Composable
private fun LinhaTotal(nome: String, total: Double) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(nome, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(total.emReais(), fontWeight = FontWeight.SemiBold)
    }
}
