package com.sagestock.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sagestock.ui.detail.DetailScreen
import com.sagestock.ui.detail.DetailViewModel
import com.sagestock.ui.home.HomeScreen
import com.sagestock.ui.search.SearchScreen
import com.sagestock.ui.signal.SignalScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Signals : Screen("signals")
    data object Detail : Screen("detail/{ticker}?index={index}") {
        fun go(ticker: String) = "detail/$ticker"
        fun goWithIndex(ticker: String, index: Int) = "detail/$ticker?index=$index"
    }
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStockClick = { ticker -> navController.navigate(Screen.Detail.go(ticker)) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onStockClick = { ticker -> navController.navigate(Screen.Detail.go(ticker)) },
                onSignalsClick = { navController.navigate(Screen.Signals.route) },
            )
        }
        composable(Screen.Signals.route) {
            SignalScreen(
                onBack = { navController.popBackStack() },
                onSignalClick = { signal ->
                    navController.navigate(Screen.Detail.goWithIndex(signal.ticker, signal.candleIndex))
                },
            )
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
