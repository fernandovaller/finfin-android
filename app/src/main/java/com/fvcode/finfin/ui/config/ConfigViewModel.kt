package com.fvcode.finfin.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fvcode.finfin.core.network.ApiResult
import com.fvcode.finfin.data.model.Contagem
import com.fvcode.finfin.data.model.DemoStatus
import com.fvcode.finfin.data.model.IntegracoesResposta
import com.fvcode.finfin.data.repository.FinfinRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Arquivo pendente de gravação via Storage Access Framework. */
data class ArquivoPendente(
    val nomeSugerido: String,
    val texto: String,
    val comBom: Boolean = false,
)

/** Prévia de backup importado (validação `app === 'finfin'`). */
data class PreviaBackup(
    val json: JsonObject,
    val contas: Int,
    val receitas: Int,
    val despesas: Int,
    val categorias: Int,
    val formas: Int,
)

data class ConfigUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val info: String? = null,
    val sessaoExpirada: Boolean = false,
    val aba: Int = 0,
    val ocupado: Boolean = false, // alguma ação em andamento
    val contagem: Contagem? = null,
    val demo: DemoStatus? = null,
    val integracoes: IntegracoesResposta? = null,
    val arquivoPendente: ArquivoPendente? = null,
    val previaBackup: PreviaBackup? = null,
    val modoImport: String = "mesclar",
)

/**
 * Espelha `Configuracoes.tsx:52-727`: 4 abas (geral, backup, email, perigo).
 * Aparência (tema) vive no `SessaoViewModel`, como `ProvedorAparencia`.
 */
