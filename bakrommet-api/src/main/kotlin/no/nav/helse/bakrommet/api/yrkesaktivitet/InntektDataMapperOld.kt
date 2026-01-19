package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.InntektAarDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.InntektDataDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.PensjonsgivendeInntektDto
import no.nav.helse.bakrommet.behandling.inntekter.InntektAarOld
import no.nav.helse.bakrommet.behandling.inntekter.InntektDataOld
import no.nav.helse.bakrommet.util.toJsonNode
import java.time.format.DateTimeFormatter

fun InntektDataOld.tilInntektDataDto(): InntektDataDto =
    when (this) {
        is InntektDataOld.ArbeidstakerInntektsmelding -> {
            InntektDataDto.ArbeidstakerInntektsmelding(
                inntektsmeldingId = inntektsmeldingId,
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                inntektsmelding = inntektsmelding.toJsonNode(),
                sporing = sporing.name,
            )
        }

        is InntektDataOld.ArbeidstakerAinntekt -> {
            InntektDataDto.ArbeidstakerAinntekt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
                kildedata =
                    kildedata
                        .mapKeys { it.key.format(DateTimeFormatter.ofPattern("yyyy-MM")) }
                        .mapValues { it.value.beløp },
            )
        }

        is InntektDataOld.ArbeidstakerSkjønnsfastsatt -> {
            InntektDataDto.ArbeidstakerSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
            )
        }

        is InntektDataOld.FrilanserAinntekt -> {
            InntektDataDto.FrilanserAinntekt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
                kildedata =
                    kildedata
                        .mapKeys { it.key.format(DateTimeFormatter.ofPattern("yyyy-MM")) }
                        .mapValues { it.value.beløp },
            )
        }

        is InntektDataOld.FrilanserSkjønnsfastsatt -> {
            InntektDataDto.FrilanserSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
            )
        }

        is InntektDataOld.Arbeidsledig -> {
            InntektDataDto.Arbeidsledig(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
            )
        }

        is InntektDataOld.InaktivSkjønnsfastsatt -> {
            InntektDataDto.InaktivSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
            )
        }

        is InntektDataOld.InaktivPensjonsgivende -> {
            InntektDataDto.InaktivPensjonsgivende(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
                pensjonsgivendeInntekt = pensjonsgivendeInntekt.tilPensjonsgivendeInntektDto(),
            )
        }

        is InntektDataOld.SelvstendigNæringsdrivendePensjonsgivende -> {
            InntektDataDto.SelvstendigNæringsdrivendePensjonsgivende(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
                pensjonsgivendeInntekt = pensjonsgivendeInntekt.tilPensjonsgivendeInntektDto(),
            )
        }

        is InntektDataOld.SelvstendigNæringsdrivendeSkjønnsfastsatt -> {
            InntektDataDto.SelvstendigNæringsdrivendeSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
                sporing = sporing.name,
            )
        }
    }

fun InntektDataOld.PensjonsgivendeInntekt.tilPensjonsgivendeInntektDto(): PensjonsgivendeInntektDto =
    PensjonsgivendeInntektDto(
        omregnetÅrsinntekt = omregnetÅrsinntekt.beløp,
        pensjonsgivendeInntekt = pensjonsgivendeInntekt.map { it.tilInntektAarDto() },
        anvendtGrunnbeløp = anvendtGrunnbeløp.beløp,
    )

fun InntektAarOld.tilInntektAarDto(): InntektAarDto =
    InntektAarDto(
        år = år.toString(),
        rapportertinntekt = rapportertinntekt.beløp,
        justertÅrsgrunnlag = justertÅrsgrunnlag.beløp,
        antallGKompensert = antallGKompensert,
        snittG = snittG.beløp,
    )
