package com.fvcode.finfin.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.fvcode.finfin.core.util.mesLabel
import java.util.Locale

/** `MesNav`: `‹ mes ›` + Hoje (espelha `ui.tsx:240-267`). */
@Composable
fun MesNav(mes: String, aoAnterior: () -> Unit, aoProximo: () -> Unit, aoHoje: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = aoAnterior) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mês anterior")
        }
        Text(
            mesLabel(mes).replaceFirstChar { it.titlecase(Locale("pt", "BR")) },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = aoHoje) { Text("Hoje") }
        IconButton(onClick = aoProximo) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo mês")
        }
    }
}

/** Filtro de conta com "Todas as contas" + `★ principal`. */
@Composable
fun FiltroConta(
    contas: List<Pair<Int, String>>,
    selecionada: Int?,
    aoTrocar: (Int?) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    val rotulo = contas.firstOrNull { it.first == selecionada }?.second ?: "Todas as contas"
    Column {
        OutlinedButton(onClick = { aberto = true }) { Text(rotulo) }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            DropdownMenuItem(text = { Text("Todas as contas") }, onClick = {
                aoTrocar(null)
                aberto = false
            })
            contas.forEach { (id, nome) ->
                DropdownMenuItem(text = { Text(nome) }, onClick = {
                    aoTrocar(id)
                    aberto = false
                })
            }
        }
    }
}

@Composable
fun SecaoTitulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

/** `ConfirmarExclusao` (espelha `ui.tsx`). */
@Composable
fun DialogoConfirmarExclusao(
    titulo: String,
    texto: String,
    salvando: Boolean,
    textoConfirmar: String = "Excluir",
    aoCancelar: () -> Unit,
    aoConfirmar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            Button(enabled = !salvando, onClick = aoConfirmar) {
                Text(if (salvando) "Excluindo..." else textoConfirmar)
            }
        },
        dismissButton = {
            TextButton(enabled = !salvando, onClick = aoCancelar) { Text("Cancelar") }
        },
    )
}
