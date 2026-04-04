package com.relicrush.game.monetization

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.relicrush.game.utils.GameConstants

class BillingManager(
    private val activity: Activity,
    private val listener: Listener
) : PurchasesUpdatedListener {

    interface Listener {
        fun onRemoveAdsPurchased()
        fun onCoinPackPurchased(amount: Int)
        fun onStoreMessage(message: String)
    }

    private val productDetails = mutableMapOf<String, ProductDetails>()

    private val billingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private var ready = false

    fun start() {
        if (ready) {
            refreshCatalog()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                ready = result.responseCode == BillingClient.BillingResponseCode.OK
                if (ready) {
                    refreshCatalog()
                    restorePurchases()
                } else {
                    listener.onStoreMessage("Store connection unavailable.")
                }
            }

            override fun onBillingServiceDisconnected() {
                ready = false
            }
        })
    }

    fun refreshCatalog() {
        if (!ready) {
            return
        }

        val products = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(GameConstants.PRODUCT_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(GameConstants.PRODUCT_COIN_PACK)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                listener.onStoreMessage("Store catalog is not ready yet.")
                return@queryProductDetailsAsync
            }

            productDetails.clear()
            detailsResult.productDetailsList
                .forEach { details -> productDetails[details.productId] = details }
        }
    }

    fun launchPurchase(productId: String): Boolean {
        if (!ready) {
            listener.onStoreMessage("Store is still connecting.")
            start()
            return false
        }

        val details = productDetails[productId]
        if (details == null) {
            refreshCatalog()
            listener.onStoreMessage("Product data is still loading.")
            return false
        }

        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()

        billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(params))
                .build()
        )
        return true
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) {
            if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                listener.onStoreMessage("Purchase did not complete.")
            }
            return
        }

        purchases.forEach(::handlePurchase)
    }

    private fun restorePurchases() {
        if (!ready) {
            return
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach(::handlePurchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return
        }

        val productId = purchase.products.firstOrNull() ?: return
        when (productId) {
            GameConstants.PRODUCT_REMOVE_ADS -> acknowledgeNonConsumable(purchase)
            GameConstants.PRODUCT_COIN_PACK -> consumeCoinPack(purchase)
        }
    }

    private fun acknowledgeNonConsumable(purchase: Purchase) {
        if (purchase.isAcknowledged) {
            listener.onRemoveAdsPurchased()
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                listener.onRemoveAdsPurchased()
                listener.onStoreMessage("Ads removed permanently.")
            }
        }
    }

    private fun consumeCoinPack(purchase: Purchase) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(params) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                listener.onCoinPackPurchased(GameConstants.COIN_PACK_REWARD)
                listener.onStoreMessage("Coin pack delivered.")
            }
        }
    }

    fun end() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
