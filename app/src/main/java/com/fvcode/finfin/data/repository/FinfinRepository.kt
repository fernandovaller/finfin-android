package com.fvcode.finfin.data.repository

import com.fvcode.finfin.core.datastore.FinfinPreferences
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.core.network.aoResultado
import com.fvcode.finfin.data.model.Categoria
import com.fvcode.finfin.data.model.CategoriaCorpo
import com.fvcode.finfin.data.model.CategoriaEdicao
import com.fvcode.finfin.data.model.Conta
import com.fvcode.finfin.data.model.ContaCorpo
import com.fvcode.finfin.data.model.Contagem
import com.fvcode.finfin.data.model.Despesa
import com.fvcode.finfin.data.model.DespesaCorpo
import com.fvcode.finfin.data.model.ExcluirGrupoResposta
import com.fvcode.finfin.data.model.FormaCorpo
import com.fvcode.finfin.data.model.FormaPagamento
import com.fvcode.finfin.data.model.Receita
import com.fvcode.finfin.data.model.ReceitaCorpo
import com.fvcode.finfin.data.model.Resumo
import com.fvcode.finfin.data.remote.FinfinApi
import com.fvcode.finfin.data.remote.despesasCriadasResposta
import com.fvcode.finfin.data.remote.excluirDespesaResposta
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinfinRepository @Inject constructor(
    private val api: FinfinApi,
    private val prefs: FinfinPreferences,
    private val auth: AuthRepository,
) {
    private suspend fun semToken(): Boolean = prefs.token.first().isNullOrBlank()

    private suspend fun <T> chamada(bloco: suspend () -> T): ApiResult<T> {
        val r = aoResultado(bloco, semToken())
        if (r is ApiResult.Erro && r.codigo == 401) auth.aoSessaoExpirada()
        return r
    }

    suspend fun receitas(contaId: Int? = null): ApiResult<List<Receita>> = chamada { api.receitas(contaId) }
    suspend fun despesas(contaId: Int? = null): ApiResult<List<Despesa>> = chamada { api.despesas(contaId) }
    suspend fun criarReceita(c: ReceitaCorpo): ApiResult<Receita> = chamada { api.criarReceita(c) }
    suspend fun criarDespesa(c: DespesaCorpo): ApiResult<List<Despesa>> =
        chamada { despesasCriadasResposta(api.criarDespesa(c)) }
    suspend fun editarReceita(id: Int, c: ReceitaCorpo): ApiResult<Receita> = chamada { api.editarReceita(id, c) }
    suspend fun editarDespesa(id: Int, c: DespesaCorpo): ApiResult<Despesa> = chamada { api.editarDespesa(id, c) }
    suspend fun excluirReceita(id: Int): ApiResult<Unit> = chamada { api.excluirReceita(id) }
    suspend fun excluirDespesa(id: Int, grupo: Boolean): ApiResult<ExcluirGrupoResposta> = chamada {
        excluirDespesaResposta(api.excluirDespesa(id, if (grupo) "grupo" else null))
    }
    suspend fun resumo(mes: String?, contaId: Int?): ApiResult<Resumo> = chamada { api.resumo(mes, contaId) }
    suspend fun contagem(): ApiResult<Contagem> = chamada { api.contagem() }

    suspend fun categorias(tipo: String? = null): ApiResult<List<Categoria>> = chamada { api.categorias(tipo) }
    suspend fun criarCategoria(c: CategoriaCorpo) = chamada { api.criarCategoria(c) }
    suspend fun editarCategoria(id: Int, c: CategoriaEdicao) = chamada { api.editarCategoria(id, c) }
    suspend fun excluirCategoria(id: Int): ApiResult<Unit> = chamada { api.excluirCategoria(id) }

    suspend fun formas(): ApiResult<List<FormaPagamento>> = chamada { api.formas() }
    suspend fun criarForma(nome: String) = chamada { api.criarForma(FormaCorpo(nome)) }
    suspend fun excluirForma(id: Int): ApiResult<Unit> = chamada { api.excluirForma(id) }

    suspend fun contas(): ApiResult<List<Conta>> = chamada { api.contas() }
    suspend fun criarConta(c: ContaCorpo) = chamada { api.criarConta(c) }
    suspend fun excluirConta(id: Int): ApiResult<Unit> = chamada { api.excluirConta(id) }
}
