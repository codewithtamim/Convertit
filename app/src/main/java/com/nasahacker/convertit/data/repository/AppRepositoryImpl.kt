package com.nasahacker.convertit.data.repository

import com.nasahacker.convertit.data.local.UserPreferencesDataSource
import com.nasahacker.convertit.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Convertit Android app
 * Copyright (c) 2025 The Byte Array LTD.
 *
 * @author Tamim Hossain
 * @company The Byte Array LTD
 * @year 2025
 */

class AppRepositoryImpl
    @Inject
    constructor(
        private val dataSource: UserPreferencesDataSource,
    ) : AppRepository {
        override val isDontShowAgain: Flow<Boolean>
            get() = dataSource.isDontShowAgain
        override val selectedCustomLocation: Flow<String>
            get() = dataSource.selectedCustomSaveLocation
        override val lastFormat: Flow<String>
            get() = dataSource.lastFormat
        override val lastBitrate: Flow<String>
            get() = dataSource.lastBitrate
        override val lastSampleRate: Flow<String>
            get() = dataSource.lastSampleRate
        override val lastSpeed: Flow<String>
            get() = dataSource.lastSpeed

        override suspend fun saveIsDontShowAgain(value: Boolean) {
            dataSource.saveIsDontShowAgain(value)
        }

        override suspend fun saveSelectedCustomLocation(value: String) {
            dataSource.saveSelectedCustomSaveLocation(value)
        }

        override suspend fun saveLastFormat(value: String) {
            dataSource.saveLastFormat(value)
        }

        override suspend fun saveLastBitrate(value: String) {
            dataSource.saveLastBitrate(value)
        }

        override suspend fun saveLastSampleRate(value: String) {
            dataSource.saveLastSampleRate(value)
        }

        override suspend fun saveLastSpeed(value: String) {
            dataSource.saveLastSpeed(value)
        }
    }
