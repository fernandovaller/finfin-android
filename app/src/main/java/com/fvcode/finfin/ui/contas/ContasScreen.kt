package com.fvcode.finfin.ui.contas

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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.core.util.emReais
import com.fvcode.finfin.data.model.Conta
import com.fvcode.finfin.ui.components.SecaoTitulo

/** Sugestões de ícone (campo aceita qualquer emoji; fallback 💰). */
private val ICONES = listOf("💰", "💳", "🏦", "💵", "🏠", "🚗", "✈️", "🛒")

@Composable
fun ContasScreen(
    vm: ContasViewModel = hiltViewModel(),
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
            SecaoTitulo("Contas")
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
            Text("Nenhuma conta.", style = MaterialTheme.typography.bodyMedium)
        }
        estado.itens.forEachIndexed { i, item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.icone.ifBlank { "💰" } + "  " + item.nome,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                if (item.principal) {
                    AssistChip(onClick = {}, label = { Text("Principal") })
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = { vm.abrirEdicao(item) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { vm.pedirExclusao(item) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                }
            }
            Text(
                "Saldo inicial ${item.saldoInicial.emReais()}" +
                    (item.nota.ifBlank { "" }.let { if (it.isBlank()) "" else " • $it" }),
                style = MaterialTheme.typography.bodySmall,
            )
            if (i < estado.itens.lastIndex) HorizontalDivider()
        }
    }

    when (val d = estado.dialogo) {
        is DialogoConta.Novo -> DialogoContaForm(
            titulo = "Nova conta",
            inicial = ContaValores(),
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoFechar = { vm.fecharDialogo() },
            aoSalvar = { vm.salvar(it, null) },
        )
        is DialogoConta.Edicao -> DialogoContaForm(
            titulo = "Editar conta",
            inicial = ContaValores(
                nome = d.item.nome,
                saldoTxt = formatarSaldo(d.item.saldoInicial),
                nota = d.item.nota,
                icone = d.item.icone,
                principal = d.item.principal,
            ),
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoFechar = { vm.fecharDialogo() },
            aoSalvar = { vm.salvar(it, d.item) },
        )
        is DialogoConta.Exclusao -> DialogoExclusaoConta(
            item = d.item,
            salvando = estado.salvando,
            erro = estado.erroForm,
            aoCancelar = { vm.fecharDialogo() },
            aoConfirmar = { vm.excluir(d.item) },
        )
        DialogoConta.Oculto -> Unit
    }
}

@Composable
private fun DialogoContaForm(
    titulo: String,
    inicial: ContaValores,
    salvando: Boolean,
    erro: String?,
    aoFechar: () -> Unit,
    aoSalvar: (ContaValores) -> Unit,
) {
    var nome by remember(inicial) { mutableStateOf(inicial.nome) }
    var saldo by remember(inicial) { mutableStateOf(inicial.saldoTxt) }
    var nota by remember(inicial) { mutableStateOf(inicial.nota) }
    var icone by remember(inicial) { mutableStateOf(inicial.icone) }
    var principal by remember(inicial) { mutableStateOf(inicial.principal) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = saldo,
                    onValueChange = { saldo = it },
                    label = { Text("Saldo inicial (ex. 1.200,50)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = icone,
                    onValueChange = { icone = it },
                    label = { Text("Ícone (emoji)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ICONES.forEach { e ->
                        FilterChip(
                            selected = icone == e,
                            onClick = { icone = if (icone == e) "" else e },
                            label = { Text(e) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = principal, onCheckedChange = { principal = it })
                    Text("Conta principal")
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !salvando,
                onClick = {
                    aoSalvar(ContaValores(nome, saldo, nota, icone, principal))
                },
            ) { Text(if (salvando) "Salvando..." else "Salvar") }
        },
        dismissButton = {
            TextButton(enabled = !salvando, onClick = aoFechar) { Text("Cancelar") }
        },
    )
}

@Composable
private fun DialogoExclusaoConta(
    item: Conta,
    salvando: Boolean,
    erro: String?,
    aoCancelar: () -> Unit,
    aoConfirmar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Excluir conta?") },
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

private fun formatarSaldo(v: Double): String {
    return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}
