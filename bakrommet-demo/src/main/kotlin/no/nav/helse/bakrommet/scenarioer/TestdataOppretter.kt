package no.nav.helse.bakrommet.scenarioer

import no.nav.helse.bakrommet.Services
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.hentSession
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.bakrommet.sessionsDaoer

suspend fun Services.opprettTestdata(testpersoner: List<Testperson>) {
    val db = sessionsDaoer[hentSession()]!!

    testpersoner
        .filter {
            it.spilleromId != null // Må ha spilleromId for å opprette testdata
        }.forEach { testperson ->
            db.personDao.opprettPerson(
                naturligIdent = testperson.fnr,
                spilleromId = testperson.spilleromId!!,
            )

            testperson.saksbehandingsperioder.forEach {
                this.saksbehandlingsperiodeService.opprettNySaksbehandlingsperiode(
                    spilleromPersonId = SpilleromPersonId(testperson.spilleromId),
                    fom = it.fom,
                    tom = it.tom,
                    søknader = emptySet(),
                    saksbehandler =
                        BrukerOgToken(
                            bruker =
                                Bruker(
                                    navn = "ds",
                                    navIdent = "ds",
                                    preferredUsername = "dsf",
                                    roller = emptySet(),
                                ),
                            token = SpilleromBearerToken("token"),
                        ),
                )
            }
        }
}
