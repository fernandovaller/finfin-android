package com.fvcode.finfin.ui.contas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.data.model.Conta
import com.fvcode.finfin.data.model.ContaCorpo
import com.fvcode.finfin.data.repository.FinfinRepository
import com.fvcode.finfin.ui.relatorios.parseValorBR
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContaValores(
    val nome: String = "",
    val saldoTxt: String = "0",
    val nota: String = "",
    val icone: String = "",
    val principal: Boolean = false,
)

sealed interface DialogoConta {
    data object Oculto : DialogoConta
    data object Novo : DialogoConta
    data class Edicao(val item: Conta) : DialogoConta
    data class Exclusao(val item: Conta) : DialogoConta
}

data class ContasUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    val itens: List<Conta> = emptyList(),
    val dialogo: DialogoConta = DialogoConta.Oculto,
    val salvando: Boolean = false,
    val erroForm: String? = null,
    val info: String? = null,
)

/** Espelha `Contas.tsx`: CRUD `{nome, saldoInicial, nota, icone, principal}`. */
@HiltViewModel
class ContasViewModel @Inject constructor(
    private val repo: FinfinRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(ContasUiState())
    val estado: StateFlow<ContasUiState> = _estado

    init {
        carregar()
    }

    fun recarregar() = carregar()

    fun abrirNovo() {
        _estado.value = _estado.value.copy(dialogo = DialogoConta.Novo, erroForm = null)
    }

    fun abrirEdicao(item: Conta) {
        _estado.value = _estado.value.copy(dialogo = DialogoConta.Edicao(item), erroForm = null)
    }

    fun pedirExclusao(item: Conta) {
        _estado.value = _estado.value.copy(dialogo = DialogoConta.Exclusao(item), erroForm = null)
    }

    fun fecharDialogo() {
        if (!_estado.value.salvando) {
            _estado.value = _estado.value.copy(dialogo = DialogoConta.Oculto, erroForm = null)
        }
    }

    fun consumirSessaoExpirada() {
        _estado.value = _estado.value.copy(sessaoExpirada = false)
    }

    fun consumirInfo() {
        _estado.value = _estado.value.copy(info = null)
    }

    fun salvar(v: ContaValores, editando: Conta?) {
        if (v.nome.isBlank()) {
            _estado.value = _estado.value.copy(erroForm = "Informe o nome.")
            return
        }
        val saldo = parseValorBR(v.saldoTxt)
        if (saldo == null) {
            _estado.value = _estado.value.copy(erroForm = "Saldo inicial inválido.")
            return
        }
        val corpo = ContaCorpo(
            nome = v.nome.trim(),
            saldoInicial = saldo,
            nota = v.nota.trim(),
            icone = v.icone.trim(),
            principal = v.principal,
        )
        _estado.value = _estado.value.copy(salvando = true, erroForm = null)
        viewModelScope.launch {
            val resultado: ApiResult<String> = if (editando == null) {
                when (val r = repo.criarConta(corpo)) {
                    is ApiResult.Ok -> ApiResult.Ok("Conta criada")
                    is ApiResult.Erro -> r
                }
            } else {
                when (val r = repo.editarConta(editando.id, corpo)) {
                    is ApiResult.Ok -> ApiResult.Ok("Conta atualizada")
                    is ApiResult.Erro -> r
                }
            }
            when (resultado) {
                is ApiResult.Ok -> {
                    _estado.value = _estado.value.copy(
                        salvando = false,
                        dialogo = DialogoConta.Oculto,
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

    fun excluir(item: Conta) {
        _estado.value = _estado.value.copy(salvando = true, erroForm = null)
        viewModelScope.launch {
            when (val r = repo.excluirConta(item.id)) {
                is ApiResult.Ok -> {
                    _estado.value = _estado.value.copy(
                        salvando = false,
                        dialogo = DialogoConta.Oculto,
                        info = "Conta excluída",
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
            when (val r = repo.contas()) {
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
