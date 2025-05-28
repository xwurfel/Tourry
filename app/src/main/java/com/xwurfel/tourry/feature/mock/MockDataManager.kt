package com.xwurfel.tourry.feature.mock

import com.xwurfel.tourry.feature.profile.domain.model.User
import com.xwurfel.tourry.feature.profile.domain.model.UserStats
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
 * Enhanced MockDataManager with comprehensive profile support and realistic data generation
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

    // Enhanced User state
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()

    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    // Settings state

    // Live tour simulation
    private val _liveTours = MutableStateFlow<Set<String>>(emptySet())
    val liveTours: StateFlow<Set<String>> = _liveTours.asStateFlow()

    // Enhanced user data storage
    private val userProfiles = mutableMapOf<String, User>()
    private val userStatsMap = mutableMapOf<String, UserStats>()
    private val userPreferences = mutableMapOf<String, UserPreferences>()

    init {
        startLiveTourSimulation()
        startDynamicDataUpdates()
        seedMockUserData()
    }

    // Enhanced Profile Management
    suspend fun loadUserProfile(userId: String): User? {
        delay(800) // Simulate network delay
        return userProfiles[userId]?.let { profile ->
            // Always return profile with latest stats
            val stats = userStatsMap[userId]
            profile.copy(stats = stats)
        }
    }

    suspend fun loadUserStats(userId: String): UserStats? {
        delay(500)
        return userStatsMap[userId]
    }

    // Enhanced Authentication
    fun signIn(userId: String, userType: String = "email") {
        _isAuthenticated.value = true
        _currentUserId.value = userId

        scope.launch {
            // Load or create user profile
            val stats = loadUserStats(userId) ?: createDefaultStats(userId)

            _userStats.value = stats

            // Load user's created and joined tours
            loadUserTours(userId)
        }
    }

    fun signOut() {
        _isAuthenticated.value = false
        _currentUserId.value = null
        _currentUserProfile.value = null
        _userStats.value = null
        _joinedTourIds.value = emptySet()
    }

    // Enhanced Analytics and Statistics
    fun trackProfileView(viewedUserId: String) {
        scope.launch {
            println("Analytics: Profile viewed - User: $viewedUserId")
            // Could increment profile view count here
        }
    }

    fun trackProfileEdit(userId: String, changedFields: List<String>) {
        scope.launch {
            println("Analytics: Profile edited - User: $userId, Fields: $changedFields")
            // Update user engagement metrics
            val currentStats = userStatsMap[userId]
            if (currentStats != null) {
                // Could track profile completeness, last edit time, etc.
            }
        }
    }

    // Existing tour methods (keeping same functionality)
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
        val tourExists =
            _availableTours.value.any { it.id == tourId } || _createdTours.value.any { it.id == tourId }

        if (!tourExists) return false

        val currentJoined = _joinedTourIds.value.toMutableSet()
        val wasAlreadyJoined = tourId in currentJoined

        if (!wasAlreadyJoined) {
            currentJoined.add(tourId)
            _joinedTourIds.value = currentJoined
            updateTourSpots(tourId, -1)
        }
        return true
    }

    suspend fun createTour(
        title: String, description: String, stops: List<Any>, startDateTime: Long?, price: Double
    ): String {
        delay(1000)
        val currentUserId = _currentUserId.value ?: "anonymous"
        val newTourId = "created_${currentUserId}_${System.currentTimeMillis()}"

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
        return newTourId
    }

    fun getJoinedTours(): List<MyTour> {
        val joinedIds = _joinedTourIds.value
        val currentTime = System.currentTimeMillis()

        return _availableTours.value.filter { it.id in joinedIds }.map { tour ->
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
                rating = if (status == TourStatus.COMPLETED) (4.0f + Random.nextFloat()).coerceAtMost(
                    5.0f
                ) else null
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
    private fun createDefaultProfile(userId: String, userType: String): User {
        val profile = when {
            userId.startsWith("google_") -> User(
                id = userId,
                name = "John Doe",
                email = "john.doe@gmail.com",
                avatarUrl = generateRandomAvatarUrl(),
            )

            userId.startsWith("email_") -> User(
                id = userId,
                name = "Jane Smith",
                email = "jane.smith@example.com",
                avatarUrl = generateRandomAvatarUrl(),
            )

            userId.startsWith("guest_") -> User(
                id = userId,
                name = "Guest User",
                email = "",
                avatarUrl = null,
            )

            else -> User(
                id = userId,
                name = generateRandomName(),
                email = "${userId.lowercase()}@example.com",
                avatarUrl = generateRandomAvatarUrl(),
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
            )
        } else {
            val toursCreated = Random.nextInt(0, 12)
            val toursJoined = Random.nextInt(5, 25)

            UserStats(
                toursCreated = toursCreated,
                toursJoined = toursJoined,
            )
        }

        userStatsMap[userId] = stats
        return stats
    }

    private fun loadUserTours(userId: String) {
        // Load joined tours based on some pattern or stored data
        val mockJoinedTours = when {
            userId.startsWith("google_") -> setOf("1", "3", "5")
            userId.startsWith("email_") -> setOf("2", "4")
            else -> emptySet()
        }
        _joinedTourIds.value = mockJoinedTours
    }

    private fun seedMockUserData() {
        scope.launch {
            // Create a diverse set of mock users for demo
            val mockUsers = listOf(
                "demo_creator_1" to "Professional Guide",
                "demo_creator_2" to "Local Historian",
                "demo_creator_3" to "Food Blogger",
                "demo_user_1" to "Travel Enthusiast",
                "demo_user_2" to "Adventure Seeker"
            )

            mockUsers.forEach { (userId, type) ->
                createDefaultProfile(userId, type)
                createDefaultStats(userId)
                userPreferences[userId] = UserPreferences()
            }
        }
    }

    // Helper methods for realistic data generation
    private fun generateRandomAvatarUrl(): String? {
        return if (Random.nextBoolean()) {
            "https://images.unsplash.com/photo-${
                Random.nextInt(
                    1500000000, 1600000000
                )
            }-${
                Random.nextInt(
                    100000, 999999
                )
            }?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&h=150&q=80"
        } else {
            null
        }
    }

    private fun generateRandomName(): String {
        val firstNames = listOf(
            "Alex", "Jordan", "Casey", "Morgan", "Riley", "Avery", "Quinn", "Sage", "River", "Rowan"
        )
        val lastNames = listOf(
            "Thompson",
            "Garcia",
            "Martinez",
            "Rodriguez",
            "Wilson",
            "Anderson",
            "Taylor",
            "Brown",
            "Davis",
            "Miller"
        )
        return "${firstNames.random()} ${lastNames.random()}"
    }

    private fun generateRandomBio(): String {
        val bios = listOf(
            "Passionate about sharing the stories and secrets of my hometown with curious travelers.",
            "Local expert with 10+ years of experience guiding visitors through hidden gems and must-see spots.",
            "Food lover and cultural enthusiast who believes the best way to know a place is through its flavors.",
            "History buff and storyteller who brings the past to life through engaging walking tours.",
            "Adventure guide specializing in off-the-beaten-path experiences and sustainable tourism."
        )
        return bios.random()
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    // Existing simulation methods remain the same
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
                delay(30000) // Check every 30 seconds
                val currentTime = System.currentTimeMillis()
                val upcomingTours = _availableTours.value.filter { tour ->
                    val timeDiff = tour.startTime - currentTime
                    timeDiff in 0..1800000 // Within 30 minutes
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
                        currentTime - it.startTime > 10800000 // 3 hours after start
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
                val updatedTours = _availableTours.value.map { tour ->
                    val newRating =
                        (tour.rating + (Random.nextFloat() - 0.5f) * 0.1f).coerceIn(3.5f, 5.0f)
                    tour.copy(rating = (newRating * 10).toInt() / 10.0f)
                }
                _availableTours.value = updatedTours
            }
        }
    }

    // Keep existing tour detail creation methods unchanged...
    private fun generateInitialTours(): List<TourPreview> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            TourPreview(
                id = "1",
                title = "Hidden Gems of Paris",
                description = "Discover secret spots and local favorites in the City of Light. Walk through charming neighborhoods and hidden courtyards.",
                coverImageUrl = "https://images.unsplash.com/photo-1502602898536-47ad22581b52?auto=format&fit=crop&w=400&q=80",
                rating = 4.8f,
                price = 25.0,
                isFree = false,
                isLiveSoon = false,
                startTime = currentTime + 3600000,
                duration = 120,
                distance = 3.2f
            ), TourPreview(
                id = "2",
                title = "Street Art Walking Tour",
                description = "Explore the vibrant street art scene and learn about the artists behind the masterpieces.",
                coverImageUrl = "https://images.unsplash.com/photo-1541961017774-22349e4a1262?auto=format&fit=crop&w=400&q=80",
                rating = 4.6f,
                price = 0.0,
                isFree = true,
                isLiveSoon = true,
                startTime = currentTime + 1800000,
                duration = 90,
                distance = 2.1f
            ), TourPreview(
                id = "3",
                title = "Historic Downtown Walk",
                description = "Journey through centuries of history in the heart of the old city.",
                coverImageUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=400&q=80",
                rating = 4.5f,
                price = 18.0,
                isFree = false,
                isLiveSoon = false,
                startTime = currentTime + 7200000,
                duration = 75,
                distance = 1.8f
            ), TourPreview(
                id = "4",
                title = "Food & Culture Experience",
                description = "Taste authentic local cuisine while learning about cultural traditions.",
                coverImageUrl = "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=400&q=80",
                rating = 4.9f,
                price = 35.0,
                isFree = false,
                isLiveSoon = false,
                startTime = currentTime + 86400000,
                duration = 150,
                distance = 2.7f
            ), TourPreview(
                id = "5",
                title = "Architecture Highlights",
                description = "Marvel at stunning architectural styles from Gothic to Modern.",
                coverImageUrl = "https://images.unsplash.com/photo-1449824913935-59a10b8d2000?auto=format&fit=crop&w=400&q=80",
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
                coverImageUrl = "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?auto=format&fit=crop&w=400&q=80",
                startTime = currentTime + 7200000,
                status = TourStatus.UPCOMING,
                participantsCount = 5,
                rating = null
            ), MyTour(
                id = "created_demo_2",
                title = "Local Artisan Workshop",
                coverImageUrl = "https://images.unsplash.com/photo-1452860606245-08befc0ff44b?auto=format&fit=crop&w=400&q=80",
                startTime = currentTime - 172800000,
                status = TourStatus.COMPLETED,
                participantsCount = 8,
                rating = 4.8f
            )
        )
    }

    // Keep existing tour detail methods...
    private fun createTourDetail1() = TourDetail(
        id = "1",
        title = "Hidden Gems of Paris",
        description = "Discover the secret spots of Paris that most tourists never see. This walking tour takes you through charming neighborhoods, hidden courtyards, and local favorites. Learn about the history, culture, and stories that make these places special.",
        coverImageUrl = "https://images.unsplash.com/photo-1502602898536-47ad22581b52?auto=format&fit=crop&w=800&q=80",
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
            avatarUrl = "https://images.unsplash.com/photo-1494790108755-2616b612b97c?auto=format&fit=crop&w=150&h=150&q=80",
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
            ), TourStopDetail(
                id = "1_2",
                name = "Passage des Panoramas",
                description = "Historic covered passage with vintage shops and authentic Parisian atmosphere",
                latitude = 48.8714,
                longitude = 2.3417,
                order = 2
            ), TourStopDetail(
                id = "1_3",
                name = "Square Suzanne Buisson",
                description = "Romantic hidden square in Montmartre with stunning city views",
                latitude = 48.8867,
                longitude = 2.3339,
                order = 3
            )
        )
    )

    // Continue with other tour detail methods... (keeping them as they were)
    private fun createTourDetail2() = TourDetail(
        id = "2",
        title = "Street Art Walking Tour",
        description = "Explore the vibrant street art scene and learn about the artists behind the masterpieces. Discover how urban art has transformed neighborhoods and become a voice for social change.",
        coverImageUrl = "https://images.unsplash.com/photo-1541961017774-22349e4a1262?auto=format&fit=crop&w=800&q=80",
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
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&h=150&q=80",
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
            ), TourStopDetail(
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
        coverImageUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=800&q=80",
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
            avatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=150&h=150&q=80",
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
            ), TourStopDetail(
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
        coverImageUrl = "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=800&q=80",
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
            avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=150&h=150&q=80",
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
            ), TourStopDetail(
                id = "4_2",
                name = "Historic Bakery",
                description = "Family bakery operating since 1890 with original recipes",
                latitude = 48.8576,
                longitude = 2.3532,
                order = 2
            ), TourStopDetail(
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
        coverImageUrl = "https://images.unsplash.com/photo-1449824913935-59a10b8d2000?auto=format&fit=crop&w=800&q=80",
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
            avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=150&h=150&q=80",
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
            ), TourStopDetail(
                id = "5_2",
                name = "Art Deco Building",
                description = "Beautiful 1920s Art Deco facade with geometric patterns",
                latitude = 48.8576,
                longitude = 2.3532,
                order = 2
            ), TourStopDetail(
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
        coverImageUrl = "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?auto=format&fit=crop&w=800&q=80",
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
            avatarUrl = "https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=150&h=150&q=80",
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
            ), TourStopDetail(
                id = "${id}_2",
                name = "Main Square",
                description = "The heart of the city with beautiful architecture",
                latitude = 48.8576,
                longitude = 2.3532,
                order = 2
            )
        )
    )

    suspend fun updateTour(
        tourId: String,
        title: String,
        description: String,
        stops: List<Any>,
        startDateTime: Long?,
        price: Double
    ): Boolean {
        delay(800)

        try {
            val currentTours = _availableTours.value.toMutableList()
            val tourIndex = currentTours.indexOfFirst { it.id == tourId }
            if (tourIndex != -1) {
                val updatedTour = currentTours[tourIndex].copy(
                    title = title,
                    description = description,
                    price = price,
                    isFree = price == 0.0,
                    startTime = startDateTime ?: currentTours[tourIndex].startTime,
                    duration = stops.size * 15,
                    distance = stops.size * 0.3f
                )
                currentTours[tourIndex] = updatedTour
                _availableTours.value = currentTours
            }

            // Update in created tours
            val currentCreatedTours = _createdTours.value.toMutableList()
            val createdTourIndex = currentCreatedTours.indexOfFirst { it.id == tourId }
            if (createdTourIndex != -1) {
                val updatedCreatedTour = currentCreatedTours[createdTourIndex].copy(
                    title = title,
                    startTime = startDateTime ?: currentCreatedTours[createdTourIndex].startTime
                )
                currentCreatedTours[createdTourIndex] = updatedCreatedTour
                _createdTours.value = currentCreatedTours
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }
}

// Data classes for enhanced profile support
data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val locationSharingEnabled: Boolean = true,
    val language: String = "en",
    val currency: String = "USD",
    val darkMode: Boolean = false
)