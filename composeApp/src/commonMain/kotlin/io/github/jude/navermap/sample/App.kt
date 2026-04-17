package io.github.hyungju.navermap.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.hyungju.navermap.compose.NaverMapAuthProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = SampleDestination.fromRoute(backStackEntry?.destination?.route)

    MaterialTheme {
        NaverMapAuthProvider(ncpKeyId = SampleNaverMapClientId) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(currentDestination.title)
                        },
                        navigationIcon = {
                            if (currentDestination != SampleDestination.Home) {
                                TextButton(
                                    onClick = { navController.popBackStack() },
                                ) {
                                    Text("뒤로")
                                }
                            }
                        },
                    )
                },
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = SampleDestination.Home.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    composable(SampleDestination.Home.route) {
                        SampleHomeScreen(
                            onOpenSample = { destination ->
                                navController.navigate(destination.route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable(SampleDestination.Overview.route) {
                        OverviewSampleScreen()
                    }
                    composable(SampleDestination.RenderKey.route) {
                        RenderKeyMarkerSampleScreen()
                    }
                    composable(SampleDestination.Nationwide.route) {
                        NationwideMarkerSampleScreen()
                    }
                }
            }
        }
    }
}
