package com.fvcode.finfin.ui.auditoria

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.data.model.AuditoriaItem
import com.fvcode.finfin.ui.components.SecaoTitulo
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

@Composable
fun AuditoriaScreen(
    vm: AuditoriaViewModel = hiltViewModel(),
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
            SecaoTitulo("Auditoria")
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { vm.abrirLimpeza() }) { Text("Limpar") }
        }

        FiltrosAuditoriaForm(
            filtros = estado.filtros,
            aoAplicar = { vm.aplicarFiltros(it) },
        )

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

        val pagina = estado.pagina
        if (estado.carregando && pagina == null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        }
        pagina?.let {
            Text(
                "Página ${it.pagina} de ${it.totalPaginas} • ${it.total} registro(s)",
                style = MaterialTheme.typography.bodySmall,
            )
            if (it.itens.isEmpty()) {
                Text("Sem registros.", style = MaterialTheme.typography.bodyMedium)
            }
            it.itens.forEachIndexed { i, item ->
                LinhaAuditoria(
                    item = item,
                    aoAbrir = { vm.abrirDetalhe(item) },
                    aoRestaurar = { vm.pedirRestaurar(item) },
                )
                if (i < it.itens.lastIndex) HorizontalDivider()
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    enabled = it.pagina > 1 && !estado.carregando,
                    onClick = { vm.mudarPagina(-1) },
                ) { Text("Anterior") }
                if (estado.carregando) CircularProgressIndicator()
                OutlinedButton(
                    enabled = it.pagina < it.totalPaginas && !estado.carregando,
                    onClick = { vm.mudarPagina(1) },
                ) { Text("Próxima") }
            }
        }
    }

    estado.detalhe?.let { item ->
        AlertDialog(
            onDismissRequest = { vm.fecharDetalhe() },
            title = { Text("${item.modulo} • ${item.acao}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(item.descricao.ifBlank { "Sem descrição" })
                    Text(formatarCriadoEm(item.criadoEm), style = MaterialTheme.typography.bodySmall)
                    Text(
                        jsonBonito(item.detalhes),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.fecharDetalhe() }) { Text("Fechar") }
            },
        )
    }

    estado.confirmarRestaurar?.let { item ->
        AlertDialog(
            onDismissRequest = { vm.cancelarRestaurar() },
            title = { Text("Restaurar item?") },
            text = { Text("\"${item.descricao.ifBlank { "${item.modulo} #${item.registroId ?: item.id}" }}\" será recriado como novo registro.") },
            confirmButton = {
                Button(enabled = !estado.ocupado, onClick = { vm.confirmarRestaurar() }) {
                    Text(if (estado.ocupado) "Restaurando..." else "Restaurar")
                }
            },
            dismissButton = {
                TextButton(enabled = !estado.ocupado, onClick = { vm.cancelarRestaurar() }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (estado.dialogoLimpeza) {
        var antesDe by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { vm.fecharLimpeza() },
            title = { Text("Limpar trilha?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Apaga tudo ou só o anterior a uma data. Vazio = tudo.")
                    OutlinedTextField(
                        value = antesDe,
                        onValueChange = { antesDe = it },
                        label = { Text("Antes de (AAAA-MM-DD, opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(enabled = !estado.ocupado, onClick = { vm.confirmarLimpeza(antesDe.trim()) }) {
                    Text(if (estado.ocupado) "Limpando..." else "Limpar")
                }
            },
            dismissButton = {
                TextButton(enabled = !estado.ocupado, onClick = { vm.fecharLimpeza() }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun FiltrosAuditoriaForm(filtros: FiltrosAuditoria, aoAplicar: (FiltrosAuditoria) -> Unit) {
    var modulo by remember(filtros) { mutableStateOf(filtros.modulo) }
    var acao by remember(filtros) { mutableStateOf(filtros.acao) }
    var descricao by remember(filtros) { mutableStateOf(filtros.descricao) }
    var dataInicio by remember(filtros) { mutableStateOf(filtros.dataInicio) }
    var dataFim by remember(filtros) { mutableStateOf(filtros.dataFim) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SeletorSimples(
                rotulo = "Módulo",
                valor = modulo.ifBlank { "Todos" },
                opcoes = listOf("") + MODULOS,
                rotuloOpcao = { if (it.isBlank()) "Todos" else it },
                aoEscolher = { modulo = it },
                modifier = Modifier.weight(1f),
            )
            SeletorSimples(
                rotulo = "Ação",
                valor = acao.ifBlank { "Todas" },
                opcoes = listOf("") + ACOES,
                rotuloOpcao = { if (it.isBlank()) "Todas" else it },
                aoEscolher = { acao = it },
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição contém") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = dataInicio,
                onValueChange = { dataInicio = it },
                label = { Text("De (AAAA-MM-DD)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = dataFim,
                onValueChange = { dataFim = it },
                label = { Text("Até (AAAA-MM-DD)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Button(
            onClick = {
                aoAplicar(
                    FiltrosAuditoria(
                        modulo = modulo,
                        acao = acao,
                        descricao = descricao.trim(),
                        dataInicio = dataInicio.trim(),
                        dataFim = dataFim.trim(),
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Filtrar") }
    }
}

@Composable
private fun SeletorSimples(
    rotulo: String,
    valor: String,
    opcoes: List<String>,
    rotuloOpcao: (String) -> String,
    aoEscolher: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var aberto by remember { mutableStateOf(false) }
    Column(modifier) {
        OutlinedButton(onClick = { aberto = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$rotulo: ${rotuloOpcao(valor)}", modifier = Modifier.weight(1f), maxLines = 1)
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
private fun LinhaAuditoria(item: AuditoriaItem, aoAbrir: () -> Unit, aoRestaurar: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = aoAbrir).padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip(onClick = {}, label = { Text(item.modulo) })
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            AssistChip(onClick = {}, label = { Text(item.acao) })
            Spacer(Modifier.weight(1f))
            if (item.restauravel()) {
                TextButton(onClick = aoRestaurar) { Text("Restaurar") }
            }
        }
        Text(
            item.descricao.ifBlank { "Sem descrição" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
        )
        Text(formatarCriadoEm(item.criadoEm), style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatarCriadoEm(iso: String): String {
    return if (iso.length >= 16) iso.take(16).replace("T", " ") else iso
}

private fun jsonBonito(raw: String?): String {
    if (raw.isNullOrBlank()) return "Sem detalhes."
    return try {
        GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(raw))
    } catch (_: Exception) {
        raw
    }
}
