package com.fvcode.finfin.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fvcode.finfin.R
import com.fvcode.finfin.ui.servidor.ServidorFormulario
import com.fvcode.finfin.ui.session.SessaoViewModel
import com.fvcode.finfin.ui.theme.Marinho
import com.fvcode.finfin.ui.theme.Slate100

private val Verde = Color(0xFF059669)
private val CinzaTexto = Color(0xFF64748B)
private val TintaTitulo = Color(0xFF0F172A)
private val Azulado = Color(0xFF94A3B8)

/**
 * Tela pública de autenticação — modos entrar|criar com abas
 * (espelha `Login.tsx:8-145`).
 */
@Composable
fun TelaAuth(
    vm: SessaoViewModel,
    aoEntrar: () -> Unit,
    aoRecuperar: () -> Unit,
    modoInicialCriar: Boolean = false,
) {
    var modoCriar by remember(modoInicialCriar) { mutableStateOf(modoInicialCriar) }
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }
    var mostrarServidor by remember { mutableStateOf(false) }

    fun submeter() {
        erro = null
        if (modoCriar && nome.isBlank()) {
            erro = "Informe seu nome."
            return
        }
        if (email.isBlank()) {
            erro = "Informe o e-mail."
            return
        }
        if (senha.length < 8) {
            erro = "Senha deve ter no mínimo 8 caracteres."
            return
        }
        carregando = true
        if (modoCriar) {
            vm.criarConta(nome.trim(), email.trim(), senha, { carregando = false; aoEntrar() }) {
                carregando = false
                erro = it
            }
        } else {
            vm.entrar(email.trim(), senha, { carregando = false; aoEntrar() }) {
                carregando = false
                erro = it
            }
        }
    }

    AuthFundo {
        AuthCabecalho()
        AuthCard {
            AbasAuth(modoCriar = modoCriar, aoTrocar = {
                modoCriar = it
                erro = null
            })
            Spacer(Modifier.height(20.dp))
            if (modoCriar) {
                Text("Crie sua conta", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TintaTitulo)
                Text(
                    "Cada conta enxerga só os próprios lançamentos.",
                    fontSize = 14.sp,
                    color = CinzaTexto,
                )
            } else {
                Text("Acesse sua conta", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TintaTitulo)
                Text("Entre para ver seus lançamentos.", fontSize = 14.sp, color = CinzaTexto)
            }
            Spacer(Modifier.height(16.dp))
            if (modoCriar) {
                CampoRotulo("Nome")
                CampoAuth(
                    valor = nome,
                    aoTrocar = { nome = it; erro = null },
                    placeholder = "Seu nome",
                )
                Spacer(Modifier.height(12.dp))
            }
            CampoRotulo("E-mail")
            CampoAuth(
                valor = email,
                aoTrocar = { email = it; erro = null },
                placeholder = "voce@exemplo.com",
            )
            Spacer(Modifier.height(12.dp))
            CampoRotulo("Senha")
            CampoAuth(
                valor = senha,
                aoTrocar = { senha = it; erro = null },
                placeholder = if (modoCriar) "Mínimo 8 caracteres" else "Sua senha",
                senha = true,
            )
            erro?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
            BotaoVerde(
                texto = if (carregando) "Aguarde..." else if (modoCriar) "Criar conta" else "Entrar",
                habilitado = !carregando,
                aoClicar = ::submeter,
            )
            if (!modoCriar) {
                TextButton(onClick = aoRecuperar, modifier = Modifier.fillMaxWidth()) {
                    Text("Esqueci a senha", color = CinzaTexto, fontWeight = FontWeight.SemiBold)
                }
            }
            TextButton(onClick = { mostrarServidor = true }, modifier = Modifier.fillMaxWidth()) {
                Text("⚙ Alterar servidor", color = CinzaTexto, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (mostrarServidor) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mostrarServidor = false },
            title = { Text("Servidor da API") },
            text = { ServidorFormulario(aoTrocou = { mostrarServidor = false }) },
            confirmButton = {
                TextButton(onClick = { mostrarServidor = false }) { Text("Fechar") }
            },
        )
    }
}

