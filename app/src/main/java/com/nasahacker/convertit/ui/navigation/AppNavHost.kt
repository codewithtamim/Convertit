package com.nasahacker.convertit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nasahacker.convertit.domain.model.BottomNavigation
import com.nasahacker.convertit.ui.about.AboutScreen
import com.nasahacker.convertit.ui.home.HomeScreen
import com.nasahacker.convertit.ui.home.HomeViewModel
import com.nasahacker.convertit.ui.library.LibraryScreen
import com.nasahacker.convertit.ui.pro.ProScreen

/**
 * Convertit Android app
 *
 * Created by Tamim Hossain.
 * Copyright (c) 2025 The Byte Array LTD.
 *
 * @author Tamim Hossain
 * @company The Byte Array LTD
 * @year 2025
 */

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    controller: NavHostController,
    homeViewModel: HomeViewModel,
) {
    NavHost(
        modifier = modifier,
        navController = controller,
        startDestination = BottomNavigation.Home.route,
    ) {
        composable(BottomNavigation.Home.route) {
            HomeScreen(viewModel = homeViewModel)
        }
        composable(BottomNavigation.Library.route) {
            LibraryScreen()
        }
        composable("about") {
            AboutScreen(onNavigateBack = { controller.popBackStack() })
        }
        composable("pro") {
            ProScreen(onNavigateBack = { controller.popBackStack() })
        }
    }
}
