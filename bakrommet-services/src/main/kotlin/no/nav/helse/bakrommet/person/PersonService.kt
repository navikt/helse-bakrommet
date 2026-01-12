package no.nav.helse.bakrommet.person

import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.clients.PdlIdent
import no.nav.helse.bakrommet.clients.PdlProvider
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import java.time.LocalDate
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
    private val pdlClient: PdlProvider,
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

    suspend fun slettPseudoIderEldreEnn(antallDager: Int = 14): Int =
        db.nonTransactional {
            personPseudoIdDao.slettPseudoIderEldreEnn(
                OffsetDateTime.now().minusDays(antallDager.toLong()),
            )
        }
}

fun no.nav.helse.bakrommet.clients.Navn.formattert(): String =
    when {
        mellomnavn.isNullOrBlank() -> "$fornavn $etternavn"
        else -> "$fornavn $mellomnavn $etternavn"
    }

fun no.nav.helse.bakrommet.clients.PersonInfo.alder(): Int? {
    val today = LocalDate.now()
    if (fodselsdato == null) {
        return null
    }
    val age = today.year - fodselsdato.year
    if (today.monthValue < fodselsdato.monthValue || (today.monthValue == fodselsdato.monthValue && today.dayOfMonth < fodselsdato.dayOfMonth)) {
        return age - 1
    }
    return age
}
