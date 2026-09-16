package com.fvcode.finfin.ui.perfil

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.ui.components.SecaoTitulo
import com.fvcode.finfin.ui.home.COR_POR_NOME
import com.fvcode.finfin.ui.session.SessaoViewModel

private val MIMES_ACEITOS = mapOf(
    "image/png" to "png",
    "image/jpeg" to "jpg",
    "image/jpg" to "jpg",
    "image/gif" to "gif",
    "image/webp" to "webp",
)

/** Limite `≤ 500k chars` de dataURL (~375KB) — `auth.service.ts:236-244`. */
private const val MAX_BYTES_AVATAR = 370_000

/** Espelha `Perfil.tsx`: avatar `dataURL ≤ 500k`, PUT perfil + PUT senha. */
@Composable
fun PerfilScreen(
    vm: PerfilViewModel = hiltViewModel(),
    sessao: SessaoViewModel = hiltViewModel(),
    aoSessaoExpirada: () -> Unit = {},
) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current

    if (estado.sessaoExpirada) {
        vm.consumirSessaoExpirada()
        aoSessaoExpirada()
    }

    var nome by remember(estado.usuario) { mutableStateOf(estado.usuario?.nome ?: "") }
    var email by remember(estado.usuario) { mutableStateOf(estado.usuario?.email ?: "") }
    var avatarNovo by remember { mutableStateOf<String?>(null) }
    var removerAvatar by remember { mutableStateOf(false) }
    var erroAvatar by remember { mutableStateOf<String?>(null) }

    val escolherImagem = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val mime = contexto.contentResolver.getType(uri)
            val tipo = MIMES_ACEITOS[mime]
            if (tipo == null) {
                erroAvatar = "Formato inválido. Use PNG, JPG, GIF ou WebP."
                return@rememberLauncherForActivityResult
            }
            val bytes = contexto.contentResolver.openInputStream(uri)?.use {
                it.readBytes()
            } ?: byteArrayOf()
            if (bytes.size > MAX_BYTES_AVATAR) {
                erroAvatar = "Imagem grande demais (máx ~375KB)."
                return@rememberLauncherForActivityResult
            }
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            avatarNovo = "data:image/$tipo;base64,$b64"
            removerAvatar = false
            erroAvatar = null
        } catch (_: Exception) {
            erroAvatar = "Falha ao ler a imagem."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecaoTitulo("Perfil")

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

        val usuario = estado.usuario
        if (usuario == null) {
            Text("Não foi possível carregar o perfil.", color = MaterialTheme.colorScheme.error)
            return@Column
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarPerfil(
                        nome = usuario.nome,
                        dataUrl = if (removerAvatar) null else avatarNovo ?: usuario.avatar,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { escolherImagem.launch(arrayOf("image/*")) }) {
                                Text("Escolher imagem")
                            }
                        }
                        if (avatarNovo != null || usuario.avatar != null) {
                            TextButton(onClick = {
                                avatarNovo = null
                                removerAvatar = true
                            }) { Text("Remover avatar") }
                        }
                    }
                }
                erroAvatar?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                estado.erroPerfil?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    enabled = !estado.salvandoPerfil,
                    onClick = {
                        val acao = when {
                            removerAvatar -> AcaoAvatar.Remover
                            avatarNovo != null -> AcaoAvatar.Trocar(avatarNovo!!)
                            else -> AcaoAvatar.Manter
                        }
                        vm.salvarPerfil(nome, email, acao) { sessao.sincronizar(it) }
                    },
                ) { Text(if (estado.salvandoPerfil) "Salvando..." else "Salvar perfil") }
            }
        }

        SecaoTitulo("Trocar senha")
        Card {
            var atual by remember { mutableStateOf("") }
            var nova by remember { mutableStateOf("") }
            var confirmacao by remember { mutableStateOf("") }
            androidx.compose.runtime.LaunchedEffect(estado.senhaOk) {
                if (estado.senhaOk) {
                    atual = ""
                    nova = ""
                    confirmacao = ""
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = atual,
                    onValueChange = { atual = it },
                    label = { Text("Senha atual") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = nova,
                    onValueChange = { nova = it },
                    label = { Text("Nova senha (8–128)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = confirmacao,
                    onValueChange = { confirmacao = it },
                    label = { Text("Confirmar nova senha") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                estado.erroSenha?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    enabled = !estado.trocandoSenha,
                    onClick = { vm.trocarSenha(atual, nova, confirmacao) },
                ) { Text(if (estado.trocandoSenha) "Trocando..." else "Trocar senha") }
                if (estado.senhaOk) {
                    Text(
                        "Senha trocada. Outras sessões foram encerradas.",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** `Avatar`: iniciais 1–2 letras, cor por hash `% 8` (espelha `ui.tsx:190-236`). */
@Composable
private fun AvatarPerfil(nome: String, dataUrl: String?) {
    val bitmap = remember(dataUrl) { dataUrl?.let { dataUrlParaBitmap(it) } }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Avatar",
            modifier = Modifier.size(72.dp).clip(CircleShape),
        )
        return
    }
    val iniciais = remember(nome) {
        nome.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            .let { partes ->
                when {
                    partes.isEmpty() -> "?"
                    partes.size == 1 -> partes[0].take(2).uppercase()
                    else -> "${partes[0].first()}${partes.last().first()}".uppercase()
                }
            }
    }
    val cor = remember(nome) {
        COR_POR_NOME.values.elementAt((nome.hashCode() and Int.MAX_VALUE) % COR_POR_NOME.size)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(72.dp).clip(CircleShape).background(cor),
    ) {
        Text(
            iniciais,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

private fun dataUrlParaBitmap(dataUrl: String): ImageBitmap? {
    return try {
        val b64 = dataUrl.substringAfter("base64,", "")
        if (b64.isEmpty()) return null
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
