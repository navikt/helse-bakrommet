package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.bakrommet.errorhandling.InputValideringException

fun Map<String, String>.fromMap(): YrkesaktivitetKategorisering = YrkesaktivitetKategoriseringMapper.fromMap(this)

object YrkesaktivitetKategoriseringMapper {
    fun fromMap(map: Map<String, String>): YrkesaktivitetKategorisering {
        val inntektskategori =
            map["INNTEKTSKATEGORI"]
                ?: throw InputValideringException("INNTEKTSKATEGORI mangler")

        return when (inntektskategori) {
            "ARBEIDSTAKER" -> mapArbeidstaker(map)
            "FRILANSER" -> mapFrilanser(map)
            "SELVSTENDIG_NÆRINGSDRIVENDE" -> mapSelvstendigNæringsdrivende(map)
            "INAKTIV" -> mapInaktiv(map)
            "ARBEIDSLEDIG" -> YrkesaktivitetKategorisering.Arbeidsledig()
            else -> throw InputValideringException("Ugyldig INNTEKTSKATEGORI: $inntektskategori")
        }
    }

    private fun mapArbeidstaker(map: Map<String, String>): YrkesaktivitetKategorisering.Arbeidstaker {
        val orgnummer =
            map["ORGNUMMER"]
                ?: throw InputValideringException("ORGNUMMER mangler for ARBEIDSTAKER")

        val sykmeldt = mapSykmeldt(map, "ARBEIDSTAKER")

        val typeArbeidstaker =
            map["TYPE_ARBEIDSTAKER"]?.let {
                try {
                    TypeArbeidstaker.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    throw InputValideringException("Ugyldig TYPE_ARBEIDSTAKER: $it")
                }
            } ?: throw InputValideringException("TYPE_ARBEIDSTAKER mangler for ARBEIDSTAKER")

        return YrkesaktivitetKategorisering.Arbeidstaker(
            orgnummer = orgnummer,
            sykmeldt = sykmeldt,
            typeArbeidstaker = typeArbeidstaker,
        )
    }

    private fun mapFrilanser(map: Map<String, String>): YrkesaktivitetKategorisering.Frilanser {
        val orgnummer =
            map["ORGNUMMER"]
                ?: throw InputValideringException("ORGNUMMER mangler for FRILANSER")

        val sykmeldt = mapSykmeldt(map, "FRILANSER")

        val forsikring =
            map["FRILANSER_FORSIKRING"]?.let {
                try {
                    FrilanserForsikring.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    throw InputValideringException("Ugyldig FRILANSER_FORSIKRING: $it")
                }
            } ?: throw InputValideringException("FRILANSER_FORSIKRING mangler for FRILANSER")

        return YrkesaktivitetKategorisering.Frilanser(
            orgnummer = orgnummer,
            sykmeldt = sykmeldt,
            forsikring = forsikring,
        )
    }

