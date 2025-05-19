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
    setupLimitedVersionText()
    setupTermsAndPrivacyText()
}

fun PaywallActivity.loadPriceUI(products: List<BaseProductDetails>) {
    detailsMap[1] = products.find { it.productId == item1ProductId }
    detailsMap[2] = products.find { it.productId == item2ProductId }
    detailsMap[3] = products.find { it.productId == item3ProductId }
    if (detailsMap.isNotEmpty() && selectPosition == 0 && purchasedProducts.isEmpty()) {
        selectPosition = 1
        updateItem()
    }
    setupPlanTexts(detailsMap)
}

fun PaywallActivity.updatePurchases(purchases: List<BaseProductDetails>) {
    purchasedProducts.clear() // Xóa danh sách cũ để tránh trùng lặp
    purchases.forEach { purchasedProducts.add(it.productId) }
    updatePlanSelectionBasedOnPurchases()
    updateItem() // Gọi để cập nhật UI sau khi thay đổi purchasedProducts
}

fun PaywallActivity.setOnClicks() {
    binding.btnMonthly.setOnClickListener {
        selectPosition = 1
        updateItem()
    }
    binding.btnYearly.setOnClickListener {
        selectPosition = 2
        updateItem()
    }
    binding.btnLifetime.setOnClickListener {
        selectPosition = 3
        updateItem()
    }
    binding.btnStartFreeTrial.setOnClickListener {
        Log.d("TAG", "setOnClicks: btnStartFreeTrial clicked")
        detailsMap[selectPosition]?.let { product ->
            val currentSubscription = purchasedProducts.firstOrNull { it in ConstantsProductID.subsListProduct }
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
        detailsMap[position]?.let { button.setupPlanButton(this, it, it.productId, position == selectPosition) }
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
            val periodText = parsePeriodToReadableText(phase.billingPeriod ?: "")
            tv3.setVisibleText(
                when {
                    phase.priceAmountMicros == 0L -> "Miễn phí"
                    periodText != getString(R.string.unclear) -> "${phase.formattedPrice}/$periodText"
                    else -> getString(R.string.unknown_price)
                }
            )
        } ?: tv3.setVisibleText("N/A")

        lastOffer?.pricingPhases?.pricingPhaseList?.firstOrNull { it.priceAmountMicros > 0 }?.let { phase ->
            val periodText = parsePeriodToReadableText(phase.billingPeriod ?: "")
            tv4.text = phase.formattedPrice
            tv5.text = if (periodText != getString(R.string.unclear)) periodText else getString(R.string.unclear)
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
            tv3.setVisibleText("${skuDetails.introductoryPrice}/${parsePeriodToReadableText(skuDetails.subscriptionPeriod)}")
            tv4.text = skuDetails.price
            tv5.text = parsePeriodToReadableText(skuDetails.subscriptionPeriod)
        }
        else -> {
            tv3.visibility = View.GONE
            tv4.text = skuDetails.price
            tv5.text = if (productId == PRODUCT_ID_LIFETIME) "trọn đời" else parsePeriodToReadableText(skuDetails.subscriptionPeriod)
        }
    }
}

private fun PaywallActivity.getSubscriptionSummary(plan: BaseProductDetails, productId: String): String {
    return when (plan) {
        is ProductDetailsWrapper -> isProductDetails(plan, productId)

        is SkuDetailsWrapper -> isSkuDetails(plan, productId)
    }
}

