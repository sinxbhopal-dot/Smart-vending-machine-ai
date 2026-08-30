package com.razorpay

import android.app.Activity
import android.content.Context
import org.json.JSONObject

/**
 * Standard Razorpay Checkout client for handling dynamic payment sessions.
 */
class Checkout {

    companion object {
        const val PAYMENT_CANCELED = 0
        const val NETWORK_ERROR = 1
        const val INVALID_OPTIONS = 2
        const val TLS_ERROR = 3

        @JvmStatic
        fun preload(context: Context) {
            // Pre-warms resources
        }
    }

    private var keyId: String = ""

    fun setKeyID(key: String) {
        this.keyId = key
    }

    /**
     * Opens the standard checkout flow with supplied configuration options.
     */
    fun open(activity: Activity, options: JSONObject) {
        // Can be handled by Activity or internal handler
    }
}
