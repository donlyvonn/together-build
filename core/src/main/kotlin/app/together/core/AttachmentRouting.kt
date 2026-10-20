package app.together.core

/** No invented deep links: adapters must explicitly supply a verified destination. */
data class AttachmentRef(
    val id: String,
    val source: Source,
    val sourceThreadId: String,
    val sourceMessageId: String?,
    val localUri: String? = null,
    val verifiedThreadUri: String? = null,
    val mimeType: String? = null
) {
    init { require(id.isNotBlank() && sourceThreadId.isNotBlank()) }
}

sealed class AttachmentAction {
    data class PreviewLocal(val uri: String, val mimeType: String?) : AttachmentAction()
    data class OpenVerifiedThread(val uri: String) : AttachmentAction()
    data class Unavailable(val reason: String) : AttachmentAction()
}

/** Caller must validate URI scheme, permission grants and intent resolution on Android. */
object AttachmentRouter {
    fun action(ref: AttachmentRef): AttachmentAction {
        if (ref.source == Source.GOOGLE_MESSAGES && !ref.localUri.isNullOrBlank())
            return AttachmentAction.PreviewLocal(ref.localUri, ref.mimeType)
        if (!ref.verifiedThreadUri.isNullOrBlank())
            return AttachmentAction.OpenVerifiedThread(ref.verifiedThreadUri)
        return AttachmentAction.Unavailable(
            if (ref.source == Source.OPENBUBBLES)
                "OpenBubbles has not exposed a verified conversation destination"
            else "Attachment is not accessible through the current Google Messages integration"
        )
    }
}
