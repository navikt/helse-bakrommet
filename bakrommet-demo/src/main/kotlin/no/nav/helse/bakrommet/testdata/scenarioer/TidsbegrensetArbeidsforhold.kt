package no.nav.helse.bakrommet.testdata.scenarioer

import no.nav.helse.bakrommet.aareg.arbeidsforhold
import no.nav.helse.bakrommet.aareg.midlertidigAnsettelse
import no.nav.helse.bakrommet.testdata.Testperson
import java.time.LocalDate

val tidsbegrensetArbeidsforhold =
    Testscenario(
        tittel = "Tidsbegrenset arbeidsforhold",
        testperson =
            Testperson(
                fornavn = "Viktor",
                fødselsdato = LocalDate.now().minusYears(32),
                fnr = "15069212345",
                spilleromId = "viktor",
                etternavn = "Vikar",
                aaregData =
                    listOf(
                        // Første arbeidsforhold: vikar deltid fra 15.09.24 - 31.03.25
                        arbeidsforhold(
                            fnr = "15069212345",
                            orgnummer = "111222333",
                            startdato = LocalDate.of(2024, 9, 15),
                            sluttdato = LocalDate.of(2025, 3, 31),
                            id = "viktor-1",
                            stillingsprosent = 50.0, // Deltid
                            timerPrUke = 18.75,
                            ansettelsesform = midlertidigAnsettelse(),
                            navArbeidsforholdId = 10001,
                        ),
                        // Andre arbeidsforhold: vikariat fulltid fra 01.04.25 - 31.08.25
                        arbeidsforhold(
                            fnr = "15069212345",
                            orgnummer = "444555666",
                            startdato = LocalDate.of(2025, 4, 1),
                            sluttdato = LocalDate.of(2025, 8, 31),
                            id = "viktor-2",
                            stillingsprosent = 100.0, // Fulltid
                            ansettelsesform = midlertidigAnsettelse(),
                            navArbeidsforholdId = 10002,
                        ),
                    ),
            ),
        beskrivelse =
            """
            Jobbet som vikar deltid fra 15.09.24 - 31.03.25.
            Fikk vikariat i full stilling hos annen arbeidsgiver fra 01.04.25 - 31.08.25.
            Ble syk underveis i dette arbeidsforholdet.
            """.trimIndent(),
    )
