package app.together.core

fun main() {
    val engine = SyncCoordinator()
    val g = SourceThread(Source.GOOGLE_MESSAGES, "squish-sms", setOf("+15550000000"))
    val o = SourceThread(Source.OPENBUBBLES, "squish-im", setOf("+15550000000"))
    val caps = SourceCapabilities(false, false, false, false)
    fun msg(source: Source, thread: String, id: String, t: Long, state: Delivery) =
        Message(source, thread, id, "Squish", t, "hello", state, localId = id)
    engine.ingest(SyncBatch(Source.GOOGLE_MESSAGES, listOf(msg(g.source, g.threadId, "a", 10, Delivery.SENT)), caps, 100))
    engine.ingest(SyncBatch(Source.OPENBUBBLES, listOf(msg(o.source, o.threadId, "b", 20, Delivery.RECEIVED)), caps, 110, "Notification-only; history not guaranteed"))
    check(engine.timeline(listOf(g, o)).map { it.sourceMessageId } == listOf("a", "b"))
    engine.ingest(SyncBatch(Source.GOOGLE_MESSAGES, emptyList(), caps, 120))
    check(engine.observedCount() == 2) // partial refresh must not erase outgoing message
    engine.ingest(SyncBatch(Source.GOOGLE_MESSAGES, listOf(msg(g.source, g.threadId, "a", 10, Delivery.UNKNOWN)), caps, 130))
    check(engine.timeline(listOf(g)).single().delivery == Delivery.SENT)
    engine.recordFailure(Source.OPENBUBBLES, "Permission missing")
    check(engine.status(Source.OPENBUBBLES).lastSuccessMs == 110L)
    check(engine.status(Source.OPENBUBBLES).warning == "Permission missing")
    check(engine.capabilities(Source.OPENBUBBLES)?.canReadHistory == false)
    println("SyncCoordinatorSmoke PASS")
}
