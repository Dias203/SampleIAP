package com.eco.musicplayer.audioplayer.screens.paywall

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import com.eco.musicplayer.audioplayer.billing.model.BaseProductDetails
import com.eco.musicplayer.audioplayer.billing.model.ProductDetailsWrapper
import com.eco.musicplayer.audioplayer.billing.model.SkuDetailsWrapper
import com.eco.musicplayer.audioplayer.constants.ConstantsProductID
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_LIFETIME
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_MONTH
import com.eco.musicplayer.audioplayer.constants.PRODUCT_ID_YEAR
import com.eco.musicplayer.audioplayer.music.R

fun PaywallActivity.loadSubsPolicyContent() {
    Log.d("TAG", "loadSubsPolicyContent:1111")
    setupLimitedVersionText()
    setupTermsAndPrivacyText()
}

fun PaywallActivity.loadPriceUI(products: List<BaseProductDetails>) {
    detailsMap[1] = products.find { it.productId == item1ProductId }
    detailsMap[2] = products.find { it.productId == item2ProductId }
    detailsMap[3] = products.find { it.productId == item3ProductId }
    Log.d("TAG", "loadPriceUI: detailsMap=$detailsMap, purchasedProducts=$purchasedProducts")
    if (detailsMap.isNotEmpty() && selectPosition == 0 && purchasedProducts.isEmpty()) {
        selectPosition = 1
        Log.d("TAG", "loadPriceUI: Setting selectPosition=1 (monthly)")
        updateItem()
        updatePlanSelectionBasedOnPurchases() // Cập nhật lại UI sau khi chọn monthly
    }
    setupPlanTexts(detailsMap)
}

fun PaywallActivity.setOnClicks() {
    binding.btnMonthly.setOnClickListener {
        selectPosition = 1
        updateItem()
        updatePlanSelectionBasedOnPurchases()
    }
    binding.btnYearly.setOnClickListener {
        selectPosition = 2
        updateItem()
        updatePlanSelectionBasedOnPurchases()
    }
    binding.btnLifetime.setOnClickListener {
        selectPosition = 3
        updateItem()
        updatePlanSelectionBasedOnPurchases()
    }
    binding.btnStartFreeTrial.setOnClickListener {
        Log.d("TAG", "setOnClicks: btnStartFreeTrial clicked")
        detailsMap[selectPosition]?.let { product ->
            val currentSubscription =
                purchasedProducts.firstOrNull { it in ConstantsProductID.subsListProduct }
            if (currentSubscription != null && product.productId in ConstantsProductID.subsListProduct) {
                // Gọi nâng cấp nếu đang có gói đăng ký
                inAppBillingManager.launchBillingFlowForUpgrade(this, product, currentSubscription)
            } else {
                // Gọi mua mới nếu chưa có gói hoặc chọn gói lifetime
                inAppBillingManager.launchPurchase(this, product)
            }
        }
    }
}

fun PaywallActivity.updateItem() {
    updateSelectedPlanUi()
}

private fun PaywallActivity.setupPlanTexts(detailsMap: Map<Int, BaseProductDetails?>) {
    listOf(
        1 to binding.btnMonthly,
        2 to binding.btnYearly,
        3 to binding.btnLifetime
    ).forEach { (position, button) ->
        detailsMap[position]?.let {
            button.setupPlanButton(
                this,
                it,
                it.productId,
                position == selectPosition
            )
        }
            ?: showToast("Không tìm thấy thông tin gói cho vị trí $position")
    }
}

private fun ViewGroup.setupPlanButton(
    activity: PaywallActivity,
    plan: BaseProductDetails,
    productId: String,
    isSelected: Boolean
) {
    val tv2 = findViewById<TextView>(R.id.tv2).apply { text = activity.getTitleText(productId) }
    val tv3 = findViewById<TextView>(R.id.tv3)
    val tv4 = findViewById<TextView>(R.id.tv4)
    val tv5 = findViewById<TextView>(R.id.tv5)

    when (plan) {
        is ProductDetailsWrapper -> activity.setupProductDetails(plan, productId, tv3, tv4, tv5)
        is SkuDetailsWrapper -> activity.setupSkuDetails(plan, productId, tv3, tv4, tv5)
    }

    updateSelection(isSelected)
    if (isSelected) {
        activity.binding.tvSub.apply {
            text = activity.getSubscriptionSummary(plan, productId)
            visibility = View.VISIBLE
        }
    }
}

