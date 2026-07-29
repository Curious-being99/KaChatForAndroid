package com.kachat.app.models

/** Which block explorer website transaction links open in — user picks in Settings > Kaspa Explorer. */
enum class KaspaExplorer(
    val displayName: String,
    private val txBaseUrl: String,
    private val addressBaseUrl: String
) {
    KASPA_STREAM("kaspa.stream", "https://kaspa.stream/transactions/", "https://kaspa.stream/addresses/"),
    KASPA_ORG("explorer.kaspa.org", "https://explorer.kaspa.org/txs/", "https://explorer.kaspa.org/addresses/");

    fun txUrl(txId: String): String = "$txBaseUrl$txId"
    fun addressUrl(address: String): String = "$addressBaseUrl$address"

    companion object {
        val default: KaspaExplorer = KASPA_ORG

        fun fromName(name: String?): KaspaExplorer =
            entries.firstOrNull { it.name == name } ?: default
    }
}
