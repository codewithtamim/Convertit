package com.nasahacker.convertit.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Notifies Compose to process a pending share / view intent (e.g. after onNewIntent). */
object ShareIntentRefresh {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun notifyPendingShareIntent() {
        _events.tryEmit(Unit)
    }
}
