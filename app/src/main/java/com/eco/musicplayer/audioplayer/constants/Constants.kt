package com.eco.musicplayer.audioplayer.constants

const val PRODUCT_ID_WEEK = "test1"
const val PRODUCT_ID_YEAR = "test2"
const val PRODUCT_ID_LIFETIME = "test3"
const val PRODUCT_ID_FREE_TRIAL = "free_123"

object ConstantsProductID {
    val subsListProduct = mutableListOf<String>(
        PRODUCT_ID_WEEK,
        PRODUCT_ID_YEAR,
        PRODUCT_ID_FREE_TRIAL
    )
    val inAppListProduct = mutableListOf<String>(
        PRODUCT_ID_LIFETIME
    )
}