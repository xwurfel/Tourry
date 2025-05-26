package com.xwurfel.tourry.ui.tour.creation.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleStep(
    startDateTime: Long?,
    price: Double,
    recurrenceRule: String?,
    onScheduleChanged: (Long, Double, String?) -> Unit
) {
    val scrollState = rememberScrollState()
    var priceText by remember(price) { mutableStateOf(if (price > 0) price.toString() else "") }
    var isFree by remember(price) { mutableStateOf(price == 0.0) }
    var isRecurring by remember(recurrenceRule) { mutableStateOf(recurrenceRule != null) }

    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDateTime ?: (System.currentTimeMillis() + 86400000)
    )

    // Time picker state
    var showTimePicker by remember { mutableStateOf(false) }
    val calendar = remember(startDateTime) {
        Calendar.getInstance().apply {
            if (startDateTime != null) timeInMillis = startDateTime
            else {
                add(Calendar.HOUR_OF_DAY, 2) // Default to 2 hours from now
            }
        }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE)
    )

    val finalDateTime by remember {
        derivedStateOf {
            val selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
            val selectedCalendar = Calendar.getInstance().apply {
                timeInMillis = selectedDate
                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                set(Calendar.MINUTE, timePickerState.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedCalendar.timeInMillis
        }
    }

    LaunchedEffect(finalDateTime, isFree, priceText, isRecurring) {
        onScheduleChanged(
            finalDateTime,
            if (isFree) 0.0 else priceText.toDoubleOrNull() ?: 0.0,
            if (isRecurring) "weekly" else null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "When does your tour start?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = SimpleDateFormat(
                        "EEEE, MMMM d, yyyy",
                        Locale.getDefault()
                    ).format(
                        Date(
                            finalDateTime
                        )
                    ),
                    onValueChange = { },
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select date")
                        }
                    })

                OutlinedTextField(
                    value = SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                    ).format(Date(finalDateTime)),
                    onValueChange = { },
                    label = { Text("Time") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Select time")
                        }
                    })

                // Validation message for past dates
                if (finalDateTime <= System.currentTimeMillis()) {
                    Text(
                        "⚠️ Tour must be scheduled for the future",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Pricing Section (unchanged)
        Card(
            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "How much does it cost?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("This is a free tour")
                    Switch(
                        checked = isFree, onCheckedChange = { checked ->
                            isFree = checked
                            if (checked) priceText = ""
                        })
                }

                if (!isFree) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { newValue ->
                            priceText = newValue
                        },
                        label = { Text("Price per person (USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("$") },
                        supportingText = { Text("Set a fair price for your time and expertise") })
                }
            }
        }

        // Recurring tours section (unchanged)
        Card(
            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Recurring tours",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Repeat this tour weekly")
                        Text(
                            "Same time, same day of the week",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isRecurring, onCheckedChange = { checked ->
                            isRecurring = checked
                        })
                }
            }
        }

        // Tips card (unchanged)
        Card(
            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "💡 Tips for scheduling",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "• Schedule tours at least 24 hours in advance\n" + "• Consider weather and lighting conditions\n" + "• Popular times: mornings (9-11 AM) and late afternoons (3-5 PM)\n" + "• Allow 2-3 hours for most walking tours",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = { showDatePicker = false }) {
                Text("OK")
            }
        }, dismissButton = {
            TextButton(onClick = { showDatePicker = false }) {
                Text("Cancel")
            }
        }) {
            DatePicker(
                state = datePickerState,
                title = { Text("Select tour date") },
                headline = { Text("Choose when your tour starts") })
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onCancel = { showTimePicker = false },
            onConfirm = { showTimePicker = false },
            title = "Select tour time",
        ) {
            TimePicker(
                state = timePickerState, modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun TimePickerDialog(
    title: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    toggle()
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onCancel
                    ) { Text("Cancel") }
                    TextButton(
                        onClick = onConfirm
                    ) { Text("OK") }
                }
            }
        }
    }
}