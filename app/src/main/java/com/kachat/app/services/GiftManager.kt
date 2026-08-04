package com.kachat.app.services

import android.content.Context
import android.provider.Settings
import android.util.Base64
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State machine for the one-per-device Kaspa "welcome gift" claim - mirrors iOS's
 * `GiftService.GiftClaimState`. The gift is a server-funded faucet (kachatgift.duckdns.org): the client
 * proves the device is genuine + unclaimed, and the server sends KAS on-chain to [claimGift]'s
 * wallet address. The client never signs or sweeps anything; it just receives a txId.
 */
sealed class GiftClaimState {
    object Checking : GiftClaimState()
    object Eligible : GiftClaimState()
    object Claiming : GiftClaimState()
    data class Claimed(val txId: String) : GiftClaimState()
    object AlreadyClaimed : GiftClaimState()
    data class Unavailable(val reason: String) : GiftClaimState()
}

/** Gift faucet REST API (base url https://kachatgift.duckdns.org/ - see AppModule.provideGiftApi). */
interface GiftApi {
    @GET("gift/challenge")
    suspend fun getChallenge(): GiftChallengeResponse

    @POST("gift/claim")
    suspend fun claim(@Body body: GiftClaimRequest): Response<GiftClaimResponse>
}

data class GiftChallengeResponse(val challenge: String)

/**
 * Android claim payload. Unlike iOS (Apple DeviceCheck + App Attest -> `deviceToken`/`attestation`/
 * `keyId`), Android sends a single Play Integrity [integrityToken]. `platform = "android"` lets the
 * server route to the Play Integrity verifier. The server must be updated to accept this shape.
 */
data class GiftClaimRequest(
    val platform: String = "android",
    val integrityToken: String,
    val walletAddress: String,
    val challenge: String,
    /**
     * Stable per-device pseudonym: base64url(sha256(ANDROID_ID)). Folded into the Play Integrity
     * nonce (see [claimGift]) so the server can trust it came from the genuine app and enforce
     * one-claim-per-device. Survives reinstalls; resets on factory reset / new user profile.
     */
    val deviceId: String
)

data class GiftClaimResponse(val txId: String? = null, val error: String? = null)

@Singleton
class GiftManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val giftApi: GiftApi
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _state = MutableStateFlow<GiftClaimState>(GiftClaimState.Checking)
    val state: StateFlow<GiftClaimState> = _state.asStateFlow()

    /** Local UX cache only (NOT a security boundary - the server's Play Integrity check is the
     *  real one-per-device enforcement, exactly as iOS relies on server-side DeviceCheck). */
    fun checkEligibility() {
        _state.value = if (prefs.getBoolean(CLAIMED_KEY, false)) {
            GiftClaimState.AlreadyClaimed
        } else {
            GiftClaimState.Eligible
        }
    }

    suspend fun claimGift(walletAddress: String) {
        if (_state.value != GiftClaimState.Eligible) return
        _state.value = GiftClaimState.Claiming
        try {
            // 1. One-time challenge from the server.
            val challenge = giftApi.getChallenge().challenge

            // 2. Stable per-device id = base64url(sha256(ANDROID_ID)). Hashing keeps the raw
            //    ANDROID_ID on the device; the server only ever sees the pseudonym.
            @Suppress("HardwareIds")
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
            val deviceId = base64UrlNoPadding(sha256(androidId.toByteArray(Charsets.UTF_8)))

            // 3. Play Integrity token, bound to BOTH the challenge and the deviceId via the nonce.
            //    Binding deviceId here makes it tamper-proof: the server recomputes
            //    sha256("$challenge:$deviceId") and compares it to the nonce inside the signed token,
            //    so a repackaged app can't swap in a different deviceId to re-claim.
            val nonce = base64UrlNoPadding(sha256("$challenge:$deviceId".toByteArray(Charsets.UTF_8)))
            val integrityManager = IntegrityManagerFactory.create(context)
            val tokenResponse = integrityManager.requestIntegrityToken(
                IntegrityTokenRequest.builder()
                    .setNonce(nonce)
                    .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
                    .build()
            ).await()
            val integrityToken = tokenResponse.token()

            // 4. Submit the claim. The server verifies the token, enforces one-per-device, and sends KAS.
            val response = giftApi.claim(
                GiftClaimRequest(
                    integrityToken = integrityToken,
                    walletAddress = walletAddress,
                    challenge = challenge,
                    deviceId = deviceId
                )
            )
            when {
                response.isSuccessful -> {
                    val txId = response.body()?.txId
                    if (txId.isNullOrEmpty()) {
                        _state.value = GiftClaimState.Unavailable("The gift server did not return a transaction.")
                    } else {
                        prefs.edit().putBoolean(CLAIMED_KEY, true).apply()
                        _state.value = GiftClaimState.Claimed(txId)
                    }
                }
                response.code() == 409 -> {
                    prefs.edit().putBoolean(CLAIMED_KEY, true).apply()
                    _state.value = GiftClaimState.AlreadyClaimed
                }
                else -> {
                    val msg = response.errorBody()?.string()?.let { parseError(it) }
                        ?: "Gift claim failed (${response.code()})."
                    _state.value = GiftClaimState.Unavailable(msg)
                }
            }
        } catch (e: Exception) {
            _state.value = GiftClaimState.Unavailable(e.message ?: "Device verification failed.")
        }
    }

    /** Hidden support tool (Profile 10-tap on "already claimed") - clears the local claimed cache so
     *  the gift can be requested again. Real enforcement is still server-side. */
    fun resetClaimStateForRetry() {
        prefs.edit().putBoolean(CLAIMED_KEY, false).apply()
        checkEligibility()
    }

    private fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    private fun base64UrlNoPadding(data: ByteArray): String =
        Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    private fun parseError(body: String): String? =
        try { gson.fromJson(body, GiftClaimResponse::class.java)?.error } catch (e: Exception) { null }

    companion object {
        private const val PREFS_NAME = "gift_prefs"
        private const val CLAIMED_KEY = "kachat_gift_claimed"

        /**
         * This app's Google Cloud project number. The gift server must verify Play Integrity tokens
         * against this same project (Play Integrity API enabled there + the app linked in Play Console).
         */
        const val CLOUD_PROJECT_NUMBER = 1037094663882L
    }
}
