package no.nav.helse.bakrommet.person

import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.PdlIdent
import no.nav.helse.bakrommet.infrastruktur.provider.PersoninfoProvider
import java.time.OffsetDateTime
import java.util.UUID

interface PersonServiceDaoer {
    val personPseudoIdDao: PersonPseudoIdDao
}

data class PersonInfo(
    val fødselsnummer: String,
    val aktørId: String,
    val navn: String,
    val alder: Int?,
)

class PersonService(
    private val db: DbDaoer<PersonServiceDaoer>,
    private val personinfoProvider: PersoninfoProvider,
) {
    suspend fun finnNaturligIdent(pseudoId: UUID): NaturligIdent? =
        db.nonTransactional {
            personPseudoIdDao.finnNaturligIdent(pseudoId)
        }

    suspend fun hentPersonInfo(
        naturligIdent: NaturligIdent,
        saksbehandlerToken: SpilleromBearerToken,
    ): PersonInfo {
        val hentPersonInfo =
            personinfoProvider.hentPersonInfo(
                saksbehandlerToken = saksbehandlerToken,
                ident = naturligIdent.naturligIdent,
            )
        val identer = personinfoProvider.hentIdenterFor(saksbehandlerToken, naturligIdent.naturligIdent)

        return PersonInfo(
            fødselsnummer = naturligIdent.naturligIdent,
            aktørId = identer.first { it.gruppe == PdlIdent.AKTORID }.ident,
            navn = hentPersonInfo.navn.formattert(),
            alder = hentPersonInfo.alder(),
        )
    }

    suspend fun slettPseudoIderEldreEnn(antallDager: Int = 14): Int =
        db.nonTransactional {
            personPseudoIdDao.slettPseudoIderEldreEnn(
                OffsetDateTime.now().minusDays(antallDager.toLong()),
            )
        }
}
