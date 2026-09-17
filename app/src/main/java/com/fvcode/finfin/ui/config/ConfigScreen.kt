package com.fvcode.finfin.ui.config

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.core.datastore.FinfinPreferences
import com.fvcode.finfin.ui.components.FinfinCard
import com.fvcode.finfin.ui.servidor.ServidorFormulario
import com.fvcode.finfin.ui.session.SessaoViewModel

private val ABAS = listOf("Geral", "Backup", "E-mail", "Perigo")

/** Espelha `Configuracoes.tsx`: 4 abas (geral, backup, email, perigo). */
@Composable
fun ConfigScreen(
    vm: ConfigViewModel = hiltViewModel(),
    sessao: SessaoViewModel = hiltViewModel(),
    aoSessaoExpirada: () -> Unit = {},
) {
    val estado by vm.estado.collectAsState()
    val tema by sessao.tema.collectAsState()
    val contexto = LocalContext.current

    if (estado.sessaoExpirada) {
        vm.consumirSessaoExpirada()
        aoSessaoExpirada()
    }

    val criarArquivo = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        val pendente = estado.arquivoPendente
        if (uri != null && pendente != null) {
            try {
                contexto.contentResolver.openOutputStream(uri)?.use { saida ->
                    if (pendente.comBom) saida.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    saida.write(pendente.texto.toByteArray(Charsets.UTF_8))
                }
                vm.informar("Arquivo salvo: ${pendente.nomeSugerido}")
            } catch (_: Exception) {
                vm.informar("Falha ao salvar o arquivo.")
            }
        }
        vm.consumirArquivoPendente()
    }

    val abrirBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                val texto = contexto.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: ""
                vm.prepararImportacao(texto)
            } catch (_: Exception) {
                vm.prepararImportacao("")
            }
        }
    }

    LaunchedEffect(estado.arquivoPendente) {
        estado.arquivoPendente?.let { criarArquivo.launch(it.nomeSugerido) }
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = estado.aba) {
            ABAS.forEachIndexed { i, titulo ->
                Tab(
                    selected = estado.aba == i,
                    onClick = { vm.trocarAba(i) },
                    text = { Text(titulo) },
                )
            }
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
            if (estado.carregando && estado.contagem == null) {
                FinfinCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            when (estado.aba) {
                0 -> AbaGeral(
                    estado = estado,
                    tema = tema,
                    aoTema = { sessao.salvarTema(it) },
                    aoServidorTrocado = {
                        sessao.sair { aoSessaoExpirada() }
                    },
                )
                1 -> AbaBackup(
                    estado = estado,
                    ocupado = estado.ocupado,
                    aoExportarJson = { vm.exportarJson() },
                    aoExportarCsv = { vm.exportarCsv(it) },
                    aoImportar = { abrirBackup.launch(arrayOf("application/json")) },
                    aoRestaurar = { vm.restaurarPadrao() },
                    aoGerarDemo = { vm.gerarDemo() },
                    aoRemoverDemo = { vm.removerDemo() },
                )
                2 -> AbaEmail(
                    estado = estado,
                    ocupado = estado.ocupado,
                    aoSalvar = { vm.salvarChaveEmail(it) },
                )
                3 -> AbaPerigo(
                    ocupado = estado.ocupado,
                    aoApagarLancamentos = { vm.apagarLancamentos() },
                    aoApagarTudo = { vm.apagarTudo() },
                )
            }
        }
    }

    estado.previaBackup?.let { previa ->
        var modo by remember(previa) { mutableStateOf(estado.modoImport) }
        AlertDialog(
            onDismissRequest = { vm.fecharPrevia() },
            title = { Text("Importar backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${previa.contas} contas • ${previa.receitas} receitas • " +
                            "${previa.despesas} despesas • ${previa.categorias} categorias • " +
                            "${previa.formas} formas",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = modo == "mesclar",
                            onClick = { modo = "mesclar" },
                            label = { Text("Mesclar") },
                        )
                        FilterChip(
                            selected = modo == "substituir",
                            onClick = { modo = "substituir" },
                            label = { Text("Substituir") },
                        )
                    }
                    if (modo == "substituir") {
                        Text(
                            "Substituir apaga receitas, despesas e contas atuais antes de importar.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !estado.ocupado,
                    onClick = {
                        vm.trocarModoImport(modo)
                        vm.confirmarImportacao()
                    },
                ) { Text("Importar") }
            },
            dismissButton = {
                TextButton(onClick = { vm.fecharPrevia() }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun AbaGeral(
    estado: ConfigUiState,
    tema: String,
    aoTema: (String) -> Unit,
    aoServidorTrocado: () -> Unit = {},
) {
    val c = estado.contagem
    FinfinCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Dados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CartaoNumero("Contas", c?.contas, Modifier.weight(1f))
                CartaoNumero("Receitas", c?.receitas, Modifier.weight(1f))
                CartaoNumero("Despesas", c?.despesas, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CartaoNumero("Categorias", c?.categorias, Modifier.weight(1f))
                CartaoNumero("Formas", c?.formas, Modifier.weight(1f))
            }
        }
    }
    FinfinCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Aparência", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    FinfinPreferences.TEMA_CLARO to "Claro",
                    FinfinPreferences.TEMA_ESCURO to "Escuro",
                    FinfinPreferences.TEMA_SISTEMA to "Sistema",
                ).forEach { (v, r) ->
                    FilterChip(selected = tema == v, onClick = { aoTema(v) }, label = { Text(r) })
                }
            }
        }
    }
    FinfinCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Servidor da API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Trocar de servidor desconecta (token do backend antigo não vale no novo). " +
                    "A URL é testada via GET /api/saude antes de salvar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ServidorFormulario(aoTrocou = aoServidorTrocado)
        }
    }
}

