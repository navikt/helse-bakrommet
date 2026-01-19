package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.InntektAarDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.InntektDataDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.PensjonsgivendeInntektDto
import no.nav.helse.bakrommet.asJsonNode
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektAar
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektData
import java.time.format.DateTimeFormatter

fun InntektData.tilInntektDataDto(): InntektDataDto =
    when (this) {
        is InntektData.ArbeidstakerInntektsmelding -> {
            InntektDataDto.ArbeidstakerInntektsmelding(
                inntektsmeldingId = inntektsmeldingId,
                omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
                inntektsmelding = inntektsmelding.asJsonNode(),
                sporing = sporing.name,
            )
        }

        is InntektData.ArbeidstakerAinntekt -> {
            InntektDataDto.ArbeidstakerAinntekt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
                sporing = sporing.name,
                kildedata =
                    kildedata
                        .mapKeys { it.key.format(DateTimeFormatter.ofPattern("yyyy-MM")) }
                        .mapValues { it.value.månedlig },
            )
        }

        is InntektData.ArbeidstakerSkjønnsfastsatt -> {
            InntektDataDto.ArbeidstakerSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
                sporing = sporing.name,
            )
        }

        is InntektData.FrilanserAinntekt -> {
            InntektDataDto.FrilanserAinntekt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
                sporing = sporing.name,
                kildedata =
                    kildedata
                        .mapKeys { it.key.format(DateTimeFormatter.ofPattern("yyyy-MM")) }
                        .mapValues { it.value.månedlig },
            )
        }

        is InntektData.FrilanserSkjønnsfastsatt -> {
            InntektDataDto.FrilanserSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
                sporing = sporing.name,
            )
        }

        is InntektData.Arbeidsledig -> {
            InntektDataDto.Arbeidsledig(
                omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
                sporing = sporing.name,
            )
        }

        is InntektData.InaktivSkjønnsfastsatt -> {
            InntektDataDto.InaktivSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
                sporing = sporing.name,
            )
        }

        is InntektData.InaktivPensjonsgivende -> {
            InntektDataDto.InaktivPensjonsgivende(
                omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
                sporing = sporing.name,
                pensjonsgivendeInntekt = pensjonsgivendeInntekt.tilPensjonsgivendeInntektDto(),
            )
        }

        is InntektData.SelvstendigNæringsdrivendePensjonsgivende -> {
            InntektDataDto.SelvstendigNæringsdrivendePensjonsgivende(
                omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
                sporing = sporing.name,
                pensjonsgivendeInntekt = pensjonsgivendeInntekt.tilPensjonsgivendeInntektDto(),
            )
        }

        is InntektData.SelvstendigNæringsdrivendeSkjønnsfastsatt -> {
            InntektDataDto.SelvstendigNæringsdrivendeSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
                sporing = sporing.name,
            )
        }
    }

fun InntektData.PensjonsgivendeInntekt.tilPensjonsgivendeInntektDto(): PensjonsgivendeInntektDto =
    PensjonsgivendeInntektDto(
        omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
        pensjonsgivendeInntekt = pensjonsgivendeInntekt.map { it.tilInntektAarDto() },
        anvendtGrunnbeløp = anvendtGrunnbeløp.årlig,
    )

fun InntektAar.tilInntektAarDto(): InntektAarDto =
    InntektAarDto(
        år = år.toString(),
        rapportertinntekt = rapportertinntekt.årlig,
        justertÅrsgrunnlag = justertÅrsgrunnlag.årlig,
        antallGKompensert = antallGKompensert,
        snittG = snittG.årlig,
    )
