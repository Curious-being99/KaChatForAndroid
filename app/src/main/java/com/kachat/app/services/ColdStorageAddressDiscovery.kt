package com.kachat.app.services

import android.util.Log
import com.kachat.app.util.KaspaExtendedPublicKey
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.bitcoinj.crypto.DeterministicKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gap-limit scan over a kpub's derived addresses — the Cold Storage analogue of
 * [SpendingAddressDiscovery], but for a watch-only public key rather than a locally-held
 * mnemonic, and returning every discovered address with its live balance rather than just a
 * single boundary index, since the Cold Storage detail screen needs to show the whole
 * used-address history, not just "the current one."
 */
@Singleton
class ColdStorageAddressDiscovery @Inject constructor(
    private val networkService: NetworkService
) {
    data class DiscoveredAddress(val index: Int, val address: String, val balanceSompi: Long, val hasHistory: Boolean)

    /**
     * @param chain 0 = external/receive, 1 = internal/change (KaChat only ever sources sends/
     * change from chain 0 for cold storage — see [KaspaExtendedPublicKey] doc).
     */
    suspend fun discoverAddresses(
        rootKey: DeterministicKey,
        chain: Int = 0,
        gapLimit: Int = 5,
        batchSize: Int = 10
    ): List<DiscoveredAddress> = coroutineScope {
        networkService.kaspaRestApi.value ?: return@coroutineScope emptyList()
        val results = mutableListOf<DiscoveredAddress>()
        var consecutiveUnused = 0
        var index = 0

        // Fetch a batch of addresses concurrently instead of one at a time - the gap-limit
        // stopping condition only needs each batch's results in order, not a strictly one-by-one
        // fetch, so this turns what was O(n) sequential network round trips into O(n / batchSize)
        // sequential rounds. A wallet needing 15 addresses to satisfy the gap limit went from 15
        // sequential round trips to 2 batches of 10 - the actual cause of Cold Storage's slow load.
        outer@ while (consecutiveUnused < gapLimit) {
            val batch = (index until index + batchSize).map { i ->
                async { checkAddress(rootKey, chain, i) }
            }
            val batchResults = batch.awaitAll()

            for (result in batchResults) {
                if (result == null) break@outer
                results.add(result)
                consecutiveUnused = if (result.hasHistory || result.balanceSompi > 0) 0 else consecutiveUnused + 1
                index++
                if (consecutiveUnused >= gapLimit) break@outer
            }
        }

        results
    }

    /**
     * One specific address's live balance/history, outside the gap-limit scan — used to pull in
     * an index a user manually generated past the scan's own stopping point (see
     * [com.kachat.app.viewmodels.ColdStorageViewModel.generateMoreAddresses]), which
     * [discoverAddresses] alone would never reach on a fresh unused-account rescan.
     */
    suspend fun checkAddress(rootKey: DeterministicKey, chain: Int, index: Int): DiscoveredAddress? {
        val api = networkService.kaspaRestApi.value ?: return null
        val address = try {
            KaspaExtendedPublicKey.deriveChildAddress(rootKey, chain, index)
        } catch (e: Exception) {
            return null
        }
        // History and balance are two independent single-address lookups — run them
        // concurrently instead of one after the other, roughly halving this address's
        // contribution to the overall (sequential, gap-limit-bounded) scan below.
        return coroutineScope {
            val historyDeferred = async {
                try {
                    api.getTransactions(address, limit = 1).isNotEmpty()
                } catch (e: Exception) {
                    Log.w("ColdStorageAddressDiscovery", "Lookup failed for index $index", e)
                    null
                }
            }
            val balanceDeferred = async {
                try {
                    api.getBalance(address).balance
                } catch (e: Exception) {
                    0L
                }
            }
            val hasHistory = historyDeferred.await() ?: return@coroutineScope null
            DiscoveredAddress(index, address, balanceDeferred.await(), hasHistory)
        }
    }

    data class AddressTransaction(
        val txId: String,
        val sent: Boolean, // true = this address was a sender on this tx
        val amountSompi: Long, // net amount that left (sent) or arrived (received) — excludes change back to itself
        val blockTimeMillis: Long?
    )

    /**
     * On-chain transaction history for a single address, newest first. Direction/amount aren't
     * fields the REST API returns directly — a tx is only "sent" from [address] if one of its
     * inputs' resolved previous-outpoint address matches (the default `resolve_previous_outpoints`
     * behavior on [KaspaRestApi.getTransactions] already resolves this); the amount then excludes
     * whatever output pays change back to [address] itself, mirroring the same sent-vs-received
     * inference [com.kachat.app.repository.ChatRepository]'s payment sync already relies on.
     */
    suspend fun getTransactionHistory(address: String, limit: Int = 50): List<AddressTransaction> {
        val api = networkService.kaspaRestApi.value ?: return emptyList()
        val transactions = try {
            api.getTransactions(address, limit = limit)
        } catch (e: Exception) {
            Log.w("ColdStorageAddressDiscovery", "Failed to fetch transaction history for $address", e)
            return emptyList()
        }
        return transactions.map { tx ->
            val sent = tx.inputs.any { it.previousOutpointAddress == address }
            val amount = if (sent) {
                tx.outputs.filter { it.scriptPublicKeyAddress != address }.sumOf { it.amount }
            } else {
                tx.outputs.filter { it.scriptPublicKeyAddress == address }.sumOf { it.amount }
            }
            AddressTransaction(tx.transactionId, sent, amount, tx.blockTime)
        }.sortedByDescending { it.blockTimeMillis ?: 0L }
    }

    /**
     * Full paginated transaction history for a single address, oldest first — unlike
     * [getTransactionHistory] (a single page, newest 50, for the Cold Storage display list), this
     * loops [KaspaRestApi.getTransactions]' `offset` until a page returns fewer than [pageSize]
     * rows or [maxTransactions] is hit, for callers that need a complete history rather than a
     * recent-activity list (currently only "Add Kaspa Address" portfolio auto-import). Mirrors
     * iOS's `ChatService.fetchFullTransactionsPaginated`'s loop shape.
     */
    suspend fun getFullTransactionHistoryPaginated(
        address: String,
        pageSize: Int = 50,
        maxTransactions: Int = 500
    ): List<AddressTransaction> {
        val api = networkService.kaspaRestApi.value ?: return emptyList()
        val all = mutableListOf<AddressTransaction>()
        var offset = 0

        while (all.size < maxTransactions) {
            val page = try {
                api.getTransactions(address, limit = pageSize, offset = offset)
            } catch (e: Exception) {
                Log.w("ColdStorageAddressDiscovery", "Paginated fetch failed for $address at offset $offset", e)
                break
            }
            if (page.isEmpty()) break

            all.addAll(
                page.map { tx ->
                    val sent = tx.inputs.any { it.previousOutpointAddress == address }
                    val amount = if (sent) {
                        tx.outputs.filter { it.scriptPublicKeyAddress != address }.sumOf { it.amount }
                    } else {
                        tx.outputs.filter { it.scriptPublicKeyAddress == address }.sumOf { it.amount }
                    }
                    AddressTransaction(tx.transactionId, sent, amount, tx.blockTime)
                }
            )

            if (page.size < pageSize) break
            offset += pageSize
        }

        return all.sortedBy { it.blockTimeMillis ?: 0L }
    }

    data class AddressUtxo(
        val transactionId: String,
        val index: Int,
        val amountSompi: Long,
        val isCoinbase: Boolean
    )

    /** Unspent outputs currently sitting at a single address — backs the Cold Storage tx history
     *  screen's "UTXOs" tab. */
    suspend fun getUtxos(address: String): List<AddressUtxo> {
        val api = networkService.kaspaRestApi.value ?: return emptyList()
        return try {
            api.getUtxos(address).map {
                AddressUtxo(it.outpoint.transactionId, it.outpoint.index, it.utxoEntry.amount, it.utxoEntry.isCoinbase)
            }
        } catch (e: Exception) {
            Log.w("ColdStorageAddressDiscovery", "Failed to fetch UTXOs for $address", e)
            emptyList()
        }
    }
}
