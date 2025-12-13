package com.example.pam_1.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property untuk DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class DataStoreManager(private val context: Context) {

    companion object {
        private val REMEMBER_ME_KEY = booleanPreferencesKey("remember_me")
    }

    // Menyimpan status remember me
    suspend fun saveRememberMe(rememberMe: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REMEMBER_ME_KEY] = rememberMe
        }
    }

    // Membaca status remember me
    val rememberMeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[REMEMBER_ME_KEY] ?: false
        }
}