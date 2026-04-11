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
        private val USER_ID_KEY = longPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
    }

    suspend fun saveAuthData(
        token: String,
        tokenType: String,
        userId: Long,
        userName: String,
        userEmail: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[TOKEN_TYPE_KEY] = tokenType
            preferences[USER_ID_KEY] = userId
            preferences[USER_NAME_KEY] = userName
            preferences[USER_EMAIL_KEY] = userEmail
            preferences[IS_LOGGED_IN_KEY] = true
        }
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

    suspend fun getUserId(): Long {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY] ?: 0
        }.first()
    }

    suspend fun getUserName(): String {
        return context.dataStore.data.map { preferences ->
            preferences[USER_NAME_KEY] ?: 0
        }.first() as String
    }

    suspend fun getUserEmail(): String {
        return context.dataStore.data.map { preferences ->
            preferences[USER_EMAIL_KEY] ?: 0
        }.first() as String
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
}