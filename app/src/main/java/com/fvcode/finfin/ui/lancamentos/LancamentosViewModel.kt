package com.fvcode.finfin.ui.lancamentos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.core.util.deslocarMes
import com.fvcode.finfin.core.util.mesAtual
import com.fvcode.finfin.data.model.Conta
import com.fvcode.finfin.data.model.DespesaCorpo
import com.fvcode.finfin.data.model.ReceitaCorpo
import com.fvcode.finfin.data.repository.FinfinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Item unificado p/ lista (sort desc por data). */
data class Lancamento(
    val tipo: String, // receita|despesa
    val id: Int,
    val data: String,
    val valor: Double,
    val categoria: String,
    val titulo: String, // origem (receita) ou descricao (despesa)
    val formaPagamento: String,
    val contaId: Int?,
    val nota: String,
    val grupoParcela: String?,
    val parcelaAtual: Int?,
    val parcelaTotal: Int?,
)

/** Valores do formulário (espelha `LancamentoValues` de `LancamentoForm.tsx`). */
data class LancamentoValores(
    val data: String = LocalDate.now().toString(),
    val centavos: String = "",
    val categoria: String = "",
    val titulo: String = "",
    val formaPagamento: String = "",
    val contaId: Int? = null,
    val nota: String = "",
    val parcelas: Int = 1,
)

sealed interface DialogoLancamento {
    data object Oculto : DialogoLancamento
    data class Novo(val tipo: String) : DialogoLancamento
    data class Edicao(val item: Lancamento) : DialogoLancamento
    data class Exclusao(val item: Lancamento) : DialogoLancamento
}

data class LancamentosUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    val mes: String = mesAtual(),
    val contaFiltro: Int? = null,
    val filtroTipo: String = "todos", // todos|receita|despesa
    val itens: List<Lancamento> = emptyList(),
    val contas: List<Conta> = emptyList(),
    val categoriasReceita: List<String> = emptyList(),
    val categoriasDespesa: List<String> = emptyList(),
    val formas: List<String> = emptyList(),
    val dialogo: DialogoLancamento = DialogoLancamento.Oculto,
    val salvando: Boolean = false,
    val erroForm: String? = null,
    val info: String? = null, // ex.: "3 parcelas criadas"
)

/**
 * Espelha `Lancamentos.tsx:26-438`: GET no mount + filtro mes+conta,
 * POST/PUT/DELETE, exclusão de parcelada com `?escopo=grupo`.
 */
