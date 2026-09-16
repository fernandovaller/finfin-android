package com.fvcode.finfin.ui.categorias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.data.model.Categoria
import com.fvcode.finfin.data.model.CategoriaCorpo
import com.fvcode.finfin.data.model.CategoriaEdicao
import com.fvcode.finfin.data.repository.FinfinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DialogoCategoria {
    data object Oculto : DialogoCategoria
    data object Novo : DialogoCategoria
    data class Edicao(val item: Categoria) : DialogoCategoria
    data class Exclusao(val item: Categoria) : DialogoCategoria
}

data class CategoriasUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    val itens: List<Categoria> = emptyList(),
    val aba: String = "despesa", // despesa|receita
    val dialogo: DialogoCategoria = DialogoCategoria.Oculto,
    val salvando: Boolean = false,
    val erroForm: String? = null,
    val info: String? = null,
)

/** Espelha `Categorias.tsx`: aba despesa|receita, tipo imutável na edição. */
@HiltViewModel
class CategoriasViewModel @Inject constructor(
    private val repo: FinfinRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(CategoriasUiState())
    val estado: StateFlow<CategoriasUiState> = _estado

    init {
        carregar()
    }

    fun recarregar() = carregar()

    fun trocarAba(aba: String) {
        _estado.value = _estado.value.copy(aba = aba)
    }

    fun abrirNovo() {
        _estado.value = _estado.value.copy(dialogo = DialogoCategoria.Novo, erroForm = null)
    }

    fun abrirEdicao(item: Categoria) {
        _estado.value = _estado.value.copy(dialogo = DialogoCategoria.Edicao(item), erroForm = null)
    }

    fun pedirExclusao(item: Categoria) {
        _estado.value = _estado.value.copy(dialogo = DialogoCategoria.Exclusao(item), erroForm = null)
    }

    fun fecharDialogo() {
        if (!_estado.value.salvando) {
            _estado.value = _estado.value.copy(dialogo = DialogoCategoria.Oculto, erroForm = null)
        }
    }

    fun consumirSessaoExpirada() {
        _estado.value = _estado.value.copy(sessaoExpirada = false)
    }

    fun consumirInfo() {
        _estado.value = _estado.value.copy(info = null)
    }

    fun salvar(nome: String, cor: String, editando: Categoria?) {
        if (nome.isBlank()) {
            _estado.value = _estado.value.copy(erroForm = "Informe o nome.")
            return
        }
        _estado.value = _estado.value.copy(salvando = true, erroForm = null)
        viewModelScope.launch {
            val resultado: ApiResult<String> = if (editando == null) {
                when (val r = repo.criarCategoria(CategoriaCorpo(nome.trim(), _estado.value.aba, cor))) {
                    is ApiResult.Ok -> ApiResult.Ok("Categoria criada")
                    is ApiResult.Erro -> r
                }
            } else {
                when (val r = repo.editarCategoria(editando.id, CategoriaEdicao(nome.trim(), cor))) {
                    is ApiResult.Ok -> ApiResult.Ok("Categoria atualizada")
                    is ApiResult.Erro -> r
                }
            }
            when (resultado) {
                is ApiResult.Ok -> {
                    _estado.value = _estado.value.copy(
                        salvando = false,
                        dialogo = DialogoCategoria.Oculto,
                        info = resultado.dados,
                    )
                    carregar()
                }
                is ApiResult.Erro -> {
                    if (resultado.codigo == 401) {
                        _estado.value = _estado.value.copy(salvando = false, sessaoExpirada = true)
                    } else {
                        // 409 (duplicada / em uso) aparece no próprio diálogo.
                        _estado.value = _estado.value.copy(salvando = false, erroForm = resultado.mensagem)
                    }
                }
            }
        }
    }

    fun excluir(item: Categoria) {
        _estado.value = _estado.value.copy(salvando = true, erroForm = null)
        viewModelScope.launch {
            when (val r = repo.excluirCategoria(item.id)) {
                is ApiResult.Ok -> {
                    _estado.value = _estado.value.copy(
                        salvando = false,
                        dialogo = DialogoCategoria.Oculto,
                        info = "Categoria excluída",
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
            when (val r = repo.categorias()) {
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
