package com.xwurfel.tourry.ui.tour.mine

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.xwurfel.tourry.R
import com.xwurfel.tourry.core.extension.collectWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MyToursRoute(
    onNavigateToTourDetail: (String) -> Unit,
    onNavigateToLiveTour: (String) -> Unit,
    onNavigateToTourSummary: (String) -> Unit,
    onNavigateToTourEdit: (String) -> Unit,
    viewModel: MyToursViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.event.collectWithLifecycle { event ->
        when (event) {
            is MyToursEvent.NavigateToTourDetail -> onNavigateToTourDetail(event.tourId)
            is MyToursEvent.NavigateToLiveTour -> onNavigateToLiveTour(event.tourId)
            is MyToursEvent.NavigateToTourSummary -> onNavigateToTourSummary(event.tourId)
            is MyToursEvent.NavigateToTourEdit -> onNavigateToTourEdit(event.tourId)
        }
    }

    MyToursScreen(
        uiState = uiState,
        onIntent = viewModel::acceptIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyToursScreen(
    uiState: MyToursUiState,
    onIntent: (MyToursIntent) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = if (uiState.selectedTab == MyToursTab.JOINED) 0 else 1,
        pageCount = { 2 }
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.selectedTab) {
        pagerState.animateScrollToPage(
            if (uiState.selectedTab == MyToursTab.JOINED) 0 else 1
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_my_tours)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                            onIntent(MyToursIntent.TabChanged(MyToursTab.JOINED))
                        }
                    },
                    text = { Text(stringResource(R.string.tab_joined)) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                            onIntent(MyToursIntent.TabChanged(MyToursTab.CREATED))
                        }
                    },
                    text = { Text(stringResource(R.string.tab_created)) }
                )
            }

            // Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ToursList(
                        tours = uiState.joinedTours,
                        isLoading = uiState.isLoading,
                        emptyMessage = "You haven't joined any tours yet",
                        onTourClick = { tour ->
                            onIntent(MyToursIntent.TourClicked(tour.id, tour.status))
                        },
                        showManagementOptions = false
                    )

                    1 -> ToursList(
                        tours = uiState.createdTours,
                        isLoading = uiState.isLoading,
                        emptyMessage = "You haven't created any tours yet",
                        onTourClick = { tour ->
                            onIntent(MyToursIntent.TourClicked(tour.id, tour.status))
                        },
                        showManagementOptions = true,
                        onEditClick = { tour ->
                            onIntent(MyToursIntent.EditTour(tour.id))
                        },
                        onCancelClick = { tour ->
                            onIntent(MyToursIntent.CancelTour(tour.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ToursList(
    tours: List<MyTour>,
    isLoading: Boolean,
    emptyMessage: String,
    onTourClick: (MyTour) -> Unit,
    showManagementOptions: Boolean,
    onEditClick: ((MyTour) -> Unit)? = null,
    onCancelClick: ((MyTour) -> Unit)? = null
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        tours.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tours) { tour ->
                    MyTourCard(
                        tour = tour,
                        onClick = { onTourClick(tour) },
                        showManagementOptions = showManagementOptions,
                        onEditClick = { onEditClick?.invoke(tour) },
                        onCancelClick = { onCancelClick?.invoke(tour) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTourCard(
    tour: MyTour,
    onClick: () -> Unit,
    showManagementOptions: Boolean,
    onEditClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    AsyncImage(
                        model = tour.coverImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status badge
                    Surface(
                        color = when (tour.status) {
                            TourStatus.LIVE -> MaterialTheme.colorScheme.error
                            TourStatus.UPCOMING -> MaterialTheme.colorScheme.primary
                            TourStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = tour.status.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (tour.status) {
                                TourStatus.LIVE -> MaterialTheme.colorScheme.onError
                                TourStatus.UPCOMING -> MaterialTheme.colorScheme.onPrimary
                                TourStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Title
                    Text(
                        text = tour.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Date/Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                .format(Date(tour.startTime)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Additional info
                    if (showManagementOptions && tour.participantsCount > 0) {
                        Text(
                            text = stringResource(
                                R.string.participants_count,
                                tour.participantsCount
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (tour.rating != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = tour.rating.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Management options
            if (showManagementOptions && tour.status == TourStatus.UPCOMING) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onEditClick?.invoke() }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.edit))
                    }

                    TextButton(
                        onClick = { onCancelClick?.invoke() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}