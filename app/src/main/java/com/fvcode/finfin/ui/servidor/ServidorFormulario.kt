package com.fvcode.finfin.ui.servidor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Formulário reutilizável de servidor: usado deslogado (login) e logado (config).
 * Testa via GET /api/saude antes de salvar.
 */
@Composable
fun ServidorFormulario(
    vm: ServidorViewModel = hiltViewModel(),
    aoTrocou: () -> Unit = {},
    compacto: Boolean = false,
) {
    val estado by vm.estado.collectAsState()

    if (estado.trechoTrocado) {
        vm.consumirTroca()
        aoTrocou()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!compacto) {
            Text(
                "Conectado em: ${estado.efetiva}" +
                    if (estado.personalizada) " (personalizado)" else " (padrão)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = estado.campo,
            onValueChange = vm::aoDigitar,
            label = { Text("URL do servidor") },
            placeholder = { Text("http://192.168.0.10:3001") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = estado.erroCampo != null,
            supportingText = {
                (estado.erroCampo ?: estado.resultado)?.let { Text(it) }
            },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                enabled = !estado.testando && !estado.salvando,
                onClick = vm::testar,
            ) {
                if (estado.testando) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                }
                Text(if (estado.testando) "Testando..." else "Testar")
            }
            Button(
                enabled = !estado.testando && !estado.salvando,
                onClick = { vm.salvar() },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (estado.salvando) "Salvando..." else "Salvar servidor")
            }
        }
        if (estado.personalizada) {
            TextButton(enabled = !estado.testando && !estado.salvando, onClick = { vm.restaurarPadrao() }) {
                Text("Restaurar padrão de build")
            }
        }
    }
}
