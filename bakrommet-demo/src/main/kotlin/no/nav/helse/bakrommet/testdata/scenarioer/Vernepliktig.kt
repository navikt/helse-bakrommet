package no.nav.helse.bakrommet.testdata.scenarioer

import no.nav.helse.bakrommet.testdata.Testperson
import java.time.LocalDate

val vernepliktig =
    Testscenario(
        tittel = "Vernepliktig som dimitteres pga skade",
        testperson =
            Testperson(
                fornavn = "August",
                fødselsdato = LocalDate.now().minusYears(20),
                fnr = "20029712345",
                etternavn = "Stomperud",
            ),
        beskrivelse =
            """
            Dimitert pga kneskade etter ca 3 mnd tjeneste.

            Situasjon før verneplikt: Jobbet som assistent ved SFO. 
            """.trimIndent(),
    )
