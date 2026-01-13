package no.nav.helse.bakrommet.testdata

import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.fakerConfig
import no.nav.helse.bakrommet.*
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.somReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.util.somGyldigUUID
import java.util.*

suspend fun Services.opprettTestdata(testpersoner: List<Testperson>) {
    val db = sessionsDaoer[hentSession()]!!

    val saksbehandlerBrukerOgToken =
        BrukerOgToken(
            bruker = saksMcBehandlersen,
            token = AccessToken("token"),
        )
    testpersoner
        .forEach { testperson ->
            db.personPseudoIdDao.opprettPseudoId(
                naturligIdent = NaturligIdent(testperson.fnr),
                pseudoId = testperson.pseudoId,
            )
            val seed = testperson.fnr.toLong()
            val config =
                fakerConfig {
                    locale = "nb_NO"
                    random = Random(seed)
                }
            val faker = Faker(config)

            testperson.behandlinger.forEach { periode ->
                // Konverter søknad-ID-ene til UUID-er
                val søknadUUIDer = periode.søknadIder.toSet()

                val nySaksbehandlingsperiode =
                    this.behandlingService.opprettNyBehandling(
                        id = faker.random.nextUUID().somGyldigUUID(),
                        naturligIdent = NaturligIdent(testperson.fnr),
                        fom = periode.fom,
                        tom = periode.tom,
                        søknader = søknadUUIDer,
                        saksbehandler =
                        saksbehandlerBrukerOgToken,
                    )
                if (periode.inntektRequest != null) {
                    this.yrkesaktivitetService
                        .hentYrkesaktivitetFor(nySaksbehandlingsperiode.somReferanse())
                        .let { yrkesaktiviteter ->
                            this.inntektService.oppdaterInntekt(
                                ref =
                                    YrkesaktivitetReferanse(
                                        nySaksbehandlingsperiode.somReferanse(),
                                        yrkesaktiviteter.first().yrkesaktivitet.id,
                                    ),
                                request = periode.inntektRequest,
                                saksbehandler = saksbehandlerBrukerOgToken,
                            )
                        }
                }
                if (periode.avsluttet) {
                    this.behandlingService.sendTilBeslutning(
                        periodeRef = nySaksbehandlingsperiode.somReferanse(),
                        individuellBegrunnelse = "En god begrunnelse",
                        saksbehandler = saksMcBehandlersen,
                    )
                    this.behandlingService.taTilBeslutning(
                        periodeRef = nySaksbehandlingsperiode.somReferanse(),
                        saksbehandler = beritBeslutter,
                    )
                    this.behandlingService.godkjennPeriode(
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
