package com.jinggong.timer.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.jinggong.timer.R
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.ui.history.HistoryScreen
import com.jinggong.timer.ui.home.HomeScreen
import com.jinggong.timer.ui.practice.PracticeScreen
import com.jinggong.timer.ui.settings.AboutScreen
import com.jinggong.timer.ui.settings.AudioManageScreen
import com.jinggong.timer.ui.settings.SettingsScreen
import com.jinggong.timer.ui.stats.StatsScreen

private data class BottomDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination(Routes.HOME, R.string.nav_today, Icons.Outlined.Home),
    BottomDestination(Routes.HISTORY, R.string.nav_history, Icons.Outlined.History),
    BottomDestination(Routes.STATS, R.string.nav_stats, Icons.Outlined.BarChart),
    BottomDestination(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings),
)

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == null || currentRoute in Routes.topLevel

    val openPractice by container.navigationEvents.openPractice.collectAsStateWithLifecycle()
    LaunchedEffect(openPractice?.nonce) {
        val request = openPractice ?: return@LaunchedEffect
        container.awaitReady()
        val timer = container.timerManager.state.value
        val activeMode = timer.mode?.takeIf { timer.isActive }
        if (activeMode != null) {
            navController.navigate(Routes.practice(activeMode)) {
                launchSingleTop = true
            }
        }
        container.navigationEvents.consumeOpenPractice()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp,
                    ) {
                        bottomDestinations.forEach { destination ->
                            val selected = currentRoute == destination.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(destination.route) {
                                            popUpTo(
                                                navController.graph.findStartDestination().id,
                                            ) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = null,
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(destination.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    onOpenMode = { mode -> navController.navigate(Routes.practice(mode)) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(container = container)
            }
            composable(Routes.STATS) {
                StatsScreen(container = container)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container = container,
                    onOpenAudioManage = { navController.navigate(Routes.AUDIO_MANAGE) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                )
            }
            composable(
                route = Routes.PRACTICE_PATTERN,
                arguments = listOf(navArgument("mode") { type = NavType.StringType }),
            ) { entry ->
                val mode = PracticeMode.fromRoute(entry.arguments?.getString("mode"))
                if (mode == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    PracticeScreen(
                        container = container,
                        mode = mode,
                        onBack = { navController.popBackStack() },
                        onOpenSettings = {
                            navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                        },
                        onOpenActiveMode = { activeMode ->
                            navController.navigate(Routes.practice(activeMode)) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
            composable(Routes.AUDIO_MANAGE) {
                AudioManageScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
