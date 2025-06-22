package com.xwurfel.tourry.ui.main

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.xwurfel.tourry.feature.tours.data.model.FirestoreStopContent
import com.xwurfel.tourry.feature.tours.data.model.FirestoreTour
import com.xwurfel.tourry.feature.tours.data.model.FirestoreTourStop
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FirebaseMockDataManager @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val toursCollection = firestore.collection("tours")

    /**
     * Clears all documents from the tours collection
     */
    suspend fun clearFirebaseDatabase() {
        try {
            val batch = firestore.batch()
            val documents = toursCollection.get().await()

            documents.forEach { document ->
                batch.delete(document.reference)
            }

            batch.commit().await()
            println("✅ Firebase database cleared successfully")
        } catch (e: Exception) {
            println("❌ Error clearing database: ${e.message}")
        }
    }

    /**
     * Loads mock tour data into Firebase
     */
    suspend fun loadMockTourData() {
        try {
            val mockTours = generateMockTours()

            mockTours.forEach { tour ->
                toursCollection.add(tour).await()
            }

            println("✅ Mock tour data loaded successfully (${mockTours.size} tours)")
        } catch (e: Exception) {
            println("❌ Error loading mock data: ${e.message}")
        }
    }

    private fun generateMockTours(): List<FirestoreTour> {
        return listOf(
            // 1. Historic Old Town Tour
            FirestoreTour(
                title = "Legends of Lviv Old Town",
                description = "Discover the medieval charm and fascinating legends of Lviv's UNESCO World Heritage Old Town",
                theme = "History",
                coverImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6c/Lviv_Market_Square_RB.jpg/800px-Lviv_Market_Square_RB.jpg",
                authorId = "author_1",
                authorName = "Anna Petrenko",
                authorAvatarUrl = "https://i.pravatar.cc/150?img=1",
                price = 0.0,
                currency = "USD",
                startTime = getRandomFutureTimestamp(),
                duration = 120,
                maxParticipants = 15,
                currentParticipants = Random.nextInt(0, 8),
                rating = 4.8f,
                reviewsCount = 42,
                isActive = true,
                isLive = false,
                stops = listOf(
                    createTourStop(
                        name = "Market Square (Rynok Square)",
                        description = "The heart of Lviv's Old Town, surrounded by magnificent Renaissance buildings",
                        lat = 49.8414,
                        lng = 24.0315,
                        order = 1,
                        content = "One of the largest medieval market squares in Europe, built in the 14th century. Each building tells a unique story of merchants, craftsmen, and city life."
                    ),
                    createTourStop(
                        name = "Latin Cathedral",
                        description = "Gothic Roman Catholic cathedral with stunning architecture",
                        lat = 49.8422,
                        lng = 24.0318,
                        order = 2,
                        content = "Built in the 14th-15th centuries, this cathedral witnessed many historical events and houses priceless artifacts."
                    ),
                    createTourStop(
                        name = "City Hall Tower",
                        description = "Climb the iconic tower for panoramic views of the city",
                        lat = 49.8415,
                        lng = 24.0316,
                        order = 3,
                        content = "The 65-meter tower offers breathtaking views of Lviv's red rooftops and surrounding hills. Built in the 19th century."
                    ),
                    createTourStop(
                        name = "Armenian Quarter",
                        description = "Explore the historic Armenian merchant district",
                        lat = 49.8408,
                        lng = 24.0325,
                        order = 4,
                        content = "Home to one of the oldest Armenian communities in Europe, featuring unique architecture and cultural heritage."
                    )
                ),
                tags = listOf("history", "architecture", "UNESCO", "walking", "cultural")
            ),

            // 2. Coffee Culture Tour
            FirestoreTour(
                title = "Lviv Coffee Culture Experience",
                description = "Immerse yourself in Lviv's famous coffee culture and visit the city's most iconic cafés",
                theme = "Food & Drink",
                coverImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/Lviv_coffeehouse.jpg/800px-Lviv_coffeehouse.jpg",
                authorId = "author_2",
                authorName = "Viktor Kovalenko",
                authorAvatarUrl = "https://i.pravatar.cc/150?img=2",
                price = 25.0,
                currency = "USD",
                startTime = getRandomFutureTimestamp(),
                duration = 180,
                maxParticipants = 12,
                currentParticipants = Random.nextInt(0, 6),
                rating = 4.9f,
                reviewsCount = 38,
                isActive = true,
                isLive = false,
                stops = listOf(
                    createTourStop(
                        name = "Lviv Coffee Mining Manufacture",
                        description = "Underground coffee mine experience",
                        lat = 49.8425,
                        lng = 24.0302,
                        order = 1,
                        content = "Unique underground coffee roastery where you'll learn about coffee 'mining' and taste exceptional blends in a former cellar."
                    ),
                    createTourStop(
                        name = "Svit Kavy (Coffee World)",
                        description = "Traditional coffeehouse with historical ambiance",
                        lat = 49.8418,
                        lng = 24.0320,
                        order = 2,
                        content = "One of Lviv's oldest coffeehouses, serving traditional Viennese-style coffee in an authentic Habsburg-era setting."
                    ),
                    createTourStop(
                        name = "Coffee Manufacture Gas Lamp",
                        description = "Steampunk-themed coffee shop",
                        lat = 49.8395,
                        lng = 24.0285,
                        order = 3,
                        content = "Industrial-themed café with unique coffee brewing methods and steampunk décor. Try their signature gas lamp coffee."
                    ),
                    createTourStop(
                        name = "Dzyga Coffee Roasters",
                        description = "Modern specialty coffee roastery",
                        lat = 49.8435,
                        lng = 24.0340,
                        order = 4,
                        content = "Contemporary coffee roastery focusing on single-origin beans and innovative brewing techniques."
                    )
                ),
                tags = listOf("coffee", "food", "culture", "tasting", "indoor")
            ),

            // 3. Architecture Walking Tour
            FirestoreTour(
                title = "Architectural Gems of Lviv",
                description = "Journey through centuries of architecture from Gothic to Art Nouveau",
                theme = "Architecture",
                coverImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1c/Lviv_Opera_House.jpg/800px-Lviv_Opera_House.jpg",
                authorId = "author_3",
                authorName = "Maria Ivanova",
                authorAvatarUrl = "https://i.pravatar.cc/150?img=3",
                price = 15.0,
                currency = "USD",
                startTime = getRandomFutureTimestamp(),
                duration = 150,
                maxParticipants = 20,
                currentParticipants = Random.nextInt(0, 12),
                rating = 4.7f,
                reviewsCount = 56,
                isActive = true,
                isLive = false,
                stops = listOf(
                    createTourStop(
                        name = "Lviv Opera House",
                        description = "Neo-Renaissance opera house modeled after Vienna State Opera",
                        lat = 49.8431,
                        lng = 24.0252,
                        order = 1,
                        content = "Built in 1900, this magnificent opera house showcases Neo-Renaissance architecture and hosts world-class performances."
                    ),
                    createTourStop(
                        name = "Dominican Church",
                        description = "Baroque church with ornate interior",
                        lat = 49.8421,
                        lng = 24.0308,
                        order = 2,
                        content = "18th-century Baroque masterpiece with stunning frescoes and elaborate altar decorations."
                    ),
                    createTourStop(
                        name = "Potocki Palace",
                        description = "French classical palace architecture",
                        lat = 49.8445,
                        lng = 24.0335,
                        order = 3,
                        content = "19th-century palace built in French classical style, now housing the European Palace hotel."
                    ),
                    createTourStop(
                        name = "St. George's Cathedral",
                        description = "Baroque Ukrainian Greek Catholic cathedral",
                        lat = 49.8465,
                        lng = 24.0375,
                        order = 4,
                        content = "UNESCO World Heritage site featuring exceptional Baroque architecture and beautiful gardens."
                    )
                ),
                tags = listOf("architecture", "baroque", "renaissance", "churches", "historical")
            ),

            // 4. Jewish Heritage Tour
            FirestoreTour(
                title = "Jewish Lviv: Memory and Heritage",
                description = "Explore the rich Jewish history and heritage of pre-war Lviv",
                theme = "Cultural Heritage",
                coverImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Golden_Rose_Synagogue_Lviv.jpg/800px-Golden_Rose_Synagogue_Lviv.jpg",
                authorId = "author_4",
                authorName = "David Goldstein",
                authorAvatarUrl = "https://i.pravatar.cc/150?img=4",
                price = 20.0,
                currency = "USD",
                startTime = getRandomFutureTimestamp(),
                duration = 135,
                maxParticipants = 15,
                currentParticipants = Random.nextInt(0, 7),
                rating = 4.9f,
                reviewsCount = 29,
                isActive = true,
                isLive = false,
                stops = listOf(
                    createTourStop(
                        name = "Golden Rose Synagogue Site",
                        description = "Former site of the main synagogue of Lviv",
                        lat = 49.8405,
                        lng = 24.0298,
                        order = 1,
                        content = "Once the center of Jewish religious life in Lviv, this Renaissance synagogue was destroyed during WWII. Now a memorial site."
                    ),
                    createTourStop(
                        name = "Jewish Quarter",
                        description = "Historic center of Jewish community life",
                        lat = 49.8400,
                        lng = 24.0290,
                        order = 2,
                        content = "Explore the streets where Jewish families lived, worked, and built their community for centuries."
                    ),
                    createTourStop(
                        name = "Beit Aharon V'Israel Synagogue",
                        description = "Active Orthodox synagogue",
                        lat = 49.8398,
                        lng = 24.0295,
                        order = 3,
                        content = "The main active synagogue in Lviv today, serving the local Jewish community and visitors."
                    ),
                    createTourStop(
                        name = "Janusz Korczak Orphanage Memorial",
                        description = "Memorial to the famous educator",
                        lat = 49.8392,
                        lng = 24.0288,
                        order = 4,
                        content = "Commemorating Janusz Korczak, who worked in Lviv before becoming a famous educator and children's rights advocate."
                    )
                ),
                tags = listOf("jewish", "heritage", "memorial", "cultural", "history")
            ),

            // 5. Underground Lviv Tour
            FirestoreTour(
                title = "Mysteries of Underground Lviv",
                description = "Discover hidden tunnels, cellars, and underground secrets beneath the city",
                theme = "Adventure",
                coverImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Lviv_underground.jpg/800px-Lviv_underground.jpg",
                authorId = "author_5",
                authorName = "Oleg Shevchenko",
                authorAvatarUrl = "https://i.pravatar.cc/150?img=5",
                price = 30.0,
                currency = "USD",
                startTime = getRandomFutureTimestamp(),
                duration = 90,
                maxParticipants = 8,
                currentParticipants = Random.nextInt(0, 4),
                rating = 4.6f,
                reviewsCount = 24,
                isActive = true,
                isLive = false,
                stops = listOf(
                    createTourStop(
                        name = "Arsenal Museum Underground",
                        description = "Medieval weapon storage cellars",
                        lat = 49.8420,
                        lng = 24.0310,
                        order = 1,
                        content = "Explore ancient underground chambers that once stored weapons and served as secret passages during sieges."
                    ),
                    createTourStop(
                        name = "Underground Coffee Laboratory",
                        description = "Secret coffee roasting chambers",
                        lat = 49.8415,
                        lng = 24.0305,
                        order = 2,
                        content = "Hidden underground spaces where coffee masters experiment with unique roasting techniques away from prying eyes."
                    ),
                    createTourStop(
                        name = "Medieval Tunnels",
                        description = "Merchant tunnel network",
                        lat = 49.8408,
                        lng = 24.0318,
                        order = 3,
                        content = "Network of tunnels used by medieval merchants to transport goods safely through the city."
                    )
                ),
                tags = listOf("underground", "adventure", "mysterious", "medieval", "unique")
            ),

            // 6. Street Art & Modern Culture
            FirestoreTour(
                title = "Modern Lviv: Street Art & Culture",
                description = "Explore contemporary Lviv through street art, galleries, and cultural spaces",
                theme = "Art & Culture",
                coverImageUrl = "https://images.unsplash.com/photo-1541961017774-22349e4a1262?w=800&h=600&fit=crop",
                authorId = "author_6",
                authorName = "Sofia Moroz",
                authorAvatarUrl = "https://i.pravatar.cc/150?img=6",
                price = 0.0,
                currency = "USD",
                startTime = getRandomFutureTimestamp(),
                duration = 105,
                maxParticipants = 25,
                currentParticipants = Random.nextInt(0, 15),
                rating = 4.5f,
                reviewsCount = 18,
                isActive = true,
                isLive = false,
                stops = listOf(
                    createTourStop(
                        name = "Vulytsia Kniazia Romana",
                        description = "Street art gallery under open sky",
                        lat = 49.8380,
                        lng = 24.0250,
                        order = 1,
                        content = "Discover amazing murals and street art that tell stories of modern Ukraine and global culture."
                    ),
                    createTourStop(
                        name = "Dzyga Art Centre",
                        description = "Contemporary art gallery and cultural space",
                        lat = 49.8435,
                        lng = 24.0340,
                        order = 2,
                        content = "Modern art gallery showcasing works by Ukrainian and international contemporary artists."
                    ),
                    createTourStop(
                        name = "Urban Space 500",
                        description = "Cultural hub in converted industrial space",
                        lat = 49.8385,
                        lng = 24.0220,
                        order = 3,
                        content = "Innovative cultural space hosting exhibitions, workshops, and community events in a former industrial building."
                    )
                ),
                tags = listOf("art", "modern", "street-art", "culture", "contemporary")
            ),

            // 7. Lviv Culinary Adventure
            FirestoreTour(
                title = "Traditional Ukrainian Cuisine Tour",
                description = "Taste authentic Ukrainian dishes and learn about local culinary traditions",
                theme = "Food & Drink",
                coverImageUrl = "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=800&h=600&fit=crop",
                authorId = "author_7",
                authorName = "Halyna Kovalchuk",
                authorAvatarUrl = "https://i.pravatar.cc/150?img=7",
                price = 35.0,
                currency = "USD",
                startTime = getRandomFutureTimestamp(),
                duration = 210,
                maxParticipants = 10,
                currentParticipants = Random.nextInt(0, 5),
                rating = 4.8f,
                reviewsCount = 33,
                isActive = true,
                isLive = false,
                stops = listOf(
                    createTourStop(
                        name = "Baczewski Restaurant",
                        description = "Historical restaurant serving traditional dishes",
                        lat = 49.8412,
                        lng = 24.0315,
                        order = 1,
                        content = "Authentic Ukrainian cuisine in a restaurant that recreates the atmosphere of old Galicia. Try their famous varenyky!"
                    ),
                    createTourStop(
                        name = "Pid Synoyu Plaskoyu",
                        description = "Traditional tavern with folk atmosphere",
                        lat = 49.8405,
                        lng = 24.0295,
                        order = 2,
                        content = "Experience traditional Ukrainian hospitality and taste regional specialties in this authentic folk-style tavern."
                    ),
                    createTourStop(
                        name = "Lviv Handmade Chocolate",
                        description = "Artisan chocolate workshop and café",
                        lat = 49.8425,
                        lng = 24.0308,
                        order = 3,
                        content = "Watch chocolatiers at work and taste unique chocolate creations made with traditional and modern techniques."
                    )
                ),
                tags = listOf("food", "traditional", "cooking", "tasting", "cultural")
            ),

            // 8. High Castle Hill Nature Walk
            FirestoreTour(
                title = "High Castle Hill Sunset Walk",
                description = "Scenic walk to the highest point in Lviv for panoramic city views",
                theme = "Nature",
                coverImageUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop",
                authorId = "author_8",
                authorName = "Andriy Boyko",
                authorAvatarUrl = "https://i.pravatar.cc/150?img=8",
                price = 0.0,
                currency = "USD",
                startTime = getRandomFutureTimestamp(),
                duration = 75,
                maxParticipants = 30,
                currentParticipants = Random.nextInt(0, 18),
                rating = 4.7f,
                reviewsCount = 67,
                isActive = true,
                isLive = false,
                stops = listOf(
                    createTourStop(
                        name = "High Castle Park Entrance",
                        description = "Start of the scenic walking trail",
                        lat = 49.8455,
                        lng = 24.0395,
                        order = 1,
                        content = "Begin your ascent through this beautiful park, established on the site of a 13th-century castle."
                    ),
                    createTourStop(
                        name = "Memorial Cross",
                        description = "Historical monument with city views",
                        lat = 49.8465,
                        lng = 24.0405,
                        order = 2,
                        content = "Stop at this memorial cross commemorating historical events while enjoying partial views of the city."
                    ),
                    createTourStop(
                        name = "High Castle Summit",
                        description = "Highest point with 360° panoramic views",
                        lat = 49.8475,
                        lng = 24.0415,
                        order = 3,
                        content = "Reach the summit at 413 meters above sea level for breathtaking panoramic views of Lviv and surrounding landscapes."
                    )
                ),
                tags = listOf("nature", "hiking", "views", "sunset", "photography")
            )
        )
    }

    private fun createTourStop(
        name: String,
        description: String,
        lat: Double,
        lng: Double,
        order: Int,
        content: String
    ): FirestoreTourStop {
        return FirestoreTourStop(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            location = GeoPoint(lat, lng),
            order = order,
            content = FirestoreStopContent(
                text = content,
                imageUrls = emptyList(), // Add specific images if needed
                audioUrl = null,
                videoUrl = null
            )
        )
    }

    private fun getRandomFutureTimestamp(): Timestamp {
        val now = Calendar.getInstance()
        val hoursToAdd = Random.nextInt(1, 6) // 1 to 5 hours from now
        now.add(Calendar.HOUR_OF_DAY, hoursToAdd)

        // Add some random minutes for variety
        val minutesToAdd = Random.nextInt(0, 60)
        now.add(Calendar.MINUTE, minutesToAdd)

        return Timestamp(now.time)
    }
}