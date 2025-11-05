package no.nav.helse.bakrommet.testdata.testpersoner

import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import java.time.LocalDate

val mattisMatros =
    Testperson(
        fnr = "13065212348",
        aktorId = "1234567891011",
        spilleromId = "jf74h",
        fornavn = "Mattis",
        etternavn = "Matros",
        fødselsdato = LocalDate.of(2006, 6, 12), // ca. 18 år basert på alder 18
        saksbehandingsperioder = emptyList(),
        soknader =
            listOf(
                soknad(
                    fnr = "13065212348",
                    fom = LocalDate.of(2025, 6, 1),
                    tom = LocalDate.of(2025, 6, 24),
                ) {
                    id = "ab0c5c03-0acf-3e42-a0cc-fa74281d5bba"
                    type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE
                    status = SoknadsstatusDTO.SENDT
                    arbeidssituasjon = ArbeidssituasjonDTO.FRILANSER
                    sykmeldingSkrevet = LocalDate.of(2025, 6, 18)
                    startSyketilfelle = LocalDate.of(2025, 5, 27)
                    opprettet = LocalDate.of(2025, 6, 25)
                    sendtNav = LocalDate.of(2025, 6, 25)
                },
            ),
    )
