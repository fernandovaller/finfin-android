package com.fvcode.finfin.ui.formas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.data.model.FormaPagamento
import com.fvcode.finfin.data.repository.FinfinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DialogoForma {
    data object Oculto : DialogoForma
    data object Novo : DialogoForma
    data class Edicao(val item: FormaPagamento) : DialogoForma
    data class Exclusao(val item: FormaPagamento) : DialogoForma
}

data class FormasUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    val itens: List<FormaPagamento> = emptyList(),
    val dialogo: DialogoForma = DialogoForma.Oculto,
    val salvando: Boolean = false,
    val erroForm: String? = null,
    val info: String? = null,
)

/** Espelha `FormasPagamento.tsx`: CRUD só `{nome}`. */
@HiltViewModel
class FormasViewModel @Inject constructor(
    private val repo: FinfinRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(FormasUiState())
    val estado: StateFlow<FormasUiState> = _estado

    init {
        carregar()
    }

    fun recarregar() = carregar()

    fun abrirNovo() {
        _estado.value = _estado.value.copy(dialogo = DialogoForma.Novo, erroForm = null)
    }

    fun abrirEdicao(item: FormaPagamento) {
        _estado.value = _estado.value.copy(dialogo = DialogoForma.Edicao(item), erroForm = null)
    }

    fun pedirExclusao(item: FormaPagamento) {
        _estado.value = _estado.value.copy(dialogo = DialogoForma.Exclusao(item), erroForm = null)
    }

    fun fecharDialogo() {
        if (!_estado.value.salvando) {
            _estado.value = _estado.value.copy(dialogo = DialogoForma.Oculto, erroForm = null)
        }
    }

    fun consumirSessaoExpirada() {
        _estado.value = _estado.value.copy(sessaoExpirada = false)
    }

    fun consumirInfo() {
        _estado.value = _estado.value.copy(info = null)
    }

    fun salvar(nome: String, editando: FormaPagamento?) {
        if (nome.isBlank()) {
            _estado.value = _estado.value.copy(erroForm = "Informe o nome.")
            return
        }
        _estado.value = _estado.value.copy(salvando = true, erroForm = null)
        viewModelScope.launch {
            val resultado: ApiResult<String> = if (editando == null) {
                when (val r = repo.criarForma(nome.trim())) {
                    is ApiResult.Ok -> ApiResult.Ok("Forma criada")
                    is ApiResult.Erro -> r
                }
            } else {
                when (val r = repo.editarForma(editando.id, nome.trim())) {
                    is ApiResult.Ok -> ApiResult.Ok("Forma atualizada")
                    is ApiResult.Erro -> r
                }
            }
            when (resultado) {
                is ApiResult.Ok -> {
                    _estado.value = _estado.value.copy(
                        salvando = false,
                        dialogo = DialogoForma.Oculto,
                        info = resultado.dados,
                    )
                    carregar()
                }
                is ApiResult.Erro -> {
                    if (resultado.codigo == 401) {
                        _estado.value = _estado.value.copy(salvando = false, sessaoExpirada = true)
                    } else {
                        _estado.value = _estado.value.copy(salvando = false, erroForm = resultado.mensagem)
                    }
                }
            }
        }
    }

    fun excluir(item: FormaPagamento) {
        _estado.value = _estado.value.copy(salvando = true, erroForm = null)
        viewModelScope.launch {
            when (val r = repo.excluirForma(item.id)) {
                is ApiResult.Ok -> {
                    _estado.value = _estado.value.copy(
                        salvando = false,
                        dialogo = DialogoForma.Oculto,
                        info = "Forma excluída",
                    )
                    carregar()
                }
                is ApiResult.Erro -> {
                    if (r.codigo == 401) {
                        _estado.value = _estado.value.copy(salvando = false, sessaoExpirada = true)
                    } else {
                        _estado.value = _estado.value.copy(salvando = false, erroForm = r.mensagem)
                    }
                }
            }
        }
    }

    private fun carregar() {
        _estado.value = _estado.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            when (val r = repo.formas()) {
                is ApiResult.Ok -> _estado.value = _estado.value.copy(carregando = false, itens = r.dados)
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
