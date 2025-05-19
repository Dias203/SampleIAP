/*
package com.eco.musicplayer.audioplayer.screens.paywall

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.eco.musicplayer.audioplayer.billing.InAppBillingManager
import com.eco.musicplayer.audioplayer.billing.model.BaseProductDetails
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_LIFETIME
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_MONTH
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_YEAR
import com.eco.musicplayer.audioplayer.music.databinding.ActivityPaywallBinding

class PaywallActivity : AppCompatActivity() {
    lateinit var binding: ActivityPaywallBinding
    lateinit var inAppBillingManager: InAppBillingManager
    val detailsMap = hashMapOf<Int, BaseProductDetails?>()
    var selectPosition = 0
    val purchasedProducts = mutableSetOf<String>()

    val item1ProductId = PRODUCT_ID_MONTH
    val item2ProductId = PRODUCT_ID_YEAR
    val item3ProductId = PRODUCT_ID_LIFETIME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaywallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inAppBillingManager = InAppBillingManager(this)

        // Gọi các hàm khởi tạo
        loadSubsPolicyContent()
        setOnClicks()
        initBilling()

        // Kiểm tra và khôi phục trạng thái nếu cần
        savedInstanceState?.let {
            selectPosition = it.getInt("selectPosition", 0)
            if (selectPosition != 0) {
                updateItem() // Gọi để cập nhật UI nếu selectPosition đã được lưu
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectPosition", selectPosition)
    }

    override fun onDestroy() {
        inAppBillingManager.endConnectToGooglePlay()
        inAppBillingManager.destroy()
        super.onDestroy()
    }
}*/

package com.eco.musicplayer.audioplayer.screens.paywall

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.eco.musicplayer.audioplayer.billing.InAppBillingManager
import com.eco.musicplayer.audioplayer.billing.model.BaseProductDetails
import com.eco.musicplayer.audioplayer.helpers.PurchasePrefsHelper
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_LIFETIME
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_MONTH
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_YEAR
import com.eco.musicplayer.audioplayer.music.databinding.ActivityPaywallBinding

class PaywallActivity : AppCompatActivity() {
    lateinit var binding: ActivityPaywallBinding
     val inAppBillingManager: InAppBillingManager by lazy {
         InAppBillingManager(this)
     }
    val detailsMap = hashMapOf<Int, BaseProductDetails?>()
    var selectPosition = 0
    val purchasedProducts = mutableSetOf<String>()

    val item1ProductId = PRODUCT_ID_MONTH
    val item2ProductId = PRODUCT_ID_YEAR
    val item3ProductId = PRODUCT_ID_LIFETIME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaywallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tải trạng thái mua hàng từ SharedPreferences
        purchasedProducts.addAll(PurchasePrefsHelper.getPurchasedProducts(this))
        updatePlanSelectionBasedOnPurchases()

        // Gọi các hàm khởi tạo
        loadSubsPolicyContent()
        setOnClicks()
        initBilling()

        savedInstanceState?.let {
            selectPosition = it.getInt("selectPosition", 0)
            if (selectPosition != 0) {
                updateItem()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectPosition", selectPosition)
    }

    override fun onDestroy() {
        inAppBillingManager.endConnectToGooglePlay()
        inAppBillingManager.destroy()
        super.onDestroy()
    }
}
