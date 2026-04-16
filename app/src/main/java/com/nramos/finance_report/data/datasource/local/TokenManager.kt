package com.nramos.finance_report.data.datasource.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("access_token")
        private val TOKEN_TYPE_KEY = stringPreferencesKey("token_type")
        private val USER_PROFILE_ID_KEY = stringPreferencesKey("profile_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")

        private val USER_PATERNAL_SURNAME_KEY = stringPreferencesKey("user_paternal_surname")
        private val USER_MATERNAL_SURNAME_KEY = stringPreferencesKey("user_maternal_surname")
        private val USER_GENDER_KEY = stringPreferencesKey("user_gender")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")

        private val USER_AVATAR_URL_KEY = stringPreferencesKey("user_avatar_url")

    }

    suspend fun saveAuthData(
        token: String,
        tokenType: String,
        profileId: String,
        userName: String,
        userEmail: String,
        paternalSurname: String? = null,
        maternalSurname: String? = null,
        gender: Char? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[TOKEN_TYPE_KEY] = tokenType
            preferences[USER_PROFILE_ID_KEY] = profileId
            preferences[USER_NAME_KEY] = userName
            preferences[USER_EMAIL_KEY] = userEmail
            preferences[USER_PATERNAL_SURNAME_KEY] = paternalSurname ?: ""
            preferences[USER_MATERNAL_SURNAME_KEY] = maternalSurname ?: ""
            preferences[USER_GENDER_KEY] = gender?.toString() ?: ""
            preferences[IS_LOGGED_IN_KEY] = true
        }
    }

    suspend fun saveAvatarUrl(avatarUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_AVATAR_URL_KEY] = avatarUrl
        }
    }

    suspend fun getAvatarUrl(): String {
        return context.dataStore.data.map { preferences ->
            preferences[USER_AVATAR_URL_KEY] ?: ""
        }.first()
    }

    fun getToken(): String? {
        return runBlocking {
            context.dataStore.data.map { preferences ->
                preferences[TOKEN_KEY]
            }.first()
        }
    }

    fun getAuthHeader(): String? {
        val token = getToken() ?: return null
        val tokenType = runBlocking {
            context.dataStore.data.map { preferences ->
                preferences[TOKEN_TYPE_KEY] ?: "Bearer"
            }.first()
        }
        return "$tokenType $token"
    }

    suspend fun isLoggedIn(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[IS_LOGGED_IN_KEY] ?: false
        }.first()
    }

    suspend fun getUserProfileId(): String {
        return context.dataStore.data.map { preferences ->
            preferences[USER_PROFILE_ID_KEY] ?: ""
        }.first()
    }

    suspend fun getUserName(): String {
        return context.dataStore.data.map { preferences ->
            preferences[USER_NAME_KEY] ?: ""
        }.first()
    }

    suspend fun getUserEmail(): String {
        return context.dataStore.data.map { preferences ->
            preferences[USER_EMAIL_KEY] ?: ""
        }.first()
    }

    suspend fun clearAuthData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    fun getAuthTokenFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }

    suspend fun getUserPaternalSurname(): String {
        return context.dataStore.data.map { preferences ->
            preferences[USER_PATERNAL_SURNAME_KEY] ?: ""
        }.first()
    }

    suspend fun getUserMaternalSurname(): String {
        return context.dataStore.data.map { preferences ->
            preferences[USER_MATERNAL_SURNAME_KEY] ?: ""
        }.first()
    }

    suspend fun getUserGender(): Char? {
        val gender = context.dataStore.data.map { preferences ->
            preferences[USER_GENDER_KEY] ?: ""
        }.first()
        return gender.takeIf { it.isNotEmpty() }?.firstOrNull()
    }
}