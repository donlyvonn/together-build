package app.together.core

fun main() {
    val google = AttachmentRef("a", Source.GOOGLE_MESSAGES, "t", "m", localUri="content://example/1", mimeType="image/jpeg")
    check(AttachmentRouter.action(google) == AttachmentAction.PreviewLocal("content://example/1", "image/jpeg"))
    val bubble = AttachmentRef("b", Source.OPENBUBBLES, "t", "m")
    check(AttachmentRouter.action(bubble) is AttachmentAction.Unavailable)
    check(AttachmentRouter.action(bubble.copy(verifiedThreadUri="openbubbles://verified-thread")) is AttachmentAction.OpenVerifiedThread)
    val noGoogle = AttachmentRef("c", Source.GOOGLE_MESSAGES, "t", null)
    check(AttachmentRouter.action(noGoogle) is AttachmentAction.Unavailable)
    println("Attachment routing smoke tests: PASS")
}
