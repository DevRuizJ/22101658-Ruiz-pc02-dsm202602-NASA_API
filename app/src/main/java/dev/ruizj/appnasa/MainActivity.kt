package dev.ruizj.appnasa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.ruizj.appnasa.apod.ApodViewModel
import dev.ruizj.appnasa.apod.ImagesFeature
import dev.ruizj.appnasa.neows.AsteroidsFeature
import dev.ruizj.appnasa.neows.NeoWsViewModel
import dev.ruizj.appnasa.ui.theme.AppNASATheme

class MainActivity : ComponentActivity() {
    private val apodViewModel: ApodViewModel by viewModels()
    private val neoWsViewModel: NeoWsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNASATheme {
                AppNavigation(apodViewModel, neoWsViewModel)
            }
        }
    }
}

@Composable
private fun AppNavigation(apodViewModel: ApodViewModel, neoWsViewModel: NeoWsViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedSection = if (currentRoute == AppSection.Asteroids.route) {
        AppSection.Asteroids
    } else {
        AppSection.Images
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedSection == AppSection.Images,
                    onClick = {
                        navController.navigate(AppSection.Images.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Text(text = stringResource(R.string.images_icon)) },
                    label = { Text(text = stringResource(R.string.images_tab)) },
                )
                NavigationBarItem(
                    selected = selectedSection == AppSection.Asteroids,
                    onClick = {
                        navController.navigate(AppSection.Asteroids.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Text(text = stringResource(R.string.asteroids_icon)) },
                    label = { Text(text = stringResource(R.string.asteroids_tab)) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppSection.Images.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(AppSection.Images.route) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    apodViewModel.refreshIfNeeded()
                }
                ImagesFeature(viewModel = apodViewModel)
            }
            composable(AppSection.Asteroids.route) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    neoWsViewModel.refreshIfNeeded()
                }
                AsteroidsFeature(viewModel = neoWsViewModel)
            }
        }
    }
}

private enum class AppSection(val route: String, val titleRes: Int) {
    Images("images", R.string.images_tab),
    Asteroids("asteroids", R.string.asteroids_tab),
}