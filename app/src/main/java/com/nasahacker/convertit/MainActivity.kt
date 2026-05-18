package com.nasahacker.convertit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nasahacker.convertit.domain.model.BottomNavigation
import com.nasahacker.convertit.service.ConvertItService
import com.nasahacker.convertit.ui.component.BottomNavigationBar
import com.nasahacker.convertit.ui.component.MainAppBar
import com.nasahacker.convertit.ui.home.HomeViewModel
import com.nasahacker.convertit.ui.navigation.AppNavHost
import com.nasahacker.convertit.ui.theme.AppTheme
import com.nasahacker.convertit.util.AppUtil
import com.nasahacker.convertit.util.ShareIntentRefresh
import dagger.hilt.android.AndroidEntryPoint

/**
 * Convertit Android app
 * Copyright (c) 2025 The Byte Array LTD.
 *
 * @author Tamim Hossain
 * @company The Byte Array LTD
 * @year 2025
 */

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        var pendingShareIntent: Intent? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            MainScreen()
        }

        AppUtil.handleNotificationPermission(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
        if (pendingShareIntent != null) {
            ShareIntentRefresh.notifyPendingShareIntent()
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND ||
            intent?.action == Intent.ACTION_SEND_MULTIPLE ||
            intent?.action == Intent.ACTION_VIEW
        ) {
            pendingShareIntent = intent
        }
    }
}


@Composable
fun MainScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    AppTheme {
        val context = LocalContext.current
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        LaunchedEffect(Unit) {
            suspend fun processPendingShareIntent() {
                val intent = MainActivity.pendingShareIntent ?: return
                val uris = AppUtil.shareOrViewUrisFromIntent(intent)
                MainActivity.pendingShareIntent = null
                if (uris.isEmpty()) return
                if (ConvertItService.isForegroundServiceStarted) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.label_warning),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return
                }
                navController.navigate(BottomNavigation.Home.route) {
                    launchSingleTop = true
                }
                homeViewModel.applySharedUrisForConversion(uris)
            }
            processPendingShareIntent()
            ShareIntentRefresh.events.collect {
                processPendingShareIntent()
            }
        }

        val isDetailScreen = currentRoute == "about" || currentRoute == "pro"
        Scaffold(
            topBar = {
                if (!isDetailScreen) {
                    MainAppBar(
                        onNavigateToPro = { navController.navigate("pro") },
                        onNavigateToAbout = { navController.navigate("about") },
                    )
                }
            },
            bottomBar = {
                if (!isDetailScreen) {
                    BottomNavigationBar(navController = navController)
                }
            },
            // Detail screens host their own ExpressiveScaffold (with its own
            // TopAppBar). Letting the outer Scaffold also reserve systemBars
            // insets here would double-count the status-bar height and push
            // the inner top app bar down by ~status-bar-height of empty space.
            // On detail routes we hand inset ownership to the inner scaffold
            // by zeroing out the outer one's contentWindowInsets.
            contentWindowInsets = if (isDetailScreen) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        ) { innerPadding ->
            AppNavHost(
                modifier = Modifier.padding(innerPadding),
                controller = navController,
                homeViewModel = homeViewModel,
            )
        }
    }
}
