package com.fvcode.finfin.ui.lancamentos

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.core.util.emReais
import com.fvcode.finfin.core.util.formatarData
import com.fvcode.finfin.core.util.mesLabel
import com.fvcode.finfin.ui.components.FiltroConta
import com.fvcode.finfin.ui.components.FinfinCard
import com.fvcode.finfin.ui.components.MesNavEscuro
import java.text.NumberFormat
import java.util.Locale

private val VERDE = Color(0xFF16A34A)
private val VERMELHO = Color(0xFFDC2626)

/** Espelha `Lancamentos.tsx`: lista + modais novo/edição/`ConfirmarExclusao`. */
@Composable
fun LancamentosScreen(
    vm: LancamentosViewModel = hiltViewModel(),
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

        FinfinCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Filtros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("todos" to "Todos", "receita" to "Receitas", "despesa" to "Despesas").forEach { (v, r) ->
                        FilterChip(
                            selected = estado.filtroTipo == v,
                            onClick = { vm.trocarFiltroTipo(v) },
                            label = { Text(r) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { vm.abrirNovo("receita") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        ),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Receita")
                    }
                    Button(
                        onClick = { vm.abrirNovo("despesa") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        ),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Despesa")
                    }
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

        estado.info?.let { msg ->
            FinfinCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(msg, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.consumirInfo() }) { Text("OK") }
                }
            }
        }

        val receitas = estado.itens.filter { it.tipo == "receita" }
        val despesas = estado.itens.filter { it.tipo == "despesa" }
        val mostrarReceitas = estado.filtroTipo == "todos" || estado.filtroTipo == "receita"
        val mostrarDespesas = estado.filtroTipo == "todos" || estado.filtroTipo == "despesa"

        if (estado.carregando && estado.itens.isEmpty()) {
            FinfinCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        if (mostrarReceitas) {
            CardLancamentos(
                titulo = "Receitas",
                mes = estado.mes,
                itens = receitas,
                vazioTexto = "Sem receitas no mês.",
                contaNomeDe = { vm.contaPorId(it) },
                aoEditar = { vm.abrirEdicao(it) },
                aoExcluir = { vm.pedirExclusao(it) },
            )
        }

        if (mostrarDespesas) {
            CardLancamentos(
                titulo = "Despesas",
                mes = estado.mes,
                itens = despesas,
                vazioTexto = "Sem despesas no mês.",
                contaNomeDe = { vm.contaPorId(it) },
                aoEditar = { vm.abrirEdicao(it) },
                aoExcluir = { vm.pedirExclusao(it) },
            )
        }
    }

    when (val d = estado.dialogo) {
        is DialogoLancamento.Novo -> DialogoForm(
            titulo = if (d.tipo == "receita") "Nova receita" else "Nova despesa",
            tipoInicial = d.tipo,
            trocaTipo = true,
            editando = null,
            estado = estado,
            contaPadrao = vm.contaPrincipalOuPrimeira(),
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoFechar = { vm.fecharDialogo() },
            aoSalvar = { valores, tipo -> vm.salvar(valores, tipo, null) },
        )
        is DialogoLancamento.Edicao -> DialogoForm(
            titulo = if (d.item.tipo == "receita") "Editar receita" else "Editar despesa",
            tipoInicial = d.item.tipo,
            trocaTipo = false,
            editando = d.item,
            estado = estado,
            contaPadrao = d.item.contaId,
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoFechar = { vm.fecharDialogo() },
            aoSalvar = { valores, tipo -> vm.salvar(valores, tipo, d.item) },
        )
        is DialogoLancamento.Exclusao -> DialogoExclusao(
            item = d.item,
            salvando = estado.salvando,
            aoCancelar = { vm.fecharDialogo() },
            aoExcluir = { grupo -> vm.excluir(d.item, grupo) },
        )
        DialogoLancamento.Oculto -> Unit
    }
}

