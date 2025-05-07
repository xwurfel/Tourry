package com.xwurfel.tourry.data.auth

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "auth_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
    }

    fun saveToken(token: String) {
        sharedPreferences.edit {
            putString(KEY_TOKEN, token)
            putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + 24 * 60 * 60 * 1000)
        }
    }

    fun saveRefreshToken(refreshToken: String) {
        sharedPreferences.edit {
            putString(KEY_REFRESH_TOKEN, refreshToken)
        }
    }

    fun getToken(): String? {
        val token = sharedPreferences.getString(KEY_TOKEN, null)
        val expiry = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0)

        return if (token != null && System.currentTimeMillis() < expiry) {
            token
        } else {
            null
        }
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveUserId(userId: Long) {
        sharedPreferences.edit {
            putLong(KEY_USER_ID, userId)
        }
    }

    fun getUserId(): Long {
        return sharedPreferences.getLong(KEY_USER_ID, -1)
    }

    fun clearTokens() {
        sharedPreferences.edit {
            clear()
        }
    }

    fun isTokenExpired(): Boolean {
        val expiry = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0)
        return System.currentTimeMillis() >= expiry
    }
}