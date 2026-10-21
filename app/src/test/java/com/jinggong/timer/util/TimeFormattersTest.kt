package com.jinggong.timer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormattersTest {

    @Test
    fun formatsZero() {
        assertEquals("00:00:00", TimeFormatters.formatTimer(0L))
    }

    @Test
    fun formatsSecondsAndMinutes() {
        assertEquals("00:00:59", TimeFormatters.formatTimer(59_900L))
        assertEquals("00:12:34", TimeFormatters.formatTimer(754_000L))
    }

    @Test
    fun formatsMoreThanAnHour() {
        assertEquals("01:02:03", TimeFormatters.formatTimer(3_723_000L))
    }

    @Test
    fun doesNotWrapAtOneHundredHours() {
        assertEquals("100:00:00", TimeFormatters.formatTimer(360_000_000L))
    }
}
