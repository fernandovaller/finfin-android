package com.fvcode.finfin.ui.relatorios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.data.model.Conta
import com.fvcode.finfin.data.model.Despesa
import com.fvcode.finfin.data.model.Receita
import com.fvcode.finfin.data.repository.FinfinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RelatoriosUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    val filtros: FiltrosRelatorio = FiltrosRelatorio(),
    val receitas: List<Receita> = emptyList(),
    val despesas: List<Despesa> = emptyList(),
    val contas: List<Conta> = emptyList(),
    val categoriasReceita: List<String> = emptyList(),
    val categoriasDespesa: List<String> = emptyList(),
    val formas: List<String> = emptyList(),
    val corPorCategoria: Map<String, String> = emptyMap(),
)

/**
 * Espelha `Relatorios.tsx`: GET receitas+despesas **uma vez**; o resto é
 * filtro + agregado no client.
 */
@HiltViewModel
class RelatoriosViewModel @Inject constructor(
    private val repo: FinfinRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(RelatoriosUiState())
    val estado: StateFlow<RelatoriosUiState> = _estado

    init {
        carregar()
    }

    fun recarregar() = carregar()

    fun atualizarFiltros(f: FiltrosRelatorio) {
        _estado.value = _estado.value.copy(filtros = f)
    }

    /** Limpa conta/categorias/forma/busca/min/max, mantendo o período selecionado. */
    fun limparFiltros() {
        val atual = _estado.value.filtros
        _estado.value = _estado.value.copy(
            filtros = atual.copy(
                contaId = null,
                catReceita = "",
                catDespesa = "",
                forma = "",
                busca = "",
                minTxt = "",
                maxTxt = "",
            ),
        )
    }

    fun consumirSessaoExpirada() {
        _estado.value = _estado.value.copy(sessaoExpirada = false)
    }

    private fun carregar() {
        _estado.value = _estado.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            try {
                val (rRec, rDes, rContas, rCats, rFormas) = coroutineScope {
                    val dRec = async { repo.receitas() }
                    val dDes = async { repo.despesas() }
                    val dContas = async { repo.contas() }
                    val dCats = async { repo.categorias() }
                    val dFormas = async { repo.formas() }
                    Cinco(dRec.await(), dDes.await(), dContas.await(), dCats.await(), dFormas.await())
                }
                if (listOf(rRec, rDes).any { it is ApiResult.Erro && it.codigo == 401 }) {
                    _estado.value = _estado.value.copy(carregando = false, sessaoExpirada = true)
                    return@launch
                }
                val erro = listOf(rRec, rDes).filterIsInstance<ApiResult.Erro>().firstOrNull()
                if (erro != null) {
                    _estado.value = _estado.value.copy(carregando = false, erro = erro.mensagem)
                    return@launch
                }
                val cats = (rCats as? ApiResult.Ok)?.dados ?: emptyList()
                _estado.value = _estado.value.copy(
                    carregando = false,
                    receitas = (rRec as? ApiResult.Ok)?.dados ?: emptyList(),
                    despesas = (rDes as? ApiResult.Ok)?.dados ?: emptyList(),
                    contas = (rContas as? ApiResult.Ok)?.dados ?: emptyList(),
                    categoriasReceita = cats.filter { it.tipo == "receita" }.map { it.nome },
                    categoriasDespesa = cats.filter { it.tipo == "despesa" }.map { it.nome },
                    formas = ((rFormas as? ApiResult.Ok)?.dados ?: emptyList()).map { it.nome },
                    corPorCategoria = cats.associate { it.nome to it.cor },
                )
            } catch (_: Exception) {
                _estado.value = _estado.value.copy(
                    carregando = false,
                    erro = "Falha de rede. Verifique a conexão.",
                )
            }
        }
    }

    private data class Cinco<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
