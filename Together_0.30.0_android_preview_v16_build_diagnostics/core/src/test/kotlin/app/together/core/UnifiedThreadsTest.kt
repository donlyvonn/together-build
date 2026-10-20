package app.together.core

import org.junit.Assert.*
import org.junit.Test

class UnifiedThreadsTest {
    private val google = SourceThread(Source.GOOGLE_MESSAGES, "g1", setOf("+15550001111"))
    private val bubble = SourceThread(Source.OPENBUBBLES, "b1", setOf("+15550001111"))

    @Test fun linkingAndUnlinkingNeverMutatesMessages() {
        val links = ThreadLinks()
        links.link("squish", listOf(google, bubble), true)
        assertEquals(2, links.members("squish").size)
        assertEquals(2, links.unlink("squish").size)
        assertTrue(links.members("squish").isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun linkingRequiresConfirmation() { ThreadLinks().link("squish", listOf(google, bubble), false) }

    @Test fun mergeOrdersAndDeduplicatesSourceIds() {
        val outgoing = Message(Source.GOOGLE_MESSAGES, "g1", "42", "me", 2000, "Reply", Delivery.SENT, localId = "a")
        val incoming = Message(Source.OPENBUBBLES, "b1", "7", "friend", 1000, "Hi", Delivery.RECEIVED, localId = "b")
        val merged = UnifiedTimeline.merge(listOf(google, bubble), listOf(outgoing, incoming, outgoing.copy(localId = "different")))
        assertEquals(listOf("Hi", "Reply"), merged.map { it.text })
    }

    @Test fun identicalTextAcrossSourcesIsNotSilentlyDropped() {
        val a = Message(Source.GOOGLE_MESSAGES, "g1", null, "me", 1000, "OK", Delivery.SENT, localId = "a")
        val b = Message(Source.OPENBUBBLES, "b1", null, "me", 1000, "OK", Delivery.SENT, localId = "b")
        assertEquals(2, UnifiedTimeline.merge(listOf(google, bubble), listOf(a, b)).size)
    }
}
