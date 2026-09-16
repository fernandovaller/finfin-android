package com.fvcode.finfin.data.remote

import com.fvcode.finfin.core.network.MensagemOk
import com.fvcode.finfin.data.model.AuditoriaPagina
import com.fvcode.finfin.data.model.CadastroCorpo
import com.fvcode.finfin.data.model.Categoria
import com.fvcode.finfin.data.model.CategoriaCorpo
import com.fvcode.finfin.data.model.CategoriaEdicao
import com.fvcode.finfin.data.model.Conta
import com.fvcode.finfin.data.model.ContaCorpo
import com.fvcode.finfin.data.model.Contagem
import com.fvcode.finfin.data.model.Despesa
import com.fvcode.finfin.data.model.DespesaCorpo
import com.fvcode.finfin.data.model.EuResposta
import com.fvcode.finfin.data.model.ExcluirGrupoResposta
import com.fvcode.finfin.data.model.FormaCorpo
import com.fvcode.finfin.data.model.FormaPagamento
import com.fvcode.finfin.data.model.IntegracoesCorpo
import com.fvcode.finfin.data.model.IntegracoesResposta
import com.fvcode.finfin.data.model.LimpezaResposta
import com.fvcode.finfin.data.model.LoginCorpo
import com.fvcode.finfin.data.model.PerfilCorpo
import com.fvcode.finfin.data.model.Receita
import com.fvcode.finfin.data.model.ReceitaCorpo
import com.fvcode.finfin.data.model.RestaurarResposta
import com.fvcode.finfin.data.model.Resumo
import com.fvcode.finfin.data.model.Sessao
import com.fvcode.finfin.data.model.TrocarSenhaCorpo
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Espelho de `docs/specs/10-api.md`. Base `.../api/`.
 */
interface FinfinApi {
    // Auth
    @POST("auth/cadastro") suspend fun cadastro(@Body corpo: CadastroCorpo): Sessao
    @POST("auth/login") suspend fun login(@Body corpo: LoginCorpo): Sessao
    @POST("auth/logout") suspend fun logout()
    @GET("auth/eu") suspend fun eu(): EuResposta
    @PUT("auth/perfil") suspend fun atualizarPerfil(@Body corpo: PerfilCorpo): Sessao
    @PUT("auth/senha") suspend fun trocarSenha(@Body corpo: TrocarSenhaCorpo)
    @GET("auth/integracoes") suspend fun integracoes(): IntegracoesResposta
    @PUT("auth/integracoes") suspend fun salvarIntegracoes(@Body corpo: IntegracoesCorpo)
    @POST("auth/recuperar-senha") suspend fun recuperarSenha(@Body corpo: Map<String, String>): MensagemOk
    @POST("auth/redefinir-senha") suspend fun redefinirSenha(@Body corpo: Map<String, String>): MensagemOk

    // Lancamentos
    @GET("receitas") suspend fun receitas(@Query("contaId") contaId: Int? = null): List<Receita>
    @POST("receitas") suspend fun criarReceita(@Body corpo: ReceitaCorpo): Receita
    @PUT("receitas/{id}") suspend fun editarReceita(@Path("id") id: Int, @Body corpo: ReceitaCorpo): Receita
    @DELETE("receitas/{id}") suspend fun excluirReceita(@Path("id") id: Int)

    @GET("despesas") suspend fun despesas(@Query("contaId") contaId: Int? = null): List<Despesa>
    // POST /despesas retorna OBJETO p/ 1 parcela e ARRAY p/ N — normalizado no repositorio.
    @POST("despesas") suspend fun criarDespesa(@Body corpo: DespesaCorpo): JsonElement
    @PUT("despesas/{id}") suspend fun editarDespesa(@Path("id") id: Int, @Body corpo: DespesaCorpo): Despesa
    @DELETE("despesas/{id}") suspend fun excluirDespesa(
        @Path("id") id: Int,
        @Query("escopo") escopo: String? = null,
    ): Any

    @GET("resumo") suspend fun resumo(
        @Query("mes") mes: String? = null,
        @Query("contaId") contaId: Int? = null,
    ): Resumo

    @GET("contagem") suspend fun contagem(): Contagem

    // Catalogo
    @GET("categorias") suspend fun categorias(@Query("tipo") tipo: String? = null): List<Categoria>
    @POST("categorias") suspend fun criarCategoria(@Body corpo: CategoriaCorpo): Categoria
    @PUT("categorias/{id}") suspend fun editarCategoria(@Path("id") id: Int, @Body corpo: CategoriaEdicao): Categoria
    @DELETE("categorias/{id}") suspend fun excluirCategoria(@Path("id") id: Int)

    @GET("formas-pagamento") suspend fun formas(): List<FormaPagamento>
    @POST("formas-pagamento") suspend fun criarForma(@Body corpo: FormaCorpo): FormaPagamento
    @PUT("formas-pagamento/{id}") suspend fun editarForma(@Path("id") id: Int, @Body corpo: FormaCorpo): FormaPagamento
    @DELETE("formas-pagamento/{id}") suspend fun excluirForma(@Path("id") id: Int)

    @GET("contas") suspend fun contas(): List<Conta>
    @POST("contas") suspend fun criarConta(@Body corpo: ContaCorpo): Conta
    @PUT("contas/{id}") suspend fun editarConta(@Path("id") id: Int, @Body corpo: ContaCorpo): Conta
    @DELETE("contas/{id}") suspend fun excluirConta(@Path("id") id: Int)

    @POST("restaurar") suspend fun restaurarPadrao(): RestaurarResposta

    // Auditoria
    @GET("auditoria")
    suspend fun auditoria(
        @Query("modulo") modulo: String? = null,
        @Query("acao") acao: String? = null,
        @Query("descricao") descricao: String? = null,
        @Query("dataInicio") dataInicio: String? = null,
        @Query("dataFim") dataFim: String? = null,
        @Query("pagina") pagina: Int = 1,
        @Query("porPagina") porPagina: Int = 20,
    ): AuditoriaPagina

    @POST("auditoria/{id}/restaurar") suspend fun restaurarAuditoria(@Path("id") id: Int): JsonObject
    @DELETE("auditoria") suspend fun limparAuditoria(@Query("antesDe") antesDe: String? = null): LimpezaResposta

    // Dados / backup / demo
    @DELETE("dados/lancamentos") suspend fun apagarLancamentos(): JsonObject
    @DELETE("dados/tudo") suspend fun apagarTudo(): JsonObject
    @GET("dados/demonstracao") suspend fun statusDemo(): JsonObject
    @POST("dados/demonstracao") suspend fun gerarDemo(): JsonObject
    @DELETE("dados/demonstracao") suspend fun removerDemo(): JsonObject
}

// DELETE de despesa retorna {excluidas} ou corpo vazio; normalizado no repositorio.
fun excluirDespesaResposta(raw: Any): ExcluirGrupoResposta {
    if (raw is Map<*, *>) {
        val n = (raw["excluidas"] as? Number)?.toInt() ?: 1
        return ExcluirGrupoResposta(n)
    }
    return ExcluirGrupoResposta(1)
}

/** POST /despesas: objeto unico (1 parcela) ou array (N parcelas). */
fun despesasCriadasResposta(raw: JsonElement, gson: Gson = Gson()): List<Despesa> {
    return if (raw.isJsonArray) {
        raw.asJsonArray.map { gson.fromJson(it, Despesa::class.java) }
    } else {
        listOf(gson.fromJson(raw, Despesa::class.java))
    }
}
