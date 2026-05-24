package com.nasahacker.convertit.data.local

import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nasahacker.convertit.util.AppConfig.FOLDER_DIR
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

class UserPreferencesDataSource
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val PREF_DONT_SHOW_AGAIN = booleanPreferencesKey("pref_dont_show_again")
            val PREF_CUSTOM_SAVE_LOCATION = stringPreferencesKey("pref_custom_save_location")
            val PREF_LAST_FORMAT = stringPreferencesKey("pref_last_format")
            val PREF_LAST_BITRATE = stringPreferencesKey("pref_last_bitrate")
            val PREF_LAST_SAMPLE_RATE = stringPreferencesKey("pref_last_sample_rate")
            val PREF_LAST_SPEED = stringPreferencesKey("pref_last_speed")
        }

        val isDontShowAgain: Flow<Boolean> =
            dataStore.data.map { prefs ->
                prefs[Keys.PREF_DONT_SHOW_AGAIN] ?: false
            }

        val selectedCustomSaveLocation: Flow<String> =
            dataStore.data.map { prefs ->
                val savedValue = prefs[Keys.PREF_CUSTOM_SAVE_LOCATION]
                val defaultValue = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    FOLDER_DIR,
                ).absolutePath
                savedValue ?: defaultValue
            }

        val lastFormat: Flow<String> =
            dataStore.data.map { prefs ->
                prefs[Keys.PREF_LAST_FORMAT] ?: ".mp3"
            }

        val lastBitrate: Flow<String> =
            dataStore.data.map { prefs ->
                prefs[Keys.PREF_LAST_BITRATE] ?: "256k"
            }

        val lastSampleRate: Flow<String> =
            dataStore.data.map { prefs ->
                prefs[Keys.PREF_LAST_SAMPLE_RATE] ?: "44100"
            }

        val lastSpeed: Flow<String> =
            dataStore.data.map { prefs ->
                prefs[Keys.PREF_LAST_SPEED] ?: "1.0"
            }

        suspend fun saveIsDontShowAgain(value: Boolean) {
            dataStore.edit { prefs ->
                prefs[Keys.PREF_DONT_SHOW_AGAIN] = value
            }
        }

        suspend fun saveSelectedCustomSaveLocation(value: String) {
            dataStore.edit { prefs ->
                prefs[Keys.PREF_CUSTOM_SAVE_LOCATION] = value
            }
        }

        suspend fun saveLastFormat(value: String) {
            dataStore.edit { prefs ->
                prefs[Keys.PREF_LAST_FORMAT] = value
            }
        }

        suspend fun saveLastBitrate(value: String) {
            dataStore.edit { prefs ->
                prefs[Keys.PREF_LAST_BITRATE] = value
            }
        }

        suspend fun saveLastSampleRate(value: String) {
            dataStore.edit { prefs ->
                prefs[Keys.PREF_LAST_SAMPLE_RATE] = value
            }
        }

        suspend fun saveLastSpeed(value: String) {
            dataStore.edit { prefs ->
                prefs[Keys.PREF_LAST_SPEED] = value
            }
        }
    }
