package com.hardagenda.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.hardagenda.app.ui.login.LoginScreen
import com.hardagenda.app.ui.tabs.*
import com.hardagenda.app.ui.theme.GreenDark
import com.hardagenda.app.ui.theme.GrayText
import com.hardagenda.app.util.PrefsManager

sealed class Screen(val title: String, val icon: ImageVector) {
    data object Hoy : Screen("Hoy", Icons.Filled.Today)
    data object NuevoTurno : Screen("Nuevo turno", Icons.Filled.Add)
    data object TodosTurnos : Screen("Todos", Icons.Filled.List)
    data object Historial : Screen("Historial", Icons.Filled.History)
    data object AcercaDe : Screen("Acerca de", Icons.Filled.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardAgendaApp() {
    val context = LocalContext.current
    var isLoggedIn by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableIntStateOf(0) }

    val screens = listOf(Screen.Hoy, Screen.NuevoTurno, Screen.TodosTurnos, Screen.Historial, Screen.AcercaDe)

    if (!isLoggedIn) {
        LoginScreen(onLoginSuccess = { isLoggedIn = true })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "HardAgenda - Turnero",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GreenDark,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        TextButton(onClick = {
                            PrefsManager.clearSession(context)
                            isLoggedIn = false
                        }) {
                            Text(
                                "Cerrar sesion",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    screens.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    screen.title,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            selected = currentScreen == index,
                            onClick = { currentScreen = index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GreenDark,
                                selectedTextColor = GreenDark,
                                indicatorColor = GreenDark.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    0 -> HoyTab()
                    1 -> NuevoTurnoTab()
                    2 -> TodosTurnosTab()
                    3 -> HistorialTab()
                    4 -> AcercaDeTab()
                }
            }
        }
    }
}
