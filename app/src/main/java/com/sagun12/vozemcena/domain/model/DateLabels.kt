package com.sagun12.vozemcena.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateLabels {
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timestamp

        if (diffMs < 60_000) {
            return "Agora mesmo"
        }
        val diffMinutes = diffMs / (60_000)
        if (diffMinutes < 60) {
            return "Há $diffMinutes min"
        }
        val diffHours = diffMs / (3_600_000)
        if (diffHours < 24) {
            return "Há $diffHours h"
        }

        val calNow = Calendar.getInstance()
        val calDate = Calendar.getInstance().apply { timeInMillis = timestamp }

        if (calNow.get(Calendar.YEAR) == calDate.get(Calendar.YEAR)) {
            val dayDiff = calNow.get(Calendar.DAY_OF_YEAR) - calDate.get(Calendar.DAY_OF_YEAR)
            if (dayDiff == 1) {
                return "Ontem"
            } else if (dayDiff in 2..6) {
                return "Há $dayDiff dias"
            }
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDuration(seconds: Float): String {
        val totalSec = seconds.toInt()
        val mins = totalSec / 60
        val secs = totalSec % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}
