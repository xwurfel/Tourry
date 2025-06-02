package com.xwurfel.tourry.ui.explore.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xwurfel.tourry.feature.tours.domain.model.ExploreFilters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreFiltersBar(
    filters: ExploreFilters,
    onFiltersChanged: (ExploreFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }
    var showPricePicker by remember { mutableStateOf(false) }
    var showDistancePicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        // Clear all filters chip
        if (hasActiveFilters(filters)) {
            FilterChip(
                selected = false,
                onClick = {
                    onFiltersChanged(ExploreFilters())
                },
                label = { Text("Clear") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        // Date filter
        FilterChip(
            selected = filters.dateRange != null,
            onClick = { showDatePicker = true },
            label = {
                Text(
                    if (filters.dateRange != null) {
                        val startDate = SimpleDateFormat("MMM d", Locale.getDefault())
                            .format(Date(filters.dateRange.first))
                        val endDate = SimpleDateFormat("MMM d", Locale.getDefault())
                            .format(Date(filters.dateRange.second))
                        "$startDate - $endDate"
                    } else {
                        "Date"
                    }
                )
            },
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
            onClick = { showDurationPicker = true },
            label = {
                Text(filters.maxDuration?.let { "≤ ${it}h" } ?: "Duration")
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
            onClick = { showPricePicker = true },
            label = {
                Text(filters.maxPrice?.let { "≤ $${it.toInt()}" } ?: "Price")
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
            onClick = { showDistancePicker = true },
            label = {
                Text(filters.maxDistance?.let { "≤ ${it}km" } ?: "Distance")
            },
            leadingIcon = {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        Spacer(modifier = Modifier.width(16.dp))
    }

    // Date Range Picker Dialog
    if (showDatePicker) {
        DateRangePickerDialog(
            currentRange = filters.dateRange,
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = { startDate, endDate ->
                onFiltersChanged(
                    filters.copy(
                        dateRange = if (startDate != null && endDate != null) {
                            Pair(startDate, endDate)
                        } else null
                    )
                )
                showDatePicker = false
            }
        )
    }

    // Duration Picker Dialog
    if (showDurationPicker) {
        DurationPickerDialog(
            currentDuration = filters.maxDuration,
            onDismiss = { showDurationPicker = false },
            onDurationSelected = { duration ->
                onFiltersChanged(filters.copy(maxDuration = duration))
                showDurationPicker = false
            }
        )
    }

    // Price Picker Dialog
    if (showPricePicker) {
        PricePickerDialog(
            currentPrice = filters.maxPrice,
            onDismiss = { showPricePicker = false },
            onPriceSelected = { price ->
                onFiltersChanged(filters.copy(maxPrice = price))
                showPricePicker = false
            }
        )
    }

    // Distance Picker Dialog
    if (showDistancePicker) {
        DistancePickerDialog(
            currentDistance = filters.maxDistance,
            onDismiss = { showDistancePicker = false },
            onDistanceSelected = { distance ->
                onFiltersChanged(filters.copy(maxDistance = distance))
                showDistancePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    currentRange: Pair<Long, Long>?,
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long?, Long?) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = currentRange?.first ?: System.currentTimeMillis()
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = currentRange?.second
            ?: (System.currentTimeMillis() + 86400000) // +1 day
    )

    var showingStartDate by remember { mutableStateOf(true) }

    val confirmEnabled by remember {
        derivedStateOf {
            if (showingStartDate) {
                datePickerState.selectedDateMillis != null
            } else {
                endDatePickerState.selectedDateMillis != null
            }
        }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (showingStartDate) {
                        showingStartDate = false
                    } else {
                        onDateRangeSelected(
                            datePickerState.selectedDateMillis,
                            endDatePickerState.selectedDateMillis
                        )
                    }
                },
                enabled = confirmEnabled
            ) {
                Text(if (showingStartDate) "Next" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (showingStartDate) "Select start date" else "Select end date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (showingStartDate) {
                DatePicker(state = datePickerState)
            } else {
                DatePicker(state = endDatePickerState)
            }
        }
    }
}

@Composable
private fun DurationPickerDialog(
    currentDuration: Int?,
    onDismiss: () -> Unit,
    onDurationSelected: (Int?) -> Unit
) {
    var duration by remember { mutableFloatStateOf(currentDuration?.toFloat() ?: 2f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Maximum Duration") },
        text = {
            Column {
                Text("Select maximum tour duration in hours")
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Up to ${duration.roundToInt()} hours",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Slider(
                    value = duration,
                    onValueChange = { duration = it },
                    valueRange = 0.5f..8f,
                    steps = 15, // 0.5h steps
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("30min", style = MaterialTheme.typography.bodySmall)
                    Text("8h", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDurationSelected(duration.roundToInt()) }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PricePickerDialog(
    currentPrice: Double?,
    onDismiss: () -> Unit,
    onPriceSelected: (Double?) -> Unit
) {
    var price by remember { mutableFloatStateOf(currentPrice?.toFloat() ?: 50f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Maximum Price") },
        text = {
            Column {
                Text("Select maximum tour price")
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Up to $${price.roundToInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Slider(
                    value = price,
                    onValueChange = { price = it },
                    valueRange = 0f..200f,
                    steps = 39, // $5 steps
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Free", style = MaterialTheme.typography.bodySmall)
                    Text("$200", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { onPriceSelected(0.0) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show only free tours")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPriceSelected(price.toDouble()) }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DistancePickerDialog(
    currentDistance: Float?,
    onDismiss: () -> Unit,
    onDistanceSelected: (Float?) -> Unit
) {
    var distance by remember { mutableFloatStateOf(currentDistance ?: 5f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Maximum Distance") },
        text = {
            Column {
                Text("Select maximum distance from your location")
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Within ${String.format(Locale.getDefault(), "%.1f", distance)} km",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Slider(
                    value = distance,
                    onValueChange = { distance = it },
                    valueRange = 0.5f..50f,
                    steps = 98, // 0.5km steps
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.5km", style = MaterialTheme.typography.bodySmall)
                    Text("50km", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDistanceSelected(distance) }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun hasActiveFilters(filters: ExploreFilters): Boolean {
    return filters.dateRange != null ||
            filters.maxDuration != null ||
            filters.maxPrice != null ||
            filters.maxDistance != null
}