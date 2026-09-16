package com.fvcode.finfin.ui.formas

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.data.model.FormaPagamento
import com.fvcode.finfin.ui.components.FinfinCard

/** Espelha `Lancamentos.tsx`: ação em card + lista em card + modais novo/edição/exclusão. */
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FinfinCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Formas de pagamento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    Text("Nova forma")
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

        CardFormas(
            itens = estado.itens,
            vazioTexto = "Nenhuma forma de pagamento.",
            mostrarVazio = !estado.carregando,
            aoEditar = { vm.abrirEdicao(it) },
            aoExcluir = { vm.pedirExclusao(it) },
        )
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
private fun CardFormas(
    itens: List<FormaPagamento>,
    vazioTexto: String,
    mostrarVazio: Boolean,
    aoEditar: (FormaPagamento) -> Unit,
    aoExcluir: (FormaPagamento) -> Unit,
) {
    FinfinCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Formas de pagamento",
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
                LinhaForma(
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
private fun LinhaForma(
    item: FormaPagamento,
    aoEditar: () -> Unit,
    aoExcluir: () -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Excluir forma?") },
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
