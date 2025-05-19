package com.eco.musicplayer.audioplayer.billing.model

import com.android.billingclient.api.ProductDetails

data class ProductDetailsWrapper(val productDetails: ProductDetails) : BaseProductDetails() {
    override val productId: String
        get() = productDetails.productId

    override val title: String
        get() = productDetails.title

    override val productType: String
        get() = productDetails.productType


}