package com.sagun12.vozemcena.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sagun12.vozemcena.VozEmCenaApplication
import com.sagun12.vozemcena.ui.AppViewModelProvider
import com.sagun12.vozemcena.ui.admin.AdminScreen
import com.sagun12.vozemcena.ui.admin.AdminViewModel
import com.sagun12.vozemcena.ui.create.CreateScreen
import com.sagun12.vozemcena.ui.create.CreateViewModel
import com.sagun12.vozemcena.ui.dubbing.DubbingScreen
import com.sagun12.vozemcena.ui.dubbing.DubbingViewModel
import com.sagun12.vozemcena.ui.home.HomeScreen
import com.sagun12.vozemcena.ui.home.HomeViewModel
import com.sagun12.vozemcena.ui.library.LibraryScreen
import com.sagun12.vozemcena.ui.library.LibraryViewModel
import com.sagun12.vozemcena.ui.theme.GoldPrimary
import com.sagun12.vozemcena.ui.voices.VoicesScreen
import com.sagun12.vozemcena.ui.voices.VoicesViewModel
import com.sagun12.vozemcena.ui.voices.clone.VoiceCloneScreen
import com.sagun12.vozemcena.ui.voices.clone.VoiceCloneViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    object Home : Screen("home", "Início", Icons.Default.Home)
    object Dubbing : Screen("dubbing", "Dublagem", Icons.Default.RecordVoiceOver)
    object Voices : Screen("voices", "Vozes", Icons.Default.Mic)
    object Library : Screen("library", "Biblioteca", Icons.Default.VideoLibrary)
    object Admin : Screen("admin", "Ajustes", Icons.Default.Settings)
    object VoiceClone : Screen("voices_clone", "Clonar Voz", null)
    object Create : Screen("create/{projectId}", "Criar", null) {
        fun createRoute(projectId: String) = "create/$projectId"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Dubbing,
    Screen.Voices,
    Screen.Library,
    Screen.Admin
)

@Composable
fun MainNavigationApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val showBottomBar = bottomNavItems.any { it.route == currentDestination }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination == screen.route
                        NavigationBarItem(
                            icon = { screen.icon?.let { Icon(imageVector = it, contentDescription = screen.title) } },
                            label = { Text(screen.title) },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = GoldPrimary,
                                indicatorColor = GoldPrimary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
                HomeScreen(
                    viewModel = homeViewModel,
                    onCreateProject = { projectId ->
                        navController.navigate(Screen.Create.createRoute(projectId))
                    },
                    onOpenProject = { projectId ->
                        navController.navigate(Screen.Create.createRoute(projectId))
                    },
                    onNavigateToVoices = {
                        navController.navigate(Screen.Voices.route)
                    },
                    onNavigateToLibrary = {
                        navController.navigate(Screen.Library.route)
                    },
                    onNavigateToDubbing = {
                        navController.navigate(Screen.Dubbing.route)
                    }
                )
            }

            composable(Screen.Dubbing.route) {
                val dubbingViewModel: DubbingViewModel = viewModel(factory = AppViewModelProvider.Factory)
                DubbingScreen(viewModel = dubbingViewModel)
            }

            composable(Screen.Voices.route) {
                val voicesViewModel: VoicesViewModel = viewModel(factory = AppViewModelProvider.Factory)
                VoicesScreen(
                    viewModel = voicesViewModel,
                    onNavigateToVoiceClone = {
                        navController.navigate(Screen.VoiceClone.route)
                    }
                )
            }

            composable(Screen.VoiceClone.route) {
                val voiceCloneViewModel: VoiceCloneViewModel = viewModel(factory = AppViewModelProvider.Factory)
                VoiceCloneScreen(
                    viewModel = voiceCloneViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Library.route) {
                val libraryViewModel: LibraryViewModel = viewModel(factory = AppViewModelProvider.Factory)
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onOpenProject = { projectId ->
                        navController.navigate(Screen.Create.createRoute(projectId))
                    }
                )
            }

            composable(Screen.Admin.route) {
                val adminViewModel: AdminViewModel = viewModel(factory = AppViewModelProvider.Factory)
                AdminScreen(viewModel = adminViewModel)
            }

            composable(
                route = Screen.Create.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                val context = LocalContext.current
                val app = context.applicationContext as VozEmCenaApplication

                val createViewModel = viewModel {
                    CreateViewModel(
                        context = app.applicationContext,
                        projectId = projectId,
                        projectRepository = app.projectRepository,
                        voiceRepository = app.voiceRepository
                    )
                }

                CreateScreen(
                    viewModel = createViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