@HiltViewModel
class LancamentosViewModel @Inject constructor(
    private val repo: FinfinRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(LancamentosUiState())
    val estado: StateFlow<LancamentosUiState> = _estado

    init {
        carregar()
    }

    fun recarregar() = carregar()

    fun mudarMes(delta: Long) {
        _estado.value = _estado.value.copy(mes = deslocarMes(_estado.value.mes, delta))
        carregar()
    }

    fun irParaHoje() {
        _estado.value = _estado.value.copy(mes = mesAtual())
        carregar()
    }

    fun trocarConta(contaId: Int?) {
        _estado.value = _estado.value.copy(contaFiltro = contaId)
        carregar()
    }

    fun trocarFiltroTipo(tipo: String) {
        _estado.value = _estado.value.copy(filtroTipo = tipo)
    }

    fun abrirNovo(tipo: String) {
        _estado.value = _estado.value.copy(
            dialogo = DialogoLancamento.Novo(tipo),
            erroForm = null,
        )
    }

    fun abrirEdicao(item: Lancamento) {
        _estado.value = _estado.value.copy(
            dialogo = DialogoLancamento.Edicao(item),
            erroForm = null,
        )
    }

    fun pedirExclusao(item: Lancamento) {
        _estado.value = _estado.value.copy(dialogo = DialogoLancamento.Exclusao(item))
    }

    fun fecharDialogo() {
        val atual = _estado.value
        if (!atual.salvando) {
            _estado.value = atual.copy(dialogo = DialogoLancamento.Oculto, erroForm = null)
        }
    }

    fun consumirSessaoExpirada() {
        _estado.value = _estado.value.copy(sessaoExpirada = false)
    }

    fun consumirInfo() {
        _estado.value = _estado.value.copy(info = null)
    }

    fun contaPorId(id: Int?): String =
        _estado.value.contas.firstOrNull { it.id == id }?.nome ?: ""

    fun contaPrincipalOuPrimeira(): Int? {
        val contas = _estado.value.contas
        return contas.firstOrNull { it.principal }?.id ?: contas.firstOrNull()?.id
    }

    fun salvar(valores: LancamentoValores, tipo: String, editando: Lancamento?) {
        val erro = validar(valores, tipo, editando == null)
        if (erro != null) {
            _estado.value = _estado.value.copy(erroForm = erro)
            return
        }
        _estado.value = _estado.value.copy(salvando = true, erroForm = null)
        viewModelScope.launch {
            val contaId = valores.contaId ?: 0
            val resultado: ApiResult<String> = if (editando == null) {
                if (tipo == "receita") {
                    when (val r = repo.criarReceita(valores.receitaCorpo(contaId))) {
                        is ApiResult.Ok -> ApiResult.Ok("Receita criada")
                        is ApiResult.Erro -> r
                    }
                } else {
                    when (val r = repo.criarDespesa(valores.despesaCorpo(contaId))) {
                        is ApiResult.Ok -> ApiResult.Ok(
                            if (r.dados.size > 1) "${r.dados.size} parcelas criadas" else "Despesa criada",
                        )
                        is ApiResult.Erro -> r
                    }
                }
            } else {
                // Edita só 1 item, nunca re-parcelar (02-backend.md).
                if (tipo == "receita") {
                    when (val r = repo.editarReceita(editando.id, valores.receitaCorpo(contaId))) {
                        is ApiResult.Ok -> ApiResult.Ok("Receita atualizada")
                        is ApiResult.Erro -> r
                    }
                } else {
                    when (val r = repo.editarDespesa(editando.id, valores.despesaCorpo(contaId, parcelas = 1))) {
                        is ApiResult.Ok -> ApiResult.Ok("Despesa atualizada")
                        is ApiResult.Erro -> r
                    }
                }
            }
            when (resultado) {
                is ApiResult.Ok -> {
                    _estado.value = _estado.value.copy(
                        salvando = false,
                        dialogo = DialogoLancamento.Oculto,
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

    fun excluir(item: Lancamento, grupo: Boolean) {
        _estado.value = _estado.value.copy(salvando = true)
        viewModelScope.launch {
            val resultado: ApiResult<String> = if (item.tipo == "receita") {
                when (val r = repo.excluirReceita(item.id)) {
                    is ApiResult.Ok -> ApiResult.Ok("Receita excluída")
                    is ApiResult.Erro -> r
                }
            } else {
                when (val r = repo.excluirDespesa(item.id, grupo)) {
                    is ApiResult.Ok -> ApiResult.Ok(
                        if (grupo && r.dados.excluidas > 1) "${r.dados.excluidas} parcelas excluídas" else "Despesa excluída",
                    )
                    is ApiResult.Erro -> r
                }
            }
            when (resultado) {
                is ApiResult.Ok -> {
                    _estado.value = _estado.value.copy(
                        salvando = false,
                        dialogo = DialogoLancamento.Oculto,
                        info = resultado.dados,
                    )
                    carregar()
                }
                is ApiResult.Erro -> {
                    if (resultado.codigo == 401) {
                        _estado.value = _estado.value.copy(salvando = false, sessaoExpirada = true)
                    } else {
                        _estado.value = _estado.value.copy(
                            salvando = false,
                            dialogo = DialogoLancamento.Oculto,
                            erro = resultado.mensagem,
                        )
                    }
                }
            }
        }
    }

    /** Validações de `LancamentoForm.tsx:111-131` + regras 400 do backend. */
    private fun validar(v: LancamentoValores, tipo: String, criando: Boolean): String? {
        if (v.centavos.toLongOrNull()?.let { it > 0 } != true) return "Informe um valor maior que zero."
        if (v.categoria.isBlank()) return "Escolha uma categoria."
        if (v.contaId == null) return "Escolha uma conta."
        if (tipo == "receita" && v.titulo.isBlank()) return "Informe a origem."
        try {
            LocalDate.parse(v.data)
        } catch (_: Exception) {
            return "Data inválida. Use AAAA-MM-DD."
        }
        if (tipo == "despesa" && criando && (v.parcelas < 1 || v.parcelas > 21)) {
            return "Parcelas deve ser entre 1 e 21."
        }
        return null
    }

    private fun carregar() {
        val atual = _estado.value
        _estado.value = atual.copy(carregando = true, erro = null)
        viewModelScope.launch {
            val mes = _estado.value.mes
            val contaFiltro = _estado.value.contaFiltro
            try {
                val (rRec, rDes, rContas, rCats, rFormas) = coroutineScope {
                    val dRec = async { repo.receitas(contaFiltro) }
                    val dDes = async { repo.despesas(contaFiltro) }
                    val dContas = async { repo.contas() }
                    val dCats = async { repo.categorias() }
                    val dFormas = async { repo.formas() }
                    Cinco(dRec.await(), dDes.await(), dContas.await(), dCats.await(), dFormas.await())
                }
                if (listOf(rRec, rDes).any { it is ApiResult.Erro && it.codigo == 401 }) {
                    _estado.value = _estado.value.copy(carregando = false, sessaoExpirada = true)
                    return@launch
                }
                val receitas = (rRec as? ApiResult.Ok)?.dados ?: emptyList()
                val despesas = (rDes as? ApiResult.Ok)?.dados ?: emptyList()
                val primeiroErro = listOf(rRec, rDes).filterIsInstance<ApiResult.Erro>().firstOrNull()
                if (primeiroErro != null) {
                    _estado.value = _estado.value.copy(carregando = false, erro = primeiroErro.mensagem)
                    return@launch
                }
                val itens = (receitas.filter { it.data.startsWith(mes) }.map {
                    Lancamento("receita", it.id, it.data, it.valor, it.categoria, it.origem,
                        it.formaPagamento, it.contaId, it.nota, null, null, null)
                } + despesas.filter { it.data.startsWith(mes) }.map {
                    Lancamento("despesa", it.id, it.data, it.valor, it.categoria,
                        it.descricao.ifBlank { it.categoria }, it.formaPagamento, it.contaId,
                        it.nota, it.grupoParcela, it.parcelaAtual, it.parcelaTotal)
                }).sortedWith(compareByDescending<Lancamento> { it.data }.thenByDescending { it.id })

                val cats = (rCats as? ApiResult.Ok)?.dados ?: emptyList()
                _estado.value = _estado.value.copy(
                    carregando = false,
                    itens = itens,
                    contas = (rContas as? ApiResult.Ok)?.dados ?: _estado.value.contas,
                    categoriasReceita = cats.filter { it.tipo == "receita" }.map { it.nome },
                    categoriasDespesa = cats.filter { it.tipo == "despesa" }.map { it.nome },
                    formas = ((rFormas as? ApiResult.Ok)?.dados ?: emptyList()).map { it.nome },
                )
            } catch (_: Exception) {
                _estado.value = _estado.value.copy(
                    carregando = false,
                    erro = "Falha de rede. Verifique a conexão.",
                )
            }
        }
    }

    private fun LancamentoValores.valor(): Double = (centavos.toLongOrNull() ?: 0) / 100.0

    private fun LancamentoValores.receitaCorpo(contaId: Int) = ReceitaCorpo(
        data = data,
        valor = valor(),
        categoria = categoria,
        origem = titulo,
        formaPagamento = formaPagamento,
        contaId = contaId,
        nota = nota,
    )

    private fun LancamentoValores.despesaCorpo(contaId: Int, parcelas: Int = this.parcelas) = DespesaCorpo(
        data = data,
        valor = valor(),
        categoria = categoria,
        descricao = titulo,
        formaPagamento = formaPagamento,
        contaId = contaId,
        nota = nota,
        parcelas = parcelas,
    )

    private data class Cinco<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
