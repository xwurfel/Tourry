package com.xwurfel.tourry.ui.tour.creation.mapper

import com.xwurfel.tourry.feature.tours.domain.model.CreateStopContent
import com.xwurfel.tourry.feature.tours.domain.model.CreateTourRequest
import com.xwurfel.tourry.feature.tours.domain.model.CreateTourStop
import com.xwurfel.tourry.feature.tours.domain.model.CreationTourStop
import com.xwurfel.tourry.feature.tours.domain.model.Tour
import com.xwurfel.tourry.feature.tours.domain.model.TourStop
import com.xwurfel.tourry.feature.tours.domain.model.TourTheme
import com.xwurfel.tourry.ui.tour.creation.TourCreationUiState
import com.xwurfel.tourry.feature.tours.domain.model.TourStop as DomainTourStop

object TourCreationMapper {

    fun TourCreationUiState.toCreateTourRequest(): CreateTourRequest {
        return CreateTourRequest(
            title = title,
            description = description,
            theme = theme?.name ?: "OTHER",
            coverImageUrl = coverImageUri,
            price = price,
            startTime = startDateTime ?: System.currentTimeMillis(),
            duration = calculateEstimatedDuration(),
            maxParticipants = null, // Can be added later if needed
            stops = stops.map { stop ->
                stop.toCreateTourStop()
            },
            tags = generateTags()
        )
    }

    private fun TourStop.toCreateTourStop(order: Int): CreateTourStop {
        return CreateTourStop(
            name = name,
            description = description,
            latitude = latitude,
            longitude = longitude,
            order = order,
            content = CreateStopContent(
                text = description,
                imageUrls = content?.imageUrls ?: emptyList(),
                audioUrl = content?.audioUrl,
                videoUrl = null // Can be added later if needed
            )
        )
    }

    private fun TourCreationUiState.calculateEstimatedDuration(): Int {
        // Estimate duration based on number of stops
        // Base duration + time per stop
        val baseMinutes = 30
        val minutesPerStop = 15
        return baseMinutes + (stops.size * minutesPerStop)
    }

    private fun TourCreationUiState.generateTags(): List<String> {
        val tags = mutableListOf<String>()

        // Add theme as tag
        theme?.let { tags.add(it.name.lowercase()) }

        // Add price-based tag
        if (price == 0.0) {
            tags.add("free")
        } else {
            tags.add("paid")
        }

        // Add duration-based tag
        val estimatedDuration = calculateEstimatedDuration()
        when {
            estimatedDuration <= 60 -> tags.add("short")
            estimatedDuration <= 120 -> tags.add("medium")
            else -> tags.add("long")
        }

        // Add stop count based tag
        when {
            stops.size <= 3 -> tags.add("few-stops")
            stops.size <= 6 -> tags.add("medium-stops")
            else -> tags.add("many-stops")
        }

        return tags
    }

    /**
     * Reverse mapping: Domain model to ViewModel model for editing
     */
    fun Tour.toTourCreationUiState(): TourCreationUiState {
        val viewModelStops = stops.map { it.toViewModelTourStop() }
        val theme = try {
            TourTheme.valueOf(this.theme)
        } catch (e: IllegalArgumentException) {
            TourTheme.OTHER
        }

        return TourCreationUiState(
            currentStep = 0,
            title = title,
            theme = theme,
            description = description,
            coverImageUri = coverImageUrl,
            stops = viewModelStops.map { it.toCreationTourStop() },
            startDateTime = startTime,
            price = price,
            recurrenceRule = null, // Not stored in domain model yet
            isEditing = true
        )
    }

    fun DomainTourStop.toViewModelTourStop(): TourStop {
        return TourStop(
            id = id,
            name = name,
            description = description,
            latitude = latitude,
            longitude = longitude,
            content = content,
            order = order
        )
    }

    fun DomainTourStop.toCreationTourStop(): CreationTourStop {
        return CreationTourStop(
            id = id,
            name = name,
            description = description,
            latitude = latitude,
            longitude = longitude,
            mediaUrls = content?.imageUrls ?: emptyList(),
            audioUrl = content?.audioUrl,
            order = order
        )
    }

    fun CreationTourStop.toCreateTourStop(): CreateTourStop {
        return CreateTourStop(
            name = name,
            description = description,
            latitude = latitude,
            longitude = longitude,
            order = order,
            content = CreateStopContent(
                text = description,
                imageUrls = mediaUrls,
                audioUrl = audioUrl,
                videoUrl = null
            )
        )
    }

    fun String.toTourTheme(): TourTheme {
        return try {
            TourTheme.valueOf(this)
        } catch (e: IllegalArgumentException) {
            TourTheme.OTHER
        }
    }
}