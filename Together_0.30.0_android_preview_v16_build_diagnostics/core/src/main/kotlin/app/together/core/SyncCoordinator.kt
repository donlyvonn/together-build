package app.together.core

/** A source adapter reports only data actually accessible through its integration. */
data class SyncBatch(
    val source: Source,
    val messages: List<Message>,
    val capabilities: SourceCapabilities,
    val completedAtMs: Long,
    val warning: String? = null
)

/** Non-destructive ingest. An empty/partial fetch never deletes previously observed messages. */
class SyncCoordinator {
    private val observed = linkedMapOf<String, Message>()
    private val status = mutableMapOf<Source, SyncStatus>()
    private val capabilities = mutableMapOf<Source, SourceCapabilities>()

    fun ingest(batch: SyncBatch) {
        require(batch.completedAtMs >= 0)
        require(batch.messages.all { it.source == batch.source }) { "Mixed-source sync batch" }
        batch.messages.forEach { incoming ->
            val old = observed[incoming.stableKey]
            // Never overwrite a known sent/received result with an unknown or pending state.
            val safe = if (old != null && incoming.delivery in setOf(Delivery.UNKNOWN, Delivery.PENDING)
                && old.delivery in setOf(Delivery.SENT, Delivery.RECEIVED, Delivery.FAILED))
                incoming.copy(delivery = old.delivery) else incoming
            observed[incoming.stableKey] = safe
        }
        capabilities[batch.source] = batch.capabilities
        status[batch.source] = SyncStatus(batch.source, batch.completedAtMs, batch.warning)
    }

    fun recordFailure(source: Source, warning: String) {
        val prior = status[source]
        status[source] = SyncStatus(source, prior?.lastSuccessMs, warning)
    }

    fun timeline(threads: Collection<SourceThread>): List<Message> =
        UnifiedTimeline.merge(threads, observed.values)

    fun status(source: Source): SyncStatus = status[source] ?: SyncStatus(source, null, "Not synchronized")
    fun capabilities(source: Source): SourceCapabilities? = capabilities[source]
    fun observedCount(): Int = observed.size
}
