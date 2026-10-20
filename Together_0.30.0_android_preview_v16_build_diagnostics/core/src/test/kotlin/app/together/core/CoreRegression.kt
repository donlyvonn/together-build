package app.together.core
fun main() {
    val g = SourceThread(Source.GOOGLE_MESSAGES, "g1", setOf("+15550001111"))
    val b = SourceThread(Source.OPENBUBBLES, "b1", setOf("+15550001111"))
    val links = ThreadLinks()
    links.link("squish", listOf(g, b), true)
    check(links.members("squish").size == 2)
    val sent = Message(Source.GOOGLE_MESSAGES,"g1","42","me",2000,"Reply",Delivery.SENT,localId="a")
    val received = Message(Source.OPENBUBBLES,"b1","7","friend",1000,"Hi",Delivery.RECEIVED,localId="b")
    val merged = UnifiedTimeline.merge(listOf(g,b),listOf(sent,received,sent.copy(localId="other")))
    check(merged.map { it.text } == listOf("Hi","Reply"))
    check(links.unlink("squish").size == 2)
    println("Core regression checks passed")
}
