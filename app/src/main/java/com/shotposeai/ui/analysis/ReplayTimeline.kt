package com.shotposeai.ui.analysis

/** Never display a future pose or keep a stale pose over an undecoded video segment. */
internal object ReplayTimeline {
    const val ANALYSIS_LIMIT_MS = 30_000L
    fun indexAt(timestamps: List<Long>, positionMs: Long): Int {
        if (positionMs < 0 || positionMs >= ANALYSIS_LIMIT_MS) return -1
        val found = timestamps.binarySearch(positionMs)
        val index = if (found >= 0) found else -found - 2
        return if (index >= 0 && positionMs - timestamps[index] <= 250L) index else -1
    }
}
