package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.Grunnbeløp
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.Year

fun List<HentPensjonsgivendeInntektResponse>.kanBeregnesEtter835(): Boolean = this.filter { it.pensjonsgivendeInntekt != null }.size >= 3

fun JsonNode.tilHentPensjonsgivendeInntektResponse(): HentPensjonsgivendeInntektResponse = objectMapper.readValue(this.serialisertTilString())

private fun List<PensjonsgivendeInntekt>.tilInntekt(): Inntekt = Inntekt.gjenopprett(InntektbeløpDto.Årlig(this.sumOf { it.sumAvAlleInntekter() }.toDouble()))

private fun String.toYear(): Year = Year.of(this.toInt())

fun List<HentPensjonsgivendeInntektResponse>.tilBeregnetPensjonsgivendeInntekt(skjæringstidspunkt: LocalDate): InntektData.PensjonsgivendeInntekt {
    if (this.filter { it.pensjonsgivendeInntekt != null }.size < 3) {
        throw RuntimeException("For få år med pensjonsgivende inntekt fra sigrun. Må skjønnsfastsettes")
    }

    // 2. G-verdi på skjæringstidspunkt
    val anvendtGrunnbeløp = Grunnbeløp.`1G`.beløp(skjæringstidspunkt)

    // 3. Beregn sykepengegrunnlag
    val inntekter =
        this.filter { it.pensjonsgivendeInntekt != null }.map {
            SelvstendigFaktaavklartInntekt.PensjonsgivendeInntekt(
                årstall = it.inntektsaar.toYear(),
                beløp = it.pensjonsgivendeInntekt!!.tilInntekt(),
            )
        }
    val sykepengegrunnlag =
        SelvstendigFaktaavklartInntekt.beregnInntektsgrunnlag(
            inntekter = inntekter,
            anvendtGrunnbeløp = anvendtGrunnbeløp,
        )

    val pensjonsgivendeInntektList =
        inntekter.map {
            InntektAar(
                år = it.årstall,
                rapportertinntekt = it.beløp.dto().årlig,
                justertÅrsgrunnlag =
                    it
                        .justertÅrsgrunnlag(anvendtGrunnbeløp)
                        .times(3)
                        .dto()
                        .årlig,
                // ganger 3 fordi justert årsgrunnlag er allerede 3 års snitt
                antallGKompensert = it.antallGKompensert,
                snittG = it.snitt.dto().årlig,
            )
        }

    return InntektData.PensjonsgivendeInntekt(
        omregnetÅrsinntekt = sykepengegrunnlag.dto().årlig,
        pensjonsgivendeInntekt = pensjonsgivendeInntektList,
        anvendtGrunnbeløp = anvendtGrunnbeløp.dto().årlig,
    )
}
