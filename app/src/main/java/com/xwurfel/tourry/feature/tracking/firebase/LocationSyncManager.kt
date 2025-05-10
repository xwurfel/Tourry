package com.xwurfel.tourry.feature.tracking.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwurfel.tourry.feature.tracking.domain.model.MemberLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationSyncManager @Inject constructor(
    firebaseDatabase: FirebaseDatabase
) {
    private val locationsRef = firebaseDatabase.getReference("locations")

    fun updateLocation(groupId: String, userId: String, location: MemberLocation) {
        val locationData = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "timestamp" to location.timestamp.toEpochMilli(),
            "isInGeofence" to location.isInGeofence
        )

        locationsRef.child(groupId).child(userId).setValue(locationData)
    }

    fun stopSharingLocation(groupId: String, userId: String) {
        locationsRef.child(groupId).child(userId).removeValue()
    }

    fun observeGroupLocations(groupId: String): Flow<Map<String, MemberLocation>> = callbackFlow {
        val groupLocationsRef = locationsRef.child(groupId)

        val listener = groupLocationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locations = mutableMapOf<String, MemberLocation>()

                for (childSnapshot in snapshot.children) {
                    val userId = childSnapshot.key ?: continue
                    val locationData = childSnapshot.value as? Map<*, *> ?: continue

                    val latitude = (locationData["latitude"] as? Double) ?: continue
                    val longitude = (locationData["longitude"] as? Double) ?: continue
                    val accuracy = (locationData["accuracy"] as? Number)?.toFloat() ?: 0f
                    val timestamp =
                        (locationData["timestamp"] as? Long)?.let { Instant.ofEpochMilli(it) }
                            ?: Instant.now()
                    val isInGeofence = (locationData["isInGeofence"] as? Boolean) ?: true

                    locations[userId] = MemberLocation(
                        latitude = latitude,
                        longitude = longitude,
                        accuracy = accuracy,
                        timestamp = timestamp,
                        isInGeofence = isInGeofence
                    )
                }

                trySend(locations)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            groupLocationsRef.removeEventListener(listener)
        }
    }

    fun observeMemberLocation(groupId: String, userId: String): Flow<MemberLocation?> =
        callbackFlow {
            val memberLocationRef = locationsRef.child(groupId).child(userId)

            val listener = memberLocationRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val locationData = snapshot.value as? Map<*, *>
                    if (locationData == null) {
                        trySend(null)
                        return
                    }

                    val latitude = (locationData["latitude"] as? Double) ?: return
                    val longitude = (locationData["longitude"] as? Double) ?: return
                    val accuracy = (locationData["accuracy"] as? Number)?.toFloat() ?: 0f
                    val timestamp =
                        (locationData["timestamp"] as? Long)?.let { Instant.ofEpochMilli(it) }
                            ?: Instant.now()
                    val isInGeofence = (locationData["isInGeofence"] as? Boolean) ?: true

                    val location = MemberLocation(
                        latitude = latitude,
                        longitude = longitude,
                        accuracy = accuracy,
                        timestamp = timestamp,
                        isInGeofence = isInGeofence
                    )

                    trySend(location)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            })

            awaitClose {
                memberLocationRef.removeEventListener(listener)
            }
        }
}