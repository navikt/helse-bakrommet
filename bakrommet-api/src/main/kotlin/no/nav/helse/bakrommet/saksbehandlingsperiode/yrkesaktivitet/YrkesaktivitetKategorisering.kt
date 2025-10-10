package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

sealed class YrkesaktivitetKategorisering {
    abstract val inntektskategori: Inntektskategori
    abstract val sykmeldt: Boolean

    data class Arbeidstaker(
        override val inntektskategori: Inntektskategori = Inntektskategori.ARBEIDSTAKER,
        override val sykmeldt: Boolean,
        val orgnummer: String,
        val typeArbeidstaker: TypeArbeidstaker,
    ) : YrkesaktivitetKategorisering()

    data class Frilanser(
        override val inntektskategori: Inntektskategori = Inntektskategori.FRILANSER,
        override val sykmeldt: Boolean,
        val orgnummer: String,
        val forsikring: FrilanserForsikring,
    ) : YrkesaktivitetKategorisering()

    data class SelvstendigNæringsdrivende(
        override val inntektskategori: Inntektskategori = Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE,
        override val sykmeldt: Boolean,
        val type: TypeSelvstendigNæringsdrivende,
    ) : YrkesaktivitetKategorisering()

    // Alltid sykmeldt
    data class Inaktiv(
        override val inntektskategori: Inntektskategori = Inntektskategori.INAKTIV,
        override val sykmeldt: Boolean = true,
        val variant: VariantAvInaktiv,
    ) : YrkesaktivitetKategorisering()

    // Alltid sykmeldt
    data class Arbeidsledig(
        override val inntektskategori: Inntektskategori = Inntektskategori.ARBEIDSLEDIG,
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
