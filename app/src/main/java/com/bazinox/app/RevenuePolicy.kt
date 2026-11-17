package com.bazinox.app

/**
 * Iran launch revenue policy.
 * These constants are display defaults only. Production amounts must come
 * from the trusted backend; the Android app is never authoritative.
 */
object RevenuePolicy {
    const val LISTING_FEE_RIAL: Long = 1_000_000L
    const val COMMISSION_BASIS_POINTS: Int = 500 // 5.00%

    // Fixed international display tariffs; these are not converted from IRR.
    const val LISTING_30_DAYS_USD: Int = 5
    const val LISTING_60_DAYS_USD: Int = 10
    const val LISTING_90_DAYS_USD: Int = 15
    const val LISTING_30_DAYS_EUR: Int = 5
    const val LISTING_60_DAYS_EUR: Int = 10
    const val LISTING_90_DAYS_EUR: Int = 15

    fun commissionRial(orderTotalRial: Long): Long {
        require(orderTotalRial >= 0) { "Order total cannot be negative" }
        return (orderTotalRial * COMMISSION_BASIS_POINTS) / 10_000L
    }

    fun sellerNetRial(orderTotalRial: Long): Long =
        orderTotalRial - commissionRial(orderTotalRial)
}
