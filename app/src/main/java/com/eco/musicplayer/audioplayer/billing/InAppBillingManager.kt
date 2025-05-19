package com.eco.musicplayer.audioplayer.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import com.eco.musicplayer.audioplayer.billing.model.*
import com.eco.musicplayer.audioplayer.constants.ConstantsProductID

class InAppBillingManager(context: Context) {

    private val TAG = "InAppBillingManager"

    var listener: InAppBillingListener? = null
    var productInfoList = listOf<ProductInfo>()
    val allProducts = mutableListOf<BaseProductDetails>()
    private val handler = Handler(Looper.getMainLooper())

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                        listener?.onPurchaseAcknowledged(
                            ProductInfo(
                                purchase.products.firstOrNull()?.let { productId ->
                                    allProducts.find { it.productId == productId }?.productType ?: SUBS
                                } ?: SUBS,
                                purchase.products.firstOrNull() ?: ""
                            ),
                            purchase
                        )
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> listener?.onUserCancelPurchase()
            else -> listener?.onPurchaseError(billingResult.debugMessage, ProductInfo())
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enablePrepaidPlans()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnectToGooglePlay() {
        listener?.onStartConnectToGooglePlay()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                post {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryProducts()
                    } else {
                        listener?.onInAppBillingError("Billing setup failed: ${billingResult.debugMessage}")
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                post { listener?.onInAppBillingError("Billing service disconnected") }
            }
        })
    }

    private fun queryProducts() {
        var subsQueryCompleted = ConstantsProductID.subsListProduct.isEmpty()
        var inAppQueryCompleted = ConstantsProductID.inAppListProduct.isEmpty()

        fun checkAndProcess() {
            if (subsQueryCompleted && inAppQueryCompleted) {
                post { listener?.onProductsLoaded(allProducts) }
                queryPurchases()
            }
        }

        querySUBS {
            subsQueryCompleted = true
            checkAndProcess()
        }

        queryINAPP {
            inAppQueryCompleted = true
            checkAndProcess()
        }
    }

    private fun querySUBS(onComplete: () -> Unit) {
        if (isFeatureSupported()) {
            queryProductDetails(
                productIds = ConstantsProductID.subsListProduct,
                productType = SUBS,
                onComplete = { products ->
                    allProducts.addAll(products.map { ProductDetailsWrapper(it) })
                    onComplete()
                }
            )
        } else {
            querySkuDetails(
                productIds = ConstantsProductID.subsListProduct,
                productType = SUBS,
                onComplete = { skus ->
                    allProducts.addAll(skus.map { SkuDetailsWrapper(it) })
                    onComplete()
                }
            )
        }
    }

    private fun queryINAPP(onComplete: () -> Unit) {
        if (isFeatureSupported()) {
            queryProductDetails(
                productIds = ConstantsProductID.inAppListProduct,
                productType = IN_APP,
                onComplete = { products ->
                    allProducts.addAll(products.map { ProductDetailsWrapper(it) })
                    onComplete()
                }
            )
        } else {
            querySkuDetails(
                productIds = ConstantsProductID.inAppListProduct,
                productType = IN_APP,
                onComplete = { skus ->
                    allProducts.addAll(skus.map { SkuDetailsWrapper(it) })
                    onComplete()
                }
            )
        }
    }
    private fun queryProductDetails(
        productIds: List<String>,
        productType: String,
        onComplete: (List<ProductDetails>) -> Unit
    ) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(productType)
                        .build()
                }
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onComplete(productDetailsList)
            } else {
                Log.e(TAG, "Failed to retrieve $productType product details: ${billingResult.debugMessage}")
                onComplete(emptyList())
            }
        }
    }

    private fun querySkuDetails(
        productIds: List<String>,
        productType: String,
        onComplete: (List<SkuDetails>) -> Unit
    ) {
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(productIds)
            .setType(productType)
            .build()

        billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onComplete(skuDetailsList ?: emptyList())
            } else {
                Log.e(TAG, "Failed to retrieve $productType sku details: ${billingResult.debugMessage}")
                onComplete(emptyList())
            }
        }
    }

    private fun queryPurchases() {
        val purchasedProducts = mutableListOf<BaseProductDetails>()
        var subsQueryCompleted = false
        var inAppQueryCompleted = false

        fun checkCompletion() {
            if (subsQueryCompleted && inAppQueryCompleted) {
                post { listener?.onPurchasesLoaded(purchasedProducts) }
            }
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(SUBS)
                .build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchasesList
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.isAcknowledged }
                    .flatMap { it.products }
                    .mapNotNull { productId -> allProducts.find { it.productId == productId } }
                    .let { purchasedProducts.addAll(it) }
            }
            subsQueryCompleted = true
            checkCompletion()
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(IN_APP)
                .build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchasesList
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.isAcknowledged }
                    .flatMap { it.products }
                    .mapNotNull { productId -> allProducts.find { it.productId == productId } }
                    .let { purchasedProducts.addAll(it) }
            }
            inAppQueryCompleted = true
            checkCompletion()
        }
    }

    fun launchPurchase(activity: Activity, baseProduct: BaseProductDetails) {
        when (baseProduct) {
            is ProductDetailsWrapper -> {
                val productDetails = baseProduct.productDetails
                val paramsDetailBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)

                if (productDetails.productType == SUBS) {
                    val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                    if (offerToken == null) {
                        listener?.onPurchaseError("No subscription offer available", ProductInfo(SUBS, productDetails.productId))
                        return
                    }
                    paramsDetailBuilder.setOfferToken(offerToken)
                }

                val paramsDetail = paramsDetailBuilder.build()
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(paramsDetail))
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
            is SkuDetailsWrapper -> {
                val skuDetails = baseProduct.skuDetails
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails)
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }

    private fun getOldPurchaseToken(oldProductId: String, callback: (String?) -> Unit) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(SUBS)
                .build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val purchase = purchasesList
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.isAcknowledged }
                    .find { it.products.contains(oldProductId) }
                callback(purchase?.purchaseToken)
            } else {
                Log.e(TAG, "Failed to query purchases for oldProductId: $oldProductId, error: ${billingResult.debugMessage}")
                callback(null)
            }
        }
    }

    fun launchBillingFlowForUpgrade(activity: Activity, product: BaseProductDetails, oldProductId: String) {
        getOldPurchaseToken(oldProductId) { oldPurchaseToken ->
            if (oldPurchaseToken == null) {
                Log.e(TAG, "No valid purchase token found for $oldProductId")
                listener?.onPurchaseError("Không tìm thấy giao dịch trước đó", ProductInfo())
                post { Toast.makeText(activity, "Không tìm thấy giao dịch trước đó", Toast.LENGTH_SHORT).show() }
                return@getOldPurchaseToken
            }

            when (product) {
                is ProductDetailsWrapper -> launchProductDetailsUpgradeFlow(activity, product, oldPurchaseToken)
                is SkuDetailsWrapper -> launchSkuDetailsUpgradeFlow(activity, product, oldPurchaseToken)
            }
        }
    }


    private fun launchProductDetailsUpgradeFlow(activity: Activity, product: ProductDetailsWrapper, oldPurchaseToken: String) {
        val productDetails = product.productDetails
        if (productDetails.productType != SUBS) {
            Log.e(TAG, "Unsupported product type for upgrade: ${productDetails.productType}")
            listener?.onPurchaseError("Sản phẩm không phải là đăng ký", ProductInfo())
            post { Toast.makeText(activity, "Sản phẩm không phải là đăng ký", Toast.LENGTH_SHORT).show() }
            return
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Log.e(TAG, "No offer token for subscription: ${productDetails.title}")
            listener?.onPurchaseError("Không có ưu đãi đăng ký", ProductInfo(SUBS, productDetails.productId))
            post { Toast.makeText(activity, "Không có ưu đãi đăng ký", Toast.LENGTH_SHORT).show() }
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
            .setOldPurchaseToken(oldPurchaseToken)
            .setSubscriptionReplacementMode(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .setSubscriptionUpdateParams(subscriptionUpdateParams)
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${result.responseCode}")
            listener?.onPurchaseError("Không thể khởi động thanh toán", ProductInfo())
            post { Toast.makeText(activity, "Không thể khởi động thanh toán", Toast.LENGTH_SHORT).show() }
        } else {
            Log.d(TAG, "Billing flow launched for upgrade to ${productDetails.productId}, old purchase token: $oldPurchaseToken")
        }
    }

    private fun launchSkuDetailsUpgradeFlow(activity: Activity, product: SkuDetailsWrapper, oldPurchaseToken: String) {
        val skuDetails = product.skuDetails
        if (skuDetails.type != SUBS) {
            Log.e(TAG, "Unsupported product type for upgrade: ${skuDetails.type}")
            listener?.onPurchaseError("Sản phẩm không phải là đăng ký", ProductInfo())
            post { Toast.makeText(activity, "Sản phẩm không phải là đăng ký", Toast.LENGTH_SHORT).show() }
            return
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .setObfuscatedAccountId(oldPurchaseToken) // Dùng làm liên kết nâng cấp
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${result.responseCode}")
            listener?.onPurchaseError("Không thể khởi động thanh toán", ProductInfo())
            post { Toast.makeText(activity, "Không thể khởi động thanh toán", Toast.LENGTH_SHORT).show() }
        } else {
            Log.d(TAG, "Billing flow launched for upgrade to ${skuDetails.sku}, old purchase token: $oldPurchaseToken")
        }
    }


    private fun acknowledgePurchase(purchase: Purchase) {
        listener?.onStartAcknowledgePurchase()
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                listener?.onPurchaseError("Failed to acknowledge purchase: ${result.debugMessage}", ProductInfo())
            }
        }
    }

    fun endConnectToGooglePlay() {
        billingClient.endConnection()
    }

    fun destroy() {
        listener = null
        endConnectToGooglePlay()
    }

    private fun isFeatureSupported(): Boolean {
        return billingClient.isFeatureSupported(BillingClient.FeatureType.PRODUCT_DETAILS)
            .responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun post(work: () -> Unit) {
        handler.post { work() }
    }
}