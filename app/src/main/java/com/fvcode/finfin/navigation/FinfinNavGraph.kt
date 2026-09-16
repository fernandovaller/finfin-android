package com.fvcode.finfin.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fvcode.finfin.ui.screens.TelaCarga
import com.fvcode.finfin.ui.screens.TelaLogin
import com.fvcode.finfin.ui.screens.TelaSimples
import com.fvcode.finfin.ui.auditoria.AuditoriaScreen
import com.fvcode.finfin.ui.categorias.CategoriasScreen
import com.fvcode.finfin.ui.config.ConfigScreen
import com.fvcode.finfin.ui.contas.ContasScreen
import com.fvcode.finfin.ui.ofx.OfxScreen
import com.fvcode.finfin.ui.perfil.PerfilScreen
import com.fvcode.finfin.ui.formas.FormasScreen
import com.fvcode.finfin.ui.home.HomeScreen
import com.fvcode.finfin.ui.lancamentos.LancamentosScreen
import com.fvcode.finfin.ui.relatorios.RelatoriosScreen
import com.fvcode.finfin.ui.session.SessaoUi
import com.fvcode.finfin.ui.session.SessaoViewModel
import kotlinx.coroutines.launch

private data class Destino(val rota: String, val titulo: String, val grupo: String)

private val destinos = listOf(
    Destino(Rotas.HOME, "Início", "Principal"),
    Destino(Rotas.LANCAMENTOS, "Lançamentos", "Principal"),
    Destino(Rotas.OFX, "Importar OFX", "Principal"),
    Destino(Rotas.RELATORIOS, "Relatórios", "Principal"),
    Destino(Rotas.CATEGORIAS, "Categorias", "Cadastros"),
    Destino(Rotas.CONTAS, "Contas", "Cadastros"),
    Destino(Rotas.FORMAS, "Formas de pagamento", "Cadastros"),
    Destino(Rotas.CONFIG, "Configurações", "Sistema"),
    Destino(Rotas.AUDITORIA, "Auditoria", "Sistema"),
    Destino(Rotas.PERFIL, "Perfil", "Sistema"),
)

/**
 * Equivalente de `RotaProtegida > Layout > Outlet` (`main.tsx`, `Layout.tsx`).
 */
@Composable
fun FinfinNavGraph(
    vm: SessaoViewModel = hiltViewModel(),
    nav: NavHostController = rememberNavController(),
) {
    val estado by vm.estado.collectAsState()

    when (estado) {
        SessaoUi.Carregando -> TelaCarga()
        SessaoUi.Deslogada -> NavHost(nav, startDestination = Rotas.LOGIN) {
            composable(Rotas.LOGIN) {
                TelaLogin(vm, aoEntrar = {
                    nav.navigate(Rotas.HOME) { popUpTo(Rotas.LOGIN) { inclusive = true } }
                })
            }
            composable(Rotas.RECUPERAR) { TelaSimples("Recuperar senha") }
            composable(Rotas.REDEFINIR) { TelaSimples("Redefinir senha") }
        }
        is SessaoUi.Logada -> EstruturaLogada(vm, nav)
    }
}

@Composable
private fun EstruturaLogada(vm: SessaoViewModel, nav: NavHostController) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val escopo = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalNavigationDrawerSafeContent(
                aoNavegar = { rota ->
                    escopo.launch { drawer.close() }
                    nav.navigate(rota) { launchSingleTop = true }
                },
                aoSair = {
                    escopo.launch { drawer.close() }
                    vm.sair { nav.navigate(Rotas.LOGIN) { popUpTo(0) } }
                },
            )
        },
    ) {
        Scaffold { inner ->
            NavHost(nav, startDestination = Rotas.HOME, modifier = Modifier.padding(inner)) {
                composable(Rotas.HOME) {
                    HomeScreen(
                        aoSessaoExpirada = {
                            vm.sair { nav.navigate(Rotas.LOGIN) { popUpTo(0) } }
                        },
                        aoGerenciarContas = {
                            nav.navigate(Rotas.CONTAS) { launchSingleTop = true }
                        },
                        aoNovoLancamento = {
                            nav.navigate(Rotas.LANCAMENTOS) { launchSingleTop = true }
                        },
                    )
                }
                composable(Rotas.LANCAMENTOS) {
                    LancamentosScreen(
                        aoSessaoExpirada = {
                            vm.sair { nav.navigate(Rotas.LOGIN) { popUpTo(0) } }
                        },
                    )
                }
                composable(Rotas.OFX) {
                    OfxScreen(
                        aoSessaoExpirada = {
                            vm.sair { nav.navigate(Rotas.LOGIN) { popUpTo(0) } }
                        },
                    )
                }
                composable(Rotas.RELATORIOS) {
                    RelatoriosScreen(
                        aoSessaoExpirada = {
                            vm.sair { nav.navigate(Rotas.LOGIN) { popUpTo(0) } }
                        },
                    )
                }
                composable(Rotas.CATEGORIAS) {
                    CategoriasScreen(
                        aoSessaoExpirada = {
                            vm.sair { nav.navigate(Rotas.LOGIN) { popUpTo(0) } }
                        },
                    )
                }
                composable(Rotas.CONTAS) {
                    ContasScreen(
                        aoSessaoExpirada = {
                            vm.sair { nav.navigate(Rotas.LOGIN) { popUpTo(0) } }
                        },
                    )
                }
                composable(Rotas.FORMAS) {
                    FormasScreen(
                        aoSessaoExpirada = {
                            vm.sair { nav.navigate(Rotas.LOGIN) { popUpTo(0) } }
                        },
                    )
                }
                composable(Rotas.AUDITORIA) {
                    AuditoriaScreen(
                        aoSessaoExpirada = {
                            vm.sair { nav.navigate(Rotas.LOGIN) { popUpTo(0) } }
                        },
                    )
                }
                composable(Rotas.CONFIG) {
                    ConfigScreen(
                        aoSessaoExpirada = {
                            vm.sair { nav.navigate(Rotas.LOGIN) { popUpTo(0) } }
                        },
                    )
                }
                composable(Rotas.PERFIL) {
                    PerfilScreen(
                        aoSessaoExpirada = {
                            vm.sair { nav.navigate(Rotas.LOGIN) { popUpTo(0) } }
                        },
                    )
                }
                composable(Rotas.LOGIN) { TelaSimples("Sessão encerrada") }
            }
        }
    }
}

@Composable
private fun ModalNavigationDrawerSafeContent(aoNavegar: (String) -> Unit, aoSair: () -> Unit) {
    ModalDrawerSheet {
        destinos.forEach { d ->
            NavigationDrawerItem(
                label = { Text(d.titulo) },
                selected = false,
                onClick = { aoNavegar(d.rota) },
            )
        }
        NavigationDrawerItem(label = { Text("Sair") }, selected = false, onClick = aoSair)
    }
}
