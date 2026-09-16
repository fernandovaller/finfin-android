package com.fvcode.finfin.ui.relatorios

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.core.util.deslocarMes
import com.fvcode.finfin.core.util.emReais
import com.fvcode.finfin.ui.components.FinfinCard
import com.fvcode.finfin.ui.components.MesNavEscuro
import com.fvcode.finfin.ui.home.GraficoBarrasMensal
import com.fvcode.finfin.ui.home.GraficoDonut

private val VERDE = Color(0xFF16A34A)
private val VERMELHO = Color(0xFFDC2626)

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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Navegação de período no topo, como Home/Lançamentos (MesNavEscuro primeiro).
        when (f.preset) {
            Presets.MES, Presets.SEIS_M, Presets.DOZE_M -> MesNavEscuro(
                mes = f.mes,
                aoAnterior = { vm.atualizarFiltros(f.copy(mes = deslocarMes(f.mes, -1))) },
                aoProximo = { vm.atualizarFiltros(f.copy(mes = deslocarMes(f.mes, 1))) },
                aoHoje = { vm.atualizarFiltros(f.copy(mes = java.time.YearMonth.now().toString())) },
            )
            Presets.ANO -> AnoNavEscuro(
                ano = f.ano,
                aoAnterior = { vm.atualizarFiltros(f.copy(ano = f.ano - 1)) },
                aoProximo = { vm.atualizarFiltros(f.copy(ano = f.ano + 1)) },
            )
            else -> FinfinCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
        }

        if (resultado != null && intervalo != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = {
                        ExportarRelatorio.imprimir(
                            contexto,
                            "finfin-relatorio",
                            ExportarRelatorio.html(ExportarRelatorio.descricaoPeriodo(f, intervalo), resultado),
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    ),
                ) { Text("Exportar PDF") }
            }
        }

        // Presets: mes|6m|12m|ano|intervalo
        FinfinCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Período", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            }
        }

        FinfinCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Filtros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            }
        }

        estado.erro?.let { msg ->
            FinfinCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(msg, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.recarregar() }) { Text("Tentar de novo") }
                }
            }
        }

        if (estado.carregando) {
            FinfinCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
            return@Column
        }

        if (intervalo == null) {
            FinfinCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Intervalo inválido. Confira as datas de início e fim.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            return@Column
        }
        val r = resultado ?: return@Column

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CartaoTotal(
                "Receitas",
                r.totalReceitas,
                "ticket ${r.ticketReceita.emReais()}",
                Modifier.fillMaxWidth(),
                corValor = VERDE,
                seta = Icons.Filled.NorthEast,
                corSeta = VERDE,
            )
            CartaoTotal(
                "Despesas",
                r.totalDespesas,
                "ticket ${r.ticketDespesa.emReais()}",
                Modifier.fillMaxWidth(),
                corValor = VERMELHO,
                seta = Icons.Filled.SouthEast,
                corSeta = VERMELHO,
            )
            CartaoTotal(
                "Saldo acumulado",
                r.acumulado,
                "${r.receitas.size} rec • ${r.despesas.size} des",
                Modifier.fillMaxWidth(),
                corValor = if (r.acumulado >= 0) VERDE else VERMELHO,
                seta = Icons.Filled.SouthEast,
                corSeta = VERMELHO,
            )
        }

        FinfinCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Evolução", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Receitas x despesas no período", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                GraficoBarrasMensal(r.porMes)
            }
        }

        FinfinCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Despesas por categoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Por categoria no período", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                GraficoDonut(r.porCategoria, r.totalDespesas)
            }
        }

        FinfinCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Por forma de pagamento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Totais no período", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                if (r.porForma.isEmpty()) Text("Sem movimento.", style = MaterialTheme.typography.bodyMedium)
                r.porForma.forEachIndexed { i, (nome, total) ->
                    LinhaTotal(nome, total)
                    if (i < r.porForma.size - 1) HorizontalDivider()
                }
            }
        }

        FinfinCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Por conta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Receitas, despesas e saldo", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                if (r.porConta.isEmpty()) Text("Sem movimento.", style = MaterialTheme.typography.bodyMedium)
                r.porConta.forEachIndexed { i, it ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(it.conta.nome, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Rec ${it.receitas.emReais()} • Des ${it.despesas.emReais()} • Saldo ${(it.receitas - it.despesas).emReais()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (i < r.porConta.size - 1) HorizontalDivider()
                }
            }
        }

        FinfinCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Top 5 despesas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Maiores gastos no período", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                if (r.topDespesas.isEmpty()) Text("Sem despesas.", style = MaterialTheme.typography.bodyMedium)
                r.topDespesas.forEachIndexed { i, item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("${i + 1}. ${item.titulo}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(8.dp))
                        Text(item.valor.emReais(), fontWeight = FontWeight.SemiBold)
                    }
                    Text(item.detalhe, style = MaterialTheme.typography.bodySmall)
                    if (i < r.topDespesas.size - 1) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AnoNavEscuro(
    ano: Int,
    aoAnterior: () -> Unit,
    aoProximo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fundo = MaterialTheme.colorScheme.inverseSurface
    val frente = MaterialTheme.colorScheme.inverseOnSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(fundo, RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        IconButton(onClick = aoAnterior) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ano anterior", tint = frente)
        }
        Text(
            "$ano",
            color = frente,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = aoProximo) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo ano", tint = frente)
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
    corValor: Color = VERDE,
    seta: ImageVector = Icons.Filled.NorthEast,
    corSeta: Color = VERDE,
) {
    FinfinCard(modifier) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    valor.emReais(),
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

@Composable
private fun LinhaTotal(nome: String, total: Double) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(nome, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Text(total.emReais(), fontWeight = FontWeight.SemiBold)
    }
}
