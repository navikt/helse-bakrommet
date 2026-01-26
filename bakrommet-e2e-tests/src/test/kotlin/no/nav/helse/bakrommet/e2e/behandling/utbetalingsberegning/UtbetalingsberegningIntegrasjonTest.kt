package no.nav.helse.bakrommet.e2e.behandling.utbetalingsberegning

import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.BeregningResponseDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.asJsonNode
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.enNavIdent
import no.nav.helse.bakrommet.domain.etOrganisasjonsnummer
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.TestOppsett.oAuthMock
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.sykepengesoknad.Arbeidsgiverinfo
import no.nav.helse.bakrommet.e2e.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.*
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertNotNull

class UtbetalingsberegningIntegrasjonTest {
    private val naturligIdent = enNaturligIdent()
    private val arbeidsgiversOrganisasjonsnummer = etOrganisasjonsnummer()
    private val arbeidsgiversNavn = "Test Bedrift AS"

    @Test
    fun `beregner utbetalinger korrekt med økonomi-klassene`() {
        val arbeidsgiver =
            Arbeidsgiverinfo(
                identifikator = arbeidsgiversOrganisasjonsnummer,
                navn = arbeidsgiversNavn,
            )

        val søknad =
            enSøknad(
                fnr = naturligIdent.value,
                id = UUID.randomUUID().toString(),
                arbeidsgiverinfo = arbeidsgiver,
            ).asJsonNode()

        runApplicationTest(
            sykepengesøknadProvider =
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    tokenUtvekslingProvider = TestOppsett.oboClient,
                    søknadIdTilSvar = mapOf(søknad["id"].asText() to søknad),
                ),
        ) {
            val personPseudoId = personsøk(naturligIdent)
            val tokenBeslutter = oAuthMock.token(navIdent = enNavIdent(), grupper = listOf("GRUPPE_BESLUTTER"))

            // Opprett saksbehandlingsperiode
            val behandling = opprettBehandlingOgForventOk(personPseudoId, 1.januar(2024), 31.januar(2024))

            // Opprett yrkesaktivitet som ordinær arbeidstaker
            val yrkesaktivitet =
                opprettYrkesaktivitetOgForventOk(
                    personId = personPseudoId,
                    behandling.id,
                    YrkesaktivitetKategoriseringDto.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = "123456789"),
                    ),
                )
            // Sett inntekt på yrkesaktivitet (dette trigger automatisk utbetalingsberegning)
            saksbehandlerSkjønnsfastsetterInntekt(
                personPseudoId = personPseudoId,
                behandlingId = behandling.id,
                yrkesaktivitetId = yrkesaktivitet.id,
                årsinntekt = 50000.0 * 12,
                årsak = ArbeidstakerSkjønnsfastsettelseÅrsakDto.MANGELFULL_RAPPORTERING,
                begrunnelse = "Test skjønnsfastsettelse",
                refusjon =
                    listOf(
                        RefusjonsperiodeDto(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 31),
                            beløp = 10000.0,
                        ),
                    ),
            )

            // Sett dagoversikt med forskjellige dagtyper (dette trigger også utbetalingsberegning)
            settDagoversikt(
                personPseudoId,
                behandling.id,
                yrkesaktivitet.id,
                dager =
                    listOf(
                        DagDto(
                            dato = 1.januar(2024),
                            dagtype = DagtypeDto.Syk,
                            grad = 100,
                            avslåttBegrunnelse = emptyList(),
                            kilde = KildeDto.Saksbehandler,
                        ),
                        DagDto(
                            dato = 2.januar(2024),
                            dagtype = DagtypeDto.Syk,
                            grad = 70,
                            avslåttBegrunnelse = emptyList(),
                            kilde = KildeDto.Saksbehandler,
                        ),
                        DagDto(
                            dato = 3.januar(2024),
                            dagtype = DagtypeDto.Ferie,
                            grad = null,
                            avslåttBegrunnelse = emptyList(),
                            kilde = KildeDto.Saksbehandler,
                        ),
                        DagDto(
                            dato = 4.januar(2024),
                            dagtype = DagtypeDto.Syk,
                            grad = 100,
                            avslåttBegrunnelse = emptyList(),
                            kilde = KildeDto.Saksbehandler,
                        ),
                        DagDto(
                            dato = 5.januar(2024),
                            dagtype = DagtypeDto.Arbeidsdag,
                            grad = null,
                            avslåttBegrunnelse = emptyList(),
                            kilde = KildeDto.Saksbehandler,
                        ),
                    ),
            )

            // Hent utbetalingsberegning
            val beregning = hentUtbetalingsberegning(personPseudoId, behandling.id)
            assertNotNull(beregning)

            // Verifiser resultatet
            verifiserBeregning(beregning)

            sendTilBeslutningOgForventOk(personPseudoId, behandling.id)
            taTilBeslutningOgForventOk(personPseudoId, behandling.id, tokenBeslutter)

            godkjennOgForventOk(personPseudoId, behandling.id, tokenBeslutter)
        }
    }

    private fun verifiserBeregning(beregning: BeregningResponseDto) {
        assertEquals(1, beregning.beregningData.yrkesaktiviteter.size)

        val yrkesaktivitet = beregning.beregningData.yrkesaktiviteter.first()
        assertEquals(31, yrkesaktivitet.utbetalingstidslinje.dager.size) // Januar 2024 har 31 dager
        yrkesaktivitet.dekningsgrad!!.verdi.prosentDesimal `should equal` 1.0

        // Dag 1: 100% syk - skal ha refusjon
        val dag1 = yrkesaktivitet.utbetalingstidslinje.dager.find { it.dato.isEqual(1.januar(2024)) }!!
        assertEquals(
            1846.0,
            dag1.økonomi.personbeløp,
            "Dag 1 skal ha personutbetaling siden refusjon ikke dekker alt",
        )
        assertEquals(
            462.0,
            dag1.økonomi.arbeidsgiverbeløp,
            "Dag 1 skal ha 462 i refusjon",
        )
        assertEquals(1.0, dag1.økonomi.totalGrad, "Dag 1 skal ha 100% total grad")

        // Dag 2: 70% syk - skal ha 70% refusjon
        val dag2 = yrkesaktivitet.utbetalingstidslinje.dager.find { it.dato.isEqual(2.januar(2024)) }!!
        assertEquals(
            1292.0,
            dag2.økonomi.personbeløp,
            "Dag 2 skal ha personutbetaling siden refusjon ikke dekker alt",
        )
        assertEquals(
            323.0,
            dag2.økonomi.arbeidsgiverbeløp,
            "Dag 2 skal ha 323 kr refusjon (70% av dag 1, avrundet)",
        )
        assertEquals(0.7, dag2.økonomi.totalGrad, "Dag 2 skal ha 70% total grad")

        // Dag 3: Ferie - skal ikke ha utbetaling
        val dag3 = yrkesaktivitet.utbetalingstidslinje.dager.find { it.dato.isEqual(3.januar(2024)) }!!
        assertEquals(0.0, (dag3.økonomi.personbeløp ?: 0.0) * 100, "Dag 3 (Ferie) skal ikke ha utbetaling")
        assertEquals(0.0, (dag3.økonomi.arbeidsgiverbeløp ?: 0.0) * 100, "Dag 3 (Ferie) skal ikke ha refusjon")
        assertEquals(0.0, dag3.økonomi.totalGrad, "Dag 3 (Ferie) skal ha 0% total grad")

        // Dag 4: 100% syk - skal ha samme refusjon som dag 1
        val dag4 = yrkesaktivitet.utbetalingstidslinje.dager.find { it.dato.isEqual(4.januar(2024)) }!!
        assertEquals(
            1846.0,
            dag4.økonomi.personbeløp,
            "Dag 4 skal ha samme personutbetaling som dag 1",
        )
        assertEquals(
            462.0,
            dag4.økonomi.arbeidsgiverbeløp,
            "Dag 4 skal ha samme refusjon som dag 1",
        )
        assertEquals(1.0, dag4.økonomi.totalGrad, "Dag 4 skal ha 100% total grad")

        // Dag 5: Arbeidsdag - skal ikke ha utbetaling
        val dag5 = yrkesaktivitet.utbetalingstidslinje.dager.find { it.dato.isEqual(5.januar(2024)) }!!
        assertEquals(0.0, (dag5.økonomi.personbeløp ?: 0.0) * 100, "Dag 5 (Arbeidsdag) skal ikke ha utbetaling")
        assertEquals(
            0.0,
            dag5.økonomi.arbeidsgiverbeløp,
            "Dag 5 (Arbeidsdag) skal ikke ha refusjon",
        )
        assertEquals(0.0, dag5.økonomi.totalGrad, "Dag 5 (Arbeidsdag) skal ha 0% total grad")
    }
}
