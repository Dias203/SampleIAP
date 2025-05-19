package com.eco.musicplayer.audioplayer.billing.model

import com.android.billingclient.api.SkuDetails

data class SkuDetailsWrapper(@Suppress("DEPRECATION") val skuDetails: SkuDetails) :
    BaseProductDetails() {
    override val productId: String
        get() = skuDetails.sku

    override val title: String
        get() = skuDetails.title

    override val productType: String
        get() = skuDetails.type


}