@Composable
private fun CartaoNumero(titulo: String, valor: Int?, modifier: Modifier = Modifier) {
    FinfinCard(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium)
            Text(
                valor?.toString() ?: "...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AbaBackup(
    estado: ConfigUiState,
    ocupado: Boolean,
    aoExportarJson: () -> Unit,
    aoExportarCsv: (String) -> Unit,
    aoImportar: () -> Unit,
    aoRestaurar: () -> Unit,
    aoGerarDemo: () -> Unit,
    aoRemoverDemo: () -> Unit,
) {
    val demo = estado.demo
    FinfinCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Demonstração", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                if (demo == null) "Carregando..."
                else if (demo.existe) "Demo ativa: ${demo.contas} contas, ${demo.receitas} receitas, ${demo.despesas} despesas."
                else "Sem dados de demonstração.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !ocupado && demo?.existe != true,
                    onClick = aoGerarDemo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    ),
                ) { Text("Gerar demo") }
                OutlinedButtonBotao(
                    enabled = !ocupado && demo?.existe == true,
                    onClick = aoRemoverDemo,
                    texto = "Remover demo",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    FinfinCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Exportar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(
                enabled = !ocupado,
                onClick = aoExportarJson,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ),
            ) {
                Text("Backup JSON")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !ocupado,
                    onClick = { aoExportarCsv("receitas") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    ),
                ) {
                    Text("CSV receitas")
                }
                Button(
                    enabled = !ocupado,
                    onClick = { aoExportarCsv("despesas") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    ),
                ) {
                    Text("CSV despesas")
                }
            }
        }
    }

    FinfinCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Importar / restaurar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !ocupado,
                    onClick = aoImportar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    ),
                ) {
                    Text("Importar backup")
                }
                OutlinedButtonBotao(
                    enabled = !ocupado,
                    onClick = aoRestaurar,
                    texto = "Repor padrões",
                    modifier = Modifier.weight(1f),
                )
            }
            if (ocupado) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun OutlinedButtonBotao(
    enabled: Boolean,
    onClick: () -> Unit,
    texto: String,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(enabled = enabled, onClick = onClick, modifier = modifier) { Text(texto) }
}

@Composable
private fun AbaEmail(estado: ConfigUiState, ocupado: Boolean, aoSalvar: (String) -> Unit) {
    var chave by remember { mutableStateOf("") }
    var mostrar by remember { mutableStateOf(false) }
    val st = estado.integracoes?.email
    FinfinCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("E-mail (Resend)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                when {
                    st == null -> "Carregando..."
                    st.configurado -> "Configurado (${st.origem ?: "conta"}) • ${st.mascarada ?: "••••"}"
                    else -> "Não configurado. Sem chave, a recuperação de senha só registra no log do servidor."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = chave,
                onValueChange = { chave = it },
                label = { Text("Nova chave (vazio limpa)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (mostrar) androidx.compose.ui.text.input.VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { mostrar = !mostrar }) { Text(if (mostrar) "Ocultar" else "Ver") }
                },
            )
            Button(
                enabled = !ocupado,
                onClick = { aoSalvar(chave); chave = "" },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ),
            ) { Text("Salvar chave") }
        }
    }
}

@Composable
private fun AbaPerigo(ocupado: Boolean, aoApagarLancamentos: () -> Unit, aoApagarTudo: () -> Unit) {
    FinfinCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Zona de perigo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Ações irreversíveis. Digite APAGAR para liberar cada botão.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    CartaoPerigo(
        titulo = "Apagar lançamentos",
        descricao = "Apaga receitas e despesas. Mantém catálogo e contas.",
        ocupado = ocupado,
        aoConfirmar = aoApagarLancamentos,
    )
    CartaoPerigo(
        titulo = "Apagar tudo",
        descricao = "Apaga lançamentos e contas. Mantém catálogo, perfil e auditoria.",
        ocupado = ocupado,
        aoConfirmar = aoApagarTudo,
    )
}

@Composable
private fun CartaoPerigo(titulo: String, descricao: String, ocupado: Boolean, aoConfirmar: () -> Unit) {
    var confirmacao by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf(false) }
    val liberado = confirmacao.trim() == "APAGAR"
    FinfinCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(descricao, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = confirmacao,
                onValueChange = { confirmacao = it },
                label = { Text("Digite APAGAR") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                enabled = liberado && !ocupado,
                onClick = { confirmar = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text(titulo) }
        }
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Confirmar: $titulo?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmar = false
                        confirmacao = ""
                        aoConfirmar()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) { Text("Cancelar") }
            },
        )
    }
}
