package app.together.core

/** Platform-neutral core. The Android persistence and message-source adapters are separate. */
enum class Source { GOOGLE_MESSAGES, OPENBUBBLES }
enum class Delivery { RECEIVED, PENDING, SENT, FAILED, UNKNOWN }

data class SourceThread(val source: Source, val threadId: String, val recipients: Set<String>) {
    init { require(threadId.isNotBlank()) }
    val key: String get() = "${source.name}:$threadId"
}

data class Message(
    val source: Source,
    val sourceThreadId: String,
    val sourceMessageId: String?,
    val sender: String,
    val timestampMs: Long,
    val text: String,
    val delivery: Delivery,
    val attachmentIds: List<String> = emptyList(),
    /** Local ingestion ID prevents accidental collapsing of ID-less messages. */
    val localId: String
) {
    init { require(localId.isNotBlank()); require(timestampMs >= 0) }
    val stableKey: String get() = if (!sourceMessageId.isNullOrBlank())
        "${source.name}:$sourceThreadId:id:$sourceMessageId"
    else "${source.name}:$sourceThreadId:local:$localId"
}

/** Reversible mapping; never changes source records. Persist links transactionally on Android. */
class ThreadLinks {
    private val groups = linkedMapOf<String, MutableSet<String>>()

    fun link(displayId: String, threads: Collection<SourceThread>, confirmedSameRecipients: Boolean) {
        require(displayId.isNotBlank())
        require(confirmedSameRecipients) { "Explicit recipient confirmation required" }
        require(threads.size >= 2)
        val keys = threads.map { it.key }.toSet()
        require(keys.size == threads.size)
        require(groups.filterKeys { it != displayId }.values.none { existing -> existing.any { it in keys } }) {
            "A source thread is already linked to another display thread"
        }
        groups[displayId] = keys.toMutableSet()
    }

    fun unlink(displayId: String): Set<String> = groups.remove(displayId)?.toSet() ?: emptySet()
    fun members(displayId: String): Set<String> = groups[displayId]?.toSet() ?: emptySet()
    fun snapshot(): Map<String, Set<String>> = groups.mapValues { it.value.toSet() }
}

object UnifiedTimeline {
    /** Dedup only on authoritative source IDs or stable local ingestion IDs. */
    fun merge(threads: Collection<SourceThread>, messages: Collection<Message>): List<Message> {
        val allowed = threads.map { it.key }.toSet()
        return messages.asSequence()
            .filter { "${it.source.name}:${it.sourceThreadId}" in allowed }
            .distinctBy { it.stableKey }
            .sortedWith(compareBy<Message> { it.timestampMs }.thenBy { it.stableKey })
            .toList()
    }
}

/** A source must state its capabilities, not imply unavailable history/media is synced. */
data class SourceCapabilities(
    val canReadHistory: Boolean,
    val canReadOutgoing: Boolean,
    val canReadMedia: Boolean,
    val canOpenExactThread: Boolean
)

data class SyncStatus(val source: Source, val lastSuccessMs: Long?, val warning: String?)
