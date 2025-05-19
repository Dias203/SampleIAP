package com.eco.musicplayer.audioplayer.helpers

import android.content.Context
import android.content.SharedPreferences

object PurchasePrefsHelper {
    private const val PREF_NAME = "PurchasePrefs"
    private const val KEY_PURCHASED_PRODUCTS = "purchased_products"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun savePurchasedProducts(context: Context, productIds: Set<String>) {
        getPrefs(context).edit()
            .putStringSet(KEY_PURCHASED_PRODUCTS, productIds)
            .apply()
    }

    fun getPurchasedProducts(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_PURCHASED_PRODUCTS, emptySet()) ?: emptySet()
    }

    fun clearPurchasedProducts(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_PURCHASED_PRODUCTS)
            .apply()
    }
}