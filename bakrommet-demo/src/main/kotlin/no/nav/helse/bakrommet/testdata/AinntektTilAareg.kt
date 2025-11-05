package no.nav.helse.bakrommet.testdata

import no.nav.helse.bakrommet.aareg.arbeidsforhold
import no.nav.helse.bakrommet.ainntekt.Inntektsinformasjon
import java.time.LocalDate
import java.util.UUID

/**
 * Genererer aareg arbeidsforhold automatisk basert på ainntekt-data.
 * Ser på alle måneder med inntekt og lager arbeidsforhold for hver unike virksomhet.
 *
 * @param fnr Fødselsnummer
 * @param ainntektData Liste med inntektsinformasjon
 * @param fortsattAktiveOrgnummer Liste med organisasjonsnummer som fortsatt er aktive (får ikke sluttdato)
 * @param stillingsprosent Stillingsprosent for arbeidsforholdene (default: 100.0)
 * @return Liste med Arbeidsforhold
 */
fun genererAaregFraAinntekt(
    fnr: String,
    ainntektData: List<Inntektsinformasjon>,
    fortsattAktiveOrgnummer: List<String> = emptyList(),
    stillingsprosent: Double = 100.0,
): List<no.nav.helse.bakrommet.aareg.Arbeidsforhold> {
    if (ainntektData.isEmpty()) return emptyList()

    // Grupper etter organisasjonsnummer
    val inntektPerOrgnummer =
        ainntektData.groupBy { it.underenhet }

    return inntektPerOrgnummer.map { (orgnummer, inntekter) ->
        val maaneder = inntekter.map { it.maaned }.sorted()
        val førsteMaaned = maaneder.first()
        val sisteMaaned = maaneder.last()

        // Startdato er første dag i første måned
        val startdato = LocalDate.of(førsteMaaned.year, førsteMaaned.monthValue, 1)

        // Sluttdato er siste dag i siste måned, eller null hvis fortsatt aktiv
        val sluttdato =
            if (orgnummer in fortsattAktiveOrgnummer) {
                null
            } else {
                LocalDate.of(sisteMaaned.year, sisteMaaned.monthValue, sisteMaaned.lengthOfMonth())
            }

        arbeidsforhold(
            fnr = fnr,
            orgnummer = orgnummer,
            startdato = startdato,
            sluttdato = sluttdato,
            id = "$fnr-$orgnummer-${UUID.randomUUID().toString().take(8)}",
            stillingsprosent = stillingsprosent,
            navArbeidsforholdId = (10000..99999).random(),
        )
    }
}
