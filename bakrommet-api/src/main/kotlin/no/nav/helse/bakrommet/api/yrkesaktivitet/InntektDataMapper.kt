package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.behandling.inntekter.InntektAar
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.util.objectMapper
import java.time.format.DateTimeFormatter

fun InntektData.tilInntektDataDto(): InntektDataDto =
    when (this) {
        is InntektData.ArbeidstakerInntektsmelding ->
            InntektDataDto.ArbeidstakerInntektsmelding(
                inntektsmeldingId = inntektsmeldingId,
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                inntektsmelding = inntektsmelding.tilMap(),
                sporing = sporing.name,
            )

        is InntektData.ArbeidstakerManueltBeregnet ->
            InntektDataDto.ArbeidstakerManueltBeregnet(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
            )

        is InntektData.ArbeidstakerAinntekt ->
            InntektDataDto.ArbeidstakerAinntekt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
                kildedata =
                    kildedata
                        .mapKeys { it.key.format(DateTimeFormatter.ofPattern("yyyy-MM")) }
                        .mapValues { it.value.beløp },
            )

        is InntektData.ArbeidstakerSkjønnsfastsatt ->
            InntektDataDto.ArbeidstakerSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
            )

        is InntektData.FrilanserAinntekt ->
            InntektDataDto.FrilanserAinntekt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
                kildedata =
                    kildedata
                        .mapKeys { it.key.format(DateTimeFormatter.ofPattern("yyyy-MM")) }
                        .mapValues { it.value.beløp },
            )

        is InntektData.FrilanserSkjønnsfastsatt ->
            InntektDataDto.FrilanserSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
            )

        is InntektData.Arbeidsledig ->
            InntektDataDto.Arbeidsledig(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
            )

        is InntektData.InaktivSkjønnsfastsatt ->
            InntektDataDto.InaktivSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
            )

        is InntektData.InaktivPensjonsgivende ->
            InntektDataDto.InaktivPensjonsgivende(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
                pensjonsgivendeInntekt = pensjonsgivendeInntekt.tilPensjonsgivendeInntektDto(),
            )

        is InntektData.SelvstendigNæringsdrivendePensjonsgivende ->
            InntektDataDto.SelvstendigNæringsdrivendePensjonsgivende(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
                pensjonsgivendeInntekt = pensjonsgivendeInntekt.tilPensjonsgivendeInntektDto(),
            )

        is InntektData.SelvstendigNæringsdrivendeSkjønnsfastsatt ->
            InntektDataDto.SelvstendigNæringsdrivendeSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
            )
    }

fun InntektData.PensjonsgivendeInntekt.tilPensjonsgivendeInntektDto(): PensjonsgivendeInntektDto =
    PensjonsgivendeInntektDto(
        omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
        pensjonsgivendeInntekt = pensjonsgivendeInntekt.map { it.tilInntektAarDto() },
        anvendtGrunnbeløp = anvendtGrunnbeløp.beløp,
    )

fun InntektAar.tilInntektAarDto(): InntektAarDto =
    InntektAarDto(
        år = år.toString(),
        rapportertinntekt = rapportertinntekt.beløp,
        justertÅrsgrunnlag = justertÅrsgrunnlag.beløp,
        antallGKompensert = antallGKompensert,
        snittG = snittG.beløp,
    )

private fun com.fasterxml.jackson.databind.JsonNode.tilMap(): Map<String, Any> = objectMapper.convertValue(this, Map::class.java) as Map<String, Any>
