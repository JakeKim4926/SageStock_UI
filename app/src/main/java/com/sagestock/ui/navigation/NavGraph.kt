package com.sagestock.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sagestock.ui.components.SageBottomBar
import com.sagestock.ui.components.Tab
import com.sagestock.ui.detail.DetailScreen
import com.sagestock.ui.detail.DetailViewModel
import com.sagestock.ui.home.HomeScreen
import com.sagestock.ui.login.LoginScreen
import com.sagestock.ui.paper.PaperScreen
import com.sagestock.ui.prediction.PredictionScreen
import com.sagestock.ui.search.SearchScreen
import com.sagestock.ui.settings.SettingsScreen
import com.sagestock.ui.signal.SignalScreen
import com.sagestock.ui.splash.SplashScreen

/** 모든 라우트. 탭 라우트(home~settings)는 [Tab]과 1:1. */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Home : Screen(Tab.HOME.route)
    data object Search : Screen(Tab.SEARCH.route)
    data object Signals : Screen(Tab.SIGNALS.route)
    data object Paper : Screen(Tab.PAPER.route)
    data object Settings : Screen(Tab.SETTINGS.route)
    data object Prediction : Screen("prediction")
    data object Detail : Screen("detail/{ticker}?index={index}") {
        fun go(ticker: String) = "detail/$ticker"
        fun goWithIndex(ticker: String, index: Int) = "detail/$ticker?index=$index"
    }
}

@Composable
fun SageStockApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Tab.routes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SageBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { tab -> navController.navigateToTab(tab.route) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onAutoLoggedIn = { navController.replaceWith(Screen.Home.route, Screen.Splash.route) },
                    onNeedLogin = { navController.replaceWith(Screen.Login.route, Screen.Splash.route) },
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoggedIn = { navController.replaceWith(Screen.Home.route, Screen.Login.route) },
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onStockClick = { ticker -> navController.navigate(Screen.Detail.go(ticker)) },
                    onSearchClick = { navController.navigateToTab(Screen.Search.route) },
                    onPredictionClick = { navController.navigate(Screen.Prediction.route) },
                    onSignalsClick = { navController.navigateToTab(Screen.Signals.route) },
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onStockClick = { ticker -> navController.navigate(Screen.Detail.go(ticker)) },
                    onSignalsClick = { navController.navigateToTab(Screen.Signals.route) },
                )
            }
            composable(Screen.Signals.route) {
                SignalScreen(
                    onSignalClick = { signal ->
                        navController.navigate(Screen.Detail.goWithIndex(signal.ticker, signal.candleIndex))
                    },
                )
            }
            composable(Screen.Paper.route) {
                PaperScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onLoggedOut = { navController.replaceWith(Screen.Login.route, clearAll = true) },
                )
            }

            composable(Screen.Prediction.route) {
                PredictionScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument(DetailViewModel.ARG_TICKER) { type = NavType.StringType },
                    navArgument(DetailViewModel.ARG_INDEX) { type = NavType.IntType; defaultValue = -1 },
                ),
            ) { back ->
                val ticker = back.arguments?.getString(DetailViewModel.ARG_TICKER) ?: return@composable
                DetailScreen(ticker = ticker, onBack = { navController.popBackStack() })
            }
        }
    }
}

/** 탭 전환: 상태 저장·복원 + 중복 푸시 방지. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** 단방향 전환(스플래시·로그인). [popUpToRoute]가 있으면 그 라우트를, [clearAll]이면 전체 백스택을 제거. */
private fun NavHostController.replaceWith(
    route: String,
    popUpToRoute: String? = null,
    clearAll: Boolean = false,
) {
    navigate(route) {
        when {
            clearAll -> popUpTo(0) { inclusive = true }
            popUpToRoute != null -> popUpTo(popUpToRoute) { inclusive = true }
        }
        launchSingleTop = true
    }
}
