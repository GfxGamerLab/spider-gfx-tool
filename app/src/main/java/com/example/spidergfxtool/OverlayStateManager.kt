package com.example.spidergfxtool

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global state manager for Floating Overlays.
 * Ensures the App UI stays synced with Service life cycles.
 */
object OverlayStateManager {
    
    private val _isCrosshairActive = MutableStateFlow(false)
    val isCrosshairActive = _isCrosshairActive.asStateFlow()

    private val _isHudActive = MutableStateFlow(false)
    val isHudActive = _isHudActive.asStateFlow()

    fun setCrosshairActive(active: Boolean) {
        _isCrosshairActive.value = active
    }

    fun setHudActive(active: Boolean) {
        _isHudActive.value = active
    }
}
