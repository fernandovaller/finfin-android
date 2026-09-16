package com.fvcode.finfin.ui.categorias

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.data.model.Categoria
import com.fvcode.finfin.ui.components.FinfinCard
import com.fvcode.finfin.ui.home.COR_POR_NOME
import com.fvcode.finfin.ui.home.PontoCor
import com.fvcode.finfin.ui.home.corDe

/** 8 cores válidas (`categoria.entity.ts:5-14`, `CORES_CATEGORIA`). */
private val CORES = listOf("sky", "violet", "amber", "pink", "emerald", "teal", "rose", "slate")

/** Espelha `Lancamentos.tsx`: filtros em card + lista em cards + modais novo/edição/exclusão. */
@Composable
fun CategoriasScreen(
    vm: CategoriasViewModel = hiltViewModel(),
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
        FinfinCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Filtros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("todos" to "Todas", "receita" to "Receitas", "despesa" to "Despesas").forEach { (v, r) ->
                        FilterChip(
                            selected = estado.filtroTipo == v,
                            onClick = { vm.trocarAba(v) },
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
            CardCategorias(
                titulo = "Receitas",
                itens = receitas,
                vazioTexto = "Sem categorias de receita.",
                aoEditar = { vm.abrirEdicao(it) },
                aoExcluir = { vm.pedirExclusao(it) },
            )
        }

        if (mostrarDespesas) {
            CardCategorias(
                titulo = "Despesas",
                itens = despesas,
                vazioTexto = "Sem categorias de despesa.",
                aoEditar = { vm.abrirEdicao(it) },
                aoExcluir = { vm.pedirExclusao(it) },
            )
        }
    }

    when (val d = estado.dialogo) {
        is DialogoCategoria.Novo -> DialogoCategoriaForm(
            titulo = if (d.tipo == "receita") "Nova categoria de receita" else "Nova categoria de despesa",
            tipoInicial = d.tipo,
            trocaTipo = true,
            tipoFixo = null,
            nomeInicial = "",
            corInicial = "slate",
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoFechar = { vm.fecharDialogo() },
            aoSalvar = { nome, cor, tipo -> vm.salvar(nome, cor, tipo, null) },
        )
        is DialogoCategoria.Edicao -> DialogoCategoriaForm(
            titulo = if (d.item.tipo == "receita") "Editar categoria de receita" else "Editar categoria de despesa",
            tipoInicial = d.item.tipo,
            trocaTipo = false,
            tipoFixo = d.item.tipo,
            nomeInicial = d.item.nome,
            corInicial = d.item.cor,
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoFechar = { vm.fecharDialogo() },
            aoSalvar = { nome, cor, _ -> vm.salvar(nome, cor, d.item.tipo, d.item) },
        )
        is DialogoCategoria.Exclusao -> DialogoExclusaoCategoria(
            item = d.item,
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoCancelar = { vm.fecharDialogo() },
            aoConfirmar = { vm.excluir(d.item) },
        )
        DialogoCategoria.Oculto -> Unit
    }
}

@Composable
private fun CardCategorias(
    titulo: String,
    itens: List<Categoria>,
    vazioTexto: String,
    aoEditar: (Categoria) -> Unit,
    aoExcluir: (Categoria) -> Unit,
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
                    "· ${itens.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (itens.isEmpty()) {
                Text(vazioTexto, style = MaterialTheme.typography.bodyMedium)
            }
            itens.forEachIndexed { i, item ->
                LinhaCategoria(
                    item = item,
                    aoEditar = { aoEditar(item) },
                    aoExcluir = { aoExcluir(item) },
                )
                if (i < itens.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun LinhaCategoria(
    item: Categoria,
    aoEditar: () -> Unit,
    aoExcluir: () -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }
    val cor = corDe(item.cor)
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp)
                .clip(CircleShape)
                .background(cor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            PontoCor(cor)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            item.nome,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
private fun DialogoCategoriaForm(
    titulo: String,
    tipoInicial: String,
    trocaTipo: Boolean,
    tipoFixo: String?,
    nomeInicial: String,
    corInicial: String,
    salvando: Boolean,
    erro: String?,
    aoFechar: () -> Unit,
    aoSalvar: (String, String, String) -> Unit,
) {
    var tipo by remember(tipoInicial) { mutableStateOf(tipoInicial) }
    var nome by remember(nomeInicial) { mutableStateOf(nomeInicial) }
    var cor by remember(corInicial) { mutableStateOf(corInicial) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (trocaTipo) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = tipo == "receita", onClick = { tipo = "receita" }, label = { Text("Receita") })
                        FilterChip(selected = tipo == "despesa", onClick = { tipo = "despesa" }, label = { Text("Despesa") })
                    }
                }
                tipoFixo?.let {
                    Text(
                        "Tipo: ${if (it == "receita") "receita" else "despesa"} (imutável)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text("Cor", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CORES.forEach { c ->
                        val selecionada = c == cor
                        Box(
                            modifier = Modifier.size(36.dp)
                                .clip(CircleShape)
                                .background(COR_POR_NOME.getValue(c))
                                .then(
                                    if (selecionada) Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape,
                                    ) else Modifier,
                                )
                                .clickable { cor = c },
                        )
                    }
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = !salvando, onClick = { aoSalvar(nome, cor, tipo) }) {
                Text(if (salvando) "Salvando..." else "Salvar")
            }
        },
        dismissButton = {
            TextButton(enabled = !salvando, onClick = aoFechar) { Text("Cancelar") }
        },
    )
}

@Composable
private fun DialogoExclusaoCategoria(
    item: Categoria,
    salvando: Boolean,
    erro: String?,
    aoCancelar: () -> Unit,
    aoConfirmar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Excluir ${if (item.tipo == "receita") "receita" else "despesa"}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\"${item.nome}\"")
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = !salvando, onClick = aoConfirmar) {
                Text(if (salvando) "Excluindo..." else "Excluir")
            }
        },
        dismissButton = {
            TextButton(enabled = !salvando, onClick = aoCancelar) { Text("Cancelar") }
        },
    )
}
