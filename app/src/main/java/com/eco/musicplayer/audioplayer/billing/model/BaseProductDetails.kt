package com.eco.musicplayer.audioplayer.billing.model

sealed class BaseProductDetails {
    abstract val productId:String
    abstract val title: String
    abstract val productType: String
}