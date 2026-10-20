package app.together.core

/** Experimental iMessage backends. A backend must never claim capabilities it has not verified. */
enum class IMessageEngineKind { OPENBUBBLES, OPEN_MADRID }

data class IMessageEngineCapabilities(
    val registrationVerified: Boolean = false,
    val liveSendVerified: Boolean = false,
    val liveReceiveVerified: Boolean = false,
    val attachmentAccessVerified: Boolean = false,
    val exactConversationNavigationVerified: Boolean = false
)

sealed class SendDecision {
    data class Allowed(val engine: IMessageEngineKind) : SendDecision()
    data class Blocked(val reason: String) : SendDecision()
}

/** Gate direct sends on independently verified capabilities; no automatic fallback or duplicate sends. */
class IMessageEnginePolicy(
    private val verified: Map<IMessageEngineKind, IMessageEngineCapabilities>
) {
    fun chooseDirectSend(preferred: IMessageEngineKind): SendDecision {
        val caps = verified[preferred] ?: return SendDecision.Blocked("Engine not configured")
        if (!caps.liveSendVerified) return SendDecision.Blocked("Direct sending not verified for $preferred")
        return SendDecision.Allowed(preferred)
    }

    fun canUseForReceive(engine: IMessageEngineKind): Boolean =
        verified[engine]?.liveReceiveVerified == true
}

/** As of the project's 2026-10-17 README, live courier send/receive are unverified. */
object OpenMadridExperimental {
    val documentedCapabilities = IMessageEngineCapabilities(registrationVerified = true)
    const val STATUS = "Experimental: registration/validation reported verified; live send/receive not verified"
}
