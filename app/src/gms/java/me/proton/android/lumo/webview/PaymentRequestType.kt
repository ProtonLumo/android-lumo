package me.proton.android.lumo.webview

enum class PaymentRequestType(val functionName: String) {
    PAYMENT_TOKEN("postPaymentToken"),
    SUBSCRIPTION("postSubscription"),
    GET_PLANS("getPlans"),
    GET_SUBSCRIPTIONS("getSubscriptions")
}
