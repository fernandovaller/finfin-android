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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.data.model.Categoria
import com.fvcode.finfin.ui.components.SecaoTitulo
import com.fvcode.finfin.ui.home.COR_POR_NOME
import com.fvcode.finfin.ui.home.PontoCor
import com.fvcode.finfin.ui.home.corDe

/** 8 cores válidas (`categoria.entity.ts:5-14`, `CORES_CATEGORIA`). */
private val CORES = listOf("sky", "violet", "amber", "pink", "emerald", "teal", "rose", "slate")

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

    val visiveis = estado.itens.filter { it.tipo == estado.aba }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SecaoTitulo("Categorias")
            Spacer(Modifier.weight(1f))
            Button(onClick = { vm.abrirNovo() }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Nova")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = estado.aba == "despesa",
                onClick = { vm.trocarAba("despesa") },
                label = { Text("Despesas") },
            )
            FilterChip(
                selected = estado.aba == "receita",
                onClick = { vm.trocarAba("receita") },
                label = { Text("Receitas") },
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
        if (!estado.carregando && visiveis.isEmpty()) {
            Text("Nenhuma categoria.", style = MaterialTheme.typography.bodyMedium)
        }
        visiveis.forEachIndexed { i, item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                PontoCor(corDe(item.cor))
                Spacer(Modifier.width(12.dp))
                Text(item.nome, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                IconButton(onClick = { vm.abrirEdicao(item) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { vm.pedirExclusao(item) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                }
            }
            if (i < visiveis.lastIndex) HorizontalDivider()
        }
    }

    when (val d = estado.dialogo) {
        is DialogoCategoria.Novo -> DialogoCategoriaForm(
            titulo = "Nova categoria (${if (estado.aba == "receita") "receita" else "despesa"})",
            nomeInicial = "",
            corInicial = "slate",
            tipoFixo = null,
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoFechar = { vm.fecharDialogo() },
            aoSalvar = { nome, cor -> vm.salvar(nome, cor, null) },
        )
        is DialogoCategoria.Edicao -> DialogoCategoriaForm(
            titulo = "Editar categoria",
            nomeInicial = d.item.nome,
            corInicial = d.item.cor,
            tipoFixo = d.item.tipo,
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoFechar = { vm.fecharDialogo() },
            aoSalvar = { nome, cor -> vm.salvar(nome, cor, d.item) },
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
private fun DialogoCategoriaForm(
    titulo: String,
    nomeInicial: String,
    corInicial: String,
    tipoFixo: String?,
    salvando: Boolean,
    erro: String?,
    aoFechar: () -> Unit,
    aoSalvar: (String, String) -> Unit,
) {
    var nome by remember(nomeInicial) { mutableStateOf(nomeInicial) }
    var cor by remember(corInicial) { mutableStateOf(corInicial) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            Button(enabled = !salvando, onClick = { aoSalvar(nome, cor) }) {
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
        title = { Text("Excluir categoria?") },
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
