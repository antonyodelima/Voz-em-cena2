package com.sagun12.vozemcena.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateLabelsTest {

    @Test
    fun formatDuration_correctFormatting() {
        assertEquals("00:00", DateLabels.formatDuration(0f))
        assertEquals("00:05", DateLabels.formatDuration(5.4f))
        assertEquals("01:05", DateLabels.formatDuration(65f))
        assertEquals("02:30", DateLabels.formatDuration(150f))
    }

    @Test
    fun formatRelativeTime_recentTimestamps() {
        val now = System.currentTimeMillis()
        assertEquals("Agora mesmo", DateLabels.formatRelativeTime(now - 10_000))
        assertEquals("Há 5 min", DateLabels.formatRelativeTime(now - (5 * 60_000)))
        assertEquals("Há 2 h", DateLabels.formatRelativeTime(now - (2 * 3600_000)))
    }
}
