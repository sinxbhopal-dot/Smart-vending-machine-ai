package com.razorpay

/**
 * Standard Razorpay Android SDK Payment Callback Listener interface.
 * Implemented by Activities to receive asynchronous payment status callbacks.
 */
interface PaymentResultListener {
    /**
     * Called when a payment completes successfully.
     * @param razorpayPaymentId The unique payment ID returned by Razorpay (e.g., "pay_29QQoUBi66xm2f").
     */
    fun onPaymentSuccess(razorpayPaymentId: String?)

    /**
     * Called when a payment fails or is cancelled by the user.
     * @param code The error code representing the failure reason.
     * @param response The descriptive error message or JSON string.
     */
    fun onPaymentError(code: Int, response: String?)
}
