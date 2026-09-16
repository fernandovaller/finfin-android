package com.fvcode.finfin.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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

private data class Destino(
    val rota: String,
    val titulo: String,
    val grupo: String,
    val icone: ImageVector,
)

private val destinos = listOf(
    Destino(Rotas.HOME, "Início", "Principal", Icons.Filled.Home),
    Destino(Rotas.LANCAMENTOS, "Lançamentos", "Principal", Icons.Filled.List),
    Destino(Rotas.OFX, "Importar OFX", "Principal", Icons.Filled.Upload),
    Destino(Rotas.RELATORIOS, "Relatórios", "Principal", Icons.Filled.BarChart),
    Destino(Rotas.CATEGORIAS, "Categorias", "Cadastros", Icons.Filled.Category),
    Destino(Rotas.CONTAS, "Contas", "Cadastros", Icons.Filled.AccountBalanceWallet),
    Destino(Rotas.FORMAS, "Formas de pagamento", "Cadastros", Icons.Filled.CreditCard),
    Destino(Rotas.CONFIG, "Configurações", "Sistema", Icons.Filled.Settings),
    Destino(Rotas.AUDITORIA, "Auditoria", "Sistema", Icons.Filled.History),
    Destino(Rotas.PERFIL, "Perfil", "Sistema", Icons.Filled.Person),
)

// 3 destinos principais visíveis na barra inferior — o resto fica no drawer.
private val destinosBottom = listOf(
    Rotas.HOME,
    Rotas.LANCAMENTOS,
    Rotas.RELATORIOS,
).mapNotNull { rota -> destinos.firstOrNull { it.rota == rota } }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EstruturaLogada(vm: SessaoViewModel, nav: NavHostController) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val escopo = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val rotaAtual = backStack?.destination?.route
    val destinoAtual = destinos.firstOrNull { it.rota == rotaAtual }

    fun navegar(rota: String) {
        escopo.launch { drawer.close() }
        nav.navigate(rota) {
            popUpTo(nav.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalNavigationDrawerSafeContent(
                rotaAtual = rotaAtual,
                aoNavegar = ::navegar,
                aoSair = {
                    escopo.launch { drawer.close() }
                    vm.sair { nav.navigate(Rotas.LOGIN) { popUpTo(0) } }
                },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(destinoAtual?.titulo ?: "FinFin") },
                    navigationIcon = {
                        IconButton(onClick = { escopo.launch { drawer.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Abrir menu")
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    destinosBottom.forEach { d ->
                        NavigationBarItem(
                            selected = rotaAtual == d.rota,
                            onClick = { navegar(d.rota) },
                            icon = { Icon(d.icone, contentDescription = d.titulo) },
                            label = { Text(d.titulo) },
                        )
                    }
                }
            },
        ) { inner ->
            NavHost(
                nav,
                startDestination = Rotas.HOME,
                modifier = Modifier.padding(inner),
            ) {
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
private fun ModalNavigationDrawerSafeContent(
    rotaAtual: String?,
    aoNavegar: (String) -> Unit,
    aoSair: () -> Unit,
) {
    ModalDrawerSheet {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("FinFin", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Controle financeiro", style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        destinos.groupBy { it.grupo }.forEach { (grupo, itens) ->
            Text(
                grupo,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
            )
            itens.forEach { d ->
                NavigationDrawerItem(
                    label = { Text(d.titulo) },
                    selected = rotaAtual == d.rota,
                    icon = { Icon(d.icone, contentDescription = null) },
                    onClick = { aoNavegar(d.rota) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(
            label = { Text("Sair") },
            selected = false,
            icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null) },
            onClick = aoSair,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(12.dp))
    }
}
