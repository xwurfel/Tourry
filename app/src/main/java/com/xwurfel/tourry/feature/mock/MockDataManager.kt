package com.xwurfel.tourry.feature.mock

import com.xwurfel.tourry.ui.explore.TourPreview
import com.xwurfel.tourry.ui.tour.detail.TourDetail
import com.xwurfel.tourry.ui.tour.detail.TourGuide
import com.xwurfel.tourry.ui.tour.detail.TourStopDetail
import com.xwurfel.tourry.ui.tour.mine.MyTour
import com.xwurfel.tourry.ui.tour.mine.TourStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Enhanced MockDataManager that simulates realistic backend functionality
 * with proper data updates and state management
 */
@Singleton
class MockDataManager @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory storage
    private val _availableTours = MutableStateFlow(generateInitialTours())
    val availableTours: StateFlow<List<TourPreview>> = _availableTours.asStateFlow()

    private val _joinedTourIds = MutableStateFlow<Set<String>>(emptySet())
    val joinedTourIds: StateFlow<Set<String>> = _joinedTourIds.asStateFlow()

    private val _createdTours = MutableStateFlow<List<MyTour>>(generateInitialCreatedTours())
    val createdTours: StateFlow<List<MyTour>> = _createdTours.asStateFlow()

    // User state
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // Live tour simulation
    private val _liveTours = MutableStateFlow<Set<String>>(emptySet())
    val liveTours: StateFlow<Set<String>> = _liveTours.asStateFlow()

    init {
        startLiveTourSimulation()
        startDynamicDataUpdates()
    }

    fun getTourDetail(tourId: String): TourDetail? {
        return when (tourId) {
            "1" -> createTourDetail1()
            "2" -> createTourDetail2()
            "3" -> createTourDetail3()
            "4" -> createTourDetail4()
            "5" -> createTourDetail5()
            else -> {
                // Check if it's a user-created tour
                val createdTour = _createdTours.value.find { it.id == tourId }
                if (createdTour != null) {
                    createDefaultTourDetail(tourId)
                } else {
                    null
                }
            }
        }
    }

    suspend fun joinTour(tourId: String): Boolean {
        // Simulate network delay
        delay(500)

        // Check if tour exists
        val tourExists = _availableTours.value.any { it.id == tourId } ||
                _createdTours.value.any { it.id == tourId }

        if (!tourExists) return false

        val currentJoined = _joinedTourIds.value.toMutableSet()
        val wasAlreadyJoined = tourId in currentJoined

        if (!wasAlreadyJoined) {
            currentJoined.add(tourId)
            _joinedTourIds.value = currentJoined

            // Update spots left in available tours
            updateTourSpots(tourId, -1)

            // Simulate analytics tracking
            println("Analytics: User joined tour $tourId")
        }

        return true
    }

    fun getJoinedTours(): List<MyTour> {
        val joinedIds = _joinedTourIds.value
        val currentTime = System.currentTimeMillis()

        return _availableTours.value
            .filter { it.id in joinedIds }
            .map { tour ->
                val status = when {
                    tour.id in _liveTours.value -> TourStatus.LIVE
                    tour.startTime > currentTime -> TourStatus.UPCOMING
                    else -> TourStatus.COMPLETED
                }

                MyTour(
                    id = tour.id,
                    title = tour.title,
                    coverImageUrl = tour.coverImageUrl,
                    startTime = tour.startTime,
                    status = status,
                    participantsCount = Random.nextInt(3, 15),
                    rating = if (status == TourStatus.COMPLETED)
                        (4.0f + Random.nextFloat()).coerceAtMost(5.0f) else null
                )
            }
    }

    suspend fun createTour(
        title: String,
        description: String,
        stops: List<Any>, // TourStop from creation
        startDateTime: Long?,
        price: Double
    ): String {
        // Simulate network delay
        delay(1000)

        val newTourId = "created_${System.currentTimeMillis()}"

        // Add to created tours
        val newCreatedTour = MyTour(
            id = newTourId,
            title = title,
            coverImageUrl = null,
            startTime = startDateTime ?: (System.currentTimeMillis() + 86400000),
            status = TourStatus.UPCOMING,
            participantsCount = 0,
            rating = null
        )

        val updatedCreatedTours = _createdTours.value + newCreatedTour
        _createdTours.value = updatedCreatedTours

        // Also add to available tours for others to discover
        val newTourPreview = TourPreview(
            id = newTourId,
            title = title,
            description = description,
            coverImageUrl = null,
            rating = 0f,
            price = price,
            isFree = price == 0.0,
            isLiveSoon = false,
            startTime = startDateTime ?: (System.currentTimeMillis() + 86400000),
            duration = stops.size * 15, // Estimate 15 min per stop
            distance = stops.size * 0.3f // Estimate 300m between stops
        )

        val updatedAvailableTours = _availableTours.value + newTourPreview
        _availableTours.value = updatedAvailableTours

        return newTourId
    }

    fun signIn(userId: String) {
        _isAuthenticated.value = true
        _currentUserId.value = userId

        // Simulate loading some user data
        scope.launch {
            delay(500)
            // Could load user's previous tours, favorites, etc.
        }
    }

    fun signOut() {
        _isAuthenticated.value = false
        _currentUserId.value = null
        _joinedTourIds.value = emptySet()
    }

    suspend fun cancelTour(tourId: String): Boolean {
        delay(300) // Simulate API call

        val updatedCreatedTours = _createdTours.value.filterNot { it.id == tourId }
        _createdTours.value = updatedCreatedTours

        val updatedAvailableTours = _availableTours.value.filterNot { it.id == tourId }
        _availableTours.value = updatedAvailableTours

        return true
    }

    private fun updateTourSpots(tourId: String, delta: Int) {
        val currentTours = _availableTours.value.toMutableList()
        val tourIndex = currentTours.indexOfFirst { it.id == tourId }
        if (tourIndex != -1) {
            // Since TourPreview doesn't have spots, we'll simulate this differently
            // In a real app, this would update the actual spots count
            _availableTours.value = currentTours
        }
    }

    private fun startLiveTourSimulation() {
        scope.launch {
            while (true) {
                delay(30000) // Check every 30 seconds

                val currentTime = System.currentTimeMillis()
                val upcomingTours = _availableTours.value.filter { tour ->
                    val timeDiff = tour.startTime - currentTime
                    timeDiff in 0..1800000 // Tours starting within 30 minutes
                }

                val currentlyLive = _liveTours.value.toMutableSet()

                // Add new live tours
                upcomingTours.forEach { tour ->
                    if (tour.id !in currentlyLive) {
                        currentlyLive.add(tour.id)
                        // Update tour to show as "live soon"
                        updateTourLiveStatus(tour.id, true)
                    }
                }

                // Remove tours that are no longer live (after 3 hours)
                val toursToRemove = currentlyLive.filter { liveId ->
                    val tour = _availableTours.value.find { it.id == liveId }
                    tour?.let {
                        currentTime - it.startTime > 10800000 // 3 hours
                    } ?: true
                }

                toursToRemove.forEach { tourId ->
                    currentlyLive.remove(tourId)
                    updateTourLiveStatus(tourId, false)
                }

                _liveTours.value = currentlyLive
            }
        }
    }

    private fun updateTourLiveStatus(tourId: String, isLiveSoon: Boolean) {
        val currentTours = _availableTours.value.map { tour ->
            if (tour.id == tourId) {
                tour.copy(isLiveSoon = isLiveSoon)
            } else tour
        }
        _availableTours.value = currentTours
    }

    private fun startDynamicDataUpdates() {
        scope.launch {
            while (true) {
                delay(60000) // Update every minute

                // Simulate tour ratings and participant count changes
                val updatedTours = _availableTours.value.map { tour ->
                    // Slight rating fluctuations for realism
                    val newRating = (tour.rating + (Random.nextFloat() - 0.5f) * 0.1f)
                        .coerceIn(3.5f, 5.0f)

                    tour.copy(rating = (newRating * 10).toInt() / 10.0f)
                }

                _availableTours.value = updatedTours
            }
        }
    }

    // Mock data generation methods remain the same...
    private fun generateInitialTours(): List<TourPreview> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            TourPreview(
                id = "1",
                title = "Hidden Gems of Paris",
                description = "Discover secret spots and local favorites in the City of Light. Walk through charming neighborhoods and hidden courtyards.",
                coverImageUrl = null,
                rating = 4.8f,
                price = 25.0,
                isFree = false,
                isLiveSoon = false,
                startTime = currentTime + 3600000, // 1 hour from now
                duration = 120,
                distance = 3.2f
            ),
            TourPreview(
                id = "2",
                title = "Street Art Walking Tour",
                description = "Explore the vibrant street art scene and learn about the artists behind the masterpieces.",
                coverImageUrl = null,
                rating = 4.6f,
                price = 0.0,
                isFree = true,
                isLiveSoon = true,
                startTime = currentTime + 1800000, // 30 minutes from now
                duration = 90,
                distance = 2.1f
            ),
            TourPreview(
                id = "3",
                title = "Historic Downtown Walk",
                description = "Journey through centuries of history in the heart of the old city.",
                coverImageUrl = null,
                rating = 4.5f,
                price = 18.0,
                isFree = false,
                isLiveSoon = false,
                startTime = currentTime + 7200000, // 2 hours from now
                duration = 75,
                distance = 1.8f
            ),
            TourPreview(
                id = "4",
                title = "Food & Culture Experience",
                description = "Taste authentic local cuisine while learning about cultural traditions.",
                coverImageUrl = null,
                rating = 4.9f,
                price = 35.0,
                isFree = false,
                isLiveSoon = false,
                startTime = currentTime + 86400000, // Tomorrow
                duration = 150,
                distance = 2.7f
            ),
            TourPreview(
                id = "5",
                title = "Architecture Highlights",
                description = "Marvel at stunning architectural styles from Gothic to Modern.",
                coverImageUrl = null,
                rating = 4.7f,
                price = 22.0,
                isFree = false,
                isLiveSoon = false,
                startTime = currentTime + 172800000, // Day after tomorrow
                duration = 105,
                distance = 4.1f
            )
        )
    }

    private fun generateInitialCreatedTours(): List<MyTour> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            MyTour(
                id = "created_demo_1",
                title = "My Secret Garden Tour",
                coverImageUrl = null,
                startTime = currentTime + 7200000, // 2 hours from now
                status = TourStatus.UPCOMING,
                participantsCount = 5,
                rating = null
            ),
            MyTour(
                id = "created_demo_2",
                title = "Local Artisan Workshop",
                coverImageUrl = null,
                startTime = currentTime - 172800000, // 2 days ago
                status = TourStatus.COMPLETED,
                participantsCount = 8,
                rating = 4.8f
            )
        )
    }

    // Rest of the detailed tour creation methods remain the same as before...
    private fun createTourDetail1() = TourDetail(
        id = "1",
        title = "Hidden Gems of Paris",
        description = "Discover the secret spots of Paris that most tourists never see. This walking tour takes you through charming neighborhoods, hidden courtyards, and local favorites. Learn about the history, culture, and stories that make these places special.",
        coverImageUrl = null,
        theme = "CULTURAL",
        rating = 4.8f,
        reviewsCount = 127,
        duration = 120,
        distance = 3.2f,
        price = 25.0,
        isFree = false,
        startTime = System.currentTimeMillis() + 3600000,
        isLive = false,
        isJoined = _joinedTourIds.value.contains("1"),
        spotsLeft = 6,
        guide = TourGuide(
            id = "guide1",
            name = "Marie Dubois",
            avatarUrl = null,
            rating = 4.9f,
            toursCount = 45
        ),
        stops = listOf(
            TourStopDetail(
                id = "1_1",
                name = "Secret Garden of Palais Royal",
                description = "A hidden oasis in the heart of Paris, perfect for quiet contemplation",
                latitude = 48.8634,
                longitude = 2.3375,
                order = 1
            ),
            TourStopDetail(
                id = "1_2",
                name = "Passage des Panoramas",
                description = "Historic covered passage with vintage shops and authentic Parisian atmosphere",
                latitude = 48.8714,
                longitude = 2.3417,
                order = 2
            ),
            TourStopDetail(
                id = "1_3",
                name = "Square Suzanne Buisson",
                description = "Romantic hidden square in Montmartre with stunning city views",
                latitude = 48.8867,
                longitude = 2.3339,
                order = 3
            )
        )
    )

    private fun createTourDetail2() = TourDetail(
        id = "2",
        title = "Street Art Walking Tour",
        description = "Explore the vibrant street art scene and learn about the artists behind the masterpieces. Discover how urban art has transformed neighborhoods and become a voice for social change.",
        coverImageUrl = null,
        theme = "ART",
        rating = 4.6f,
        reviewsCount = 89,
        duration = 90,
        distance = 2.1f,
        price = 0.0,
        isFree = true,
        startTime = System.currentTimeMillis() + 1800000,
        isLive = true,
        isJoined = _joinedTourIds.value.contains("2"),
        spotsLeft = null,
        guide = TourGuide(
            id = "guide2",
            name = "Carlos Rodriguez",
            avatarUrl = null,
            rating = 4.7f,
            toursCount = 23
        ),
        stops = listOf(
            TourStopDetail(
                id = "2_1",
                name = "The Colorful Alley",
                description = "Famous street art alley featuring works by renowned local artists",
                latitude = 48.8566,
                longitude = 2.3522,
                order = 1
            ),
            TourStopDetail(
                id = "2_2",
                name = "Urban Gallery Wall",
                description = "Ever-changing gallery wall where new artists showcase their work",
                latitude = 48.8576,
                longitude = 2.3532,
                order = 2
            )
        )
    )

    private fun createTourDetail3() = TourDetail(
        id = "3",
        title = "Historic Downtown Walk",
        description = "Journey through centuries of history in the heart of the old city. Perfect for history enthusiasts and curious travelers alike.",
        coverImageUrl = null,
        theme = "HISTORICAL",
        rating = 4.5f,
        reviewsCount = 156,
        duration = 75,
        distance = 1.8f,
        price = 18.0,
        isFree = false,
        startTime = System.currentTimeMillis() + 7200000,
        isLive = false,
        isJoined = _joinedTourIds.value.contains("3"),
        spotsLeft = 12,
        guide = TourGuide(
            id = "guide3",
            name = "Professor Williams",
            avatarUrl = null,
            rating = 4.8f,
            toursCount = 67
        ),
        stops = listOf(
            TourStopDetail(
                id = "3_1",
                name = "City Hall",
                description = "Historic seat of government with beautiful architecture",
                latitude = 48.8566,
                longitude = 2.3522,
                order = 1
            ),
            TourStopDetail(
                id = "3_2",
                name = "Old Cathedral",
                description = "Medieval cathedral with stunning stained glass windows",
                latitude = 48.8576,
                longitude = 2.3532,
                order = 2
            )
        )
    )

    private fun createTourDetail4() = TourDetail(
        id = "4",
        title = "Food & Culture Experience",
        description = "Taste authentic local cuisine while learning about cultural traditions and cooking techniques. A feast for all your senses!",
        coverImageUrl = null,
        theme = "FOOD",
        rating = 4.9f,
        reviewsCount = 203,
        duration = 150,
        distance = 2.7f,
        price = 35.0,
        isFree = false,
        startTime = System.currentTimeMillis() + 86400000,
        isLive = false,
        isJoined = _joinedTourIds.value.contains("4"),
        spotsLeft = 4,
        guide = TourGuide(
            id = "guide4",
            name = "Chef Isabella",
            avatarUrl = null,
            rating = 4.9f,
            toursCount = 89
        ),
        stops = listOf(
            TourStopDetail(
                id = "4_1",
                name = "Traditional Market",
                description = "Local market with fresh ingredients and artisanal products",
                latitude = 48.8566,
                longitude = 2.3522,
                order = 1
            ),
            TourStopDetail(
                id = "4_2",
                name = "Historic Bakery",
                description = "Family bakery operating since 1890 with original recipes",
                latitude = 48.8576,
                longitude = 2.3532,
                order = 2
            ),
            TourStopDetail(
                id = "4_3",
                name = "Wine Cellar",
                description = "Underground wine cellar with tastings and expert guidance",
                latitude = 48.8586,
                longitude = 2.3542,
                order = 3
            )
        )
    )

    private fun createTourDetail5() = TourDetail(
        id = "5",
        title = "Architecture Highlights",
        description = "Marvel at stunning architectural styles from Gothic to Modern, guided by a local expert with deep knowledge of building history and design.",
        coverImageUrl = null,
        theme = "ARCHITECTURE",
        rating = 4.7f,
        reviewsCount = 134,
        duration = 105,
        distance = 4.1f,
        price = 22.0,
        isFree = false,
        startTime = System.currentTimeMillis() + 172800000,
        isLive = false,
        isJoined = _joinedTourIds.value.contains("5"),
        spotsLeft = 10,
        guide = TourGuide(
            id = "guide5",
            name = "Architect Sarah Chen",
            avatarUrl = null,
            rating = 4.8f,
            toursCount = 52
        ),
        stops = listOf(
            TourStopDetail(
                id = "5_1",
                name = "Gothic Cathedral",
                description = "Prime example of Gothic architecture with flying buttresses",
                latitude = 48.8566,
                longitude = 2.3522,
                order = 1
            ),
            TourStopDetail(
                id = "5_2",
                name = "Art Deco Building",
                description = "Beautiful 1920s Art Deco facade with geometric patterns",
                latitude = 48.8576,
                longitude = 2.3532,
                order = 2
            ),
            TourStopDetail(
                id = "5_3",
                name = "Modern Glass Tower",
                description = "Contemporary architecture showcasing sustainable design",
                latitude = 48.8586,
                longitude = 2.3542,
                order = 3
            )
        )
    )

    private fun createDefaultTourDetail(id: String) = TourDetail(
        id = id,
        title = "Amazing City Tour",
        description = "Discover the beauty and history of our wonderful city through this carefully crafted walking experience.",
        coverImageUrl = null,
        theme = "GENERAL",
        rating = 4.5f,
        reviewsCount = 42,
        duration = 90,
        distance = 2.5f,
        price = 20.0,
        isFree = false,
        startTime = System.currentTimeMillis() + 3600000,
        isLive = false,
        isJoined = _joinedTourIds.value.contains(id),
        spotsLeft = 8,
        guide = TourGuide(
            id = "guide_default",
            name = "Local Guide",
            avatarUrl = null,
            rating = 4.6f,
            toursCount = 15
        ),
        stops = listOf(
            TourStopDetail(
                id = "${id}_1",
                name = "Starting Point",
                description = "Begin your journey at this historic landmark",
                latitude = 48.8566,
                longitude = 2.3522,
                order = 1
            ),
            TourStopDetail(
                id = "${id}_2",
                name = "Main Square",
                description = "The heart of the city with beautiful architecture",
                latitude = 48.8576,
                longitude = 2.3532,
                order = 2
            )
        )
    )
}