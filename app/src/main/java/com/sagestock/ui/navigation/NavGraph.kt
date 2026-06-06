package com.sagestock.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sagestock.ui.detail.DetailScreen
import com.sagestock.ui.search.SearchScreen

sealed class Screen(val route: String) {
    data object Search : Screen("search")
    data object Detail : Screen("detail/{ticker}") {
        fun go(ticker: String) = "detail/$ticker"
    }
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Search.route) {
        composable(Screen.Search.route) {
            SearchScreen(onStockClick = { ticker ->
                navController.navigate(Screen.Detail.go(ticker))
            })
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("ticker") { type = NavType.StringType })
        ) { back ->
            val ticker = back.arguments?.getString("ticker") ?: return@composable
            DetailScreen(ticker = ticker, onBack = { navController.popBackStack() })
        }
    }
}
