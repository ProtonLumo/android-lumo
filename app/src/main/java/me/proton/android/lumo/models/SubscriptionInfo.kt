package me.proton.android.lumo.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonNames
import timber.log.Timber

/**
 * Custom serializer that handles null Int values by returning a default value of 0
 */
object IntOrDefaultSerializer : KSerializer<Int> {
    private const val TAG = "IntOrDefaultSerializer"

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntOrDefault", PrimitiveKind.INT)

    @Suppress("TooGenericExceptionCaught")
    override fun deserialize(decoder: Decoder): Int {
        return try {
            decoder.decodeInt()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e,"Unable to deserialize")
            0  // default value when null or invalid
        }
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }
}

/**
 * Represents a subscription entitlement feature description
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SubscriptionEntitlement(
    @JsonNames("type", "Type") val type: String,
    @JsonNames("text", "Text") val text: String,
    @JsonNames("iconName", "IconName") val iconName: String,
    @JsonNames("hint", "Hint") val hint: String? = null
)

@Serializable
data class SubscriptionItemResponse(
    @SerialName("ID") val id: String?,
    @SerialName("InvoiceID") val invoiceId: String,
    @Serializable(with = IntOrDefaultSerializer::class)
    @SerialName("Cycle") val cycle: Int = 0,
    @SerialName("PeriodStart") val periodStart: Long,
    @SerialName("PeriodEnd") val periodEnd: Long,
    @SerialName("CreateTime") val createTime: Long,
    @SerialName("CouponCode") val couponCode: String? = null,
    @SerialName("Currency") val currency: String? = null,
    @Serializable(with = IntOrDefaultSerializer::class)
    @SerialName("Amount") val amount: Int,
    @Serializable(with = IntOrDefaultSerializer::class)
    @SerialName("Discount") val discount: Int,
    @Serializable(with = IntOrDefaultSerializer::class)
    @SerialName("RenewDiscount") val renewDiscount: Int,
    @Serializable(with = IntOrDefaultSerializer::class)
    @SerialName("RenewAmount") val renewAmount: Int,
    @Serializable(with = IntOrDefaultSerializer::class)
    @SerialName("Renew") val renew: Int,
    @Serializable(with = IntOrDefaultSerializer::class)
    @SerialName("External") val external: Int,
    @Serializable(with = IntOrDefaultSerializer::class)
    @SerialName("BillingPlatform") val billingPlatform: Int,
    @SerialName("IsTrial") val isTrial: Boolean = false,
    @SerialName("CustomerID") val customerID: String?,
    @SerialName("Title") val title: String? = null,
    @SerialName("Description") val description: String? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("CycleDescription") val cycleDescription: String? = null,
    @SerialName("Offer") val offer: String? = null,
    @SerialName("Entitlements") val entitlements: List<SubscriptionEntitlement>? = null,
    @SerialName("Decorations") val decorations: List<String>? = null,
    @SerialName("Plans") val plans: List<IncludedPlan> = emptyList(),
)

@Serializable
data class SubscriptionsResponse(
    @SerialName("Code") val code: Int,
    @SerialName("Subscriptions") val subscriptions: List<SubscriptionItemResponse>,
    @SerialName("UpcomingSubscriptions") val upcomingSubscriptions: List<SubscriptionItemResponse>? = null,
    @SerialName("uid") val uid: String? = null
)

@Serializable
data class IncludedPlan(
    @SerialName("ID") val id: String,
    @SerialName("Type") val type: Int,
    @SerialName("Name") val name: String,
    @SerialName("Title") val title: String,
    @SerialName("Services") val services: Int,
    @SerialName("Amount") val amount: Int,
    @SerialName("Offer") val offer: String,
    @SerialName("Quantity") val quantity: Int,
)

data class SubscriptionPlan(
    val productId: String,
    val planName: String,
    val durationMonths: Int,
    val description: String = "",
    var price: String = "",
    var formattedPrice: String = "",
    var periodText: String = "",
    var priceAmountMicros: Long = 0
)
