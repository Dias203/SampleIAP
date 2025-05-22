package com.eco.musicplayer.audioplayer.screens.paywall

import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.Purchase
import com.eco.musicplayer.audioplayer.billing.InAppBillingListener
import com.eco.musicplayer.audioplayer.billing.model.BaseProductDetails
import com.eco.musicplayer.audioplayer.billing.model.ProductInfo
import com.eco.musicplayer.audioplayer.billing.model.SUBS
import com.eco.musicplayer.audioplayer.billing.model.IN_APP
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_LIFETIME
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_MONTH
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_YEAR
import com.eco.musicplayer.audioplayer.helpers.PurchasePrefsHelper

const val TAG = "DUC"

fun PaywallActivity.initBilling() {
    inAppBillingManager.listener = createInAppBillingListener()
    inAppBillingManager.productInfoList = listOf(
        ProductInfo(SUBS, PRODUCT_ID_MONTH),
        ProductInfo(SUBS, PRODUCT_ID_YEAR),
        ProductInfo(IN_APP, PRODUCT_ID_LIFETIME)
    )
    Log.d(TAG, "initBilling: 0")
    inAppBillingManager.startConnectToGooglePlay()
}

fun PaywallActivity.createInAppBillingListener() = object : InAppBillingListener {
    override fun onStartConnectToGooglePlay() {
        Log.d(TAG, "onStartConnectToGooglePlay: 1")
        showToast("Connecting to Google Play...")
    }

    override fun onProductsLoaded(products: List<BaseProductDetails>) {
        Log.d(TAG, "onProductsLoaded: 2")
        loadPriceUI(products)
    }

    override fun onPurchasesLoaded(purchases: List<Purchase>) {
        Log.d(TAG, "onPurchasesLoaded: 3")
        if (purchases.isNotEmpty()) {
            updatePurchases(purchases)
        }
    }

    override fun onInAppBillingError(message: String) {
        showToast("Billing error: $message")
        // Nếu lỗi do mạng, dựa vào SharedPreferences
        purchasedProducts.clear()
        purchasedProducts.addAll(PurchasePrefsHelper.getPurchasedProducts(this@createInAppBillingListener))
        updatePlanSelectionBasedOnPurchases()
        updateItem()
    }

    override fun onStartAcknowledgePurchase() {
        // Hiển thị loading nếu cần
    }

    override fun onPurchaseAcknowledged(productInfo: ProductInfo, purchase: Purchase) {
        purchase.products.firstOrNull()?.let { productId ->
            purchasedProducts.add(productId)
            Log.i(TAG, "onPurchaseAcknowledged: PurchasedProducts MutableSet<String> Size: ${purchasedProducts.size}")
            Log.i(TAG, "onPurchaseAcknowledged: purchase MutableList<Purchase> Size: ${purchase.products.size}")
            purchasedProducts.forEach {
                Log.i(TAG, "onPurchaseAcknowledged: $it")
            }
            // Lưu vào SharedPreferences
            Log.i("TAG", "onPurchaseAcknowledged: luu vao day")
            PurchasePrefsHelper.savePurchasedProducts(
                this@createInAppBillingListener,
                purchasedProducts
            )
            updatePlanSelectionBasedOnPurchases()
            if(productId != PRODUCT_ID_LIFETIME) {
                updateItem()
            }
            showToast("Purchase successful: $productId")
        }
    }

    override fun onUserCancelPurchase() {
        showToast("Purchase cancelled")
    }

    override fun onPurchaseError(message: String, productInfo: ProductInfo) {
        showToast("Purchase error: $message")
    }


    fun PaywallActivity.updatePurchases(purchases: List<Purchase>) {
        purchases.forEach { purchase ->
            purchase.products.forEach { productId ->
                purchasedProducts.add(productId)
                Log.i("TAG", "updatePurchases: $productId")
                Log.i("TAG", "updatePurchases: ${purchasedProducts.size}")
            }
        }

        // Cập nhật SharedPreferences
        PurchasePrefsHelper.savePurchasedProducts(this, purchasedProducts)

        updatePlanSelectionBasedOnPurchases()
        updateItem()
    }

    private fun PaywallActivity.showToast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
