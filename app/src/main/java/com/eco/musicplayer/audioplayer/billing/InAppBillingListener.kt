package com.eco.musicplayer.audioplayer.billing

import com.android.billingclient.api.Purchase
import com.eco.musicplayer.audioplayer.billing.model.BaseProductDetails
import com.eco.musicplayer.audioplayer.billing.model.ProductInfo

interface InAppBillingListener {

    fun onStartConnectToGooglePlay()
    fun onProductsLoaded(products: List<BaseProductDetails>)
    fun onPurchasesLoaded(purchases: List<Purchase>)
    fun onInAppBillingError(message: String)

    fun onStartAcknowledgePurchase()
    fun onPurchaseAcknowledged(productInfo: ProductInfo, purchase: Purchase)
    fun onUserCancelPurchase()
    fun onPurchaseError(message: String, productInfo: ProductInfo)
}