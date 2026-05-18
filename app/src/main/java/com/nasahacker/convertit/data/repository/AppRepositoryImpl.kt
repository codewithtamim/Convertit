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

        override suspend fun saveIsDontShowAgain(value: Boolean) {
            dataSource.saveIsDontShowAgain(value)
        }

        override suspend fun saveSelectedCustomLocation(value: String) {
            dataSource.saveSelectedCustomSaveLocation(value)
        }
    }
