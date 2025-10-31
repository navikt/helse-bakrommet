package no.nav.helse.bakrommet

import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class CoroutineSessionContext(
    val sessionid: String,
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CoroutineSessionContext>
}

suspend fun hentSessionid(): String = currentCoroutineContext()[CoroutineSessionContext]?.sessionid ?: throw IllegalStateException("No ExtraContext found")