    private fun mapSelvstendigNæringsdrivende(map: Map<String, String>): YrkesaktivitetKategorisering.SelvstendigNæringsdrivende {
        val sykmeldt = mapSykmeldt(map, "SELVSTENDIG_NÆRINGSDRIVENDE")

        val typeString =
            map["TYPE_SELVSTENDIG_NÆRINGSDRIVENDE"]
                ?: throw InputValideringException("TYPE_SELVSTENDIG_NÆRINGSDRIVENDE mangler")

        val type =
            when (typeString) {
                "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE" -> {
                    TypeSelvstendigNæringsdrivende.Ordinær(
                        forsikring = mapSelvstendigForsikring(map),
                    )
                }
                "BARNEPASSER_EGET_HJEM" -> {
                    TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem(
                        forsikring = mapSelvstendigForsikring(map),
                    )
                }
                "FISKER" -> {
                    TypeSelvstendigNæringsdrivende.Fisker()
                }
                "JORDBRUKER" -> {
                    TypeSelvstendigNæringsdrivende.Jordbruker(
                        forsikring = mapSelvstendigForsikring(map),
                    )
                }
                "REINDRIFT" -> {
                    TypeSelvstendigNæringsdrivende.Reindrift(
                        forsikring = mapSelvstendigForsikring(map),
                    )
                }
                else -> throw InputValideringException("Ugyldig TYPE_SELVSTENDIG_NÆRINGSDRIVENDE: $typeString")
            }

        return YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
            sykmeldt = sykmeldt,
            typeSelvstendigNæringsdrivende = type,
        )
    }

    private fun mapSelvstendigForsikring(map: Map<String, String>): SelvstendigForsikring {
        val forsikringString =
            map["SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING"]
                ?: throw InputValideringException("SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING mangler")

        return try {
            SelvstendigForsikring.valueOf(forsikringString)
        } catch (e: IllegalArgumentException) {
            throw InputValideringException("Ugyldig SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING: $forsikringString")
        }
    }

    private fun mapInaktiv(map: Map<String, String>): YrkesaktivitetKategorisering.Inaktiv {
        val variantString =
            map["VARIANT_AV_INAKTIV"]
                ?: throw InputValideringException("VARIANT_AV_INAKTIV mangler for INAKTIV")

        val variant =
            try {
                VariantAvInaktiv.valueOf(variantString)
            } catch (e: IllegalArgumentException) {
                throw InputValideringException("Ugyldig VARIANT_AV_INAKTIV: $variantString")
            }

        return YrkesaktivitetKategorisering.Inaktiv(
            variant = variant,
        )
    }

    private fun mapSykmeldt(
        map: Map<String, String>,
        inntektskategori: String,
    ): Boolean {
        val sykmeldtString =
            map["ER_SYKMELDT"]
                ?: throw InputValideringException("ER_SYKMELDT mangler for $inntektskategori")

        return when (sykmeldtString) {
            "ER_SYKMELDT_JA" -> true
            "ER_SYKMELDT_NEI" -> false
            else -> throw InputValideringException("Ugyldig ER_SYKMELDT: $sykmeldtString")
        }
    }

    fun toMap(kategorisering: YrkesaktivitetKategorisering): Map<String, String> {
        val map = mutableMapOf<String, String>()

        map["INNTEKTSKATEGORI"] =
            when (kategorisering) {
                is YrkesaktivitetKategorisering.Arbeidstaker -> "ARBEIDSTAKER"
                is YrkesaktivitetKategorisering.Frilanser -> "FRILANSER"
                is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> "SELVSTENDIG_NÆRINGSDRIVENDE"
                is YrkesaktivitetKategorisering.Inaktiv -> "INAKTIV"
                is YrkesaktivitetKategorisering.Arbeidsledig -> "ARBEIDSLEDIG"
            }

        // Legg til sykmeldt (ikke for ARBEIDSLEDIG og INAKTIV siden de alltid er sykmeldt)
        if (kategorisering !is YrkesaktivitetKategorisering.Arbeidsledig && kategorisering !is YrkesaktivitetKategorisering.Inaktiv) {
            map["ER_SYKMELDT"] = if (kategorisering.sykmeldt) "ER_SYKMELDT_JA" else "ER_SYKMELDT_NEI"
        } else {
            // For INAKTIV og ARBEIDSLEDIG, sett alltid til JA
            map["ER_SYKMELDT"] = "ER_SYKMELDT_JA"
        }

        when (kategorisering) {
            is YrkesaktivitetKategorisering.Arbeidstaker -> {
                map["ORGNUMMER"] = kategorisering.orgnummer
                map["TYPE_ARBEIDSTAKER"] = kategorisering.typeArbeidstaker.name
            }
            is YrkesaktivitetKategorisering.Frilanser -> {
                map["ORGNUMMER"] = kategorisering.orgnummer
                map["FRILANSER_FORSIKRING"] = kategorisering.forsikring.name
            }
            is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> {
                val type = kategorisering.typeSelvstendigNæringsdrivende
                map["TYPE_SELVSTENDIG_NÆRINGSDRIVENDE"] =
                    when (type) {
                        is TypeSelvstendigNæringsdrivende.Ordinær -> "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE"
                        is TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem -> "BARNEPASSER_EGET_HJEM"
                        is TypeSelvstendigNæringsdrivende.Fisker -> "FISKER"
                        is TypeSelvstendigNæringsdrivende.Jordbruker -> "JORDBRUKER"
                        is TypeSelvstendigNæringsdrivende.Reindrift -> "REINDRIFT"
                    }
                map["SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING"] = type.forsikring.name
            }
            is YrkesaktivitetKategorisering.Inaktiv -> {
                map["VARIANT_AV_INAKTIV"] = kategorisering.variant.name
            }
            is YrkesaktivitetKategorisering.Arbeidsledig -> {
                // Ingen ekstra felter
            }
        }

        return map
    }
}
