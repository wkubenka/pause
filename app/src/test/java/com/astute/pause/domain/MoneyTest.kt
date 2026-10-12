package com.astute.pause.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyTest {
    @Test fun plain_whole_and_decimal_amounts() {
        assertEquals(4000L, Money.parseToCents("40"))
        assertEquals(4050L, Money.parseToCents("40.5"))
        assertEquals(4099L, Money.parseToCents("40.99"))
        assertEquals(4000L, Money.parseToCents("40."))
        assertEquals(99L, Money.parseToCents(".99"))
        assertEquals(0L, Money.parseToCents("0"))
    }

    @Test fun strips_whitespace_and_dollar_sign() {
        assertEquals(129999L, Money.parseToCents("  $1299.99 "))
        assertEquals(2500L, Money.parseToCents("$ 25"))
    }

    @Test fun thousands_grouping_is_not_a_decimal() {
        assertEquals(129900L, Money.parseToCents("1,299"))
        assertEquals(129999L, Money.parseToCents("1,299.99"))
        assertEquals(1_000_000_00L, Money.parseToCents("1,000,000"))
    }

    @Test fun decimal_comma_with_two_digits_is_a_decimal() {
        assertEquals(1250L, Money.parseToCents("12,50"))
        assertEquals(1250L, Money.parseToCents("12,5"))
    }

    @Test fun rejects_garbage_negative_and_ambiguous_input() {
        listOf("", " ", "abc", "-5", "12.345", "1.299,99", "1e5", "12,3456", "1,29,9", "$", "4 0").forEach {
            assertNull("expected null for '$it'", Money.parseToCents(it))
        }
    }

    @Test fun rejects_absurdly_large_amounts_without_overflow() {
        assertNull(Money.parseToCents("100000000.01"))
        assertNull(Money.parseToCents("99999999999999999999999"))
        assertEquals(Money.MAX_CENTS, Money.parseToCents("100000000"))
    }

    @Test fun formats_in_currency() {
        assertEquals("$1,299.99", Money.format(129999, "USD", Locale.US))
        assertEquals("$0.00", Money.format(0, "USD", Locale.US))
    }

    @Test fun formats_unknown_currency_code_without_crashing() {
        assertEquals("$25.00", Money.format(2500, "NOPE", Locale.US))
    }

    @Test fun to_input_round_trips() {
        assertEquals("25.00", Money.toInput(2500))
        assertEquals(2500L, Money.parseToCents(Money.toInput(2500)))
    }

    @Test fun default_currency_falls_back_to_usd_for_language_only_locale() {
        assertEquals("USD", Money.defaultCurrency(Locale.US))
        assertEquals("EUR", Money.defaultCurrency(Locale.GERMANY))
        assertEquals("USD", Money.defaultCurrency(Locale("en")))
    }
}
