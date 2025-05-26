package com.xwurfel.tourry.feature.mock

import com.xwurfel.tourry.ui.explore.TourPreview
import com.xwurfel.tourry.ui.profile.UserProfile
import com.xwurfel.tourry.ui.profile.UserStats
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
 * Enhanced MockDataManager with full profile support
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

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    // Settings state
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _locationSharingEnabled = MutableStateFlow(true)
    val locationSharingEnabled: StateFlow<Boolean> = _locationSharingEnabled.asStateFlow()

    // Live tour simulation
    private val _liveTours = MutableStateFlow<Set<String>>(emptySet())
    val liveTours: StateFlow<Set<String>> = _liveTours.asStateFlow()

    // User profiles storage (simulates backend)
    private val userProfiles = mutableMapOf<String, UserProfile>()
    private val userStatsMap = mutableMapOf<String, UserStats>()

    init {
        startLiveTourSimulation()
        startDynamicDataUpdates()
        seedMockUserData()
    }

    // Profile Management
    suspend fun loadUserProfile(userId: String): UserProfile? {
        delay(800) // Simulate network delay
        return userProfiles[userId]
    }

    suspend fun loadUserStats(userId: String): UserStats? {
        delay(500)
        return userStatsMap[userId]
    }

    suspend fun updateUserProfile(userId: String, profile: UserProfile): Boolean {
        delay(1000) // Simulate network delay

        return try {
            userProfiles[userId] = profile
            _currentUserProfile.value = profile
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadProfileAvatar(userId: String, imageUri: String): String? {
        delay(2000) // Simulate upload time

        // Simulate successful upload and return a mock URL
        val mockAvatarUrl =
            "https://mock-cdn.example.com/avatars/${userId}_${System.currentTimeMillis()}.jpg"

        // Update the current profile with new avatar
        _currentUserProfile.value?.let { currentProfile ->
            val updatedProfile = currentProfile.copy(avatarUrl = mockAvatarUrl)
            userProfiles[userId] = updatedProfile
            _currentUserProfile.value = updatedProfile
        }

        return mockAvatarUrl
    }

    suspend fun deleteAccount(userId: String): Boolean {
        delay(1500)

        return try {
            // Remove user data
            userProfiles.remove(userId)
            userStatsMap.remove(userId)

            // Sign out user
            signOut()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Settings Management
    suspend fun updateNotificationSettings(enabled: Boolean): Boolean {
        delay(300)
        _notificationsEnabled.value = enabled
        return true
    }

    suspend fun updateLocationSharingSettings(enabled: Boolean): Boolean {
        delay(300)
        _locationSharingEnabled.value = enabled
        return true
    }

    // Authentication
    fun signIn(userId: String, userType: String = "email") {
        _isAuthenticated.value = true
        _currentUserId.value = userId

        scope.launch {
            // Load or create user profile
            val profile = loadUserProfile(userId) ?: createDefaultProfile(userId, userType)
            val stats = loadUserStats(userId) ?: createDefaultStats(userId)

            _currentUserProfile.value = profile
            _userStats.value = stats
        }
    }

    fun signOut() {
        _isAuthenticated.value = false
        _currentUserId.value = null
        _currentUserProfile.value = null
        _userStats.value = null
        _joinedTourIds.value = emptySet()
    }

    // Analytics and Statistics
    fun trackProfileView(viewedUserId: String) {
        scope.launch {
            // Simulate analytics tracking
            println("Analytics: Profile viewed - User: $viewedUserId")
        }
    }

    fun trackProfileEdit(userId: String, changedFields: List<String>) {
        scope.launch {
            println("Analytics: Profile edited - User: $userId, Fields: $changedFields")
        }
    }

    // Existing methods remain the same...
    fun getTourDetail(tourId: String): TourDetail? {
        return when (tourId) {
            "1" -> createTourDetail1()
            "2" -> createTourDetail2()
            "3" -> createTourDetail3()
            "4" -> createTourDetail4()
            "5" -> createTourDetail5()
            else -> {
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
        delay(500)
        val tourExists = _availableTours.value.any { it.id == tourId } ||
                _createdTours.value.any { it.id == tourId }

        if (!tourExists) return false

        val currentJoined = _joinedTourIds.value.toMutableSet()
        val wasAlreadyJoined = tourId in currentJoined

        if (!wasAlreadyJoined) {
            currentJoined.add(tourId)
            _joinedTourIds.value = currentJoined
            updateTourSpots(tourId, -1)

            // Update user stats
            _currentUserId.value?.let { userId ->
                updateUserStatsAfterJoin(userId)
            }
        }
        return true
    }

    suspend fun createTour(
        title: String,
        description: String,
        stops: List<Any>,
        startDateTime: Long?,
        price: Double
    ): String {
        delay(1000)
        val newTourId = "created_${System.currentTimeMillis()}"

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
            duration = stops.size * 15,
            distance = stops.size * 0.3f
        )

        val updatedAvailableTours = _availableTours.value + newTourPreview
        _availableTours.value = updatedAvailableTours

        // Update user stats
        _currentUserId.value?.let { userId ->
            updateUserStatsAfterCreate(userId)
        }

        return newTourId
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

    suspend fun cancelTour(tourId: String): Boolean {
        delay(300)
        val updatedCreatedTours = _createdTours.value.filterNot { it.id == tourId }
        _createdTours.value = updatedCreatedTours

        val updatedAvailableTours = _availableTours.value.filterNot { it.id == tourId }
        _availableTours.value = updatedAvailableTours

        return true
    }

    // Private helper methods
    private fun createDefaultProfile(userId: String, userType: String): UserProfile {
        val profile = when {
            userId.startsWith("google_") -> UserProfile(
                id = userId,
                name = "John Doe",
                email = "john.doe@gmail.com",
                avatarUrl = null,
                bio = "Travel enthusiast and local explorer",
                rating = 4.8f,
                reviewsCount = 23
            )

            userId.startsWith("email_") -> UserProfile(
                id = userId,
                name = "Jane Smith",
                email = "jane.smith@example.com",
                avatarUrl = null,
                bio = "I love discovering hidden gems in my city",
                rating = 4.6f,
                reviewsCount = 15
            )

            userId.startsWith("guest_") -> UserProfile(
                id = userId,
                name = "Guest User",
                email = "",
                avatarUrl = null,
                bio = "",
                rating = 0f,
                reviewsCount = 0
            )

            else -> UserProfile(
                id = userId,
                name = "Tour Guide",
                email = "guide@example.com",
                avatarUrl = null,
                bio = "Professional tour guide with 5+ years experience",
                rating = 4.9f,
                reviewsCount = 67
            )
        }

        userProfiles[userId] = profile
        return profile
    }

    private fun createDefaultStats(userId: String): UserStats {
        val stats = if (userId.startsWith("guest_")) {
            UserStats(
                toursCreated = 0,
                toursJoined = 0,
                totalParticipants = 0
            )
        } else {
            UserStats(
                toursCreated = Random.nextInt(0, 8),
                toursJoined = Random.nextInt(5, 25),
                totalParticipants = Random.nextInt(10, 150)
            )
        }

        userStatsMap[userId] = stats
        return stats
    }

    private fun updateUserStatsAfterJoin(userId: String) {
        val currentStats = userStatsMap[userId] ?: createDefaultStats(userId)
        val updatedStats = currentStats.copy(toursJoined = currentStats.toursJoined + 1)
        userStatsMap[userId] = updatedStats
        _userStats.value = updatedStats
    }

    private fun updateUserStatsAfterCreate(userId: String) {
        val currentStats = userStatsMap[userId] ?: createDefaultStats(userId)
        val updatedStats = currentStats.copy(toursCreated = currentStats.toursCreated + 1)
        userStatsMap[userId] = updatedStats
        _userStats.value = updatedStats
    }

    private fun seedMockUserData() {
        // Seed some mock users for demo purposes
        scope.launch {
            createDefaultProfile("demo_user_1", "email")
            createDefaultProfile("demo_user_2", "google")
            createDefaultProfile("demo_user_3", "guest")
        }
    }

    private fun updateTourSpots(tourId: String, delta: Int) {
        val currentTours = _availableTours.value.toMutableList()
        val tourIndex = currentTours.indexOfFirst { it.id == tourId }
        if (tourIndex != -1) {
            _availableTours.value = currentTours
        }
    }

    private fun startLiveTourSimulation() {
        scope.launch {
            while (true) {
                delay(30000)
                val currentTime = System.currentTimeMillis()
                val upcomingTours = _availableTours.value.filter { tour ->
                    val timeDiff = tour.startTime - currentTime
                    timeDiff in 0..1800000
                }

                val currentlyLive = _liveTours.value.toMutableSet()
                upcomingTours.forEach { tour ->
                    if (tour.id !in currentlyLive) {
                        currentlyLive.add(tour.id)
                        updateTourLiveStatus(tour.id, true)
                    }
                }

                val toursToRemove = currentlyLive.filter { liveId ->
                    val tour = _availableTours.value.find { it.id == liveId }
                    tour?.let {
                        currentTime - it.startTime > 10800000
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
                delay(60000)
                val updatedTours = _availableTours.value.map { tour ->
                    val newRating = (tour.rating + (Random.nextFloat() - 0.5f) * 0.1f)
                        .coerceIn(3.5f, 5.0f)
                    tour.copy(rating = (newRating * 10).toInt() / 10.0f)
                }
                _availableTours.value = updatedTours
            }
        }
    }

    // Mock data generation methods (keeping existing ones)
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
                startTime = currentTime + 3600000,
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
                startTime = currentTime + 1800000,
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
                startTime = currentTime + 7200000,
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
                startTime = currentTime + 86400000,
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
                startTime = currentTime + 172800000,
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
                startTime = currentTime + 7200000,
                status = TourStatus.UPCOMING,
                participantsCount = 5,
                rating = null
            ),
            MyTour(
                id = "created_demo_2",
                title = "Local Artisan Workshop",
                coverImageUrl = null,
                startTime = currentTime - 172800000,
                status = TourStatus.COMPLETED,
                participantsCount = 8,
                rating = 4.8f
            )
        )
    }

    // Existing detail creation methods remain the same...
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