package app.together.core

import org.junit.Assert.*
import org.junit.Test

class IMessageEnginePolicyTest {
    @Test fun openMadridIsBlockedForLiveSendByDefault() {
        val policy = IMessageEnginePolicy(mapOf(IMessageEngineKind.OPEN_MADRID to OpenMadridExperimental.documentedCapabilities))
        assertTrue(policy.chooseDirectSend(IMessageEngineKind.OPEN_MADRID) is SendDecision.Blocked)
        assertFalse(policy.canUseForReceive(IMessageEngineKind.OPEN_MADRID))
    }

    @Test fun verifiedEngineCanBeSelectedWithoutImplicitFallback() {
        val policy = IMessageEnginePolicy(mapOf(IMessageEngineKind.OPENBUBBLES to IMessageEngineCapabilities(liveSendVerified = true)))
        assertEquals(SendDecision.Allowed(IMessageEngineKind.OPENBUBBLES), policy.chooseDirectSend(IMessageEngineKind.OPENBUBBLES))
        assertTrue(policy.chooseDirectSend(IMessageEngineKind.OPEN_MADRID) is SendDecision.Blocked)
    }
}
