package com.fvcode.finfin.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvcode.finfin.core.datastore.FinfinPreferences
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.data.model.Usuario
import com.fvcode.finfin.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessaoUi {
    data object Carregando : SessaoUi
    data class Logada(val usuario: Usuario?) : SessaoUi
    data object Deslogada : SessaoUi
}

/**
 * Equivalente de `ProvedorAparencia > AuthProvider + RotaProtegida`
 * (`frontend/src/main.tsx:22-38`, `auth.tsx`).
 */
@HiltViewModel
class SessaoViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val prefs: FinfinPreferences,
) : ViewModel() {
    private val _estado = MutableStateFlow<SessaoUi>(SessaoUi.Carregando)
    val estado: StateFlow<SessaoUi> = _estado

    private val _tema = MutableStateFlow(FinfinPreferences.TEMA_SISTEMA)
    val tema: StateFlow<String> = _tema

    init {
        viewModelScope.launch {
            _tema.value = prefs.tema.first()
            val temToken = auth.temSessao()
            if (!temToken) {
                _estado.value = SessaoUi.Deslogada
            } else {
                val usuario = auth.eu()
                _estado.value = if (usuario == null && prefs.token.first().isNullOrBlank()) {
                    SessaoUi.Deslogada
                } else {
                    SessaoUi.Logada(usuario)
                }
            }
        }
        viewModelScope.launch {
            prefs.tema.collect { _tema.value = it }
        }
    }

    fun entrar(email: String, senha: String, aoOk: () -> Unit, aoErro: (String) -> Unit) {
        viewModelScope.launch {
            when (val r = auth.entrar(email, senha)) {
                is ApiResult.Ok -> {
                    _estado.value = SessaoUi.Logada(r.dados.usuario)
                    aoOk()
                }
                is ApiResult.Erro -> aoErro(r.mensagem)
            }
        }
    }

    fun criarConta(nome: String, email: String, senha: String, aoOk: () -> Unit, aoErro: (String) -> Unit) {
        viewModelScope.launch {
            when (val r = auth.criarConta(nome, email, senha)) {
                is ApiResult.Ok -> {
                    _estado.value = SessaoUi.Logada(r.dados.usuario)
                    aoOk()
                }
                is ApiResult.Erro -> aoErro(r.mensagem)
            }
        }
    }

    fun sair(aoFim: () -> Unit) {
        viewModelScope.launch {
            auth.sair()
            _estado.value = SessaoUi.Deslogada
            aoFim()
        }
    }

    fun salvarTema(tema: String) {
        viewModelScope.launch { prefs.salvarTema(tema) }
    }
}
