package com.fvcode.finfin.ui.home

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.fvcode.finfin.core.util.emReais
import com.fvcode.finfin.core.util.rotuloCurto

/** `COR_MAP` simplificado p/ dots/badges (claro; dots funcionam no escuro). */
val COR_POR_NOME: Map<String, Color> = mapOf(
    "sky" to Color(0xFF0284C7),
    "violet" to Color(0xFF7C3AED),
    "amber" to Color(0xFFD97706),
    "pink" to Color(0xFFDB2777),
    "emerald" to Color(0xFF059669),
    "teal" to Color(0xFF0D9488),
    "rose" to Color(0xFFF43F5E),
    "slate" to Color(0xFF64748B),
)

fun corDe(nome: String?): Color = COR_POR_NOME[nome] ?: COR_POR_NOME.getValue("slate")

/** `GraficoBarrasMensal` — 2 barras por mes (receita verde, despesa vermelha). */
@Composable
fun GraficoBarrasMensal(pontos: List<PontoMensal>, modifier: Modifier = Modifier) {
    val receitaCor = Color(0xFF16A34A)
    val despesaCor = Color(0xFFDC2626)
    val rotuloCor = MaterialTheme.colorScheme.onSurfaceVariant
    val pintura = remember(rotuloCor) {
        Paint().apply {
            color = rotuloCor.toArgb()
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
    }
    Column(modifier) {
        CanalLegenda(receitaCor, despesaCor)
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier.fillMaxWidth().height(220.dp),
        ) {
            if (pontos.isEmpty()) return@Canvas
            val max = (pontos.maxOf { maxOf(it.receitas, it.despesas) }).coerceAtLeast(1.0)
            val n = pontos.size
            val larguraSlot = size.width / n
            val alturaGrafico = size.height - 56f
            val larguraBarra = (larguraSlot * 0.28f).coerceAtMost(52f)
            pontos.forEachIndexed { i, p ->
                val centro = larguraSlot * i + larguraSlot / 2
                val hRec = (p.receitas / max * alturaGrafico).toFloat()
                val hDes = (p.despesas / max * alturaGrafico).toFloat()
                drawRoundRect(
                    color = receitaCor,
                    topLeft = androidx.compose.ui.geometry.Offset(centro - larguraBarra - 3f, alturaGrafico - hRec),
                    size = androidx.compose.ui.geometry.Size(larguraBarra, hRec.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(8f, 8f),
                )
                drawRoundRect(
                    color = despesaCor,
                    topLeft = androidx.compose.ui.geometry.Offset(centro + 3f, alturaGrafico - hDes),
                    size = androidx.compose.ui.geometry.Size(larguraBarra, hDes.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(8f, 8f),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    rotuloCurto(p.mes),
                    centro,
                    alturaGrafico + 36f,
                    pintura,
                )
            }
        }
    }
}

@Composable
private fun CanalLegenda(receitaCor: Color, despesaCor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PontoCor(receitaCor)
        Spacer(Modifier.width(4.dp))
        Text("Receitas", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(12.dp))
        PontoCor(despesaCor)
        Spacer(Modifier.width(4.dp))
        Text("Despesas", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun PontoCor(cor: Color) {
    Canvas(Modifier.size(10.dp)) {
        drawCircle(cor)
    }
}

/** `GraficoDonut` — top 7 + Outras; total 0 vira "Sem despesas". */
@Composable
fun GraficoDonut(fatias: List<FatiaCategoria>, total: Double, modifier: Modifier = Modifier) {
    if (total <= 0 || fatias.isEmpty()) {
        Text("Sem despesas", style = MaterialTheme.typography.bodyMedium)
        return
    }
    Column(modifier) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Canvas(Modifier.size(180.dp)) {
                var inicio = -90f
                fatias.forEach { f ->
                    val varredura = (f.total / total * 360).toFloat()
                    drawArc(
                        color = corDe(f.cor),
                        startAngle = inicio,
                        sweepAngle = (varredura - 2f).coerceAtLeast(0.5f),
                        useCenter = false,
                        style = Stroke(width = 64f),
                    )
                    inicio += varredura
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", style = MaterialTheme.typography.labelSmall)
                Text(total.emReais(), style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        fatias.forEach { f ->
            val pct = if (total > 0) f.total / total * 100 else 0.0
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                PontoCor(corDe(f.cor))
                Spacer(Modifier.width(8.dp))
                Text(
                    f.nome,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "%d%% • %s".format(pct.toInt(), f.total.emReais()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
