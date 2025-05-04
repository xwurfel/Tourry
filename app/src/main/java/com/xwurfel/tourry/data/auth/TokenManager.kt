package com.xwurfel.tourry.data.auth

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.inject.Inject

class TokenManager @Inject constructor(
    private val context: Context
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
    }

    fun saveToken(token: String) {
        sharedPreferences.edit() { putString(KEY_TOKEN, token) }
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    fun saveUserId(userId: Long) {
        sharedPreferences.edit() { putLong(KEY_USER_ID, userId) }
    }

    fun getUserId(): Long {
        return sharedPreferences.getLong(KEY_USER_ID, -1)
    }

    fun clearTokens() {
        sharedPreferences.edit() { clear() }
    }
}