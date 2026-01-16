package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

sealed class YrkesaktivitetKategorisering {
    abstract val sykmeldt: Boolean

    data class Arbeidstaker(
        override val sykmeldt: Boolean,
        val typeArbeidstaker: TypeArbeidstaker,
    ) : YrkesaktivitetKategorisering()

    data class Frilanser(
        override val sykmeldt: Boolean,
        val orgnummer: String,
        val forsikring: FrilanserForsikring,
    ) : YrkesaktivitetKategorisering()

    data class SelvstendigNæringsdrivende(
        override val sykmeldt: Boolean,
        val typeSelvstendigNæringsdrivende: TypeSelvstendigNæringsdrivende,
    ) : YrkesaktivitetKategorisering()

    // Alltid sykmeldt
    data class Inaktiv(
        override val sykmeldt: Boolean = true,
    ) : YrkesaktivitetKategorisering()

    // Alltid sykmeldt
    data class Arbeidsledig(
        override val sykmeldt: Boolean = true,
    ) : YrkesaktivitetKategorisering()
}

fun YrkesaktivitetKategorisering.maybeOrgnummer(): String? =
    when (this) {
        is YrkesaktivitetKategorisering.Arbeidstaker ->
            when (val type = this.typeArbeidstaker) {
                is TypeArbeidstaker.Ordinær -> type.orgnummer
                is TypeArbeidstaker.Maritim -> type.orgnummer
                is TypeArbeidstaker.Fisker -> type.orgnummer
                else -> null
            }

        is YrkesaktivitetKategorisering.Frilanser -> this.orgnummer
        else -> null
    }

fun YrkesaktivitetKategorisering.orgnummer(): String = maybeOrgnummer() ?: throw IllegalStateException("YrkesaktivitetKategorisering har ikke orgnummer. " + this.javaClass.simpleName)

sealed class TypeArbeidstaker {
    data class Ordinær(
        val orgnummer: String,
    ) : TypeArbeidstaker()

    data class Maritim(
        val orgnummer: String,
    ) : TypeArbeidstaker()

    data class Fisker(
        val orgnummer: String,
    ) : TypeArbeidstaker()

    class DimmitertVernepliktig : TypeArbeidstaker()

    data class PrivatArbeidsgiver(
        val arbeidsgiverFnr: String,
    ) : TypeArbeidstaker()
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
