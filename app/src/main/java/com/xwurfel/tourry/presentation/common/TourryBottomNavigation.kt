package com.xwurfel.tourry.presentation.common

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.xwurfel.tourry.presentation.navigation.BottomNavItem
import com.xwurfel.tourry.presentation.navigation.Destinations

@Composable
fun TourryBottomNavigation(
    currentRoute: Destinations, onItemSelected: (Destinations) -> Unit
) {
    NavigationBar {
        BottomNavItem.entries.forEach { item ->
            val selected = currentRoute::class == item.route::class

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = selected,
                onClick = { onItemSelected(item.route) }
            )
        }
    }
}