package com.fvcode.finfin.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fvcode.finfin.ui.home.COR_POR_NOME

/**
 * Avatar do usuário: foto `dataURL` quando disponível, senão iniciais 1–2 letras
 * com cor por hash `% 8` (espelha `ui.tsx:190-236` e `PerfilScreen`).
 */
@Composable
fun AvatarUsuario(
    nome: String,
    dataUrl: String?,
    modifier: Modifier = Modifier,
    tamanho: Dp = 48.dp,
) {
    val bitmap = remember(dataUrl) { dataUrl?.let { dataUrlParaBitmap(it) } }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Avatar de $nome",
            contentScale = ContentScale.Crop,
            modifier = modifier.size(tamanho).clip(CircleShape),
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
        modifier = modifier.size(tamanho).clip(CircleShape).background(cor),
    ) {
        Text(
            iniciais,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = if (tamanho <= 48.dp) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.titleLarge,
        )
    }
}

fun dataUrlParaBitmap(dataUrl: String): ImageBitmap? {
    return try {
        val b64 = dataUrl.substringAfter("base64,", "")
        if (b64.isEmpty()) return null
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
