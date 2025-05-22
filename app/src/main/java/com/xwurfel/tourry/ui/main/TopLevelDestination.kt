package com.xwurfel.tourry.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tour
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tour
import androidx.compose.ui.graphics.vector.ImageVector
import com.xwurfel.tourry.R

/**
 * Type for the top level destinations in the application. Each of these destinations
 * can contain one or more screens (based on the window size). Navigation from one screen to the
 * next within a single destination will be handled directly in composables.
 */
enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val iconTextId: Int,
    val titleTextId: Int,
) {
    EXPLORE(
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        iconTextId = R.string.nav_explore,
        titleTextId = R.string.nav_explore
    ),
    MY_TOURS(
        selectedIcon = Icons.Filled.Tour,
        unselectedIcon = Icons.Outlined.Tour,
        iconTextId = R.string.nav_my_tours,
        titleTextId = R.string.nav_my_tours
    ),
    PROFILE(
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        iconTextId = R.string.nav_profile,
        titleTextId = R.string.nav_profile
    )
}
