package com.xwurfel.tourry.ui.tour.creation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwurfel.tourry.R
import com.xwurfel.tourry.core.extension.collectWithLifecycle
import com.xwurfel.tourry.ui.tour.creation.steps.BasicInfoStep
import com.xwurfel.tourry.ui.tour.creation.steps.PreviewStep
import com.xwurfel.tourry.ui.tour.creation.steps.ScheduleStep
import com.xwurfel.tourry.ui.tour.creation.steps.StopsStep

@Composable
fun TourCreationRoute(
    editingTourId: String?,
    onNavigateBack: () -> Unit,
    onTourCreated: (String) -> Unit,
    viewModel: TourCreationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isEditing = uiState.isEditing

    viewModel.event.collectWithLifecycle { event ->
        when (event) {
            is TourCreationEvent.NavigateToTourDetail -> onTourCreated(event.tourId)
        }
    }

    TourCreationScreen(
        uiState = uiState,
        onIntent = viewModel::acceptIntent,
        onNavigateBack = onNavigateBack,
        isEditing = isEditing
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TourCreationScreen(
    uiState: TourCreationUiState,
    onIntent: (TourCreationIntent) -> Unit,
    onNavigateBack: () -> Unit,
    isEditing: Boolean = false
) {
    val pagerState = rememberPagerState(
        initialPage = uiState.currentStep,
        pageCount = { 4 }
    )

    LaunchedEffect(uiState.currentStep) {
        pagerState.animateScrollToPage(uiState.currentStep)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditing) R.string.edit_tour_title
                            else R.string.tour_creation_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            TourCreationBottomBar(
                currentStep = uiState.currentStep,
                onPreviousClick = { onIntent(TourCreationIntent.PreviousStep) },
                onNextClick = { onIntent(TourCreationIntent.NextStep) },
                onPublishClick = {
                    if (isEditing) {
                        onIntent(TourCreationIntent.UpdateTour)
                    } else {
                        onIntent(TourCreationIntent.PublishTour)
                    }
                },
                isPublishing = uiState.isPublishing,
                isEditing = isEditing
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Step indicator
            StepIndicator(
                currentStep = uiState.currentStep,
                modifier = Modifier.padding(16.dp)
            )

            // Step content
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> BasicInfoStep(
                        title = uiState.title,
                        theme = uiState.theme,
                        description = uiState.description,
                        coverImageUri = uiState.coverImageUri,
                        onInfoChanged = { title, theme, description, imageUri ->
                            onIntent(
                                TourCreationIntent.UpdateBasicInfo(
                                    title,
                                    theme,
                                    description,
                                    imageUri
                                )
                            )
                        }
                    )

                    1 -> StopsStep(
                        stops = uiState.stops,
                        onAddStop = { onIntent(TourCreationIntent.AddStop(it)) },
                        onUpdateStop = { index, stop ->
                            onIntent(TourCreationIntent.UpdateStop(index, stop))
                        },
                        onRemoveStop = { onIntent(TourCreationIntent.RemoveStop(it)) },
                        onReorderStops = { from, to ->
                            onIntent(TourCreationIntent.ReorderStops(from, to))
                        }
                    )

                    2 -> ScheduleStep(
                        startDateTime = uiState.startDateTime,
                        price = uiState.price,
                        recurrenceRule = uiState.recurrenceRule,
                        onScheduleChanged = { dateTime, price, rule ->
                            onIntent(TourCreationIntent.UpdateSchedule(dateTime, price, rule))
                        }
                    )

                    3 -> PreviewStep(
                        title = uiState.title,
                        theme = uiState.theme,
                        description = uiState.description,
                        coverImageUri = uiState.coverImageUri,
                        stops = uiState.stops,
                        startDateTime = uiState.startDateTime,
                        price = uiState.price
                    )
                }
            }
        }
    }
}

@Composable
fun StepIndicator(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val steps = listOf(
            stringResource(R.string.step_basic_info),
            stringResource(R.string.step_stops),
            stringResource(R.string.step_schedule),
            stringResource(R.string.step_preview)
        )

        steps.forEachIndexed { index, step ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (index <= currentStep)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (index <= currentStep)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index <= currentStep)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TourCreationBottomBar(
    currentStep: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPublishClick: () -> Unit,
    isPublishing: Boolean,
    isEditing: Boolean = false
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.previous))
                }
            }

            if (currentStep < 3) {
                Button(
                    onClick = onNextClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.next))
                }
            } else {
                Button(
                    onClick = onPublishClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isPublishing
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(
                                if (isEditing) R.string.update_tour
                                else R.string.publish
                            )
                        )
                    }
                }
            }
        }
    }
}
