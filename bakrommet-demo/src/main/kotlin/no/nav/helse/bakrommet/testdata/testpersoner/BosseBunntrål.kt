package no.nav.helse.bakrommet.testdata.testpersoner

import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import java.time.LocalDate

val bosseBunntrål =
    Testperson(
        fnr = "30816199456",
        fornavn = "Bosse",
        mellomnavn = "B",
        etternavn = "Bunntrål",
        fødselsdato = LocalDate.of(1963, 8, 16), // ca. 61 år basert på alder 61
        saksbehandingsperioder = emptyList(),
        soknader =
            listOf(
                soknad(
                    fnr = "30816199456",
                    fom = LocalDate.of(2025, 6, 9),
                    tom = LocalDate.of(2025, 6, 15),
                ) {
                    type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE
                    status = SoknadsstatusDTO.SENDT
                    arbeidssituasjon = ArbeidssituasjonDTO.FISKER
                    sykmeldingSkrevet = LocalDate.of(2025, 6, 9)
                    startSyketilfelle = LocalDate.of(2025, 6, 9)
                    opprettet = LocalDate.of(2025, 6, 16)
                    sendtNav = LocalDate.of(2025, 6, 16)
                },
            ),
    )
