package com.hexdevop.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogicTest {
    @Test fun focusLastWins() = assertEquals("b", pickStartFocus("b", listOf("a", "b", YOUTUBE)))
    @Test fun focusLastMissingFallsToYoutube() = assertEquals(YOUTUBE, pickStartFocus("x", listOf("a", YOUTUBE)))
    @Test fun focusNullLastYoutube() = assertEquals(YOUTUBE, pickStartFocus(null, listOf("a", YOUTUBE)))
    @Test fun focusNoYoutubeFirst() = assertEquals("a", pickStartFocus("x", listOf("a", "b")))
    @Test fun focusEmpty() = assertNull(pickStartFocus("x", emptyList()))
    @Test fun focusEmptyNullLast() = assertNull(pickStartFocus(null, emptyList()))

    @Test fun inputsEmpty() = assertEquals(emptyList<Int>(), filterInputs(emptyList()))
    @Test fun inputsKeepsTvAndHdmi() {
        val ok = listOf(0, 1, 2, 9, 10, 11, 12, 18, 19, 20, 21)
        assertEquals(ok, filterInputs(ok))
    }
    @Test fun inputsDropsOthersAndBoundaries() =
        assertEquals(emptyList<Int>(), filterInputs(listOf(-1, 3, 8, 13, 17, 22, 100)))
    @Test fun inputsKeepsOrder() = assertEquals(listOf(12, 0, 9), filterInputs(listOf(12, 5, 0, 9)))

    @Test fun stepNormal() = assertEquals(55, stepValue(50, 5))
    @Test fun stepClampMax() = assertEquals(100, stepValue(98, 5))
    @Test fun stepClampMin() = assertEquals(0, stepValue(2, -5))
    @Test fun stepExactBounds() {
        assertEquals(100, stepValue(95, 5))
        assertEquals(0, stepValue(5, -5))
    }
    @Test fun stepZeroDelta() = assertEquals(40, stepValue(40, 0))
    @Test fun stepCustomRange() = assertEquals(-10, stepValue(-8, -5, -10, 10))
    @Test fun stepValueOutOfRange() = assertEquals(100, stepValue(500, 0))
}