private fun PaywallActivity.setupProductDetails(
    plan: ProductDetailsWrapper,
    productId: String,
    tv3: TextView,
    tv4: TextView,
    tv5: TextView
) {
    val productDetails = plan.productDetails
    if (productId == PRODUCT_ID_LIFETIME) {
        productDetails.oneTimePurchaseOfferDetails?.let { offer ->
            tv3.setVisibleText("Thanh toán một lần")
            tv4.text = offer.formattedPrice
            tv5.text = "trọn đời"
        } ?: setupEmptyLifetime(tv3, tv4, tv5)
    } else {
        val firstOffer = productDetails.subscriptionOfferDetails?.firstOrNull()
        val lastOffer = productDetails.subscriptionOfferDetails?.lastOrNull()

        firstOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.let { phase ->
            val periodText = parsePeriodToReadableText(phase.billingPeriod)
            tv3.setVisibleText(
                when {
                    phase.priceAmountMicros == 0L -> "Miễn phí"
                    periodText != getString(R.string.unclear) -> "${phase.formattedPrice}/$periodText"
                    else -> getString(R.string.unknown_price)
                }
            )
        } ?: tv3.setVisibleText("N/A")

        lastOffer?.pricingPhases?.pricingPhaseList?.firstOrNull { it.priceAmountMicros > 0 }
            ?.let { phase ->
                val periodText = parsePeriodToReadableText(phase.billingPeriod)
                tv4.text = phase.formattedPrice
                tv5.text =
                    if (periodText != getString(R.string.unclear)) periodText else getString(R.string.unclear)
            } ?: setupEmptySubscription(tv4, tv5)
    }
}

private fun PaywallActivity.setupSkuDetails(
    plan: SkuDetailsWrapper,
    productId: String,
    tv3: TextView,
    tv4: TextView,
    tv5: TextView
) {
    val skuDetails = plan.skuDetails
    when {
        skuDetails.freeTrialPeriod.isNotEmpty() -> {
            tv3.setVisibleText("Miễn phí ${parsePeriodToReadableText(skuDetails.freeTrialPeriod)}")
            tv4.text = skuDetails.price
            tv5.text = parsePeriodToReadableText(skuDetails.subscriptionPeriod)
        }

        skuDetails.introductoryPrice.isNotEmpty() -> {
            tv3.setVisibleText(
                "${skuDetails.introductoryPrice}/${
                    parsePeriodToReadableText(
                        skuDetails.subscriptionPeriod
                    )
                }"
            )
            tv4.text = skuDetails.price
            tv5.text = parsePeriodToReadableText(skuDetails.subscriptionPeriod)
        }

        else -> {
            tv3.visibility = View.GONE
            tv4.text = skuDetails.price
            tv5.text =
                if (productId == PRODUCT_ID_LIFETIME) "trọn đời" else parsePeriodToReadableText(
                    skuDetails.subscriptionPeriod
                )
        }
    }
}

private fun PaywallActivity.getSubscriptionSummary(
    plan: BaseProductDetails,
    productId: String
): String {
    return when (plan) {
        is ProductDetailsWrapper -> isProductDetails(plan, productId)
        is SkuDetailsWrapper -> isSkuDetails(plan, productId)
    }
}

private fun PaywallActivity.isProductDetails(
    plan: ProductDetailsWrapper,
    productId: String
): String {
    val productDetails = plan.productDetails
    if (productId == PRODUCT_ID_LIFETIME) {
        return productDetails.oneTimePurchaseOfferDetails?.formattedPrice
            ?.let { getString(R.string.onetime_purchases, it) }
            ?: getString(R.string.na_value)
    }

    val firstOffer = productDetails.subscriptionOfferDetails?.firstOrNull()
    val lastOffer = productDetails.subscriptionOfferDetails?.lastOrNull()

    val firstPhase = firstOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()
    val lastPhase =
        lastOffer?.pricingPhases?.pricingPhaseList?.firstOrNull { it.priceAmountMicros > 0 }

    return when {
        firstPhase != null && lastPhase != null -> {
            val firstPeriod = parsePeriodToReadableText(firstPhase.billingPeriod)
            val lastPeriod = parsePeriodToReadableText(lastPhase.billingPeriod)
            val firstPrice =
                if (firstPhase.priceAmountMicros == 0L) "Miễn phí" else firstPhase.formattedPrice
            if (firstPeriod != getString(R.string.unclear) && lastPeriod != getString(R.string.unclear)) {
                "$firstPrice/$firstPeriod đầu tiên, sau đó ${lastPhase.formattedPrice}/$lastPeriod"
            } else {
                getString(R.string.unknown_price)
            }
        }

        lastPhase != null -> {
            val lastPeriod = parsePeriodToReadableText(lastPhase.billingPeriod)
            if (lastPeriod != getString(R.string.unclear)) "${lastPhase.formattedPrice}/$lastPeriod"
            else getString(R.string.unknown_price)
        }

        else -> getString(R.string.na_value)
    }
}

