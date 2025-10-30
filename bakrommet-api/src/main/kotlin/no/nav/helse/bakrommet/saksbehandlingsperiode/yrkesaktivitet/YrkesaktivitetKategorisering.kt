package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "inntektskategori")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = YrkesaktivitetKategorisering.Arbeidstaker::class, name = "ARBEIDSTAKER"),
        JsonSubTypes.Type(value = YrkesaktivitetKategorisering.Frilanser::class, name = "FRILANSER"),
        JsonSubTypes.Type(value = YrkesaktivitetKategorisering.SelvstendigNæringsdrivende::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE"),
        JsonSubTypes.Type(value = YrkesaktivitetKategorisering.Inaktiv::class, name = "INAKTIV"),
        JsonSubTypes.Type(value = YrkesaktivitetKategorisering.Arbeidsledig::class, name = "ARBEIDSLEDIG"),
    ],
)
sealed class YrkesaktivitetKategorisering {
    abstract val sykmeldt: Boolean

    data class Arbeidstaker(
        override val sykmeldt: Boolean,
        val orgnummer: String,
        val typeArbeidstaker: TypeArbeidstaker,
    ) : YrkesaktivitetKategorisering()

    data class Frilanser(
        override val sykmeldt: Boolean,
        val orgnummer: String,
        val forsikring: FrilanserForsikring,
    ) : YrkesaktivitetKategorisering()

    data class SelvstendigNæringsdrivende(
        override val sykmeldt: Boolean,
        val type: TypeSelvstendigNæringsdrivende,
    ) : YrkesaktivitetKategorisering()

    // Alltid sykmeldt
    data class Inaktiv(
        override val sykmeldt: Boolean = true,
        val variant: VariantAvInaktiv,
    ) : YrkesaktivitetKategorisering()

    // Alltid sykmeldt
    data class Arbeidsledig(
        override val sykmeldt: Boolean = true,
    ) : YrkesaktivitetKategorisering()
}

enum class Inntektskategori {
    ARBEIDSTAKER,
    FRILANSER,
    SELVSTENDIG_NÆRINGSDRIVENDE,
    INAKTIV,
    ARBEIDSLEDIG,
}

enum class TypeArbeidstaker {
    ORDINÆRT_ARBEIDSFORHOLD,
    MARITIMT_ARBEIDSFORHOLD,
    FISKER,
    VERNEPLIKTIG,
    DAGMAMMA_BARNETS_HJEM,
}

enum class FrilanserForsikring {
    FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
    INGEN_FORSIKRING,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = TypeSelvstendigNæringsdrivende.Ordinær::class, name = "ORDINÆR"),
        JsonSubTypes.Type(value = TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem::class, name = "BARNEPASSER_EGET_HJEM"),
        JsonSubTypes.Type(value = TypeSelvstendigNæringsdrivende.Fisker::class, name = "FISKER"),
        JsonSubTypes.Type(value = TypeSelvstendigNæringsdrivende.Jordbruker::class, name = "JORDBRUKER"),
        JsonSubTypes.Type(value = TypeSelvstendigNæringsdrivende.Reindrift::class, name = "REINDRIFT"),
    ],
)
sealed class TypeSelvstendigNæringsdrivende {
    abstract val forsikring: SelvstendigForsikring

    data class Ordinær(
        override val forsikring: SelvstendigForsikring,
    ) : TypeSelvstendigNæringsdrivende()

    data class BarnepasserEgetHjem(
        override val forsikring: SelvstendigForsikring,
    ) : TypeSelvstendigNæringsdrivende()

    // Fisker har alltid 100% fra første sykedag implisitt
    data class Fisker(
        override val forsikring: SelvstendigForsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
    ) : TypeSelvstendigNæringsdrivende()

    data class Jordbruker(
        override val forsikring: SelvstendigForsikring,
    ) : TypeSelvstendigNæringsdrivende()

    data class Reindrift(
        override val forsikring: SelvstendigForsikring,
    ) : TypeSelvstendigNæringsdrivende()
}

enum class SelvstendigForsikring {
    FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG,
    FORSIKRING_100_PROSENT_FRA_17_SYKEDAG,
    FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
    INGEN_FORSIKRING,
}

enum class VariantAvInaktiv {
    INAKTIV_VARIANT_A,
    INAKTIV_VARIANT_B,
}
