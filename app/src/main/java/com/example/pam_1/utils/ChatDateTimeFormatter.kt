package com.example.pam_1.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object ChatDateTimeFormatter {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    /**
     * Format timestamp for chat messages
     * - "Just now" for < 1 minute
     * - "X min ago" for < 60 minutes
     * - "HH:mm" for today
     * - "Yesterday" for yesterday
     * - "dd/MM/yyyy" for older
     */
    fun formatMessageTime(timestamp: String): String {
        try {
            val instant = Instant.parse(timestamp)
            val now = Instant.now()
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            val nowZoned = now.atZone(ZoneId.systemDefault())

            // Calculate difference in minutes
            val minutesAgo = ChronoUnit.MINUTES.between(instant, now)

            return when {
                // Less than 1 minute
                minutesAgo < 1 -> "Just now"

                // Less than 60 minutes
                minutesAgo < 60 -> "${minutesAgo} min ago"

                // Same day
                zonedDateTime.toLocalDate() == nowZoned.toLocalDate() -> {
                    timeFormatter.format(zonedDateTime)
                }

                // Yesterday
                zonedDateTime.toLocalDate() == nowZoned.toLocalDate().minusDays(1) -> {
                    "Yesterday"
                }

                // Older
                else -> dateFormatter.format(zonedDateTime)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /** Format timestamp for message details (full date and time) */
    fun formatMessageDateTime(timestamp: String): String {
        try {
            val instant = Instant.parse(timestamp)
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            return dateTimeFormatter.format(zonedDateTime)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /** Check if timestamp is within edit window (15 minutes) */
    fun isWithinEditWindow(timestamp: String, windowMinutes: Long = 15): Boolean {
        try {
            val instant = Instant.parse(timestamp)
            val now = Instant.now()
            val minutesAgo = ChronoUnit.MINUTES.between(instant, now)
            return minutesAgo < windowMinutes
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
