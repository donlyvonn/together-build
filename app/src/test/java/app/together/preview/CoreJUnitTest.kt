package app.together.preview

import app.together.core.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CoreJUnitTest {
    @Test fun linksSurviveReloadAndCanUnlink() {
        val file = File.createTempFile("together-links", ".txt")
        try {
            val google = SourceThread(Source.GOOGLE_MESSAGES, "squish-google", setOf("+15550001111"))
            val bubbles = SourceThread(Source.OPENBUBBLES, "squish-bubbles", setOf("+15550001111"))
            val links = ThreadLinks()
            links.link("squish", listOf(google, bubbles), confirmedSameRecipients = true)
            LinkStorage(file).save(links)
            val reloaded = LinkStorage(file).load()
            assertEquals(setOf(google.key, bubbles.key), reloaded.members("squish"))
            assertEquals(2, reloaded.unlink("squish").size)
            assertTrue(reloaded.members("squish").isEmpty())
        } finally { file.delete() }
    }

    @Test fun mergeOrdersSourcesWithoutCollapsingDistinctMessages() {
        val google = SourceThread(Source.GOOGLE_MESSAGES, "g", emptySet())
        val bubbles = SourceThread(Source.OPENBUBBLES, "b", emptySet())
        val sent = Message(Source.GOOGLE_MESSAGES, "g", "42", "me", 2000, "Reply", Delivery.SENT, localId="one")
        val incoming = Message(Source.OPENBUBBLES, "b", "7", "friend", 1000, "Hello", Delivery.RECEIVED, localId="two")
        val merged = UnifiedTimeline.merge(listOf(google,bubbles), listOf(sent,incoming,sent.copy(localId="three")))
        assertEquals(listOf("Hello", "Reply"), merged.map { it.text })
        assertEquals(2, merged.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun linkingRequiresExplicitConfirmation() {
        ThreadLinks().link("squish", listOf(SourceThread(Source.GOOGLE_MESSAGES,"g",emptySet()), SourceThread(Source.OPENBUBBLES,"b",emptySet())), false)
    }
}
