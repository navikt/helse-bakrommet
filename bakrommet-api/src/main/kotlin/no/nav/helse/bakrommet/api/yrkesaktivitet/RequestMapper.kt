package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.domain.sykepenger.Periode
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.*
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt

fun YrkesaktivitetCreateRequestDto.tilYrkesaktivitetKategorisering(): YrkesaktivitetKategorisering = kategorisering.tilYrkesaktivitetKategorisering()

fun YrkesaktivitetKategoriseringDto.tilYrkesaktivitetKategorisering(): YrkesaktivitetKategorisering =
    when (this) {
        is YrkesaktivitetKategoriseringDto.Arbeidstaker -> {
            YrkesaktivitetKategorisering.Arbeidstaker(
                sykmeldt = sykmeldt,
                typeArbeidstaker = typeArbeidstaker.tilTypeArbeidstaker(),
            )
        }

        is YrkesaktivitetKategoriseringDto.Frilanser -> {
            YrkesaktivitetKategorisering.Frilanser(
                sykmeldt = sykmeldt,
                orgnummer = orgnummer,
                forsikring = forsikring.tilFrilanserForsikring(),
            )
        }

        is YrkesaktivitetKategoriseringDto.SelvstendigNæringsdrivende -> {
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = sykmeldt,
                typeSelvstendigNæringsdrivende = typeSelvstendigNæringsdrivende.tilTypeSelvstendigNæringsdrivende(),
            )
        }

        is YrkesaktivitetKategoriseringDto.Inaktiv -> {
            YrkesaktivitetKategorisering.Inaktiv(
                sykmeldt = sykmeldt,
            )
        }

        is YrkesaktivitetKategoriseringDto.Arbeidsledig -> {
            YrkesaktivitetKategorisering.Arbeidsledig(
                sykmeldt = sykmeldt,
            )
        }
    }

fun TypeArbeidstakerDto.tilTypeArbeidstaker(): TypeArbeidstaker =
    when (this) {
        is TypeArbeidstakerDto.Ordinær -> TypeArbeidstaker.Ordinær(orgnummer = orgnummer)
        is TypeArbeidstakerDto.Maritim -> TypeArbeidstaker.Maritim(orgnummer = orgnummer)
        is TypeArbeidstakerDto.Fisker -> TypeArbeidstaker.Fisker(orgnummer = orgnummer)
        is TypeArbeidstakerDto.DimmitertVernepliktig -> TypeArbeidstaker.DimmitertVernepliktig()
        is TypeArbeidstakerDto.PrivatArbeidsgiver -> TypeArbeidstaker.PrivatArbeidsgiver(arbeidsgiverFnr = arbeidsgiverFnr)
    }

fun TypeSelvstendigNæringsdrivendeDto.tilTypeSelvstendigNæringsdrivende(): TypeSelvstendigNæringsdrivende =
    when (this) {
        is TypeSelvstendigNæringsdrivendeDto.Ordinær -> {
            TypeSelvstendigNæringsdrivende.Ordinær(
                forsikring = forsikring.tilSelvstendigForsikring(),
            )
        }

        is TypeSelvstendigNæringsdrivendeDto.BarnepasserEgetHjem -> {
            TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem(
                forsikring = forsikring.tilSelvstendigForsikring(),
            )
        }

        is TypeSelvstendigNæringsdrivendeDto.Fisker -> {
            TypeSelvstendigNæringsdrivende.Fisker(
                forsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
            )
        }

        is TypeSelvstendigNæringsdrivendeDto.Jordbruker -> {
            TypeSelvstendigNæringsdrivende.Jordbruker(
                forsikring = forsikring.tilSelvstendigForsikring(),
            )
        }

        is TypeSelvstendigNæringsdrivendeDto.Reindrift -> {
            TypeSelvstendigNæringsdrivende.Reindrift(
                forsikring = forsikring.tilSelvstendigForsikring(),
            )
        }
    }

fun FrilanserForsikringDto.tilFrilanserForsikring(): FrilanserForsikring =
    when (this) {
        FrilanserForsikringDto.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG -> FrilanserForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG
        FrilanserForsikringDto.INGEN_FORSIKRING -> FrilanserForsikring.INGEN_FORSIKRING
    }

fun SelvstendigForsikringDto.tilSelvstendigForsikring(): SelvstendigForsikring =
    when (this) {
        SelvstendigForsikringDto.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG -> SelvstendigForsikring.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG
        SelvstendigForsikringDto.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG -> SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG
        SelvstendigForsikringDto.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG -> SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG
        SelvstendigForsikringDto.INGEN_FORSIKRING -> SelvstendigForsikring.INGEN_FORSIKRING
    }

fun PerioderDto.tilPerioder(): Perioder =
    Perioder(
        type = type.tilPeriodetype(),
        perioder = perioder.map { Periode(fom = it.fom, tom = it.tom) },
    )

fun PeriodetypeDto.tilPeriodetype(): Periodetype =
    when (this) {
        PeriodetypeDto.ARBEIDSGIVERPERIODE -> Periodetype.ARBEIDSGIVERPERIODE
        PeriodetypeDto.VENTETID -> Periodetype.VENTETID
        PeriodetypeDto.VENTETID_INAKTIV -> Periodetype.VENTETID_INAKTIV
    }

fun RefusjonsperiodeDto.tilRefusjonsperiode(): Refusjonsperiode =
    Refusjonsperiode(
        fom = fom,
        tom = tom,
        beløp = Inntekt.gjenopprett(InntektbeløpDto.MånedligDouble(beløp)),
    )
