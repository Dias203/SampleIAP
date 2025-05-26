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
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_WEEK
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_WEEK
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_YEAR
import com.eco.musicplayer.audioplayer.helpers.PurchasePrefsHelper

private const val TAG = "DUC"

fun PaywallActivity.initBilling() {
    inAppBillingManager.apply {
        listener = createInAppBillingListener()
        productInfoList = listOf(
            ProductInfo(SUBS, PRODUCT_ID_WEEK),
            ProductInfo(SUBS, PRODUCT_ID_YEAR),
            ProductInfo(IN_APP, PRODUCT_ID_LIFETIME)
        )
        Log.d(TAG, "initBilling: Starting connection to Google Play")
        startConnectToGooglePlay()
    }
}

fun PaywallActivity.createInAppBillingListener() = object : InAppBillingListener {
    override fun onStartConnectToGooglePlay() {
        showToast("Connecting to Google Play...")
    }


    override fun onProductsLoaded(products: List<BaseProductDetails>) {
        loadPriceUI(products)
        updatePlanSelectionBasedOnPurchases()
    }

    override fun onPurchasesLoaded(purchases: List<Purchase>) {
        Log.i(TAG, "onPurchasesLoaded: 3")
        Log.i("TAG", "onPurchasesLoaded: ${purchases.size}")
        if (purchases.isNotEmpty()) {
            Log.i("TAG", "onPurchasesLoaded isNotEmpty: ${purchases.size}")
            updatePurchases(purchases)
        }
    }

    override fun onInAppBillingError(message: String) {
        showToast("Billing error: $message")
        purchasedProducts.apply {
            clear()
            addAll(PurchasePrefsHelper.getPurchasedProducts(this@createInAppBillingListener))
        }
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
        Log.i("TAG", "updatePurchases: ${purchasedProducts.size}")
        purchases.forEach { purchase ->
            purchase.products.forEach { productId ->
                purchasedProducts.add(productId)
                Log.i("TAG", "updatePurchases: $productId")
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