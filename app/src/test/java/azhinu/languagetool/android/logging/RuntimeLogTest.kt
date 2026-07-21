package azhinu.languagetool.android.logging

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeLogTest {
    @After
    fun cleanUp() = RuntimeLog.clear()

    @Test
    fun capitalizesMessagesAndClearsEntries() {
        RuntimeLog.info("сервер доступен")
        assertEquals("Сервер доступен", RuntimeLog.entries.value.single().message)

        RuntimeLog.clear()
        assertTrue(RuntimeLog.entries.value.isEmpty())
    }

    @Test
    fun retainsOnlyLastFiveHundredEntries() {
        repeat(510) { RuntimeLog.debug("событие $it") }

        assertEquals(500, RuntimeLog.entries.value.size)
        assertEquals("Событие 10", RuntimeLog.entries.value.first().message)
        assertEquals("Событие 509", RuntimeLog.entries.value.last().message)
    }
}
