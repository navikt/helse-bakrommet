package no.nav.helse.bakrommet.person

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.pdl.PdlIdent.Companion.FOLKEREGISTERIDENT
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.sikkerLogger
import java.lang.RuntimeException
import java.sql.SQLException

interface PersonsokDaoer {
    val personDao: PersonDao
}

class PersonsøkService(
    private val pdlClient: PdlClient,
    private val db: DbDaoer<PersonsokDaoer>,
    private val personIdFactory: PersonIdFactory = SpilleromPersonId,
) {
    suspend fun hentEllerOpprettPersonid(
        ident: String,
        saksbehandler: BrukerOgToken,
    ): SpilleromPersonId {
        // Ident må være 11 eller 13 siffer lang
        if (ident.length != 11 && ident.length != 13) {
            throw InputValideringException("Ident må være 11 eller 13 siffer lang")
        }
        val identer = pdlClient.hentIdenterFor(saksbehandlerToken = saksbehandler.token, ident = ident)
        db
            .nonTransactional {
                personDao.finnPersonId(*identer.map { it.ident }.toTypedArray())
            }?.let { return SpilleromPersonId(it) }

        // Ok? : // TODO naturlig ident her må være gjeldende fnr fra hentIdenter
        // TODO II ?: Sjekk at faktisk er "gjeldende"?
        // TODO III ?: Uansett lagre alle identer knyttet til spilleromId ??
        val folkeregisterIdent = identer.first { it.gruppe == FOLKEREGISTERIDENT }.ident

        val newPersonId =
            folkeregisterIdent.let { fnr ->
                prøvOpprettFor(naturligIdent = fnr)
                    ?: prøvOpprettFor(naturligIdent = fnr)
                    ?: prøvOpprettFor(naturligIdent = fnr)
                    ?: prøvOpprettFor(naturligIdent = fnr)
                    ?: throw RuntimeException("Klarte ikke opprette SpilleromPersonId")
            }

        return newPersonId
    }

    private suspend fun prøvOpprettFor(naturligIdent: String): SpilleromPersonId? {
        val newPersonId = personIdFactory.lagNy()
        try {
            db.nonTransactional {
                personDao.opprettPerson(naturligIdent = naturligIdent, newPersonId.personId)
            }
        } catch (ex: SQLException) {
            val basisTekst = "SQLException ved opprettelese av ny SpilleromPersonId. Antakeligvis kollisjon."
            logg.error("$basisTekst, se sikkerlogg")
            sikkerLogger.error("$basisTekst, personId=${newPersonId.personId}, ident[0:6]=${naturligIdent.take(6)}", ex)
            return null
        }
        return newPersonId
    }
}
