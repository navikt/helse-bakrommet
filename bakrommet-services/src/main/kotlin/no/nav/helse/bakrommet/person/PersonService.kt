package no.nav.helse.bakrommet.person

import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.pdl.PdlIdent
import no.nav.helse.bakrommet.pdl.alder
import no.nav.helse.bakrommet.pdl.formattert

interface PersonServiceDaoer {
    val personDao: PersonDao
}

data class PersonInfo(
    val fødselsnummer: String,
    val aktørId: String,
    val navn: String,
    val alder: Int?,
)

class PersonService(
    private val db: DbDaoer<PersonServiceDaoer>,
    private val pdlClient: PdlClient,
) {
    suspend fun finnNaturligIdent(spilleromId: String): String? =
        db.nonTransactional {
            personDao.finnNaturligIdent(spilleromId)
        }

    suspend fun hentPersonInfo(
        spilleromPersonId: SpilleromPersonId,
        saksbehandlerToken: SpilleromBearerToken,
    ): PersonInfo {
        val fnr =
            finnNaturligIdent(spilleromPersonId.personId)
                ?: throw IllegalStateException("Fant ikke naturligIdent for personId ${spilleromPersonId.personId}")

        val hentPersonInfo =
            pdlClient.hentPersonInfo(
                saksbehandlerToken = saksbehandlerToken,
                ident = fnr,
            )
        val identer = pdlClient.hentIdenterFor(saksbehandlerToken, fnr)

        return PersonInfo(
            fødselsnummer = fnr,
            aktørId = identer.first { it.gruppe == PdlIdent.AKTORID }.ident,
            navn = hentPersonInfo.navn.formattert(),
            alder = hentPersonInfo.alder(),
        )
    }
}