private fun PaywallActivity.isSkuDetails(plan: SkuDetailsWrapper, productId: String): String {
    val skuDetails = plan.skuDetails

    return when {
        skuDetails.freeTrialPeriod.isNotEmpty() -> {
            "Miễn phí ${parsePeriodToReadableText(skuDetails.freeTrialPeriod)}, sau đó ${skuDetails.price}/${
                parsePeriodToReadableText(
                    skuDetails.subscriptionPeriod
                )
            }"
        }

        skuDetails.introductoryPrice.isNotEmpty() -> {
            "${skuDetails.introductoryPrice}/${parsePeriodToReadableText(skuDetails.subscriptionPeriod)}, sau đó ${skuDetails.price}/${
                parsePeriodToReadableText(
                    skuDetails.subscriptionPeriod
                )
            }"
        }

        else -> {
            if (productId == PRODUCT_ID_LIFETIME)
                getString(R.string.onetime_purchases, skuDetails.price)
            else
                "${skuDetails.price}/${parsePeriodToReadableText(skuDetails.subscriptionPeriod)}"
        }
    }
}

private fun PaywallActivity.updateSelectedPlanUi() {
    listOf(
        1 to binding.btnMonthly,
        2 to binding.btnYearly,
        3 to binding.btnLifetime
    ).forEach { (position, button) ->
        Log.d("TAG", "updateSelectedPlanUi: $position")
        if (purchasedProducts.contains(detailsMap[position]?.productId)) return
        val isSelected = position == selectPosition
        button.updateSelection(isSelected)
        if (isSelected) {
            detailsMap[position]?.let { plan ->
                binding.tvSub.apply {
                    text = getSubscriptionSummary(plan, plan.productId)
                    visibility = View.VISIBLE
                }
            }
        }
    }
}

