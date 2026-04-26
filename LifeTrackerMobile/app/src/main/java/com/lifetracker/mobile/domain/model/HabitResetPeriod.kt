package com.lifetracker.mobile.domain.model

enum class HabitResetPeriod(val serialValue: String, val label: String) {
    Daily("DAILY", "Daily"),
    Weekly("WEEKLY", "Weekly"),
    Monthly("MONTHLY", "Monthly"),
    ;

    companion object {
        val Default = Daily
        private const val PREFIX = "RESET:"

        fun encode(period: HabitResetPeriod): String = PREFIX + period.serialValue

        fun decode(repeatPattern: String?): HabitResetPeriod? {
            if (repeatPattern.isNullOrBlank()) return null
            if (!repeatPattern.startsWith(PREFIX)) return null
            val value = repeatPattern.removePrefix(PREFIX).trim()
            return entries.firstOrNull { it.serialValue == value }
        }
    }
}
