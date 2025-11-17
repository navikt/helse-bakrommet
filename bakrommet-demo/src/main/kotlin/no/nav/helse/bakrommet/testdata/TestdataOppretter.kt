package no.nav.helse.bakrommet.testdata

import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.fakerConfig
import no.nav.helse.bakrommet.Services
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.behandling.somReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.beritBeslutter
import no.nav.helse.bakrommet.hentSession
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.bakrommet.saksMcBehandlersen
import no.nav.helse.bakrommet.sessionsDaoer
import no.nav.helse.bakrommet.util.somGyldigUUID
import java.util.*

suspend fun Services.opprettTestdata(testpersoner: List<Testperson>) {
    val db = sessionsDaoer[hentSession()]!!

    val saksbehandlerBrukerOgToken =
        BrukerOgToken(
            bruker = saksMcBehandlersen,
            token = SpilleromBearerToken("token"),
        )
    testpersoner
        .forEach { testperson ->
            db.personDao.opprettPerson(
                naturligIdent = testperson.fnr,
                spilleromId = testperson.spilleromId,
            )
            val seed = testperson.fnr.toLong()
            val config =
                fakerConfig {
                    locale = "nb_NO"
                    random = Random(seed)
                }
            val faker = Faker(config)

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
                        id = faker.random.nextUUID().somGyldigUUID(),
                        spilleromPersonId = SpilleromPersonId(testperson.spilleromId),
                        fom = periode.fom,
                        tom = periode.tom,
                        søknader = søknadUUIDer,
                        saksbehandler =
                        saksbehandlerBrukerOgToken,
                    )
                if (periode.inntektRequest != null) {
                    this.yrkesaktivitetService.hentYrkesaktivitetFor(nySaksbehandlingsperiode.somReferanse()).let { yrkesaktiviteter ->
                        this.inntektService.oppdaterInntekt(
                            ref = YrkesaktivitetReferanse(nySaksbehandlingsperiode.somReferanse(), yrkesaktiviteter.first().id),
                            request = periode.inntektRequest,
                            saksbehandler = saksbehandlerBrukerOgToken,
                        )
                    }
                }
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
