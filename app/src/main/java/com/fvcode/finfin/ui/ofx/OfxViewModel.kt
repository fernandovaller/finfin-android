package com.fvcode.finfin.ui.ofx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.core.util.OfxItem
import com.fvcode.finfin.core.util.parseOfx
import com.fvcode.finfin.data.model.Conta
import com.fvcode.finfin.data.model.ImportarOfxCorpo
import com.fvcode.finfin.data.model.OfxItemEnvio
import com.fvcode.finfin.data.repository.FinfinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Linha da prévia: `{item, incluir, tipo, categoria}` (espelha `ImportarOfx.tsx`). */
data class LinhaOfx(
    val item: OfxItem,
    val incluir: Boolean = true,
    val tipo: String = item.tipo,
    val categoria: String = "",
)

data class OfxUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val info: String? = null,
    val sessaoExpirada: Boolean = false,
    val contas: List<Conta> = emptyList(),
    val categoriasReceita: List<String> = emptyList(),
    val categoriasDespesa: List<String> = emptyList(),
    val formas: List<String> = emptyList(),
    val contaId: Int? = null,
    val categoriaReceita: String = "",
    val categoriaDespesa: String = "",
    val formaPagamento: String = "",
    val nomeArquivo: String = "",
    val linhas: List<LinhaOfx> = emptyList(),
    val enviando: Boolean = false,
)

/**
 * Espelha `ImportarOfx.tsx:22-342`: defaults (conta principal, cats, forma),
 * prévia com checkbox, toggle de tipo e categoria por linha.
 */
