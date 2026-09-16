package com.fvcode.finfin.ui.auditoria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.data.model.AuditoriaItem
import com.fvcode.finfin.data.model.AuditoriaPagina
import com.fvcode.finfin.data.repository.FinfinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val MODULOS = listOf(
    "receitas", "despesas", "contas", "categorias",
    "formas-pagamento", "auth", "importacao", "dados",
)

val ACOES = listOf(
    "criar", "atualizar", "excluir", "login", "logout",
    "importar", "exportar", "apagar", "restaurar",
)

/** Módulos com restauração (`auditoria.service.ts:131-254`). */
private val RESTAURAVEIS = setOf("receitas", "despesas", "contas", "categorias", "formas-pagamento")

fun AuditoriaItem.restauravel(): Boolean = acao == "excluir" && modulo in RESTAURAVEIS

data class FiltrosAuditoria(
    val modulo: String = "",
    val acao: String = "",
    val descricao: String = "",
    val dataInicio: String = "",
    val dataFim: String = "",
)

data class AuditoriaUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val info: String? = null,
    val sessaoExpirada: Boolean = false,
    val filtros: FiltrosAuditoria = FiltrosAuditoria(),
    val pagina: AuditoriaPagina? = null,
    val numeroPagina: Int = 1,
    val detalhe: AuditoriaItem? = null,
    val confirmarRestaurar: AuditoriaItem? = null,
    val dialogoLimpeza: Boolean = false,
    val ocupado: Boolean = false,
)

/** Espelha `Auditoria.tsx:50-317`: filtros, paginação, detalhe, restaurar, limpar. */
@HiltViewModel
class AuditoriaViewModel @Inject constructor(
    private val repo: FinfinRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(AuditoriaUiState())
    val estado: StateFlow<AuditoriaUiState> = _estado

    init {
        carregar()
    }

    fun recarregar() = carregar()

    fun aplicarFiltros(f: FiltrosAuditoria) {
        _estado.value = _estado.value.copy(filtros = f, numeroPagina = 1)
        carregar()
    }

    fun mudarPagina(delta: Int) {
        val atual = _estado.value
        val total = atual.pagina?.totalPaginas ?: 1
        val nova = (atual.numeroPagina + delta).coerceIn(1, total.coerceAtLeast(1))
        if (nova != atual.numeroPagina) {
            _estado.value = atual.copy(numeroPagina = nova)
            carregar()
        }
    }

    fun abrirDetalhe(item: AuditoriaItem) {
        _estado.value = _estado.value.copy(detalhe = item)
    }

    fun fecharDetalhe() {
        _estado.value = _estado.value.copy(detalhe = null)
    }

    fun pedirRestaurar(item: AuditoriaItem) {
        _estado.value = _estado.value.copy(confirmarRestaurar = item)
    }

    fun cancelarRestaurar() {
        if (!_estado.value.ocupado) _estado.value = _estado.value.copy(confirmarRestaurar = null)
    }

    fun abrirLimpeza() {
        _estado.value = _estado.value.copy(dialogoLimpeza = true)
    }

    fun fecharLimpeza() {
        if (!_estado.value.ocupado) _estado.value = _estado.value.copy(dialogoLimpeza = false)
    }

    fun consumirSessaoExpirada() {
        _estado.value = _estado.value.copy(sessaoExpirada = false)
    }

    fun consumirInfo() {
        _estado.value = _estado.value.copy(info = null)
    }

    fun confirmarRestaurar() {
        val item = _estado.value.confirmarRestaurar ?: return
        _estado.value = _estado.value.copy(ocupado = true)
        viewModelScope.launch {
            when (val r = repo.restaurarAuditoria(item.id)) {
                is ApiResult.Ok -> {
                    _estado.value = _estado.value.copy(
                        ocupado = false,
                        confirmarRestaurar = null,
                        info = "Item restaurado.",
                    )
                    carregar()
                }
                is ApiResult.Erro -> {
                    if (r.codigo == 401) {
                        _estado.value = _estado.value.copy(ocupado = false, sessaoExpirada = true)
                    } else {
                        _estado.value = _estado.value.copy(
                            ocupado = false,
                            confirmarRestaurar = null,
                            erro = r.mensagem,
                        )
                    }
                }
            }
        }
    }

    fun confirmarLimpeza(antesDe: String) {
        _estado.value = _estado.value.copy(ocupado = true)
        viewModelScope.launch {
            when (val r = repo.limparAuditoria(antesDe.ifBlank { null })) {
                is ApiResult.Ok -> {
                    _estado.value = _estado.value.copy(
                        ocupado = false,
                        dialogoLimpeza = false,
                        numeroPagina = 1,
                        info = "${r.dados.excluidas} registro(s) removido(s).",
                    )
                    carregar()
                }
                is ApiResult.Erro -> {
                    if (r.codigo == 401) {
                        _estado.value = _estado.value.copy(ocupado = false, sessaoExpirada = true)
                    } else {
                        _estado.value = _estado.value.copy(
                            ocupado = false,
                            dialogoLimpeza = false,
                            erro = r.mensagem,
                        )
                    }
                }
            }
        }
    }

    private fun carregar() {
        val atual = _estado.value
        _estado.value = atual.copy(carregando = true, erro = null)
        viewModelScope.launch {
            val f = _estado.value.filtros
            val pag = _estado.value.numeroPagina
            when (
                val r = repo.auditoria(
                    modulo = f.modulo.ifBlank { null },
                    acao = f.acao.ifBlank { null },
                    descricao = f.descricao.ifBlank { null },
                    dataInicio = f.dataInicio.ifBlank { null },
                    dataFim = f.dataFim.ifBlank { null },
                    pagina = pag,
                )
            ) {
                is ApiResult.Ok -> _estado.value = _estado.value.copy(carregando = false, pagina = r.dados)
                is ApiResult.Erro -> {
                    if (r.codigo == 401) {
                        _estado.value = _estado.value.copy(carregando = false, sessaoExpirada = true)
                    } else {
                        _estado.value = _estado.value.copy(carregando = false, erro = r.mensagem)
                    }
                }
            }
        }
    }
}
