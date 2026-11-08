package com.neilyin.nnowork.data.shop

import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.backup.BackupDateCodec
import com.neilyin.nnowork.data.identity.DeviceIdentityRepository
import com.neilyin.nnowork.data.local.AppDatabase
import com.neilyin.nnowork.data.remote.SUPABASE_ANON_KEY
import com.neilyin.nnowork.data.remote.SUPABASE_URL
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

class ShopPurchaseRepository(
    database: AppDatabase,
    private val deviceIdentityRepository: DeviceIdentityRepository,
    private val accessTokenProvider: () -> String? = { null },
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val dao = database.shopPurchaseDao()
    private val syncMutex = Mutex()

    private val api: ShopPurchaseApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("Content-Type", "application/json")
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .build(),
                )
            }
            .build()
        Retrofit.Builder()
            .baseUrl(SUPABASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(JsonProvider.moshi))
            .build()
            .create(ShopPurchaseApi::class.java)
    }

    /** 重送由 purchase id 防重；單筆失敗不阻塞後續交易。 */
    suspend fun syncPending(): Int = syncMutex.withLock {
        var synced = 0
        dao.getPending().forEach { record ->
            val success = runCatching {
                val request = record.toRequest(deviceIdentityRepository.deviceHash())
                val userToken = accessTokenProvider()
                val first = api.recordPurchase("Bearer ${userToken ?: SUPABASE_ANON_KEY}", request)
                if ((first.code() == 401 || first.code() == 403) && userToken != null) {
                    api.recordPurchase("Bearer $SUPABASE_ANON_KEY", request).isSuccessful
                } else {
                    first.isSuccessful
                }
            }.getOrDefault(false)
            if (success && dao.markSynced(record.id, nowMillis()) > 0) synced++
        }
        synced
    }
}

private interface ShopPurchaseApi {
    @POST("rest/v1/rpc/record_shop_purchase")
    suspend fun recordPurchase(
        @Header("Authorization") authorization: String,
        @Body request: RecordShopPurchaseRequest,
    ): Response<Boolean>
}

@JsonClass(generateAdapter = false)
private data class RecordShopPurchaseRequest(
    @Json(name = "p_purchase_id") val purchaseId: String,
    @Json(name = "p_device_hash") val deviceHash: String,
    @Json(name = "p_product_type") val productType: String,
    @Json(name = "p_product_id") val productId: String,
    @Json(name = "p_product_name") val productName: String,
    @Json(name = "p_quantity") val quantity: Int,
    @Json(name = "p_unit_coin_cost") val unitCoinCost: Int,
    @Json(name = "p_total_coin_cost") val totalCoinCost: Int,
    @Json(name = "p_currency") val currency: String,
    @Json(name = "p_status") val status: String,
    @Json(name = "p_source") val source: String,
    @Json(name = "p_purchased_at") val purchasedAt: String,
)

private fun ShopPurchaseEntity.toRequest(deviceHash: String) = RecordShopPurchaseRequest(
    purchaseId = id,
    deviceHash = deviceHash,
    productType = productType,
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitCoinCost = unitCoinCost,
    totalCoinCost = totalCoinCost,
    currency = currency,
    status = status,
    source = source,
    purchasedAt = BackupDateCodec.format(purchasedAt),
)
