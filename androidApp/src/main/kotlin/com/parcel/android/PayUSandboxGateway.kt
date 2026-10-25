package com.parcel.android

import android.app.Activity
import android.webkit.WebView
import com.parcel.domain.*
import com.payu.base.models.PayUPaymentParams
import com.payu.base.models.ErrorResponse
import com.payu.checkoutpro.PayUCheckoutPro
import com.payu.checkoutpro.utils.PayUCheckoutProConstants
import com.payu.ui.model.listeners.PayUCheckoutProListener
import com.payu.ui.model.listeners.PayUHashGenerationListener
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.lang.ref.WeakReference
import java.net.URI
import java.math.BigDecimal

/** Android-only SDK launcher. The merchant salt and hash generation belong to the server. */
class PayUSandboxGateway(private val baseUrl: String) : SandboxPaymentGateway {
    private var host: WeakReference<Activity>? = null
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        followRedirects = false
        install(HttpTimeout) { requestTimeoutMillis = 20_000; connectTimeoutMillis = 10_000 }
    }
    override val configured: Boolean = baseUrl.isNotBlank() && validBaseUrl(baseUrl)
    fun attach(activity: Activity) { host = WeakReference(activity) }
    fun detach(activity: Activity) { if (host?.get() === activity) host = null }
    private fun validBaseUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.userInfo == null && uri.query == null && uri.fragment == null && !uri.host.isNullOrBlank() &&
            (uri.scheme == "https" || (BuildConfig.DEBUG && uri.scheme == "http" && uri.host in listOf("10.0.2.2", "localhost")))
    }.getOrDefault(false)
    private suspend fun post(path: String, body: String, token: String? = null): JsonObject {
        check(configured) { "PayU sandbox backend is not configured." }
        val response = client.post("${baseUrl.trimEnd('/')}$path") {
            contentType(ContentType.Application.Json)
            if (token != null) bearerAuth(token)
            setBody(body)
        }
        return json.parseToJsonElement(response.bodyAsText()).jsonObject
    }
    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
    private suspend fun session(checkout: PendingCheckout): JsonObject {
        val result = post("/v1/payu/checkouts", json.encodeToString(checkout))
        check(result.string("environment") == "test" && result.string("checkoutId") == checkout.id)
        check(BigDecimal(result.string("amount")).movePointRight(2).longValueExact() == checkout.totals.total.minorUnits)
        check(result.string("currency") == "INR")
        check(result.string("key").isNotBlank())
        check(listOf("surl", "furl").all { key ->
            val uri = URI(result.string(key)); uri.scheme == "https" && !uri.host.isNullOrBlank()
        }) { "The sandbox server must supply HTTPS callback URLs." }
        return result
    }
    override suspend fun pay(checkout: PendingCheckout): PaymentResult {
        if (!configured) return PaymentResult.Failed("PayU sandbox is not configured.")
        var sdkLaunched = false
        var cancelledBeforeInitiation = false
        return try {
            coroutineScope {
                val session = session(checkout)
                val callback = CompletableDeferred<Unit>()
                withContext(Dispatchers.Main.immediate) {
                    val activity = host?.get()?.takeUnless { it.isFinishing || it.isDestroyed }
                        ?: error("Reopen checkout to launch the PayU sandbox.")
                    val params = PayUPaymentParams.Builder()
                        .setIsProduction(false) // This simulator has no live payment path.
                        .setKey(session.string("key"))
                        .setAmount(session.string("amount"))
                        .setProductInfo(session.string("productInfo"))
                        .setTransactionId(session.string("transactionId"))
                        .setFirstName(session.string("firstName"))
                        .setEmail(session.string("email"))
                        .setPhone(session.string("phone"))
                        .setUserCredential("${session.string("key")}:${session.string("email")}")
                        .setSurl(session.string("surl"))
                        .setFurl(session.string("furl"))
                        .build()
                    sdkLaunched = true
                    PayUCheckoutPro.open(activity, params, object : PayUCheckoutProListener {
                        override fun onPaymentSuccess(response: Any) { callback.complete(Unit) }
                        override fun onPaymentFailure(response: Any) { callback.complete(Unit) }
                        override fun onPaymentCancel(isTxnInitiated: Boolean) { cancelledBeforeInitiation = !isTxnInitiated; callback.complete(Unit) }
                        override fun onError(errorResponse: ErrorResponse) { callback.complete(Unit) }
                        override fun setWebViewProperties(webView: WebView?, bank: Any?) = Unit
                        override fun generateHash(valueMap: HashMap<String, String?>, listener: PayUHashGenerationListener) {
                            launch {
                                val name = valueMap[PayUCheckoutProConstants.CP_HASH_NAME] ?: ""
                                try {
                                    val body = buildJsonObject {
                                        put("checkoutId", checkout.id)
                                        put("sdk", buildJsonObject { valueMap.forEach { (key, value) -> if (value != null) put(key, value) } })
                                    }
                                    val hash = post("/v1/payu/hash", body.toString(), session.string("sessionToken"))
                                    check(hash.string("hashName") == name)
                                    val value = hash.string("hash")
                                    check(value.matches(Regex("[a-fA-F0-9]{40,128}")))
                                    listener.onHashGenerated(hashMapOf(name to value))
                                } catch (cancel: CancellationException) { throw cancel }
                                catch (_: Exception) {
                                    listener.onHashGenerated(hashMapOf())
                                }
                            }
                        }
                    })
                }
                withTimeout(10 * 60_000L) { callback.await() }
                if (cancelledBeforeInitiation) PaymentResult.Cancelled else verify(checkout, session)
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) {
            if (!sdkLaunched) PaymentResult.Failed("Could not start PayU sandbox. Check the backend setup and connection; your cart is unchanged.")
            else PaymentResult.Pending("Could not confirm the test payment. Your cart is preserved. Use Resume / check payment before retrying.")
        }
    }
    override suspend fun reconcile(checkout: PendingCheckout): PaymentResult = try {
        verify(checkout, session(checkout))
    } catch (cancel: CancellationException) { throw cancel }
    catch (_: Exception) { PaymentResult.Pending("Payment verification is unavailable. Your cart and pending checkout are saved.") }

    private suspend fun verify(checkout: PendingCheckout, session: JsonObject): PaymentResult {
        // Never treat an SDK callback as proof of payment. The backend queries PayU.
        val result = post("/v1/payu/verify", buildJsonObject { put("checkoutId", checkout.id) }.toString(), session.string("sessionToken"))
        check(result.string("checkoutId") == checkout.id && result.string("environment") == "test")
        return when (result.string("status")) {
            "verified" -> {
                val amount = result.getValue("amountMinor").jsonPrimitive.long
                check(result.string("currency") == "INR" && amount == checkout.totals.total.minorUnits)
                PaymentResult.Verified(checkout.id, amount)
            }
            "failed" -> PaymentResult.Failed("The sandbox payment failed. Your cart is unchanged.")
            "cancelled" -> PaymentResult.Cancelled
            else -> PaymentResult.Pending("Test payment is pending. Recheck its status before starting another checkout.")
        }
    }
}
