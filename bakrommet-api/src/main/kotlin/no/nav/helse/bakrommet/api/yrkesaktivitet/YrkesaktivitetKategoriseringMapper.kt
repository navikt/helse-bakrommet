package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.*

fun YrkesaktivitetKategorisering.tilYrkesaktivitetKategoriseringDto(): YrkesaktivitetKategoriseringDto =
    when (this) {
        is YrkesaktivitetKategorisering.Arbeidstaker ->
            YrkesaktivitetKategoriseringDto.Arbeidstaker(
                sykmeldt = sykmeldt,
                typeArbeidstaker = typeArbeidstaker.tilTypeArbeidstakerDto(),
            )

        is YrkesaktivitetKategorisering.Frilanser ->
            YrkesaktivitetKategoriseringDto.Frilanser(
                sykmeldt = sykmeldt,
                orgnummer = orgnummer,
                forsikring = forsikring.tilFrilanserForsikringDto(),
            )

        is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende ->
            YrkesaktivitetKategoriseringDto.SelvstendigNæringsdrivende(
                sykmeldt = sykmeldt,
                typeSelvstendigNæringsdrivende = typeSelvstendigNæringsdrivende.tilTypeSelvstendigNæringsdrivendeDto(),
            )

        is YrkesaktivitetKategorisering.Inaktiv ->
            YrkesaktivitetKategoriseringDto.Inaktiv(
                sykmeldt = sykmeldt,
            )

        is YrkesaktivitetKategorisering.Arbeidsledig ->
            YrkesaktivitetKategoriseringDto.Arbeidsledig(
                sykmeldt = sykmeldt,
            )
    }

fun TypeArbeidstaker.tilTypeArbeidstakerDto(): TypeArbeidstakerDto =
    when (this) {
        is TypeArbeidstaker.Ordinær ->
            TypeArbeidstakerDto.Ordinær(orgnummer = orgnummer)

        is TypeArbeidstaker.Maritim ->
            TypeArbeidstakerDto.Maritim(orgnummer = orgnummer)

        is TypeArbeidstaker.Fisker ->
            TypeArbeidstakerDto.Fisker(orgnummer = orgnummer)

        is TypeArbeidstaker.DimmitertVernepliktig ->
            TypeArbeidstakerDto.DimmitertVernepliktig(tjenesteMerEnn28Dager = tjenesteMerEnn28Dager)

        is TypeArbeidstaker.PrivatArbeidsgiver ->
            TypeArbeidstakerDto.PrivatArbeidsgiver(arbeidsgiverFnr = arbeidsgiverFnr)
    }

fun TypeSelvstendigNæringsdrivende.tilTypeSelvstendigNæringsdrivendeDto(): TypeSelvstendigNæringsdrivendeDto =
    when (this) {
        is TypeSelvstendigNæringsdrivende.Ordinær ->
            TypeSelvstendigNæringsdrivendeDto.Ordinær(
                forsikring = forsikring.tilSelvstendigForsikringDto(),
            )

        is TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem ->
            TypeSelvstendigNæringsdrivendeDto.BarnepasserEgetHjem(
                forsikring = forsikring.tilSelvstendigForsikringDto(),
            )

        is TypeSelvstendigNæringsdrivende.Fisker ->
            TypeSelvstendigNæringsdrivendeDto.Fisker(
                forsikring = FrilanserForsikringDto.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
            )

        is TypeSelvstendigNæringsdrivende.Jordbruker ->
            TypeSelvstendigNæringsdrivendeDto.Jordbruker(
                forsikring = forsikring.tilSelvstendigForsikringDto(),
            )

        is TypeSelvstendigNæringsdrivende.Reindrift ->
            TypeSelvstendigNæringsdrivendeDto.Reindrift(
                forsikring = forsikring.tilSelvstendigForsikringDto(),
            )
    }

fun FrilanserForsikring.tilFrilanserForsikringDto(): FrilanserForsikringDto =
    when (this) {
        FrilanserForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG -> FrilanserForsikringDto.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG
        FrilanserForsikring.INGEN_FORSIKRING -> FrilanserForsikringDto.INGEN_FORSIKRING
    }

fun SelvstendigForsikring.tilSelvstendigForsikringDto(): SelvstendigForsikringDto =
    when (this) {
        SelvstendigForsikring.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG -> SelvstendigForsikringDto.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG
        SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG -> SelvstendigForsikringDto.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG
        SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG -> SelvstendigForsikringDto.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG
        SelvstendigForsikring.INGEN_FORSIKRING -> SelvstendigForsikringDto.INGEN_FORSIKRING
    }
