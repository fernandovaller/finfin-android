package com.fvcode.finfin.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.data.model.Usuario
import com.fvcode.finfin.data.repository.AuthRepository
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Avatar: manter | trocar (dataURL) | remover (null explícito). */
sealed interface AcaoAvatar {
    data object Manter : AcaoAvatar
    data class Trocar(val dataUrl: String) : AcaoAvatar
    data object Remover : AcaoAvatar
}

data class PerfilUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val info: String? = null,
    val sessaoExpirada: Boolean = false,
    val usuario: Usuario? = null,
    val salvandoPerfil: Boolean = false,
    val erroPerfil: String? = null,
    val trocandoSenha: Boolean = false,
    val erroSenha: String? = null,
    val senhaOk: Boolean = false,
)

/** Espelha `Perfil.tsx`: PUT perfil + PUT senha. */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(PerfilUiState())
    val estado: StateFlow<PerfilUiState> = _estado

    init {
        carregar()
    }

    fun recarregar() = carregar()

    fun consumirSessaoExpirada() {
        _estado.value = _estado.value.copy(sessaoExpirada = false)
    }

    fun consumirInfo() {
        _estado.value = _estado.value.copy(info = null, senhaOk = false)
    }

    fun salvarPerfil(
        nome: String,
        email: String,
        avatar: AcaoAvatar,
        aoSincronizar: (Usuario) -> Unit,
    ) {
        if (nome.isBlank()) {
            _estado.value = _estado.value.copy(erroPerfil = "Informe o nome.")
            return
        }
        if (!EMAIL_REGEX.matches(email.trim())) {
            _estado.value = _estado.value.copy(erroPerfil = "E-mail inválido.")
            return
        }
        _estado.value = _estado.value.copy(salvandoPerfil = true, erroPerfil = null)
        viewModelScope.launch {
            val corpo = JsonObject().apply {
                addProperty("nome", nome.trim())
                addProperty("email", email.trim())
                when (avatar) {
                    AcaoAvatar.Manter -> Unit // Gson omite: campo ausente = inalterado
                    is AcaoAvatar.Trocar -> addProperty("avatar", avatar.dataUrl)
                    AcaoAvatar.Remover -> add("avatar", JsonNull.INSTANCE)
                }
            }
            when (val r = auth.atualizarPerfilRaw(corpo)) {
                is ApiResult.Ok -> {
                    aoSincronizar(r.dados.usuario)
                    _estado.value = _estado.value.copy(
                        salvandoPerfil = false,
                        usuario = r.dados.usuario,
                        info = "Perfil atualizado.",
                    )
                }
                is ApiResult.Erro -> {
                    if (r.codigo == 401) {
                        _estado.value = _estado.value.copy(salvandoPerfil = false, sessaoExpirada = true)
                    } else {
                        _estado.value = _estado.value.copy(salvandoPerfil = false, erroPerfil = r.mensagem)
                    }
                }
            }
        }
    }

    fun trocarSenha(atual: String, nova: String, confirmacao: String) {
        if (nova != confirmacao) {
            _estado.value = _estado.value.copy(erroSenha = "Confirmação não confere.")
            return
        }
        if (nova.length !in 8..128) {
            _estado.value = _estado.value.copy(erroSenha = "Nova senha deve ter 8–128 caracteres.")
            return
        }
        _estado.value = _estado.value.copy(trocandoSenha = true, erroSenha = null)
        viewModelScope.launch {
            when (val r = auth.trocarSenha(atual, nova)) {
                is ApiResult.Ok -> {
                    _estado.value = _estado.value.copy(
                        trocandoSenha = false,
                        senhaOk = true,
                        info = "Senha trocada. Outras sessões foram encerradas.",
                    )
                }
                is ApiResult.Erro -> {
                    if (r.codigo == 401 && r.mensagem.contains("Sessão")) {
                        _estado.value = _estado.value.copy(trocandoSenha = false, sessaoExpirada = true)
                    } else {
                        _estado.value = _estado.value.copy(trocandoSenha = false, erroSenha = r.mensagem)
                    }
                }
            }
        }
    }

    private fun carregar() {
        _estado.value = _estado.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            val usuario = auth.eu()
            if (usuario == null && !auth.temSessao()) {
                _estado.value = _estado.value.copy(carregando = false, sessaoExpirada = true)
            } else {
                _estado.value = _estado.value.copy(carregando = false, usuario = usuario)
            }
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
