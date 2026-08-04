package com.kachat.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kachat.app.services.GiftClaimState
import com.kachat.app.services.GiftManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Thin Compose-facing wrapper over the singleton [GiftManager]. Every screen that shows the gift
 * button (Profile row, setup-wizard funding step) gets its own instance via `hiltViewModel()`, but
 * they all observe the same [GiftManager.state], so claiming in one place updates all - matching
 * iOS's shared `GiftService.shared`.
 */
@HiltViewModel
class GiftViewModel @Inject constructor(
    private val giftManager: GiftManager
) : ViewModel() {

    val state: StateFlow<GiftClaimState> = giftManager.state

    fun checkEligibility() = giftManager.checkEligibility()

    fun claim(walletAddress: String) {
        viewModelScope.launch { giftManager.claimGift(walletAddress) }
    }

    fun resetForRetry() = giftManager.resetClaimStateForRetry()
}
