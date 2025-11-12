package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.FrilanserForsikring
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.SelvstendigForsikring
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.TypeArbeidstaker
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.TypeSelvstendigNæringsdrivende
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.VariantAvInaktiv
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.YrkesaktivitetKategorisering

fun arbeidstakerKategorisering(
    orgnummer: String = "123456789",
    erSykmeldt: Boolean = true,
) = YrkesaktivitetKategorisering.Arbeidstaker(
    sykmeldt = erSykmeldt,
    typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = orgnummer),
)

fun frilanserKategorisering(
    orgnummer: String = "123456789",
    erSykmeldt: Boolean = true,
    forsikring: String = "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG",
) = YrkesaktivitetKategorisering.Frilanser(
    orgnummer = orgnummer,
    sykmeldt = erSykmeldt,
    forsikring = FrilanserForsikring.valueOf(forsikring),
)

fun selvstendigNæringsdrivendeKategorisering(
    erSykmeldt: Boolean = true,
    type: String = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
    forsikring: String = "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG",
): YrkesaktivitetKategorisering {
    val forsikringEnum = SelvstendigForsikring.valueOf(forsikring)
    val typeSelvstendigNæringsdrivende =
        when (type) {
            "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE" -> TypeSelvstendigNæringsdrivende.Ordinær(forsikring = forsikringEnum)
            "BARNEPASSER_EGET_HJEM" -> TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem(forsikring = forsikringEnum)
            "FISKER" -> TypeSelvstendigNæringsdrivende.Fisker()
            "JORDBRUKER" -> TypeSelvstendigNæringsdrivende.Jordbruker(forsikring = forsikringEnum)
            "REINDRIFT" -> TypeSelvstendigNæringsdrivende.Reindrift(forsikring = forsikringEnum)
            else -> throw IllegalArgumentException("Ugyldig type: $type")
        }
    return YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
        sykmeldt = erSykmeldt,
        typeSelvstendigNæringsdrivende = typeSelvstendigNæringsdrivende,
    )
}

fun inaktivKategorisering(variant: String = "INAKTIV_VARIANT_A"): YrkesaktivitetKategorisering =
    YrkesaktivitetKategorisering.Inaktiv(
        variant = VariantAvInaktiv.valueOf(variant),
    )