private fun PaywallActivity.isProductDetails(plan: ProductDetailsWrapper, productId: String): String {
    val productDetails = plan.productDetails
    if (productId == PRODUCT_ID_LIFETIME) {
        return productDetails.oneTimePurchaseOfferDetails?.formattedPrice
            ?.let { getString(R.string.onetime_purchases, it) }
            ?: getString(R.string.na_value)
    }

    val firstOffer = productDetails.subscriptionOfferDetails?.firstOrNull()
    val lastOffer = productDetails.subscriptionOfferDetails?.lastOrNull()

    val firstPhase = firstOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()
    val lastPhase = lastOffer?.pricingPhases?.pricingPhaseList?.firstOrNull { it.priceAmountMicros > 0 }

    return when {
        firstPhase != null && lastPhase != null -> {
            val firstPeriod = parsePeriodToReadableText(firstPhase.billingPeriod)
            val lastPeriod = parsePeriodToReadableText(lastPhase.billingPeriod)
            val firstPrice = if (firstPhase.priceAmountMicros == 0L) "Miễn phí" else firstPhase.formattedPrice
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
            "Miễn phí ${parsePeriodToReadableText(skuDetails.freeTrialPeriod)}, sau đó ${skuDetails.price}/${parsePeriodToReadableText(skuDetails.subscriptionPeriod)}"
        }
        skuDetails.introductoryPrice.isNotEmpty() -> {
            "${skuDetails.introductoryPrice}/${parsePeriodToReadableText(skuDetails.subscriptionPeriod)}, sau đó ${skuDetails.price}/${parsePeriodToReadableText(skuDetails.subscriptionPeriod)}"
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
        if (purchasedProducts.contains(detailsMap[position]?.productId)) return@forEach
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
        buttons.forEach { (button, id) ->
            if (id == PRODUCT_ID_LIFETIME) button.disablePurchasedButton() else button.disable()
        }
        binding.btnStartFreeTrial.apply {
            isEnabled = false
            text = getString(R.string.purchased)
        }
        return
    }

    val currentSubscription = purchasedProducts.firstOrNull { it in ConstantsProductID.subsListProduct }

    buttons.forEach { (button, productId) ->
        when {
            purchasedProducts.contains(productId) -> {
                button.disablePurchasedButton()
                updatePurchasedBadge(productId)
            }
            currentSubscription != null && getSubscriptionLevel(productId) <= getSubscriptionLevel(currentSubscription) -> {
                button.disable()
            }
            else -> {
                button.updateSelection(selectPosition == when (productId) {
                    PRODUCT_ID_MONTH -> 1
                    PRODUCT_ID_YEAR -> 2
                    PRODUCT_ID_LIFETIME -> 3
                    else -> 0
                })
            }
        }
    }

    binding.btnStartFreeTrial.text = if (currentSubscription != null) getString(R.string.upgrade) else getString(R.string.free_trial)
}

private fun PaywallActivity.updatePurchasedBadge(productId: String) {
    when (productId) {
        PRODUCT_ID_MONTH -> binding.bestDeal.updatePurchasedBadge()
        PRODUCT_ID_YEAR -> binding.bestDeal2.updatePurchasedBadge()
        PRODUCT_ID_LIFETIME -> binding.bestDeal3.updatePurchasedBadge()
    }
}

private fun PaywallActivity.getSubscriptionLevel(productId: String) = when (productId) {
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
            setSpan(createClickableSpan { showToast("Sử dụng phiên bản giới hạn") }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        movementMethod = LinkMovementMethod.getInstance()
    }
}

private fun PaywallActivity.setupTermsAndPrivacyText() {
    val fullText = getString(R.string.full_text)
    val spannable = SpannableString(fullText).apply {
        setClickableText(fullText, getString(R.string.terms)) { showToast("Đã nhấn Điều khoản") }
        setClickableText(fullText, getString(R.string.privacy_policies)) { showToast("Đã nhấn Chính sách bảo mật") }
    }
    binding.tv7.apply {
        text = spannable
        movementMethod = LinkMovementMethod.getInstance()
        highlightColor = Color.TRANSPARENT
    }
}

private fun SpannableString.setClickableText(fullText: String, keyword: String, onClick: () -> Unit) {
    val start = fullText.indexOf(keyword)
    if (start >= 0) {
        setSpan(createClickableSpanGray(onClick), start, start + keyword.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

private fun PaywallActivity.createClickableSpan(onClick: () -> Unit) = object : android.text.style.ClickableSpan() {
    override fun onClick(widget: View) = onClick()
    override fun updateDrawState(ds: TextPaint) {
        ds.color = Color.parseColor("#C81212")
        ds.isUnderlineText = true
    }
}

private fun createClickableSpanGray(onClick: () -> Unit) = object : android.text.style.ClickableSpan() {
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

private fun PaywallActivity.showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()