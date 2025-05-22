package com.xwurfel.tourry.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost

@Composable
fun TemplateNavHost(
    appState: TourryAppState,
    modifier: Modifier = Modifier,
    startDestination: String = appState.startDestination.collectAsStateWithLifecycle().value,
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {

    }
}