fun PaywallActivity.updatePlanSelectionBasedOnPurchases() {
    val buttons = listOf(
        binding.btnMonthly to PRODUCT_ID_MONTH,
        binding.btnYearly to PRODUCT_ID_YEAR,
        binding.btnLifetime to PRODUCT_ID_LIFETIME
    )

    if (purchasedProducts.contains(PRODUCT_ID_LIFETIME)) {
        // Disable tất cả các nút (gói lifetime, tháng, năm) khi đã mua gói lifetime
        buttons.forEach { (button, id) ->
            button.disablePurchasedButton()
            if (id == PRODUCT_ID_LIFETIME) {
                updatePurchasedBadge(id)
            }
        }
        binding.btnStartFreeTrial.apply {
            text = getString(R.string._continue)
            setOnClickListener {
                showToast("Open Activity 2")
            }
        }
        Log.d(
            "TAG",
            "updatePlanSelectionBasedOnPurchases: Lifetime purchased, button text set to Đã mua"
        )
        return
    }

    val currentSubscription =
        purchasedProducts.firstOrNull { it in ConstantsProductID.subsListProduct }

    // Theo dõi xem có nút nào được chọn không
    var anyButtonSelected = false

    buttons.forEach { (button, productId) ->
        when {
            purchasedProducts.contains(productId) -> {
                button.disablePurchasedButton()
                updatePurchasedBadge(productId)
            }

            else -> {
                val buttonPosition = when (productId) {
                    PRODUCT_ID_MONTH -> 1
                    PRODUCT_ID_YEAR -> 2
                    PRODUCT_ID_LIFETIME -> 3
                    else -> 0
                }

                val isSelected = selectPosition == buttonPosition
                button.updateSelection(isSelected)

                if (isSelected) {
                    anyButtonSelected = true
                    // Cập nhật văn bản tvSub cho gói đã chọn
                    detailsMap[buttonPosition]?.let { plan ->
                        binding.tvSub.apply {
                            text = getSubscriptionSummary(plan, plan.productId)
                            visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    // Nếu không có nút nào được chọn mà vẫn muốn phải chọn một nút, hãy tìm nút đủ điều kiện đầu tiên
    if (!anyButtonSelected && selectPosition != 0) {
        // Tìm gói đầu tiên chưa mua và chọn gói đó
        val firstAvailablePosition = buttons.indexOfFirst { (_, productId) ->
            !purchasedProducts.contains(productId)
        } + 1

        if (firstAvailablePosition > 0) {
            selectPosition = firstAvailablePosition
            // Cập nhật UI cho vị trí mới được chọn
            detailsMap[selectPosition]?.let { plan ->
                binding.tvSub.apply {
                    text = getSubscriptionSummary(plan, plan.productId)
                    visibility = View.VISIBLE
                }
            }

            // Cập nhật lựa chọn nút
            buttons.forEachIndexed { index, (button, productId) ->
                if (!purchasedProducts.contains(productId)) {
                    button.updateSelection(index + 1 == selectPosition)
                }
            }
        }
    }

    // Cập nhật text cho btnStartFreeTrial dựa trên gói được chọn
    binding.btnStartFreeTrial.text = getButtonText(currentSubscription)
    Log.d(
        "TAG",
        "updatePlanSelectionBasedOnPurchases: Button text set to ${binding.btnStartFreeTrial.text}, hasFreeTrial=${hasFreeTrial()}, selectPosition=$selectPosition"
    )
}


// Hàm xác định text cho btnStartFreeTrial
private fun PaywallActivity.getButtonText(currentSubscription: String?): String {
    val selectedProduct = detailsMap[selectPosition]?.productId
    Log.d("TAG", "getButtonText: selectedProduct=$selectedProduct, currentSubscription=$currentSubscription"
    )

    // Nếu selectPosition không hợp lệ, trả về text mặc định
    if (selectedProduct == null || selectPosition == 0) {
        Log.d("TAG", "getButtonText: Invalid selectPosition or no product selected, returning 'Mua'")
        return getString(R.string.buy)
    }

    // Ưu tiên kiểm tra free trial trước
    if (hasFreeTrial()) {
        Log.d("TAG", "getButtonText: Free trial detected for product $selectedProduct")
        return getString(R.string.start_free_trial)
    }


    // Nếu đã có gói subscription
    if (currentSubscription != null) {
        val currentLevel = getSubscriptionLevel(currentSubscription)
        val selectedProductId = detailsMap[selectPosition]?.productId ?: ""
        val newLevel = getSubscriptionLevel(selectedProductId)

        return when {
            newLevel > currentLevel -> getString(R.string.upgrade)
           else -> getString(R.string.downgrade)
        }
    }


    // Nếu không có subscription, hiển thị "Mua" cho cả lifetime và subscription không có free trial
    Log.d("TAG", "getButtonText: No subscription, returning 'Mua' for product $selectedProduct")
    return getString(R.string.buy)
}

// Kiểm tra xem gói được chọn có free trial hay không
private fun PaywallActivity.hasFreeTrial(): Boolean {
    val plan = detailsMap[selectPosition]
    if (plan == null) {
        Log.d("TAG", "hasFreeTrial: No plan found for selectPosition=$selectPosition")
        return false
    }

    return when (plan) {
        is ProductDetailsWrapper -> {
            val hasFreeTrial = plan.productDetails.subscriptionOfferDetails
                ?.firstOrNull()?.pricingPhases?.pricingPhaseList
                ?.firstOrNull()?.priceAmountMicros == 0L
            Log.d(
                "TAG",
                "hasFreeTrial: ProductDetailsWrapper, hasFreeTrial=$hasFreeTrial, productId=${plan.productId}"
            )
            hasFreeTrial
        }

        is SkuDetailsWrapper -> {
            val hasFreeTrial = plan.skuDetails.freeTrialPeriod.isNotEmpty()
            Log.d(
                "TAG",
                "hasFreeTrial: SkuDetailsWrapper, hasFreeTrial=$hasFreeTrial, productId=${plan.productId}"
            )
            hasFreeTrial
        }
    }
}

private fun PaywallActivity.updatePurchasedBadge(productId: String) {
    when (productId) {
        PRODUCT_ID_MONTH -> binding.bestDeal.updatePurchasedBadge()
        PRODUCT_ID_YEAR -> binding.bestDeal2.updatePurchasedBadge()
        PRODUCT_ID_LIFETIME -> binding.bestDeal3.updatePurchasedBadge()
    }
}

private fun getSubscriptionLevel(productId: String) = when (productId) {
    PRODUCT_ID_MONTH -> 1
    PRODUCT_ID_YEAR -> 2
    PRODUCT_ID_LIFETIME -> 3
    else -> 0
}

private fun PaywallActivity.getTitleText(billingTitle: String) = when {
    billingTitle.contains(PRODUCT_ID_MONTH, true) -> getString(R.string.sub_week)
    billingTitle.contains(PRODUCT_ID_YEAR, true) -> getString(R.string.sub_year)
    billingTitle.contains(PRODUCT_ID_LIFETIME, true) -> getString(R.string.life_time_vie)
    else -> billingTitle
}

private fun PaywallActivity.parsePeriodToReadableText(period: String): String {
    if (period.isEmpty()) return getString(R.string.unclear)
    return when {
        period.contains("D") -> "${period.filter { it.isDigit() }} ${getString(R.string.text_day)}"
        period.contains("W") -> "${period.filter { it.isDigit() }} ${getString(R.string.text_week)}"
        period.contains("M") -> "${period.filter { it.isDigit() }} ${getString(R.string.text_month)}"
        period.contains("Y") -> "${period.filter { it.isDigit() }} ${getString(R.string.text_year)}"
        else -> getString(R.string.unclear)
    }
}

private fun PaywallActivity.setupLimitedVersionText() {
    binding.tv6.apply {
        text = SpannableString(getString(R.string.using_limited)).apply {
            setSpan(
                createClickableSpan { showToast("Sử dụng phiên bản giới hạn") },
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        movementMethod = LinkMovementMethod.getInstance()
    }
}

private fun PaywallActivity.setupTermsAndPrivacyText() {
    val fullText = getString(R.string.full_text)
    val spannable = SpannableString(fullText).apply {
        setClickableText(fullText, getString(R.string.terms)) { showToast("Đã nhấn Điều khoản") }
        setClickableText(
            fullText,
            getString(R.string.privacy_policies)
        ) { showToast("Đã nhấn Chính sách bảo mật") }
    }
    binding.tv7.apply {
        text = spannable
        movementMethod = LinkMovementMethod.getInstance()
        highlightColor = Color.TRANSPARENT
    }
}

private fun SpannableString.setClickableText(
    fullText: String,
    keyword: String,
    onClick: () -> Unit
) {
    val start = fullText.indexOf(keyword)
    if (start >= 0) {
        setSpan(
            createClickableSpanGray(onClick),
            start,
            start + keyword.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}

private fun PaywallActivity.createClickableSpan(onClick: () -> Unit) =
    object : android.text.style.ClickableSpan() {
        override fun onClick(widget: View) = onClick()
        override fun updateDrawState(ds: TextPaint) {
            ds.color = Color.parseColor("#C81212")
            ds.isUnderlineText = true
        }
    }

private fun createClickableSpanGray(onClick: () -> Unit) =
    object : android.text.style.ClickableSpan() {
        override fun onClick(widget: View) = onClick()
        override fun updateDrawState(ds: TextPaint) {
            ds.color = Color.parseColor("#9E9E9E")
            ds.isUnderlineText = true
        }
    }

private fun ViewGroup.disablePurchasedButton() {
    isEnabled = false
    findViewById<AppCompatImageView>(R.id.radioButton1).setImageResource(R.drawable.ic_uncheck)
    setBackgroundResource(R.drawable.bg_disable)
    alpha = 0.8f
}

private fun ViewGroup.updateSelection(isSelected: Boolean) {
    setBackgroundResource(if (isSelected) R.drawable.bg_selected_paywall else R.drawable.bg_unselected_paywall)
    findViewById<AppCompatImageView>(R.id.radioButton1).setImageResource(if (isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
    isEnabled = true
    alpha = 1.0f
}

private fun View.disable() {
    isEnabled = false
    alpha = 0.5f
}

private fun TextView.updatePurchasedBadge() {
    text = (context as PaywallActivity).getString(R.string.purchased)
    setBackgroundResource(R.drawable.bg_disable)
    visibility = View.VISIBLE
}

private fun TextView.setVisibleText(text: String) {
    this.text = text
    visibility = View.VISIBLE
}

private fun PaywallActivity.setupEmptyLifetime(tv3: TextView, tv4: TextView, tv5: TextView) {
    tv3.visibility = View.GONE
    tv4.text = ""
    tv5.text = getString(R.string.life_time_vie)
}

private fun PaywallActivity.setupEmptySubscription(tv4: TextView, tv5: TextView) {
    tv4.text = ""
    tv5.text = getString(R.string.unclear)
}

private fun PaywallActivity.showToast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()