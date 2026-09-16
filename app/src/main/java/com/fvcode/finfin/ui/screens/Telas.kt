package com.fvcode.finfin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fvcode.finfin.ui.session.SessaoViewModel

@Composable
fun TelaCarga() {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Carregando...")
    }
}

@Composable
fun TelaLogin(vm: SessaoViewModel, aoEntrar: () -> Unit) {
    var modoCriar by remember { mutableStateOf(false) }
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("FinFin", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (modoCriar) {
            OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") })
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha (8-128)") },
            visualTransformation = PasswordVisualTransformation(),
        )
        erro?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (modoCriar) vm.criarConta(nome, email, senha, aoEntrar) { erro = it }
            else vm.entrar(email, senha, aoEntrar) { erro = it }
        }) {
            Text(if (modoCriar) "Criar conta" else "Entrar")
        }
        TextButton(onClick = { modoCriar = !modoCriar }) {
            Text(if (modoCriar) "Já tenho conta" else "Criar conta")
        }
    }
}

@Composable
fun TelaSimples(titulo: String, subtitulo: String = "Em construção na base A.") {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(titulo, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(subtitulo, style = MaterialTheme.typography.bodyMedium)
    }
}
