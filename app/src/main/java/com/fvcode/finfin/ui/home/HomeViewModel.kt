package com.fvcode.finfin.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.core.util.deslocarMes
import com.fvcode.finfin.core.util.mesAtual
import com.fvcode.finfin.core.util.ultimosMeses
import com.fvcode.finfin.data.model.Conta
import com.fvcode.finfin.data.model.Resumo
import com.fvcode.finfin.data.repository.FinfinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Ponto mensal p/ barras (espelha `PontoMensal` de `Graficos.tsx`). */
data class PontoMensal(
    val mes: String,
    val receitas: Double,
    val despesas: Double,
)

/** Fatia de categoria p/ donut (espelha `FatiaCategoria`). */
data class FatiaCategoria(
    val nome: String,
    val cor: String,
    val total: Double,
)

/** Item unificado p/ "Atividade recente" (top 8, sort desc por data). */
data class ItemRecente(
    val tipo: String, // receita|despesa
    val descricao: String,
    val categoria: String,
    val valor: Double,
    val data: String,
    val contaId: Int?,
)

data class HomeUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    val mes: String = mesAtual(),
    val contaFiltro: Int? = null,
    val contas: List<Conta> = emptyList(),
    val resumo: Resumo? = null,
    val qtdReceitas: Int = 0,
    val qtdDespesas: Int = 0,
    val saldosPorConta: Map<Int, Double> = emptyMap(),
    val recDesMesPorConta: Map<Int, Pair<Double, Double>> = emptyMap(),
    val evolucao: List<PontoMensal> = emptyList(),
    val donut: List<FatiaCategoria> = emptyList(),
    val totalDonut: Double = 0.0,
    val recentes: List<ItemRecente> = emptyList(),
    val corPorCategoria: Map<String, String> = emptyMap(),
)

/**
 * Espelha `Home.tsx:28-305`: `Promise.all` receitas+despesas+resumo,
 * recarrega a cada `mes, contaFiltro`, deriva o resto no client.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: FinfinRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(HomeUiState())
    val estado: StateFlow<HomeUiState> = _estado

    init {
        carregar()
    }

    fun recarregar() = carregar()

    fun mudarMes(delta: Long) {
        _estado.value = _estado.value.copy(mes = deslocarMes(_estado.value.mes, delta))
        carregar()
    }

    fun irParaHoje() {
        _estado.value = _estado.value.copy(mes = mesAtual())
        carregar()
    }

    fun trocarConta(contaId: Int?) {
        _estado.value = _estado.value.copy(contaFiltro = contaId)
        carregar()
    }

    fun consumirSessaoExpirada() {
        _estado.value = _estado.value.copy(sessaoExpirada = false)
    }

    fun contaPorId(id: Int?): String =
        _estado.value.contas.firstOrNull { it.id == id }?.nome ?: ""

    private fun carregar() {
        val atual = _estado.value
        _estado.value = atual.copy(carregando = true, erro = null)
        viewModelScope.launch {
            val mes = _estado.value.mes
            val contaFiltro = _estado.value.contaFiltro
            try {
                val (rResumo, rReceitas, rDespesas, rContas, rCats) = coroutineScope {
                    val dResumo = async { repo.resumo(mes, contaFiltro) }
                    val dRec = async { repo.receitas(contaFiltro) }
                    val dDes = async { repo.despesas(contaFiltro) }
                    val dContas = async { repo.contas() }
                    val dCats = async { repo.categorias() }
                    Quinteto(dResumo.await(), dRec.await(), dDes.await(), dContas.await(), dCats.await())
                }

                // 401 em qualquer chamada -> sessao expirada (repo ja limpou o token).
                val expirada = listOf(rResumo, rReceitas, rDespesas, rContas, rCats)
                    .any { it is ApiResult.Erro && it.codigo == 401 }
                if (expirada) {
                    _estado.value = _estado.value.copy(carregando = false, sessaoExpirada = true)
                    return@launch
                }

                val primeiroErro = listOf(rResumo, rReceitas, rDespesas, rContas, rCats)
                    .filterIsInstance<ApiResult.Erro>().firstOrNull()
                // Contas tem `.catch(() => [])` no web; aqui toleramos falha de contas/categorias.
                val contas = (rContas as? ApiResult.Ok)?.dados ?: emptyList()
                val corPorCategoria = ((rCats as? ApiResult.Ok)?.dados ?: emptyList())
                    .associate { it.nome to it.cor }

                if (primeiroErro != null && (rResumo is ApiResult.Erro || rReceitas is ApiResult.Erro || rDespesas is ApiResult.Erro)) {
                    _estado.value = _estado.value.copy(carregando = false, erro = primeiroErro.mensagem)
                    return@launch
                }

                val receitas = (rReceitas as? ApiResult.Ok)?.dados ?: emptyList()
                val despesas = (rDespesas as? ApiResult.Ok)?.dados ?: emptyList()
                val resumo = (rResumo as? ApiResult.Ok)?.dados

                val receitasMes = receitas.filter { it.data.startsWith(mes) }
                val despesasMes = despesas.filter { it.data.startsWith(mes) }

                val recentes = (receitasMes.map {
                    ItemRecente("receita", it.origem.ifBlank { it.categoria }, it.categoria, it.valor, it.data, it.contaId)
                } + despesasMes.map {
                    val desc = it.descricao.ifBlank { it.categoria }
                    ItemRecente("despesa", desc, it.categoria, it.valor, it.data, it.contaId)
                }).sortedByDescending { it.data }.take(8)

                val evolucao = ultimosMeses(mes, 6).map { m ->
                    PontoMensal(
                        mes = m,
                        receitas = receitas.filter { it.data.startsWith(m) }.sumOf { it.valor },
                        despesas = despesas.filter { it.data.startsWith(m) }.sumOf { it.valor },
                    )
                }

                val porCategoria = despesasMes.groupBy { it.categoria }
                    .mapValues { (_, itens) -> itens.sumOf { it.valor } }
                    .toList().sortedByDescending { it.second }
                val top = porCategoria.take(7)
                val resto = porCategoria.drop(7).sumOf { it.second }
                val donut = top.map { (nome, total) ->
                    FatiaCategoria(nome, corPorCategoria[nome] ?: "slate", total)
                } + (if (resto > 0) listOf(FatiaCategoria("Outras", "slate", resto)) else emptyList())

                val saldos = contas.associate { conta ->
                    conta.id to (conta.saldoInicial +
                        receitas.filter { it.contaId == conta.id }.sumOf { it.valor } -
                        despesas.filter { it.contaId == conta.id }.sumOf { it.valor })
                }
                val recDesMes = contas.associate { conta ->
                    conta.id to (
                        receitasMes.filter { it.contaId == conta.id }.sumOf { it.valor } to
                            despesasMes.filter { it.contaId == conta.id }.sumOf { it.valor }
                        )
                }

                _estado.value = _estado.value.copy(
                    carregando = false,
                    contas = contas,
                    resumo = resumo,
                    qtdReceitas = receitasMes.size,
                    qtdDespesas = despesasMes.size,
                    saldosPorConta = saldos,
                    recDesMesPorConta = recDesMes,
                    evolucao = evolucao,
                    donut = donut,
                    totalDonut = despesasMes.sumOf { it.valor },
                    recentes = recentes,
                    corPorCategoria = corPorCategoria,
                )
            } catch (_: Exception) {
                _estado.value = _estado.value.copy(
                    carregando = false,
                    erro = "Falha de rede. Verifique a conexão.",
                )
            }
        }
    }

    private data class Quinteto<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
