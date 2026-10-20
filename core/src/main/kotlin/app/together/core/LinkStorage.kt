package app.together.core

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64

/**
 * Stores display-thread mappings, never message contents. Caller must keep this file in
 * Android app-private storage. The Android layer must serialize concurrent writes.
 * V1 is intentionally simple and rejects malformed or duplicate membership records.
 */
class LinkStorage(private val file: File) {
    private fun encode(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun decode(value: String): String = String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

    fun save(links: ThreadLinks) {
        file.parentFile?.mkdirs()
        val lines = mutableListOf("TOGETHER_LINKS_V1")
        for ((displayId, members) in links.snapshot().toSortedMap()) {
            require(members.size >= 2) { "Invalid linked group" }
            lines += (listOf(encode(displayId)) + members.sorted().map(::encode)).joinToString("\t")
        }
        val temp = File(file.parentFile ?: File("."), file.name + ".tmp")
        temp.writeText(lines.joinToString("\n", postfix = "\n"), Charsets.UTF_8)
        try {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE)
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun load(): ThreadLinks {
        val links = ThreadLinks()
        if (!file.exists()) return links
        val lines = file.readLines(Charsets.UTF_8)
        require(lines.firstOrNull() == "TOGETHER_LINKS_V1") { "Unsupported link storage format" }
        val used = mutableSetOf<String>()
        for (line in lines.drop(1).filter { it.isNotBlank() }) {
            val cells = line.split('\t')
            require(cells.size >= 3) { "Invalid link entry" }
            val displayId = decode(cells.first())
            val keys = cells.drop(1).map(::decode)
            require(keys.distinct().size == keys.size && keys.all(used::add)) { "Duplicate source thread" }
            val threads = keys.map { key ->
                val parts = key.split(':', limit = 2)
                require(parts.size == 2 && parts[1].isNotBlank()) { "Invalid source thread" }
                SourceThread(Source.valueOf(parts[0]), parts[1], emptySet())
            }
            require(links.members(displayId).isEmpty()) { "Duplicate display thread" }
            links.link(displayId, threads, confirmedSameRecipients = true)
        }
        return links
    }
}
