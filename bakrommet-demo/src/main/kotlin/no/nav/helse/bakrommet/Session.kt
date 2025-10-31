package no.nav.helse.bakrommet

import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class ExtraContext(
    val sessionid: String,
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ExtraContext>
}

suspend fun hentSessionid(): String = currentCoroutineContext()[ExtraContext]?.sessionid ?: throw IllegalStateException("No ExtraContext found")
