package no.nav.helse.bakrommet.testdata.testpersoner

import no.nav.helse.bakrommet.testdata.Behandling
import no.nav.helse.bakrommet.testdata.Testperson
import java.time.LocalDate

val blankeArk =
    Testperson(
        fnr = "13064512348",
        fornavn = "Blanke",
        etternavn = "Ark",
        fødselsdato = LocalDate.of(2003, 6, 13), // ca. 22 år basert på alder 22
        behandlinger =
            listOf(
                Behandling(
                    fom = LocalDate.of(2025, 3, 1),
                    tom = LocalDate.of(2025, 3, 31),
                ),
            ),
        soknader = emptyList(),
    )
