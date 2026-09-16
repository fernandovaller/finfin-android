package com.fvcode.finfin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier,
) {
    var aberto by remember { mutableStateOf(false) }
    val rotulo = contas.firstOrNull { it.first == selecionada }?.second ?: "Todas as contas"
    Column(modifier) {
        OutlinedButton(onClick = { aberto = true }, modifier = Modifier.fillMaxWidth()) {
            Text(rotulo, modifier = Modifier.weight(1f), maxLines = 1)
        }
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

/**
 * Cartão padrão do app (paleta do web): fundo branco, borda #e2e8f0,
 * cantos 16dp. Use nas seções; mantenha `Card` puro p/ avisos/auxílios.
 */
@Composable
fun FinfinCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        content = content,
    )
}

/**
 * Pill escura do navegador de mês (`‹ setembro de 2026 📅 ›`), como no web.
 * Toque no rótulo volta para o mês atual.
 */
@Composable
fun MesNavEscuro(
    mes: String,
    aoAnterior: () -> Unit,
    aoProximo: () -> Unit,
    aoHoje: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fundo = MaterialTheme.colorScheme.inverseSurface
    val frente = MaterialTheme.colorScheme.inverseOnSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(fundo, RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        IconButton(onClick = aoAnterior) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mês anterior", tint = frente)
        }
        TextButton(onClick = aoHoje, modifier = Modifier.weight(1f)) {
            Text(
                mesLabel(mes),
                color = frente,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Icon(
                Icons.Filled.DateRange,
                contentDescription = null,
                tint = frente,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        IconButton(onClick = aoProximo) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo mês", tint = frente)
        }
    }
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
