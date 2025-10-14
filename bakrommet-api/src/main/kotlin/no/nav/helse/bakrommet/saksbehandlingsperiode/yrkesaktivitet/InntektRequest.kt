package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate

// ARBEIDSTAKER
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ArbeidstakerInntektRequest.Inntektsmelding::class, name = "INNTEKTSMELDING"),
    JsonSubTypes.Type(value = ArbeidstakerInntektRequest.Ainntekt::class, name = "AINNTEKT"),
    JsonSubTypes.Type(value = ArbeidstakerInntektRequest.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
    JsonSubTypes.Type(value = ArbeidstakerInntektRequest.ManueltBeregnet::class, name = "MANUELLT_BEREGNET"),
)
sealed class ArbeidstakerInntektRequest {
    data class Inntektsmelding(
        val inntektsmeldingId: String,
    ) : ArbeidstakerInntektRequest()

    class Ainntekt : ArbeidstakerInntektRequest()

    data class Skjønnsfastsatt(
        val månedsbeløp: Int,
        val årsak: ArbeidstakerSkjønnsfastsettelseÅrsak,
        val begrunnelse: String,
        val refusjon: RefusjonInfo? = null,
    ) : ArbeidstakerInntektRequest()

    data class ManueltBeregnet(
        val månedsbeløp: Int,
        val begrunnelse: String,
    ) : ArbeidstakerInntektRequest()
}

enum class ArbeidstakerSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT,
    MANGFULL_RAPPORTERING,
    TIDSAVGRENSET,
}

// SELVSTENDIG_NÆRINGSDRIVENDE / INAKTIV
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = PensjonsgivendeInntektRequest.PensjonsgivendeInntekt::class, name = "PENSJONSGIVENDE_INNTEKT"),
    JsonSubTypes.Type(value = PensjonsgivendeInntektRequest.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
)
sealed class PensjonsgivendeInntektRequest {
    class PensjonsgivendeInntekt : PensjonsgivendeInntektRequest()

    data class Skjønnsfastsatt(
        val årsinntekt: Int,
        val årsak: PensjonsgivendeSkjønnsfastsettelseÅrsak,
        val begrunnelse: String,
    ) : PensjonsgivendeInntektRequest()
}

enum class PensjonsgivendeSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT_VARIG_ENDRING,
    SISTE_TRE_YRKESAKTIV,
}

// FRILANSER
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = FrilanserInntektRequest.Ainntekt::class, name = "AINNTEKT"),
    JsonSubTypes.Type(value = FrilanserInntektRequest.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
)
sealed class FrilanserInntektRequest {
    class Ainntekt : FrilanserInntektRequest()

    data class Skjønnsfastsatt(
        val månedsbeløp: Int,
        val årsak: FrilanserSkjønnsfastsettelseÅrsak,
        val begrunnelse: String,
    ) : FrilanserInntektRequest()
}

enum class FrilanserSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT,
    MANGFULL_RAPPORTERING,
}

// ARBEIDSLEDIG
data class ArbeidsledigInntektRequest(
    val type: ArbeidsledigInntektType,
    val månedligBeløp: Int,
)

enum class ArbeidsledigInntektType {
    DAGPENGER,
    VENTELONN,
    VARTPENGER,
}

// Union av alle requests
data class InntektRequest(
    val inntektskategori: Inntektskategori,
    val data: Any, // Kan være ArbeidstakerInntektRequest, PensjonsgivendeInntektRequest, FrilanserInntektRequest, eller ArbeidsledigInntektRequest
)

/**
 * Extension funksjon som deserialiserer JSON til riktig InntektRequest basert på inntektskategori feltet.
 * Først leser den inntektskategori, deretter deserialiserer data-feltet til riktig type.
 */
fun String.deserializeToInntektRequest(): InntektRequest {
    val jsonNode =
        com.fasterxml.jackson.databind
            .ObjectMapper()
            .readTree(this)
    val inntektskategori = Inntektskategori.valueOf(jsonNode.get("inntektskategori").asText())
    val dataNode = jsonNode.get("data")

    val data =
        when (inntektskategori) {
            Inntektskategori.ARBEIDSTAKER -> {
                com.fasterxml.jackson.databind
                    .ObjectMapper()
                    .treeToValue(dataNode, ArbeidstakerInntektRequest::class.java)
            }
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.INAKTIV -> {
                com.fasterxml.jackson.databind
                    .ObjectMapper()
                    .treeToValue(dataNode, PensjonsgivendeInntektRequest::class.java)
            }
            Inntektskategori.FRILANSER -> {
                com.fasterxml.jackson.databind
                    .ObjectMapper()
                    .treeToValue(dataNode, FrilanserInntektRequest::class.java)
            }
            Inntektskategori.ARBEIDSLEDIG -> {
                com.fasterxml.jackson.databind
                    .ObjectMapper()
                    .treeToValue(dataNode, ArbeidsledigInntektRequest::class.java)
            }
        }

    return InntektRequest(inntektskategori, data)
}

// Hjelpeklasser
data class RefusjonInfo(
    val fra: LocalDate,
    val til: LocalDate,
    val beløp: Int,
)
