package com.nasahacker.convertit.util

import android.content.Intent
import android.net.Uri
import android.os.Build

/** API 33+ vs legacy [Intent] parcelable accessors for share / view intents. */
object IntentParcelableExtras {

    fun getParcelableUriArrayList(
        intent: Intent?,
        key: String,
    ): ArrayList<Uri>? {
        if (intent == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(key, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(key)
        }
    }

    fun getParcelableUri(
        intent: Intent?,
        key: String,
    ): Uri? {
        if (intent == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(key)
        }
    }
}
