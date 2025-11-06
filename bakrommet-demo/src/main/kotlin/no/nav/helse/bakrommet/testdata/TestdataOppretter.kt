package no.nav.helse.bakrommet.testdata

import no.nav.helse.bakrommet.Services
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.beritBeslutter
import no.nav.helse.bakrommet.hentSession
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.bakrommet.saksMcBehandlersen
import no.nav.helse.bakrommet.saksbehandlingsperiode.somReferanse
import no.nav.helse.bakrommet.sessionsDaoer
import java.util.*

suspend fun Services.opprettTestdata(testpersoner: List<Testperson>) {
    val db = sessionsDaoer[hentSession()]!!

    testpersoner
        .forEach { testperson ->
            db.personDao.opprettPerson(
                naturligIdent = testperson.fnr,
                spilleromId = testperson.spilleromId,
            )

            // Opprett mapping fra søknad-ID (string) til UUID
            val soknadIdTilUuid = testperson.soknader.associate { it.id to stringIdTilUUID(it.id) }

            testperson.saksbehandingsperioder.forEach { periode ->
                // Konverter søknad-ID-ene til UUID-er
                val søknadUUIDer =
                    periode.søknadIder
                        .mapNotNull { soknadId ->
                            soknadIdTilUuid[soknadId]
                        }.toSet()

                val nySaksbehandlingsperiode =
                    this.saksbehandlingsperiodeService.opprettNySaksbehandlingsperiode(
                        spilleromPersonId = SpilleromPersonId(testperson.spilleromId),
                        fom = periode.fom,
                        tom = periode.tom,
                        søknader = søknadUUIDer,
                        saksbehandler =
                            BrukerOgToken(
                                bruker = saksMcBehandlersen,
                                token = SpilleromBearerToken("token"),
                            ),
                    )
                if (periode.avsluttet) {
                    this.saksbehandlingsperiodeService.sendTilBeslutning(
                        periodeRef = nySaksbehandlingsperiode.somReferanse(),
                        individuellBegrunnelse = "En god begrunnelse",
                        saksbehandler = saksMcBehandlersen,
                    )
                    this.saksbehandlingsperiodeService.taTilBeslutning(
                        periodeRef = nySaksbehandlingsperiode.somReferanse(),
                        saksbehandler = beritBeslutter,
                    )
                    this.saksbehandlingsperiodeService.godkjennPeriode(
                        periodeRef = nySaksbehandlingsperiode.somReferanse(),
                        saksbehandler = beritBeslutter,
                    )
                }
            }
        }
}

/**
 * Konverterer en string-ID til en deterministisk UUID.
 * Dette sikrer at samme string-ID alltid gir samme UUID,
 * noe som er viktig for mock-klienten som finner søknader basert på ID.
 */
private fun stringIdTilUUID(stringId: String): UUID = UUID.nameUUIDFromBytes(stringId.toByteArray())
