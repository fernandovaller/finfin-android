package com.fvcode.finfin.ui.formas

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.data.model.FormaPagamento
import com.fvcode.finfin.ui.components.DialogoConfirmarExclusao
import com.fvcode.finfin.ui.components.SecaoTitulo

@Composable
fun FormasScreen(
    vm: FormasViewModel = hiltViewModel(),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            SecaoTitulo("Formas de pagamento")
            Spacer(Modifier.weight(1f))
            Button(onClick = { vm.abrirNovo() }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Nova")
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
        if (!estado.carregando && estado.itens.isEmpty()) {
            Text("Nenhuma forma de pagamento.", style = MaterialTheme.typography.bodyMedium)
        }
        estado.itens.forEachIndexed { i, item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.nome, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                IconButton(onClick = { vm.abrirEdicao(item) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { vm.pedirExclusao(item) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                }
            }
            if (i < estado.itens.lastIndex) HorizontalDivider()
        }
    }

    when (val d = estado.dialogo) {
        is DialogoForma.Novo -> DialogoFormaNome(
            titulo = "Nova forma de pagamento",
            nomeInicial = "",
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoFechar = { vm.fecharDialogo() },
            aoSalvar = { vm.salvar(it, null) },
        )
        is DialogoForma.Edicao -> DialogoFormaNome(
            titulo = "Editar forma de pagamento",
            nomeInicial = d.item.nome,
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoFechar = { vm.fecharDialogo() },
            aoSalvar = { vm.salvar(it, d.item) },
        )
        is DialogoForma.Exclusao -> DialogoExclusaoForma(
            item = d.item,
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoCancelar = { vm.fecharDialogo() },
            aoConfirmar = { vm.excluir(d.item) },
        )
        DialogoForma.Oculto -> Unit
    }
}

@Composable
private fun DialogoFormaNome(
    titulo: String,
    nomeInicial: String,
    salvando: Boolean,
    erro: String?,
    aoFechar: () -> Unit,
    aoSalvar: (String) -> Unit,
) {
    var nome by remember(nomeInicial) { mutableStateOf(nomeInicial) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = !salvando, onClick = { aoSalvar(nome) }) {
                Text(if (salvando) "Salvando..." else "Salvar")
            }
        },
        dismissButton = {
            TextButton(enabled = !salvando, onClick = aoFechar) { Text("Cancelar") }
        },
    )
}

@Composable
private fun DialogoExclusaoForma(
    item: FormaPagamento,
    salvando: Boolean,
    erro: String?,
    aoCancelar: () -> Unit,
    aoConfirmar: () -> Unit,
) {
    if (erro.isNullOrBlank()) {
        DialogoConfirmarExclusao(
            titulo = "Excluir forma?",
            texto = "\"${item.nome}\"",
            salvando = salvando,
            aoCancelar = aoCancelar,
            aoConfirmar = aoConfirmar,
        )
    } else {
        AlertDialog(
            onDismissRequest = aoCancelar,
            title = { Text("Excluir forma?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("\"${item.nome}\"")
                    Text(erro, color = MaterialTheme.colorScheme.error)
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
}
