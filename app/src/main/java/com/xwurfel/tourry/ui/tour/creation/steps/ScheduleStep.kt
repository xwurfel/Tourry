package com.xwurfel.tourry.ui.tour.creation.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
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
                    value = startDateTime?.let {
                        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "",
                    onValueChange = { },
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            // TODO: Open date picker
                            val currentTime = System.currentTimeMillis()
                            onScheduleChanged(
                                currentTime,
                                if (isFree) 0.0 else priceText.toDoubleOrNull() ?: 0.0,
                                if (isRecurring) "weekly" else null
                            )
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select date")
                        }
                    }
                )

                OutlinedTextField(
                    value = startDateTime?.let {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
                    } ?: "",
                    onValueChange = { },
                    label = { Text("Time") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            // TODO: Open time picker
                        }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Select time")
                        }
                    }
                )
            }
        }

        // Pricing Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
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
                        checked = isFree,
                        onCheckedChange = { checked ->
                            isFree = checked
                            if (checked) {
                                priceText = ""
                                onScheduleChanged(
                                    startDateTime ?: System.currentTimeMillis(),
                                    0.0,
                                    if (isRecurring) "weekly" else null
                                )
                            }
                        }
                    )
                }

                if (!isFree) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { newValue ->
                            priceText = newValue
                            val price = newValue.toDoubleOrNull() ?: 0.0
                            onScheduleChanged(
                                startDateTime ?: System.currentTimeMillis(),
                                price,
                                if (isRecurring) "weekly" else null
                            )
                        },
                        label = { Text("Price per person (USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("$") },
                        supportingText = { Text("Set a fair price for your time and expertise") }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
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
                        checked = isRecurring,
                        onCheckedChange = { checked ->
                            isRecurring = checked
                            onScheduleChanged(
                                startDateTime ?: System.currentTimeMillis(),
                                if (isFree) 0.0 else priceText.toDoubleOrNull() ?: 0.0,
                                if (checked) "weekly" else null
                            )
                        }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "💡 Tips for scheduling",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "• Schedule tours at least 24 hours in advance\n" +
                            "• Consider weather and lighting conditions\n" +
                            "• Popular times: mornings (9-11 AM) and late afternoons (3-5 PM)\n" +
                            "• Allow 2-3 hours for most walking tours",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}