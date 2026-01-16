package no.nav.helse.bakrommet.db.dto.yrkesaktivitet

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "inntektskategori")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = DbYrkesaktivitetKategorisering.Arbeidstaker::class, name = "ARBEIDSTAKER"),
        JsonSubTypes.Type(value = DbYrkesaktivitetKategorisering.Frilanser::class, name = "FRILANSER"),
        JsonSubTypes.Type(
            value = DbYrkesaktivitetKategorisering.SelvstendigNæringsdrivende::class,
            name = "SELVSTENDIG_NÆRINGSDRIVENDE",
        ),
        JsonSubTypes.Type(value = DbYrkesaktivitetKategorisering.Inaktiv::class, name = "INAKTIV"),
        JsonSubTypes.Type(value = DbYrkesaktivitetKategorisering.Arbeidsledig::class, name = "ARBEIDSLEDIG"),
    ],
)
sealed class DbYrkesaktivitetKategorisering {
    abstract val sykmeldt: Boolean

    data class Arbeidstaker(
        override val sykmeldt: Boolean,
        val typeArbeidstaker: DbTypeArbeidstaker,
    ) : DbYrkesaktivitetKategorisering()

    data class Frilanser(
        override val sykmeldt: Boolean,
        val orgnummer: String,
        val forsikring: DbFrilanserForsikring,
    ) : DbYrkesaktivitetKategorisering()

    data class SelvstendigNæringsdrivende(
        override val sykmeldt: Boolean,
        val typeSelvstendigNæringsdrivende: DbTypeSelvstendigNæringsdrivende,
    ) : DbYrkesaktivitetKategorisering()

    // Alltid sykmeldt
    data class Inaktiv(
        override val sykmeldt: Boolean = true,
    ) : DbYrkesaktivitetKategorisering()

    // Alltid sykmeldt
    data class Arbeidsledig(
        override val sykmeldt: Boolean = true,
    ) : DbYrkesaktivitetKategorisering()
}

fun DbYrkesaktivitetKategorisering.maybeOrgnummer(): String? =
    when (this) {
        is DbYrkesaktivitetKategorisering.Arbeidstaker ->
            when (val type = this.typeArbeidstaker) {
                is DbTypeArbeidstaker.Ordinær -> type.orgnummer
                is DbTypeArbeidstaker.Maritim -> type.orgnummer
                is DbTypeArbeidstaker.Fisker -> type.orgnummer
                else -> null
            }

        is DbYrkesaktivitetKategorisering.Frilanser -> this.orgnummer
        else -> null
    }

fun DbYrkesaktivitetKategorisering.orgnummer(): String = maybeOrgnummer() ?: throw IllegalStateException("YrkesaktivitetKategorisering har ikke orgnummer. " + this.javaClass.simpleName)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = DbTypeArbeidstaker.Ordinær::class, name = "ORDINÆR"),
        JsonSubTypes.Type(
            value = DbTypeArbeidstaker.Maritim::class,
            name = "MARITIM",
        ),
        JsonSubTypes.Type(value = DbTypeArbeidstaker.Fisker::class, name = "FISKER"),
        JsonSubTypes.Type(value = DbTypeArbeidstaker.DimmitertVernepliktig::class, name = "DIMMITERT_VERNEPLIKTIG"),
        JsonSubTypes.Type(value = DbTypeArbeidstaker.PrivatArbeidsgiver::class, name = "PRIVAT_ARBEIDSGIVER"),
    ],
)
sealed class DbTypeArbeidstaker {
    data class Ordinær(
        val orgnummer: String,
    ) : DbTypeArbeidstaker()

    data class Maritim(
        val orgnummer: String,
    ) : DbTypeArbeidstaker()

    data class Fisker(
        val orgnummer: String,
    ) : DbTypeArbeidstaker()

    class DimmitertVernepliktig : DbTypeArbeidstaker()

    data class PrivatArbeidsgiver(
        val arbeidsgiverFnr: String,
    ) : DbTypeArbeidstaker()
}

enum class DbFrilanserForsikring {
    FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
    INGEN_FORSIKRING,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = DbTypeSelvstendigNæringsdrivende.Ordinær::class, name = "ORDINÆR"),
        JsonSubTypes.Type(
            value = DbTypeSelvstendigNæringsdrivende.BarnepasserEgetHjem::class,
            name = "BARNEPASSER_EGET_HJEM",
        ),
        JsonSubTypes.Type(value = DbTypeSelvstendigNæringsdrivende.Fisker::class, name = "FISKER"),
        JsonSubTypes.Type(value = DbTypeSelvstendigNæringsdrivende.Jordbruker::class, name = "JORDBRUKER"),
        JsonSubTypes.Type(value = DbTypeSelvstendigNæringsdrivende.Reindrift::class, name = "REINDRIFT"),
    ],
)
sealed class DbTypeSelvstendigNæringsdrivende {
    abstract val forsikring: DbSelvstendigForsikring

    data class Ordinær(
        override val forsikring: DbSelvstendigForsikring,
    ) : DbTypeSelvstendigNæringsdrivende()

    data class BarnepasserEgetHjem(
        override val forsikring: DbSelvstendigForsikring,
    ) : DbTypeSelvstendigNæringsdrivende()

    // Fisker har alltid 100% fra første sykedag implisitt
    data class Fisker(
        override val forsikring: DbSelvstendigForsikring = DbSelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
    ) : DbTypeSelvstendigNæringsdrivende()

    data class Jordbruker(
        override val forsikring: DbSelvstendigForsikring,
    ) : DbTypeSelvstendigNæringsdrivende()

    data class Reindrift(
        override val forsikring: DbSelvstendigForsikring,
    ) : DbTypeSelvstendigNæringsdrivende()
}

enum class DbSelvstendigForsikring {
    FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG,
    FORSIKRING_100_PROSENT_FRA_17_SYKEDAG,
    FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
    INGEN_FORSIKRING,
}
