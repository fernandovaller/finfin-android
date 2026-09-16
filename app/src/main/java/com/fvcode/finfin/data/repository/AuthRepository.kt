package com.fvcode.finfin.data.repository

import com.fvcode.finfin.core.datastore.FinfinPreferences
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.core.network.aoResultado
import com.fvcode.finfin.data.model.CadastroCorpo
import com.fvcode.finfin.data.model.EuResposta
import com.fvcode.finfin.data.model.LoginCorpo
import com.fvcode.finfin.data.model.PerfilCorpo
import com.fvcode.finfin.data.model.Sessao
import com.fvcode.finfin.data.model.TrocarSenhaCorpo
import com.fvcode.finfin.data.model.Usuario
import com.fvcode.finfin.data.remote.FinfinApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sessao: espelha `frontend/src/auth.tsx` + `api.ts`.
 * Token em DataStore (chave finfin_token). `eu()` nunca lanca 401 p/ fora.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: FinfinApi,
    private val prefs: FinfinPreferences,
) {
    val token: Flow<String?> = prefs.token

    suspend fun entrar(email: String, senha: String): ApiResult<Sessao> {
        // clearToken antes, como auth.tsx:34-46, p/ 401 virar erro de credenciais.
        prefs.salvarToken(null)
        return when (val r = aoResultado({ api.login(LoginCorpo(email, senha)) }, semToken = true)) {
            is ApiResult.Ok -> {
                prefs.salvarToken(r.dados.token)
                r
            }
            is ApiResult.Erro -> r
        }
    }

    suspend fun criarConta(nome: String, email: String, senha: String): ApiResult<Sessao> {
        prefs.salvarToken(null)
        return when (val r = aoResultado({ api.cadastro(CadastroCorpo(nome, email, senha)) }, semToken = true)) {
            is ApiResult.Ok -> {
                prefs.salvarToken(r.dados.token)
                r
            }
            is ApiResult.Erro -> r
        }
    }

    suspend fun sair() {
        try {
            api.logout()
        } catch (_: Exception) {
        } finally {
            prefs.salvarToken(null)
        }
    }

    suspend fun eu(): Usuario? {
        val temToken = prefs.token.first()
        if (temToken.isNullOrBlank()) return null
        return try {
            val r: EuResposta = api.eu()
            r.usuario
        } catch (_: Exception) {
            null
        }
    }

    suspend fun temSessao(): Boolean = !prefs.token.first().isNullOrBlank()

    suspend fun atualizarPerfil(corpo: PerfilCorpo): ApiResult<Sessao> {
        val semToken = !temSessao()
        return aoResultado({ api.atualizarPerfil(corpo) }, semToken)
    }

    suspend fun trocarSenha(atual: String, nova: String): ApiResult<Unit> {
        val semToken = !temSessao()
        return aoResultado({ api.trocarSenha(TrocarSenhaCorpo(atual, nova)) }, semToken)
    }

    suspend fun aoSessaoExpirada() {
        prefs.salvarToken(null)
    }
}
