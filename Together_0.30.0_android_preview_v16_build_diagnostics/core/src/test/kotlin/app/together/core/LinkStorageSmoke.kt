package app.together.core

import java.nio.file.Files

/** Standalone test: compile/run with kotlinc, no Android SDK or JUnit needed. */
fun main() {
    val dir = Files.createTempDirectory("together-links-test").toFile()
    try {
        val file = java.io.File(dir, "links.dat")
        val storage = LinkStorage(file)
        val g = SourceThread(Source.GOOGLE_MESSAGES, "google:42", setOf("+15550001111"))
        val b = SourceThread(Source.OPENBUBBLES, "bubble:7", setOf("+15550001111"))
        val links = ThreadLinks()
        links.link("Squish 🪼🐙", listOf(g, b), true)
        storage.save(links)
        check(storage.load().members("Squish 🪼🐙") == setOf(g.key, b.key))
        check(storage.load().unlink("Squish 🪼🐙").size == 2)
        val a = Message(Source.GOOGLE_MESSAGES, g.threadId, "1", "me", 2000, "Hi", Delivery.SENT, localId="a")
        val c = Message(Source.OPENBUBBLES, b.threadId, "2", "friend", 1000, "Hello", Delivery.RECEIVED, localId="c")
        check(UnifiedTimeline.merge(listOf(g, b), listOf(a, c, a.copy(localId="other"))).map { it.text } == listOf("Hello", "Hi"))
        println("PASS: persistent links, unlink, timeline ordering, deduplication")
    } finally { dir.deleteRecursively() }
}
