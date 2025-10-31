package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.person.PersonDao
import java.util.concurrent.ConcurrentHashMap

class PersonDaoFake : PersonDao {
    private val spilleromIdTilNaturligIdent = ConcurrentHashMap<String, String>()

    override fun finnPersonId(vararg identer: String): String? {
        val identSet = identer.toSet()
        return spilleromIdTilNaturligIdent.entries.firstOrNull { it.value in identSet }?.key
    }

    override fun opprettPerson(
        naturligIdent: String,
        spilleromId: String,
    ) {
        spilleromIdTilNaturligIdent[spilleromId] = naturligIdent
    }

    override fun finnNaturligIdent(spilleromId: String): String? = spilleromIdTilNaturligIdent[spilleromId]

    override fun hentNaturligIdent(spilleromId: String): String = finnNaturligIdent(spilleromId) ?: throw RuntimeException("Fant ikke person med spilleromId $spilleromId")
}
