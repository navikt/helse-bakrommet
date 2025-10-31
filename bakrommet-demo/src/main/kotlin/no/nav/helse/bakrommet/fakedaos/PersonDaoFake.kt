package no.nav.helse.bakrommet.fakedaos

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.hentSessionid
import no.nav.helse.bakrommet.person.PersonDao
import java.util.concurrent.ConcurrentHashMap

class PersonDaoFake : PersonDao {
    // Map av sessionId -> spilleromId -> naturligIdent
    private val sessionData = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()

    private fun getSessionMap(): ConcurrentHashMap<String, String> =
        runBlocking {
            val sessionId = hentSessionid()

            sessionData.getOrPut(sessionId) { ConcurrentHashMap() }
        }

    override suspend fun finnPersonId(vararg identer: String): String? {
        val identSet = identer.toSet()
        val spilleromIdTilNaturligIdent = getSessionMap()
        return spilleromIdTilNaturligIdent.entries.firstOrNull { it.value in identSet }?.key
    }

    override suspend fun opprettPerson(
        naturligIdent: String,
        spilleromId: String,
    ) {
        val spilleromIdTilNaturligIdent = getSessionMap()
        spilleromIdTilNaturligIdent[spilleromId] = naturligIdent
    }

    override suspend fun finnNaturligIdent(spilleromId: String): String? {
        val spilleromIdTilNaturligIdent = getSessionMap()
        return spilleromIdTilNaturligIdent[spilleromId]
    }

    override suspend fun hentNaturligIdent(spilleromId: String): String = finnNaturligIdent(spilleromId) ?: throw RuntimeException("Fant ikke person med spilleromId $spilleromId")
}