@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val repo: FinfinRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(ConfigUiState())
    val estado: StateFlow<ConfigUiState> = _estado

    init {
        carregarTudo()
    }

    fun recarregar() = carregarTudo()

    fun trocarAba(aba: Int) {
        _estado.value = _estado.value.copy(aba = aba)
    }

    fun consumirSessaoExpirada() {
        _estado.value = _estado.value.copy(sessaoExpirada = false)
    }

    fun consumirInfo() {
        _estado.value = _estado.value.copy(info = null)
    }

    fun informar(msg: String) {
        _estado.value = _estado.value.copy(info = msg)
    }

    fun consumirArquivoPendente() {
        _estado.value = _estado.value.copy(arquivoPendente = null)
    }

    fun fecharPrevia() {
        _estado.value = _estado.value.copy(previaBackup = null)
    }

    fun trocarModoImport(modo: String) {
        _estado.value = _estado.value.copy(modoImport = modo)
    }

    fun exportarJson() = executar {
        when (val r = repo.exportar()) {
            is ApiResult.Ok -> {
                val nome = "finfin-backup-${LocalDate.now()}.json"
                _estado.value = _estado.value.copy(
                    arquivoPendente = ArquivoPendente(nome, Gson().toJson(r.dados)),
                    info = null,
                )
            }
            is ApiResult.Erro -> _estado.value = _estado.value.copy(info = null, erro = r.mensagem)
        }
    }

    fun exportarCsv(tipo: String) = executar {
        when (val r = repo.exportarCsv(tipo)) {
            is ApiResult.Ok -> {
                val nome = "finfin-$tipo-${LocalDate.now()}.csv"
                _estado.value = _estado.value.copy(
                    arquivoPendente = ArquivoPendente(nome, r.dados, comBom = true),
                )
            }
            is ApiResult.Erro -> _estado.value = _estado.value.copy(erro = r.mensagem)
        }
    }

    /** Valida o texto do backup e abre a prévia de importação. */
    fun prepararImportacao(texto: String) {
        try {
            val json = Gson().fromJson(texto, JsonObject::class.java)
            if (json == null || json.get("app")?.asString != "finfin") {
                _estado.value = _estado.value.copy(erro = "Arquivo inválido: não é um backup FinFin.")
                return
            }
            fun tam(lista: String): Int = try {
                json.getAsJsonArray(lista)?.size() ?: 0
            } catch (_: Exception) {
                0
            }
            _estado.value = _estado.value.copy(
                erro = null,
                previaBackup = PreviaBackup(
                    json = json,
                    contas = tam("contas"),
                    receitas = tam("receitas"),
                    despesas = tam("despesas"),
                    categorias = tam("categorias"),
                    formas = tam("formasPagamento"),
                ),
                aba = 1,
            )
        } catch (_: Exception) {
            _estado.value = _estado.value.copy(erro = "Arquivo inválido: JSON ilegível.")
        }
    }

    fun confirmarImportacao() {
        val previa = _estado.value.previaBackup ?: return
        executar {
            when (val r = repo.importar(_estado.value.modoImport, previa.json)) {
                is ApiResult.Ok -> {
                    val d = r.dados
                    _estado.value = _estado.value.copy(
                        previaBackup = null,
                        info = "Importado (${d.modo}): ${d.receitas} rec, ${d.despesas} des, " +
                            "${d.contas} contas, ${d.categorias} cat, ${d.formasPagamento} formas.",
                    )
                    carregarTudo()
                }
                is ApiResult.Erro -> {
                    if (r.codigo == 401) sessaoExpirada()
                    else _estado.value = _estado.value.copy(erro = r.mensagem)
                }
            }
        }
    }

    fun restaurarPadrao() = executar {
        when (val r = repo.restaurarPadrao()) {
            is ApiResult.Ok -> {
                _estado.value = _estado.value.copy(
                    info = "Padrões repostos: ${r.dados.categorias} categorias, ${r.dados.formas} formas.",
                )
                carregarTudo()
            }
            is ApiResult.Erro -> {
                if (r.codigo == 401) sessaoExpirada()
                else _estado.value = _estado.value.copy(erro = r.mensagem)
            }
        }
    }

    fun gerarDemo() = executar {
        when (val r = repo.gerarDemo()) {
            is ApiResult.Ok -> {
                _estado.value = _estado.value.copy(info = "Demonstração gerada.")
                carregarTudo()
            }
            is ApiResult.Erro -> {
                if (r.codigo == 401) sessaoExpirada()
                // 409 se já existe.
                else _estado.value = _estado.value.copy(erro = r.mensagem)
            }
        }
    }

    fun removerDemo() = executar {
        when (val r = repo.removerDemo()) {
            is ApiResult.Ok -> {
                _estado.value = _estado.value.copy(info = "Demonstração removida.")
                carregarTudo()
            }
            is ApiResult.Erro -> {
                if (r.codigo == 401) sessaoExpirada()
                else _estado.value = _estado.value.copy(erro = r.mensagem)
            }
        }
    }

    fun salvarChaveEmail(chave: String) = executar {
        val limpa = chave.trim().ifBlank { null }
        when (val r = repo.salvarIntegracoes(limpa)) {
            is ApiResult.Ok -> {
                _estado.value = _estado.value.copy(info = "Chave atualizada.")
                carregarTudo()
            }
            is ApiResult.Erro -> {
                if (r.codigo == 401) sessaoExpirada()
                else _estado.value = _estado.value.copy(erro = r.mensagem)
            }
        }
    }

    fun apagarLancamentos() = executar {
        when (val r = repo.apagarLancamentos()) {
            is ApiResult.Ok -> {
                _estado.value = _estado.value.copy(info = "Lançamentos apagados (catálogo e contas mantidos).")
                carregarTudo()
            }
            is ApiResult.Erro -> {
                if (r.codigo == 401) sessaoExpirada()
                else _estado.value = _estado.value.copy(erro = r.mensagem)
            }
        }
    }

    fun apagarTudo() = executar {
        when (val r = repo.apagarTudo()) {
            is ApiResult.Ok -> {
                _estado.value = _estado.value.copy(info = "Lançamentos e contas apagados.")
                carregarTudo()
            }
            is ApiResult.Erro -> {
                if (r.codigo == 401) sessaoExpirada()
                else _estado.value = _estado.value.copy(erro = r.mensagem)
            }
        }
    }

    private fun sessaoExpirada() {
        _estado.value = _estado.value.copy(sessaoExpirada = true)
    }

    private fun executar(bloco: suspend () -> Unit) {
        _estado.value = _estado.value.copy(ocupado = true, erro = null)
        viewModelScope.launch {
            try {
                bloco()
            } catch (_: Exception) {
                _estado.value = _estado.value.copy(erro = "Falha de rede. Verifique a conexão.")
            } finally {
                _estado.value = _estado.value.copy(ocupado = false)
            }
        }
    }

    private fun carregarTudo() {
        _estado.value = _estado.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            try {
                val contagem = repo.contagem()
                val demo = repo.demoStatus()
                val integ = repo.integracoes()
                if (listOf(contagem, demo, integ).any { it is ApiResult.Erro && it.codigo == 401 }) {
                    _estado.value = _estado.value.copy(carregando = false, sessaoExpirada = true)
                    return@launch
                }
                _estado.value = _estado.value.copy(
                    carregando = false,
                    contagem = (contagem as? ApiResult.Ok)?.dados,
                    demo = (demo as? ApiResult.Ok)?.dados,
                    integracoes = (integ as? ApiResult.Ok)?.dados,
                    erro = (contagem as? ApiResult.Erro)?.mensagem
                        ?: (demo as? ApiResult.Erro)?.mensagem,
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
