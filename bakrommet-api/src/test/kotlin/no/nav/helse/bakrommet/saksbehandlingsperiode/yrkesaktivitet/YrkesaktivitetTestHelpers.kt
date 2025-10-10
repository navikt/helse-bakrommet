package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

fun arbeidstakerKategorisering(
    orgnummer: String = "123456789",
    erSykmeldt: Boolean = true,
    typeArbeidstaker: String = "ORDINÆRT_ARBEIDSFORHOLD",
): Map<String, String> =
    mapOf(
        "INNTEKTSKATEGORI" to "ARBEIDSTAKER",
        "ORGNUMMER" to orgnummer,
        "ER_SYKMELDT" to if (erSykmeldt) "ER_SYKMELDT_JA" else "ER_SYKMELDT_NEI",
        "TYPE_ARBEIDSTAKER" to typeArbeidstaker,
    )

fun frilanserKategorisering(
    orgnummer: String = "123456789",
    erSykmeldt: Boolean = true,
    forsikring: String = "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG",
): Map<String, String> =
    mapOf(
        "INNTEKTSKATEGORI" to "FRILANSER",
        "ORGNUMMER" to orgnummer,
        "ER_SYKMELDT" to if (erSykmeldt) "ER_SYKMELDT_JA" else "ER_SYKMELDT_NEI",
        "FRILANSER_FORSIKRING" to forsikring,
    )

fun selvstendigNæringsdrivendeKategorisering(
    erSykmeldt: Boolean = true,
    type: String = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
    forsikring: String = "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG",
): Map<String, String> =
    mapOf(
        "INNTEKTSKATEGORI" to "SELVSTENDIG_NÆRINGSDRIVENDE",
        "ER_SYKMELDT" to if (erSykmeldt) "ER_SYKMELDT_JA" else "ER_SYKMELDT_NEI",
        "TYPE_SELVSTENDIG_NÆRINGSDRIVENDE" to type,
        "SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING" to forsikring,
    )

fun inaktivKategorisering(variant: String = "INAKTIV_VARIANT_A"): Map<String, String> =
    mapOf(
        "INNTEKTSKATEGORI" to "INAKTIV",
        "VARIANT_AV_INAKTIV" to variant,
    )

fun arbeidsledigKategorisering(): Map<String, String> =
    mapOf(
        "INNTEKTSKATEGORI" to "ARBEIDSLEDIG",
    )
