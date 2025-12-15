package no.nav.helse.bakrommet.testdata.testpersoner

import no.nav.helse.bakrommet.ereg.Organisasjon
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate
import java.util.UUID

val muggeMcMurstein =
    Testperson(
        fnr = "01020304050",
        fornavn = "Mugge",
        etternavn = "McMurstein",
        fødselsdato = LocalDate.of(1959, 1, 2), // ca. 65 år basert på alder 65
        behandlinger = emptyList(),
        soknader = generateMuggeSoknader(),
    )

private fun generateMuggeSoknader(): List<SykepengesoknadDTO> {
    val arbeidsgiver1 = Organisasjon(navn = "Murstein AS", orgnummer = "999999991")
    val arbeidsgiver2 = Organisasjon(navn = "Betongbygg AS", orgnummer = "999999992")

    val start1 = LocalDate.of(2025, 1, 1)
    val start2 = LocalDate.of(2025, 1, 8)

    val soknader = mutableListOf<SykepengesoknadDTO>()
    var prevTom1 = start1
    var prevTom2 = start2

    for (i in 0 until 5) {
        // Arbeidsforhold 1
        val fom1 = if (i == 0) start1 else prevTom1.plusDays(1)
        val tom1 = fom1.plusDays(13)
        soknader.add(
            soknad(
                fnr = "01020304050",
                fom = fom1,
                tom = tom1,
            ) {
                id = UUID.randomUUID()
                arbeidsgiverNavn = arbeidsgiver1.navn
                arbeidsgiverOrgnummer = arbeidsgiver1.orgnummer
                arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                sykmeldingSkrevet = fom1
                startSyketilfelle = fom1
                opprettet = fom1
                sendtNav = fom1
                sendtArbeidsgiver = fom1
            },
        )
        prevTom1 = tom1

        // Arbeidsforhold 2
        val fom2 = if (i == 0) start2 else prevTom2.plusDays(1)
        val tom2 = fom2.plusDays(13)
        soknader.add(
            soknad(
                fnr = "01020304050",
                fom = fom2,
                tom = tom2,
            ) {
                id = UUID.randomUUID()
                arbeidsgiverNavn = arbeidsgiver2.navn
                arbeidsgiverOrgnummer = arbeidsgiver2.orgnummer
                arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                sykmeldingSkrevet = fom2
                startSyketilfelle = fom2
                opprettet = fom2
                sendtNav = fom2
                sendtArbeidsgiver = fom2
            },
        )
        prevTom2 = tom2
    }

    return soknader
}
