package no.nav.helse.bakrommet.db.dto.yrkesaktivitet

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

// ARBEIDSTAKER
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DbArbeidstakerInntektRequest.Inntektsmelding::class, name = "INNTEKTSMELDING"),
    JsonSubTypes.Type(value = DbArbeidstakerInntektRequest.Ainntekt::class, name = "AINNTEKT"),
    JsonSubTypes.Type(value = DbArbeidstakerInntektRequest.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
)
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed class DbArbeidstakerInntektRequest {
    abstract val begrunnelse: String?
    abstract val refusjon: List<DbRefusjonsperiode>?

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Inntektsmelding(
        val inntektsmeldingId: String,
        override val begrunnelse: String? = null,
        override val refusjon: List<DbRefusjonsperiode>? = null,
    ) : DbArbeidstakerInntektRequest()

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Ainntekt(
        override val begrunnelse: String,
        override val refusjon: List<DbRefusjonsperiode>? = null,
    ) : DbArbeidstakerInntektRequest()

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Skjønnsfastsatt(
        val årsinntekt: Double,
        val årsak: DbArbeidstakerSkjønnsfastsettelseÅrsak,
        override val begrunnelse: String,
        override val refusjon: List<DbRefusjonsperiode>? = null,
    ) : DbArbeidstakerInntektRequest()
}

enum class DbArbeidstakerSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT,
    MANGELFULL_RAPPORTERING,
    TIDSAVGRENSET,
}

// SELVSTENDIG_NÆRINGSDRIVENDE / INAKTIV
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(
        value = DbPensjonsgivendeInntektRequest.PensjonsgivendeInntekt::class,
        name = "PENSJONSGIVENDE_INNTEKT",
    ),
    JsonSubTypes.Type(value = DbPensjonsgivendeInntektRequest.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
)
sealed class DbPensjonsgivendeInntektRequest {
    abstract val begrunnelse: String

    data class PensjonsgivendeInntekt(
        override val begrunnelse: String,
    ) : DbPensjonsgivendeInntektRequest()

    data class Skjønnsfastsatt(
        val årsinntekt: Double,
        val årsak: DbPensjonsgivendeSkjønnsfastsettelseÅrsak,
        override val begrunnelse: String,
    ) : DbPensjonsgivendeInntektRequest()
}

enum class DbPensjonsgivendeSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT_VARIG_ENDRING,
    SISTE_TRE_YRKESAKTIV,
}

// FRILANSER
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DbFrilanserInntektRequest.Ainntekt::class, name = "AINNTEKT"),
    JsonSubTypes.Type(value = DbFrilanserInntektRequest.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
)
sealed class DbFrilanserInntektRequest {
    abstract val begrunnelse: String

    data class Ainntekt(
        override val begrunnelse: String,
    ) : DbFrilanserInntektRequest()

    data class Skjønnsfastsatt(
        val årsinntekt: Double,
        val årsak: FrilanserSkjønnsfastsettelseÅrsak,
        override val begrunnelse: String,
    ) : DbFrilanserInntektRequest()
}

enum class FrilanserSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT,
    MANGELFULL_RAPPORTERING,
}

// ARBEIDSLEDIG
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DbArbeidsledigInntektRequest.Dagpenger::class, name = "DAGPENGER"),
    JsonSubTypes.Type(value = DbArbeidsledigInntektRequest.Ventelønn::class, name = "VENTELONN"),
    JsonSubTypes.Type(value = DbArbeidsledigInntektRequest.Vartpenger::class, name = "VARTPENGER"),
)
sealed class DbArbeidsledigInntektRequest {
    abstract val begrunnelse: String

    data class Dagpenger(
        val dagbeløp: Int,
        override val begrunnelse: String,
    ) : DbArbeidsledigInntektRequest()

    data class Ventelønn(
        val årsinntekt: Double,
        override val begrunnelse: String,
    ) : DbArbeidsledigInntektRequest()

    data class Vartpenger(
        val årsinntekt: Double,
        override val begrunnelse: String,
    ) : DbArbeidsledigInntektRequest()
}

// Union av alle requests med Jackson discriminator
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "inntektskategori")
@JsonSubTypes(
    JsonSubTypes.Type(value = DbInntektRequest.Arbeidstaker::class, name = "ARBEIDSTAKER"),
    JsonSubTypes.Type(value = DbInntektRequest.SelvstendigNæringsdrivende::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE"),
    JsonSubTypes.Type(value = DbInntektRequest.Inaktiv::class, name = "INAKTIV"),
    JsonSubTypes.Type(value = DbInntektRequest.Frilanser::class, name = "FRILANSER"),
    JsonSubTypes.Type(value = DbInntektRequest.Arbeidsledig::class, name = "ARBEIDSLEDIG"),
)
sealed class DbInntektRequest {
    data class Arbeidstaker(
        val data: DbArbeidstakerInntektRequest,
    ) : DbInntektRequest()

    data class SelvstendigNæringsdrivende(
        val data: DbPensjonsgivendeInntektRequest,
    ) : DbInntektRequest()

    data class Inaktiv(
        val data: DbPensjonsgivendeInntektRequest,
    ) : DbInntektRequest()

    data class Frilanser(
        val data: DbFrilanserInntektRequest,
    ) : DbInntektRequest()

    data class Arbeidsledig(
        val data: DbArbeidsledigInntektRequest,
    ) : DbInntektRequest()
}
