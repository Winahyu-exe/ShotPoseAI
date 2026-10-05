package com.shotposeai.ui.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class ReplayTimelineTest {
    private val timestamps = listOf(0L, 125L, 250L, 375L, 500L)

    @Test fun selectsThePoseAtThePlaybackTimestamp() {
        assertEquals(2, ReplayTimeline.indexAt(timestamps, 250))
    }

    @Test fun doesNotShowAFuturePoseBetweenSamples() {
        assertEquals(2, ReplayTimeline.indexAt(timestamps, 374))
    }

    @Test fun canSeekBackwardsAfterPlayingForward() {
        assertEquals(4, ReplayTimeline.indexAt(timestamps, 520))
        assertEquals(1, ReplayTimeline.indexAt(timestamps, 140))
        assertEquals(0, ReplayTimeline.indexAt(timestamps, 0))
    }

    @Test fun clearsPoseAcrossALongDecodingGap() {
        assertEquals(-1, ReplayTimeline.indexAt(listOf(0L, 1000L), 600))
    }

    @Test fun clearsPoseOutsideTheAnalyzedThirtySeconds() {
        assertEquals(0, ReplayTimeline.indexAt(listOf(29875L, 30000L), 29999))
        assertEquals(-1, ReplayTimeline.indexAt(listOf(29875L, 30000L), 30000))
        assertEquals(-1, ReplayTimeline.indexAt(listOf(29875L, 30000L), 45000))
    }

    @Test fun doesNotInventPoseBeforeFirstSampleOrWhenEmpty() {
        assertEquals(-1, ReplayTimeline.indexAt(listOf(125L), 0))
        assertEquals(-1, ReplayTimeline.indexAt(emptyList(), 1000))
        assertEquals(-1, ReplayTimeline.indexAt(timestamps, -1))
    }
}
