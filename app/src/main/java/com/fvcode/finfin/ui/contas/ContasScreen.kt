package com.fvcode.finfin.ui.contas

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.core.util.emReais
import com.fvcode.finfin.data.model.Conta
import com.fvcode.finfin.ui.components.FinfinCard

/** Sugestões de ícone (campo aceita qualquer emoji; fallback 💰). */
private val ICONES = listOf("💰", "💳", "🏦", "💵", "🏠", "🚗", "✈️", "🛒")

/** Espelha `Lancamentos.tsx`: ação em card + lista em card + modais novo/edição/exclusão. */
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FinfinCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Contas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { vm.abrirNovo() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    ),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Nova conta")
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

        if (estado.carregando && estado.itens.isEmpty()) {
            FinfinCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        CardContas(
            itens = estado.itens,
            vazioTexto = "Nenhuma conta.",
            mostrarVazio = !estado.carregando,
            aoEditar = { vm.abrirEdicao(it) },
            aoExcluir = { vm.pedirExclusao(it) },
        )
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
private fun CardContas(
    itens: List<Conta>,
    vazioTexto: String,
    mostrarVazio: Boolean,
    aoEditar: (Conta) -> Unit,
    aoExcluir: (Conta) -> Unit,
) {
    FinfinCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Contas",
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
                if (mostrarVazio) Text(vazioTexto, style = MaterialTheme.typography.bodyMedium)
            }
            itens.forEachIndexed { i, item ->
                LinhaConta(
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
private fun LinhaConta(
    item: Conta,
    aoEditar: () -> Unit,
    aoExcluir: () -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.icone.ifBlank { "💰" })
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.nome,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.principal) {
                    Spacer(Modifier.width(6.dp))
                    BadgePrincipal()
                }
            }
            Text(
                buildString {
                    append("Saldo inicial ${item.saldoInicial.emReais()}")
                    if (item.nota.isNotBlank()) append(" · ${item.nota}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
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
private fun BadgePrincipal() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            "Principal",
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
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