@Composable
private fun CardLancamentos(
    titulo: String,
    mes: String,
    itens: List<Lancamento>,
    vazioTexto: String,
    contaNomeDe: (Int?) -> String,
    aoEditar: (Lancamento) -> Unit,
    aoExcluir: (Lancamento) -> Unit,
) {
    FinfinCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "· ${mesLabel(mes)} · ${itens.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (itens.isEmpty()) {
                Text(vazioTexto, style = MaterialTheme.typography.bodyMedium)
            }
            itens.forEachIndexed { i, item ->
                LinhaLancamento(
                    item = item,
                    contaNome = contaNomeDe(item.contaId),
                    aoEditar = { aoEditar(item) },
                    aoExcluir = { aoExcluir(item) },
                )
                if (i < itens.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun LinhaLancamento(
    item: Lancamento,
    contaNome: String,
    aoEditar: () -> Unit,
    aoExcluir: () -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }
    val receita = item.tipo == "receita"
    val corSinal = if (receita) VERDE else VERMELHO
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                item.titulo.ifBlank { item.categoria },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildString {
                        append(formatarData(item.data))
                        if (contaNome.isNotBlank()) append(" · $contaNome")
                        if (item.parcelaAtual != null && item.parcelaTotal != null) {
                            append(" (${item.parcelaAtual}/${item.parcelaTotal})")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(6.dp))
                BadgeCategoria(item.categoria)
            }
            if (item.formaPagamento.isNotBlank()) {
                Text(
                    item.formaPagamento,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            item.valor.emReais(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (receita) VERDE else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        // Um único overflow substitui os 2 IconButtons (economia de ~56dp por linha).
        Box {
            IconButton(
                onClick = { menuAberto = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Ações",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                DropdownMenuItem(
                    text = { Text("Editar") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = {
                        menuAberto = false
                        aoEditar()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        menuAberto = false
                        aoExcluir()
                    },
                )
            }
        }
    }
}

@Composable
private fun BadgeCategoria(nome: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(nome, color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/**
 * Form receita/despesa (espelha `LancamentoForm.tsx`): máscara `R$`,
 * parcelas só na criação de despesa, validações via `onErro`.
 */
@Composable
private fun DialogoForm(
    titulo: String,
    tipoInicial: String,
    trocaTipo: Boolean,
    editando: Lancamento?,
    estado: LancamentosUiState,
    contaPadrao: Int?,
    salvando: Boolean,
    erro: String?,
    aoFechar: () -> Unit,
    aoSalvar: (LancamentoValores, String) -> Unit,
) {
    var tipo by remember(tipoInicial) { mutableStateOf(tipoInicial) }
    var data by remember(editando) { mutableStateOf(editando?.data ?: java.time.LocalDate.now().toString()) }
    var centavos by remember(editando) {
        mutableStateOf(editando?.let { ((it.valor * 100).toLong()).toString() } ?: "")
    }
    var categoria by remember(editando) { mutableStateOf(editando?.categoria ?: "") }
    var tituloValor by remember(editando) { mutableStateOf(editando?.titulo ?: "") }
    var forma by remember(editando) { mutableStateOf(editando?.formaPagamento ?: "") }
    var contaId by remember(editando, contaPadrao) { mutableStateOf(editando?.contaId ?: contaPadrao) }
    var nota by remember(editando) { mutableStateOf(editando?.nota ?: "") }
    var parcelasTxt by remember { mutableStateOf("1") }

    val opcoesCategoria = if (tipo == "receita") estado.categoriasReceita else estado.categoriasDespesa
    // Preserva categoria histórica (catálogo async pode não contê-la).
    val catsExibidas = remember(opcoesCategoria, categoria) {
        if (categoria.isNotBlank() && categoria !in opcoesCategoria) listOf(categoria) + opcoesCategoria
        else opcoesCategoria
    }
    if (categoria.isBlank() && catsExibidas.isNotEmpty()) categoria = catsExibidas.first()
    if (contaId == null) contaId = contaPadrao

    val valorNum = (centavos.toLongOrNull() ?: 0) / 100.0
    val parcelasNum = parcelasTxt.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (trocaTipo) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = tipo == "receita", onClick = {
                            tipo = "receita"
                            categoria = ""
                        }, label = { Text("Receita") })
                        FilterChip(selected = tipo == "despesa", onClick = {
                            tipo = "despesa"
                            categoria = ""
                        }, label = { Text("Despesa") })
                    }
                }
                OutlinedTextField(
                    value = data,
                    onValueChange = { data = it },
                    label = { Text("Data (AAAA-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = formatarEntradaMoeda(centavos),
                    onValueChange = { centavos = it.filter(Char::isDigit).take(12) },
                    label = { Text("Valor") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("R$") },
                )
                Seletor(
                    rotulo = "Categoria",
                    valor = categoria,
                    opcoes = catsExibidas,
                    aoEscolher = { categoria = it },
                )
                OutlinedTextField(
                    value = tituloValor,
                    onValueChange = { tituloValor = it },
                    label = { Text(if (tipo == "receita") "Origem" else "Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Seletor(
                    rotulo = "Forma de pagamento",
                    valor = forma,
                    opcoes = listOf("") + estado.formas,
                    rotuloOpcao = { if (it.isBlank()) "Não informada" else it },
                    aoEscolher = { forma = it },
                )
                SeletorConta(
                    contas = estado.contas,
                    selecionada = contaId,
                    aoEscolher = { contaId = it },
                )
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (tipo == "despesa" && editando == null) {
                    OutlinedTextField(
                        value = parcelasTxt,
                        onValueChange = { parcelasTxt = it.filter(Char::isDigit).take(2) },
                        label = { Text("Parcelas (1–21)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    if (parcelasNum in 2..21 && valorNum > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("$parcelasNum x de ${(valorNum / parcelasNum).emReais()} (ajuste na última)") },
                        )
                    }
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !salvando,
                onClick = {
                    aoSalvar(
                        LancamentoValores(
                            data = data.trim(),
                            centavos = centavos,
                            categoria = categoria,
                            titulo = tituloValor.trim(),
                            formaPagamento = forma,
                            contaId = contaId,
                            nota = nota.trim(),
                            parcelas = parcelasNum,
                        ),
                        tipo,
                    )
                },
            ) { Text(if (salvando) "Salvando..." else "Salvar") }
        },
        dismissButton = {
            TextButton(enabled = !salvando, onClick = aoFechar) { Text("Cancelar") }
        },
    )
}

/** Exclusão com `Só esta | Todas` p/ despesa parcelada (`?escopo=grupo`). */
@Composable
private fun DialogoExclusao(
    item: Lancamento,
    salvando: Boolean,
    aoCancelar: () -> Unit,
    aoExcluir: (grupo: Boolean) -> Unit,
) {
    val parcelada = item.tipo == "despesa" && item.grupoParcela != null
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Excluir ${if (item.tipo == "receita") "receita" else "despesa"}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\"${item.titulo.ifBlank { item.categoria }}\" • ${item.valor.emReais()}")
                if (parcelada) {
                    Text("É uma despesa parcelada. O que deseja excluir?")
                    OutlinedButton(
                        enabled = !salvando,
                        onClick = { aoExcluir(true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Todas as parcelas") }
                }
            }
        },
        confirmButton = {
            Button(enabled = !salvando, onClick = { aoExcluir(false) }) {
                Text(if (parcelada) "Só esta" else "Excluir")
            }
        },
        dismissButton = {
            TextButton(enabled = !salvando, onClick = aoCancelar) { Text("Cancelar") }
        },
    )
}

@Composable
private fun Seletor(
    rotulo: String,
    valor: String,
    opcoes: List<String>,
    aoEscolher: (String) -> Unit,
    rotuloOpcao: (String) -> String = { it },
) {
    var aberto by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { aberto = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (valor.isBlank()) rotulo else rotuloOpcao(valor), modifier = Modifier.weight(1f))
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { op ->
                DropdownMenuItem(text = { Text(rotuloOpcao(op)) }, onClick = {
                    aoEscolher(op)
                    aberto = false
                })
            }
        }
    }
}

@Composable
private fun SeletorConta(
    contas: List<com.fvcode.finfin.data.model.Conta>,
    selecionada: Int?,
    aoEscolher: (Int) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    val nome = contas.firstOrNull { it.id == selecionada }?.nome ?: "Conta"
    Column {
        OutlinedButton(onClick = { aberto = true }, modifier = Modifier.fillMaxWidth()) {
            Text(nome, modifier = Modifier.weight(1f))
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            contas.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.nome + if (c.principal) " ★" else "") },
                    onClick = {
                        aoEscolher(c.id)
                        aberto = false
                    },
                )
            }
        }
    }
}

/** `mascaraMoeda`: dígitos → centavos → pt-BR 2 casas. */
private fun formatarEntradaMoeda(centavos: String): String {
    val n = centavos.toLongOrNull() ?: 0
    return NumberFormat.getNumberInstance(Locale("pt", "BR")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(n / 100.0)
}
