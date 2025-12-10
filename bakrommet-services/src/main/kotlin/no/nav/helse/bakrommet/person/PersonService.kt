package no.nav.helse.bakrommet.person

import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.pdl.PdlIdent
import no.nav.helse.bakrommet.pdl.alder
import no.nav.helse.bakrommet.pdl.formattert
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
    private val pdlClient: PdlClient,
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
            pdlClient.hentPersonInfo(
                saksbehandlerToken = saksbehandlerToken,
                ident = naturligIdent.naturligIdent,
            )
        val identer = pdlClient.hentIdenterFor(saksbehandlerToken, naturligIdent.naturligIdent)

        return PersonInfo(
            fødselsnummer = naturligIdent.naturligIdent,
            aktørId = identer.first { it.gruppe == PdlIdent.AKTORID }.ident,
            navn = hentPersonInfo.navn.formattert(),
            alder = hentPersonInfo.alder(),
        )
    }
}
