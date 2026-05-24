package com.nasahacker.convertit.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppRepository {
    val isDontShowAgain: Flow<Boolean>
    val selectedCustomLocation: Flow<String>
    val lastFormat: Flow<String>
    val lastBitrate: Flow<String>
    val lastSampleRate: Flow<String>
    val lastSpeed: Flow<String>

    suspend fun saveIsDontShowAgain(value: Boolean)

    suspend fun saveSelectedCustomLocation(value: String)

    suspend fun saveLastFormat(value: String)

    suspend fun saveLastBitrate(value: String)

    suspend fun saveLastSampleRate(value: String)

    suspend fun saveLastSpeed(value: String)
}
