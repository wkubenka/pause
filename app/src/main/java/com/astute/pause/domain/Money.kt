package com.astute.pause.domain

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object Money {
    /** $100,000,000.00. Guards against typos and keeps sums far from Long overflow. */
    const val MAX_CENTS = 10_000_000_000L

    private val PLAIN = Regex("""^(\d+(\.\d{0,2})?|\.\d{1,2})$""")
    private val GROUPED = Regex("""^\d{1,3}(,\d{3})+(\.\d{0,2})?$""")
    private val DECIMAL_COMMA = Regex("""^\d+,\d{1,2}$""")

    fun parseToCents(input: String): Long? {
        val s = input.trim().removePrefix("$").trim()
        val normalized = when {
            PLAIN.matches(s) -> s
            GROUPED.matches(s) -> s.replace(",", "")
            DECIMAL_COMMA.matches(s) -> s.replace(',', '.')
            else -> return null
        }
        val cents = runCatching { BigDecimal(normalized).movePointRight(2).longValueExact() }
            .getOrNull() ?: return null
        return cents.takeIf { it in 0..MAX_CENTS }
    }

    fun format(cents: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        runCatching { format.currency = Currency.getInstance(currencyCode) }
        return format.format(BigDecimal.valueOf(cents, 2))
    }

    fun toInput(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()

    fun defaultCurrency(locale: Locale = Locale.getDefault()): String =
        runCatching { Currency.getInstance(locale).currencyCode }.getOrDefault("USD")
}
