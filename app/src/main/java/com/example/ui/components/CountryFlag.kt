package com.example.ui.components

object CountryFlag {
    val SUPPORTED_COUNTRIES = listOf(
        "US" to "United States",
        "GB" to "United Kingdom",
        "IN" to "India",
        "NO" to "Norway",
        "DE" to "Germany",
        "FR" to "France",
        "ES" to "Spain",
        "JP" to "Japan",
        "KR" to "South Korea",
        "BR" to "Brazil",
        "UA" to "Ukraine",
        "CA" to "Canada",
        "EG" to "Egypt",
        "AU" to "Australia",
        "PL" to "Poland"
    )

    fun emoji(countryCode: String): String {
        val code = countryCode.uppercase().take(2)
        if (code.length != 2 || code.any { it !in 'A'..'Z' }) return "🌍"
        val first = Character.codePointAt(code, 0) - 'A'.code + 0x1F1E6
        val second = Character.codePointAt(code, 1) - 'A'.code + 0x1F1E6
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    fun formatClockMs(ms: Long): String {
        val clamped = ms.coerceAtLeast(0L)
        val totalSec = clamped / 1000L
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        return if (clamped < 20_000L) {
            val tenths = (clamped % 1000L) / 100L
            "%d:%02d.%d".format(mins, secs, tenths)
        } else {
            "%02d:%02d".format(mins, secs)
        }
    }
}