@HiltViewModel
class OfxViewModel @Inject constructor(
    private val repo: FinfinRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(OfxUiState())
    val estado: StateFlow<OfxUiState> = _estado

    init {
        carregarDefaults()
    }

    fun recarregar() = carregarDefaults()

    fun consumirSessaoExpirada() {
        _estado.value = _estado.value.copy(sessaoExpirada = false)
    }

    fun consumirInfo() {
        _estado.value = _estado.value.copy(info = null)
    }

    fun trocarConta(id: Int) {
        _estado.value = _estado.value.copy(contaId = id)
    }

    fun trocarCategoriaReceita(nome: String) {
        val atual = _estado.value
        _estado.value = atual.copy(
            categoriaReceita = nome,
            linhas = atual.linhas.map {
                if (it.tipo == "receita" && it.categoria == atual.categoriaReceita) it.copy(categoria = nome)
                else it
            },
        )
    }

    fun trocarCategoriaDespesa(nome: String) {
        val atual = _estado.value
        _estado.value = atual.copy(
            categoriaDespesa = nome,
            linhas = atual.linhas.map {
                if (it.tipo == "despesa" && it.categoria == atual.categoriaDespesa) it.copy(categoria = nome)
                else it
            },
        )
    }

    fun trocarForma(nome: String) {
        _estado.value = _estado.value.copy(formaPagamento = nome)
    }

    fun importarTexto(texto: String, nomeArquivo: String) {
        try {
            val itens = parseOfx(texto)
            val atual = _estado.value
            if (itens.isEmpty()) {
                _estado.value = atual.copy(
                    erro = "Nenhum lançamento encontrado no arquivo.",
                    nomeArquivo = nomeArquivo,
                    linhas = emptyList(),
                )
                return
            }
            _estado.value = atual.copy(
                erro = null,
                info = null,
                nomeArquivo = nomeArquivo,
                linhas = itens.map {
                    LinhaOfx(
                        item = it,
                        categoria = if (it.tipo == "receita") atual.categoriaReceita else atual.categoriaDespesa,
                    )
                },
            )
        } catch (e: IllegalArgumentException) {
            _estado.value = _estado.value.copy(
                erro = e.message,
                nomeArquivo = nomeArquivo,
                linhas = emptyList(),
            )
        } catch (_: Exception) {
            _estado.value = _estado.value.copy(
                erro = "Falha ao ler o arquivo.",
                nomeArquivo = nomeArquivo,
                linhas = emptyList(),
            )
        }
    }

    fun alternarIncluir(indice: Int) {
        _estado.value = _estado.value.copy(
            linhas = _estado.value.linhas.mapIndexed { i, l ->
                if (i == indice) l.copy(incluir = !l.incluir) else l
            },
        )
    }

    fun marcarTodas(incluir: Boolean) {
        _estado.value = _estado.value.copy(
            linhas = _estado.value.linhas.map { it.copy(incluir = incluir) },
        )
    }

    fun trocarTipo(indice: Int, tipo: String) {
        val atual = _estado.value
        _estado.value = atual.copy(
            linhas = atual.linhas.mapIndexed { i, l ->
                if (i == indice) {
                    l.copy(
                        tipo = tipo,
                        categoria = if (tipo == "receita") atual.categoriaReceita else atual.categoriaDespesa,
                    )
                } else l
            },
        )
    }

    fun trocarCategoriaLinha(indice: Int, categoria: String) {
        _estado.value = _estado.value.copy(
            linhas = _estado.value.linhas.mapIndexed { i, l ->
                if (i == indice) l.copy(categoria = categoria) else l
            },
        )
    }

    fun enviar() {
        val atual = _estado.value
        if (atual.contaId == null) {
            _estado.value = atual.copy(erro = "Escolha a conta de destino.")
            return
        }
        if (atual.categoriaReceita.isBlank() || atual.categoriaDespesa.isBlank()) {
            _estado.value = atual.copy(erro = "Escolha as categorias padrão de receita e despesa.")
            return
        }
        val incluidos = atual.linhas.filter { it.incluir }
        if (incluidos.isEmpty()) {
            _estado.value = atual.copy(erro = "Selecione ao menos 1 lançamento.")
            return
        }
        if (incluidos.size > 2000) {
            _estado.value = atual.copy(erro = "Máximo de 2000 itens por importação.")
            return
        }
        _estado.value = atual.copy(enviando = true, erro = null)
        viewModelScope.launch {
            val corpo = ImportarOfxCorpo(
                contaId = atual.contaId,
                categoriaReceita = atual.categoriaReceita,
                categoriaDespesa = atual.categoriaDespesa,
                formaPagamento = atual.formaPagamento.ifBlank { null },
                itens = incluidos.map {
                    OfxItemEnvio(
                        data = it.item.data,
                        valor = it.item.valor,
                        tipo = it.tipo,
                        descricao = it.item.descricao,
                        fitid = it.item.fitid,
                    )
                },
            )
            when (val r = repo.importarOfx(corpo)) {
                is ApiResult.Ok -> {
                    _estado.value = _estado.value.copy(
                        enviando = false,
                        linhas = emptyList(),
                        nomeArquivo = "",
                        info = "Importados: ${r.dados.receitas} receita(s), ${r.dados.despesas} despesa(s), " +
                            "${r.dados.ignorados} ignorado(s) por FITID repetido.",
                    )
                }
                is ApiResult.Erro -> {
                    if (r.codigo == 401) {
                        _estado.value = _estado.value.copy(enviando = false, sessaoExpirada = true)
                    } else {
                        _estado.value = _estado.value.copy(enviando = false, erro = r.mensagem)
                    }
                }
            }
        }
    }

    private fun carregarDefaults() {
        _estado.value = _estado.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            try {
                val rContas = repo.contas()
                val rCats = repo.categorias()
                val rFormas = repo.formas()
                if (listOf(rContas, rCats, rFormas).any { it is ApiResult.Erro && it.codigo == 401 }) {
                    _estado.value = _estado.value.copy(carregando = false, sessaoExpirada = true)
                    return@launch
                }
                val contas = (rContas as? ApiResult.Ok)?.dados ?: emptyList()
                val cats = (rCats as? ApiResult.Ok)?.dados ?: emptyList()
                val rec = cats.filter { it.tipo == "receita" }.map { it.nome }
                val des = cats.filter { it.tipo == "despesa" }.map { it.nome }
                _estado.value = _estado.value.copy(
                    carregando = false,
                    contas = contas,
                    categoriasReceita = rec,
                    categoriasDespesa = des,
                    formas = ((rFormas as? ApiResult.Ok)?.dados ?: emptyList()).map { it.nome },
                    contaId = contas.firstOrNull { it.principal }?.id ?: contas.firstOrNull()?.id,
                    categoriaReceita = rec.firstOrNull() ?: "",
                    categoriaDespesa = des.firstOrNull() ?: "",
                    erro = (rContas as? ApiResult.Erro)?.mensagem,
                )
            } catch (_: Exception) {
                _estado.value = _estado.value.copy(
                    carregando = false,
                    erro = "Falha de rede. Verifique a conexão.",
                )
            }
        }
    }
}
