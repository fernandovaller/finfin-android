package com.fvcode.finfin.ui.lancamentos

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.core.util.emReais
import com.fvcode.finfin.core.util.formatarData
import com.fvcode.finfin.ui.components.FiltroConta
import com.fvcode.finfin.ui.components.MesNav
import java.text.NumberFormat
import java.util.Locale

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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MesNav(
            mes = estado.mes,
            aoAnterior = { vm.mudarMes(-1) },
            aoProximo = { vm.mudarMes(1) },
            aoHoje = { vm.irParaHoje() },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FiltroConta(
                contas = estado.contas.map { it.id to (it.nome + if (it.principal) " ★" else "") },
                selecionada = estado.contaFiltro,
                aoTrocar = { vm.trocarConta(it) },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("todos" to "Todos", "receita" to "Receitas", "despesa" to "Despesas").forEach { (v, r) ->
                FilterChip(
                    selected = estado.filtroTipo == v,
                    onClick = { vm.trocarFiltroTipo(v) },
                    label = { Text(r) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.abrirNovo("receita") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Receita")
            }
            Button(onClick = { vm.abrirNovo("despesa") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Despesa")
            }
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

        estado.info?.let { msg ->
            Card {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(msg, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.consumirInfo() }) { Text("OK") }
                }
            }
        }

        if (estado.carregando && estado.itens.isEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        }

        val visiveis = estado.itens.filter { estado.filtroTipo == "todos" || it.tipo == estado.filtroTipo }
        if (!estado.carregando && visiveis.isEmpty()) {
            Text("Sem lançamentos no mês.", style = MaterialTheme.typography.bodyMedium)
        }
        visiveis.forEachIndexed { i, item ->
            LinhaLancamento(
                item = item,
                contaNome = vm.contaPorId(item.contaId),
                aoEditar = { vm.abrirEdicao(item) },
                aoExcluir = { vm.pedirExclusao(item) },
            )
            if (i < visiveis.lastIndex) HorizontalDivider()
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
private fun LinhaLancamento(
    item: Lancamento,
    contaNome: String,
    aoEditar: () -> Unit,
    aoExcluir: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (item.tipo == "receita") "+" else "−",
            color = if (item.tipo == "receita") Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(item.titulo.ifBlank { item.categoria }, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                buildString {
                    append(item.categoria)
                    append(" • ")
                    append(formatarData(item.data))
                    if (contaNome.isNotBlank()) append(" • $contaNome")
                    if (item.parcelaAtual != null && item.parcelaTotal != null) {
                        append(" • (${item.parcelaAtual}/${item.parcelaTotal})")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(item.valor.emReais(), fontWeight = FontWeight.SemiBold)
        IconButton(onClick = aoEditar) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
        IconButton(onClick = aoExcluir) { Icon(Icons.Filled.Delete, contentDescription = "Excluir") }
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
