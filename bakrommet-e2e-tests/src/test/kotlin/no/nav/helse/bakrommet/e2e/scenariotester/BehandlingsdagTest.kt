package no.nav.helse.bakrommet.e2e.scenariotester

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.DagtypeDto
import no.nav.helse.bakrommet.e2e.testutils.AInntekt
import no.nav.helse.bakrommet.e2e.testutils.Arbeidstaker
import no.nav.helse.bakrommet.e2e.testutils.Scenario
import no.nav.helse.bakrommet.ereg.plankeFabrikken
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykmeldingstypeDTO
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import org.junit.jupiter.api.Test
import java.util.UUID

class BehandlingsdagTest {
    @Test
    fun `Vi behandler en behandlingsdagsøknad`() {
        val soknadId = UUID.randomUUID()
        Scenario(
            fom = 17.mai(2024),
            tom = 17.juli(2024),
            soknader =
                listOf(
                    soknad(
                        fnr = "2323232322",
                        fom = 17.mai(2024),
                        tom = 17.juli(2024),
                    ) {
                        arbeidstaker(plankeFabrikken)
                        id = soknadId
                        status = SoknadsstatusDTO.SENDT
                        type = SoknadstypeDTO.BEHANDLINGSDAGER
                        sykmeldingstype = SykmeldingstypeDTO.BEHANDLINGSDAGER
                        grad = 100
                        valgteBehandlingsdager = listOf(1.juni(2024), 13.juni(2024))
                    },
                ),
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(plankeFabrikken.orgnummer, inntekt = AInntekt(30000, 30000, 30000)),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(360000.0)
            `skal ha direkteutbetaling`(1385)
            `skal ha dagtype`(DagtypeDto.Behandlingsdag, 1.juni(2024))
            `skal ha dagtype`(DagtypeDto.Arbeidsdag, 2.juni(2024))
            `skal ha dagtype`(DagtypeDto.Behandlingsdag, 13.juni(2024))
        }
    }
}
