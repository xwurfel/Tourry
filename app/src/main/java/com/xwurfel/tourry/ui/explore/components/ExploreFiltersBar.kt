package com.xwurfel.tourry.ui.explore.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xwurfel.tourry.ui.explore.ExploreFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreFiltersBar(
    filters: ExploreFilters,
    onFiltersChanged: (ExploreFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date filter
        FilterChip(
            selected = filters.dateRange != null,
            onClick = { /* TODO: Show date picker */ },
            label = { Text("Date") },
            leadingIcon = {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        // Duration filter
        FilterChip(
            selected = filters.maxDuration != null,
            onClick = { /* TODO: Show duration picker */ },
            label = {
                Text(filters.maxDuration?.let { "${it}h" } ?: "Duration")
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        // Price filter
        FilterChip(
            selected = filters.maxPrice != null,
            onClick = { /* TODO: Show price picker */ },
            label = {
                Text(filters.maxPrice?.let { "< $${it.toInt()}" } ?: "Price")
            },
            leadingIcon = {
                Icon(
                    Icons.Default.AttachMoney,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        // Distance filter
        FilterChip(
            selected = filters.maxDistance != null,
            onClick = { /* TODO: Show distance picker */ },
            label = {
                Text(filters.maxDistance?.let { "< ${it}km" } ?: "Distance")
            },
            leadingIcon = {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}