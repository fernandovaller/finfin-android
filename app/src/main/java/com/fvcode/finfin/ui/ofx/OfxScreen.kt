package com.fvcode.finfin.ui.ofx

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.core.util.emReais
import com.fvcode.finfin.core.util.formatarData
import com.fvcode.finfin.data.model.Conta
import com.fvcode.finfin.ui.components.SecaoTitulo

@Composable
fun OfxScreen(
    vm: OfxViewModel = hiltViewModel(),
    aoSessaoExpirada: () -> Unit = {},
) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current

    if (estado.sessaoExpirada) {
        vm.consumirSessaoExpirada()
        aoSessaoExpirada()
    }

    val abrirArquivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                val nome = nomeArquivo(contexto.contentResolver, uri) ?: "extrato.ofx"
                val texto = contexto.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: ""
                vm.importarTexto(texto, nome)
            } catch (_: Exception) {
                vm.importarTexto("", "arquivo")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecaoTitulo("Importar OFX")

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

        if (estado.carregando) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        SeletorContaOfx(
            contas = estado.contas,
            selecionada = estado.contaId,
            aoEscolher = { vm.trocarConta(it) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SeletorTextoOfx(
                rotulo = "Cat. receita",
                valor = estado.categoriaReceita.ifBlank { "—" },
                opcoes = estado.categoriasReceita,
                aoEscolher = { vm.trocarCategoriaReceita(it) },
                modifier = Modifier.weight(1f),
            )
            SeletorTextoOfx(
                rotulo = "Cat. despesa",
                valor = estado.categoriaDespesa.ifBlank { "—" },
                opcoes = estado.categoriasDespesa,
                aoEscolher = { vm.trocarCategoriaDespesa(it) },
                modifier = Modifier.weight(1f),
            )
        }
        SeletorTextoOfx(
            rotulo = "Forma de pagamento",
            valor = estado.formaPagamento.ifBlank { "Não informada" },
            opcoes = listOf("") + estado.formas,
            rotuloOpcao = { if (it.isBlank()) "Não informada" else it },
            aoEscolher = { vm.trocarForma(it) },
        )

        OutlinedButton(
            onClick = { abrirArquivo.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (estado.nomeArquivo.isBlank()) "Escolher arquivo .ofx" else estado.nomeArquivo) }

        if (estado.linhas.isNotEmpty()) {
            val incluidos = estado.linhas.count { it.incluir }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$incluidos de ${estado.linhas.size} incluídos",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.marcarTodas(true) }) { Text("Todas") }
                TextButton(onClick = { vm.marcarTodas(false) }) { Text("Nenhuma") }
            }
            estado.linhas.forEachIndexed { i, linha ->
                LinhaPrevia(
                    linha = linha,
                    catsReceita = estado.categoriasReceita,
                    catsDespesa = estado.categoriasDespesa,
                    aoIncluir = { vm.alternarIncluir(i) },
                    aoTipo = { vm.trocarTipo(i, it) },
                    aoCategoria = { vm.trocarCategoriaLinha(i, it) },
                )
                if (i < estado.linhas.lastIndex) HorizontalDivider()
            }
            Button(
                enabled = !estado.enviando,
                onClick = { vm.enviar() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (estado.enviando) "Importando..." else "Importar selecionados") }
        }
    }
}

@Composable
private fun LinhaPrevia(
    linha: LinhaOfx,
    catsReceita: List<String>,
    catsDespesa: List<String>,
    aoIncluir: () -> Unit,
    aoTipo: (String) -> Unit,
    aoCategoria: (String) -> Unit,
) {
    val cats = if (linha.tipo == "receita") catsReceita else catsDespesa
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = linha.incluir, onCheckedChange = { aoIncluir() })
            Column(Modifier.weight(1f)) {
                Text(linha.item.descricao, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Text(
                    "${formatarData(linha.item.data)}${linha.item.fitid?.let { " • $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                linha.item.valor.emReais(),
                fontWeight = FontWeight.SemiBold,
                color = if (linha.tipo == "receita") Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = linha.tipo == "receita",
                onClick = { aoTipo("receita") },
                label = { Text("Receita") },
            )
            FilterChip(
                selected = linha.tipo == "despesa",
                onClick = { aoTipo("despesa") },
                label = { Text("Despesa") },
            )
            SeletorTextoOfx(
                rotulo = "Categoria",
                valor = linha.categoria.ifBlank { "—" },
                opcoes = if (linha.categoria.isNotBlank() && linha.categoria !in cats) {
                    listOf(linha.categoria) + cats
                } else cats,
                aoEscolher = aoCategoria,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SeletorContaOfx(contas: List<Conta>, selecionada: Int?, aoEscolher: (Int) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    val nome = contas.firstOrNull { it.id == selecionada }?.nome ?: "Conta"
    Column {
        OutlinedButton(onClick = { aberto = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Conta: $nome", modifier = Modifier.weight(1f))
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

@Composable
private fun SeletorTextoOfx(
    rotulo: String,
    valor: String,
    opcoes: List<String>,
    aoEscolher: (String) -> Unit,
    modifier: Modifier = Modifier,
    rotuloOpcao: (String) -> String = { it },
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

private fun nomeArquivo(resolver: android.content.ContentResolver, uri: android.net.Uri): String? {
    return try {
        resolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    } catch (_: Exception) {
        null
    }
}
