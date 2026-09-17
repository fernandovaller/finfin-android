package com.fvcode.finfin.data.model

import com.google.gson.annotations.SerializedName

// --- Auth (04-auth.md) ---

data class Usuario(
    val id: Int,
    val nome: String,
    val email: String,
    val avatar: String? = null,
)

data class Sessao(
    val usuario: Usuario,
    val token: String,
)

data class CadastroCorpo(val nome: String, val email: String, val senha: String)
data class LoginCorpo(val email: String, val senha: String)
data class PerfilCorpo(val nome: String? = null, val email: String? = null, val avatar: String? = null)
data class TrocarSenhaCorpo(val senhaAtual: String, val novaSenha: String)
data class IntegracoesCorpo(@SerializedName("resendApiKey") val resendApiKey: String?)

data class EuResposta(val usuario: Usuario?)
data class IntegracoesResposta(val email: EmailStatus)
data class EmailStatus(
    val configurado: Boolean,
    val origem: String?,
    val mascarada: String?,
)

// --- Saúde (GET /api/saude, sem auth) ---

data class SaudeResposta(
    val ok: Boolean = false,
    val app: String? = null,
    val versao: Int? = null,
)

// --- Catalogo (03-catalogo.md) ---

data class Categoria(
    val id: Int,
    val nome: String,
    val tipo: String, // receita|despesa
    val cor: String = "slate",
)

data class CategoriaCorpo(val nome: String, val tipo: String, val cor: String)
data class CategoriaEdicao(val nome: String? = null, val cor: String? = null)

data class FormaPagamento(val id: Int, val nome: String)
data class FormaCorpo(val nome: String)

data class Conta(
    val id: Int,
    val nome: String,
    val saldoInicial: Double = 0.0,
    val nota: String = "",
    val icone: String = "",
    val principal: Boolean = false,
)

data class ContaCorpo(
    val nome: String,
    val saldoInicial: Double = 0.0,
    val nota: String = "",
    val icone: String = "",
    val principal: Boolean = false,
)

data class RestaurarResposta(val categorias: Int, val formas: Int)

// --- Lancamentos (02-backend.md) ---

data class Receita(
    val id: Int,
    val data: String, // YYYY-MM-DD
    val valor: Double,
    val categoria: String,
    val origem: String,
    val formaPagamento: String = "",
    val contaId: Int? = null,
    val nota: String = "",
    val fitid: String? = null,
)

data class Despesa(
    val id: Int,
    val data: String,
    val valor: Double,
    val categoria: String,
    val descricao: String = "",
    val formaPagamento: String = "",
    val contaId: Int? = null,
    val nota: String = "",
    val fitid: String? = null,
    val grupoParcela: String? = null,
    val parcelaAtual: Int? = null,
    val parcelaTotal: Int? = null,
)

data class ReceitaCorpo(
    val data: String,
    val valor: Double,
    val categoria: String,
    val origem: String,
    val formaPagamento: String = "",
    val contaId: Int,
    val nota: String = "",
)

data class DespesaCorpo(
    val data: String,
    val valor: Double,
    val categoria: String,
    val descricao: String = "",
    val formaPagamento: String = "",
    val contaId: Int,
    val nota: String = "",
    val parcelas: Int = 1,
)

data class ExcluirGrupoResposta(val excluidas: Int)

data class Resumo(
    val mes: String,
    val totalReceitas: Double,
    val totalDespesas: Double,
    val saldo: Double,
)

data class Contagem(
    val contas: Int,
    val receitas: Int,
    val despesas: Int,
    val categorias: Int,
    val formas: Int,
)

// --- Auditoria (05-auditoria.md) ---

data class AuditoriaItem(
    val id: Int,
    val modulo: String,
    val acao: String,
    val registroId: Int? = null,
    val descricao: String = "",
    val detalhes: String? = null,
    val criadoEm: String,
)

data class AuditoriaPagina(
    val itens: List<AuditoriaItem>,
    val total: Int,
    val pagina: Int,
    val porPagina: Int,
    val totalPaginas: Int,
)

data class LimpezaResposta(val excluidas: Int)

// --- Backup / demo (06-import-export-demo.md) ---

data class DemoStatus(
    val existe: Boolean = false,
    val contas: Int = 0,
    val receitas: Int = 0,
    val despesas: Int = 0,
)

data class ImportResult(
    val modo: String = "",
    val categorias: Int = 0,
    val formasPagamento: Int = 0,
    val contas: Int = 0,
    val receitas: Int = 0,
    val despesas: Int = 0,
)

// --- Importação OFX (06-import-export-demo.md) ---

data class OfxItemEnvio(
    val data: String,
    val valor: Double,
    val tipo: String, // receita|despesa
    val descricao: String = "",
    val fitid: String? = null,
)

data class ImportarOfxCorpo(
    val contaId: Int,
    val categoriaReceita: String,
    val categoriaDespesa: String,
    val formaPagamento: String? = null,
    val itens: List<OfxItemEnvio>,
)

data class ImportarOfxResposta(
    val receitas: Int = 0,
    val despesas: Int = 0,
    val ignorados: Int = 0,
)
