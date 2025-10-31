package no.nav.helse.bakrommet

import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class CoroutineSessionContext(
    val userSession: String,
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CoroutineSessionContext>
}

suspend fun hentSession(): String = currentCoroutineContext()[CoroutineSessionContext]?.userSession ?: throw IllegalStateException("No UserSession found")