/** `RecuperarSenha.tsx`: POST + mensagem genérica anti-enumeração. */
@Composable
fun TelaRecuperarSenha(
    vm: SessaoViewModel,
    aoVoltarLogin: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var enviado by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(false) }

    AuthFundo {
        AuthCabecalho()
        AuthCard {
            Text("Recuperar senha", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TintaTitulo)
            Text(
                "Informe o e-mail da conta para receber o link.",
                fontSize = 14.sp,
                color = CinzaTexto,
            )
            Spacer(Modifier.height(16.dp))
            if (enviado) {
                Text(
                    "Se o e-mail estiver cadastrado, você receberá o link em instantes.",
                    fontSize = 14.sp,
                    color = TintaTitulo,
                )
            } else {
                CampoRotulo("E-mail")
                CampoAuth(
                    valor = email,
                    aoTrocar = { email = it; erro = null },
                    placeholder = "voce@exemplo.com",
                )
                erro?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                Spacer(Modifier.height(16.dp))
                BotaoVerde(
                    texto = if (carregando) "Enviando..." else "Enviar link",
                    habilitado = !carregando,
                    aoClicar = {
                        if (email.isBlank()) {
                            erro = "Informe o e-mail."
                            return@BotaoVerde
                        }
                        carregando = true
                        erro = null
                        vm.recuperarSenha(email.trim(), {
                            carregando = false
                            enviado = true
                        }) {
                            carregando = false
                            erro = it
                        }
                    },
                )
            }
            TextButton(onClick = aoVoltarLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar ao login", color = CinzaTexto, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** `RedefinirSenha.tsx`: 3 estados (sem token / pronto / form 8–128). */
@Composable
fun TelaRedefinirSenha(
    vm: SessaoViewModel,
    token: String?,
    aoConcluir: () -> Unit,
) {
    var nova by remember { mutableStateOf("") }
    var confirmacao by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(false) }

    AuthFundo {
        AuthCabecalho()
        AuthCard {
            Text("Redefinir senha", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TintaTitulo)
            if (token.isNullOrBlank()) {
                Text(
                    "Link inválido ou expirado. Peça um novo link de recuperação.",
                    fontSize = 14.sp,
                    color = CinzaTexto,
                )
                Spacer(Modifier.height(16.dp))
                BotaoVerde(texto = "Voltar ao login", aoClicar = aoConcluir)
                return@AuthCard
            }
            if (ok) {
                Text(
                    "Senha redefinida com sucesso. Entre com a nova senha.",
                    fontSize = 14.sp,
                    color = TintaTitulo,
                )
                Spacer(Modifier.height(16.dp))
                BotaoVerde(texto = "Ir para o login", aoClicar = aoConcluir)
                return@AuthCard
            }
            Text("Escolha uma nova senha de 8 a 128 caracteres.", fontSize = 14.sp, color = CinzaTexto)
            Spacer(Modifier.height(16.dp))
            CampoRotulo("Nova senha")
            CampoAuth(
                valor = nova,
                aoTrocar = { nova = it; erro = null },
                placeholder = "Mínimo 8 caracteres",
                senha = true,
            )
            Spacer(Modifier.height(12.dp))
            CampoRotulo("Confirmar senha")
            CampoAuth(
                valor = confirmacao,
                aoTrocar = { confirmacao = it; erro = null },
                placeholder = "Repita a nova senha",
                senha = true,
            )
            erro?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
            BotaoVerde(
                texto = if (carregando) "Salvando..." else "Salvar nova senha",
                habilitado = !carregando,
                aoClicar = {
                    if (nova.length < 8) {
                        erro = "Senha deve ter no mínimo 8 caracteres."
                        return@BotaoVerde
                    }
                    if (nova != confirmacao) {
                        erro = "As senhas não conferem."
                        return@BotaoVerde
                    }
                    carregando = true
                    erro = null
                    vm.redefinirSenha(token, nova, {
                        carregando = false
                        ok = true
                    }) {
                        carregando = false
                        erro = it
                    }
                },
            )
            TextButton(onClick = aoConcluir, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar ao login", color = CinzaTexto, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// --- Componentes do layout de referência ---

@Composable
private fun AuthFundo(conteudo: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Marinho),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                conteudo()
            }
        }
    }
}

@Composable
private fun AuthCabecalho() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_finfin),
            contentDescription = "Logo FinFin",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
        )
        Spacer(Modifier.padding(6.dp))
        Column {
            Text("FinFin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Financeiro pessoal", color = Azulado, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AuthCard(conteudo: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            conteudo()
        }
    }
}

@Composable
private fun AbasAuth(modoCriar: Boolean, aoTrocar: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Slate100).padding(4.dp),
    ) {
        AbaAuth(
            texto = "Entrar",
            selecionada = !modoCriar,
            aoClicar = { aoTrocar(false) },
            modifier = Modifier.weight(1f),
        )
        AbaAuth(
            texto = "Criar conta",
            selecionada = modoCriar,
            aoClicar = { aoTrocar(true) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AbaAuth(texto: String, selecionada: Boolean, aoClicar: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(if (selecionada) Modifier.shadow(2.dp, shape).clip(shape).background(Color.White) else Modifier.clip(shape))
            .clickable(onClick = aoClicar)
            .padding(vertical = 10.dp),
    ) {
        Text(
            texto,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (selecionada) TintaTitulo else CinzaTexto,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CampoRotulo(texto: String) {
    Text(
        texto,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = TintaTitulo,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun CampoAuth(valor: String, aoTrocar: (String) -> Unit, placeholder: String, senha: Boolean = false) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoTrocar,
        placeholder = { Text(placeholder, color = Color(0xFF9CA3AF)) },
        singleLine = true,
        visualTransformation = if (senha) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color(0xFFCBD5E1),
            unfocusedBorderColor = Color(0xFFE2E8F0),
            focusedTextColor = TintaTitulo,
            unfocusedTextColor = TintaTitulo,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BotaoVerde(texto: String, aoClicar: () -> Unit, habilitado: Boolean = true) {
    Button(
        onClick = aoClicar,
        enabled = habilitado,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Verde,
            contentColor = Color.White,
            disabledContainerColor = Verde.copy(alpha = 0.6f),
        ),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        if (!habilitado && (texto == "Aguarde..." || texto == "Enviando..." || texto == "Salvando...")) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.padding(4.dp))
        }
        Text(texto, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
