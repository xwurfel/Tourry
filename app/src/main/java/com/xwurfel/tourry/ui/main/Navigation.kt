package com.xwurfel.tourry.ui.main

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Upcoming
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Grid3x3
import androidx.compose.material.icons.rounded.Upcoming
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xwurfel.tourry.core.ui.ThemePreviews
import com.xwurfel.tourry.ui.theme.TourryTheme

@Composable
fun RowScope.TemplateNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selectedIcon: @Composable () -> Unit = icon,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = if (selected) selectedIcon else icon,
        modifier = modifier,
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = TemplateNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = TemplateNavigationDefaults.navigationContentColor(),
            selectedTextColor = TemplateNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = TemplateNavigationDefaults.navigationContentColor(),
            indicatorColor = TemplateNavigationDefaults.navigationIndicatorColor(),
        ),
    )
}

@Composable
fun TemplateNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    NavigationBar(
        modifier = modifier,
        contentColor = TemplateNavigationDefaults.navigationContentColor(),
        tonalElevation = 0.dp,
        content = content,
    )
}

@Composable
fun TemplateNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selectedIcon: @Composable () -> Unit = icon,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = if (selected) selectedIcon else icon,
        modifier = modifier,
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = TemplateNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = TemplateNavigationDefaults.navigationContentColor(),
            selectedTextColor = TemplateNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = TemplateNavigationDefaults.navigationContentColor(),
            indicatorColor = TemplateNavigationDefaults.navigationIndicatorColor(),
        ),
    )
}

@Composable
fun TemplateNavigationRail(
    modifier: Modifier = Modifier,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    NavigationRail(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = TemplateNavigationDefaults.navigationContentColor(),
        header = header,
        content = content,
    )
}

@ThemePreviews
@Composable
fun TemplateNavigationPreview() {
    val items = listOf("For you", "Saved", "Interests")
    val icons = listOf(
        Icons.Outlined.Upcoming,
        Icons.Outlined.Bookmarks,
        Icons.Rounded.Grid3x3,
    )
    val selectedIcons = listOf(
        Icons.Rounded.Upcoming,
        Icons.Rounded.Bookmarks,
        Icons.Rounded.Grid3x3,
    )

    TourryTheme {
        TemplateNavigationBar {
            items.forEachIndexed { index, item ->
                TemplateNavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = item,
                        )
                    },
                    selectedIcon = {
                        Icon(
                            imageVector = selectedIcons[index],
                            contentDescription = item,
                        )
                    },
                    label = { Text(item) },
                    selected = index == 0,
                    onClick = { },
                )
            }
        }
    }
}

object TemplateNavigationDefaults {
    @Composable
    fun navigationContentColor() = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun navigationSelectedItemColor() = MaterialTheme.colorScheme.onPrimaryContainer

    @Composable
    fun navigationIndicatorColor() = MaterialTheme.colorScheme.primaryContainer
}
