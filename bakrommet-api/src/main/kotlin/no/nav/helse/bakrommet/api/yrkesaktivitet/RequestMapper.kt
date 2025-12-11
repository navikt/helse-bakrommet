package no.nav.helse.bakrommet.api.yrkesaktivitet

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.behandling.inntekter.*
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Perioder
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Periodetype
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.*
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.dto.InntektbeløpDto
import java.time.LocalDate
import no.nav.helse.dto.PeriodeDto as SpleisPeriodeDto

fun YrkesaktivitetCreateRequestDto.tilYrkesaktivitetKategorisering(): YrkesaktivitetKategorisering = kategorisering.tilYrkesaktivitetKategorisering()

fun YrkesaktivitetKategoriseringDto.tilYrkesaktivitetKategorisering(): YrkesaktivitetKategorisering =
    when (this) {
        is YrkesaktivitetKategoriseringDto.Arbeidstaker ->
            YrkesaktivitetKategorisering.Arbeidstaker(
                sykmeldt = sykmeldt,
                typeArbeidstaker = typeArbeidstaker.tilTypeArbeidstaker(),
            )

        is YrkesaktivitetKategoriseringDto.Frilanser ->
            YrkesaktivitetKategorisering.Frilanser(
                sykmeldt = sykmeldt,
                orgnummer = orgnummer,
                forsikring = forsikring.tilFrilanserForsikring(),
            )

        is YrkesaktivitetKategoriseringDto.SelvstendigNæringsdrivende ->
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = sykmeldt,
                typeSelvstendigNæringsdrivende = typeSelvstendigNæringsdrivende.tilTypeSelvstendigNæringsdrivende(),
            )

        is YrkesaktivitetKategoriseringDto.Inaktiv ->
            YrkesaktivitetKategorisering.Inaktiv(
                sykmeldt = sykmeldt,
            )

        is YrkesaktivitetKategoriseringDto.Arbeidsledig ->
            YrkesaktivitetKategorisering.Arbeidsledig(
                sykmeldt = sykmeldt,
            )
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
        is TypeSelvstendigNæringsdrivendeDto.Ordinær ->
            TypeSelvstendigNæringsdrivende.Ordinær(
                forsikring = forsikring.tilSelvstendigForsikring(),
            )

        is TypeSelvstendigNæringsdrivendeDto.BarnepasserEgetHjem ->
            TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem(
                forsikring = forsikring.tilSelvstendigForsikring(),
            )

        is TypeSelvstendigNæringsdrivendeDto.Fisker ->
            TypeSelvstendigNæringsdrivende.Fisker(
                forsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
            )

        is TypeSelvstendigNæringsdrivendeDto.Jordbruker ->
            TypeSelvstendigNæringsdrivende.Jordbruker(
                forsikring = forsikring.tilSelvstendigForsikring(),
            )

        is TypeSelvstendigNæringsdrivendeDto.Reindrift ->
            TypeSelvstendigNæringsdrivende.Reindrift(
                forsikring = forsikring.tilSelvstendigForsikring(),
            )
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

fun InntektRequestDto.tilInntektRequest(): InntektRequest =
    when (this) {
        is InntektRequestDto.Arbeidstaker ->
            InntektRequest.Arbeidstaker(
                data = data.tilArbeidstakerInntektRequest(),
            )

        is InntektRequestDto.SelvstendigNæringsdrivende ->
            InntektRequest.SelvstendigNæringsdrivende(
                data = data.tilPensjonsgivendeInntektRequest(),
            )

        is InntektRequestDto.Inaktiv ->
            InntektRequest.Inaktiv(
                data = data.tilPensjonsgivendeInntektRequest(),
            )

        is InntektRequestDto.Frilanser ->
            InntektRequest.Frilanser(
                data = data.tilFrilanserInntektRequest(),
            )

        is InntektRequestDto.Arbeidsledig ->
            InntektRequest.Arbeidsledig(
                data = data.tilArbeidsledigInntektRequest(),
            )
    }

fun ArbeidstakerInntektRequestDto.tilArbeidstakerInntektRequest(): ArbeidstakerInntektRequest =
    when (this) {
        is ArbeidstakerInntektRequestDto.Inntektsmelding ->
            ArbeidstakerInntektRequest.Inntektsmelding(
                inntektsmeldingId = inntektsmeldingId,
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.tilRefusjonsperiode() },
            )

        is ArbeidstakerInntektRequestDto.Ainntekt ->
            ArbeidstakerInntektRequest.Ainntekt(
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.tilRefusjonsperiode() },
            )

        is ArbeidstakerInntektRequestDto.Skjønnsfastsatt ->
            ArbeidstakerInntektRequest.Skjønnsfastsatt(
                årsinntekt = InntektbeløpDto.Årlig(årsinntekt),
                årsak = årsak.tilArbeidstakerSkjønnsfastsettelseÅrsak(),
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.tilRefusjonsperiode() },
            )
    }

fun PensjonsgivendeInntektRequestDto.tilPensjonsgivendeInntektRequest(): PensjonsgivendeInntektRequest =
    when (this) {
        is PensjonsgivendeInntektRequestDto.PensjonsgivendeInntekt ->
            PensjonsgivendeInntektRequest.PensjonsgivendeInntekt(
                begrunnelse = begrunnelse,
            )

        is PensjonsgivendeInntektRequestDto.Skjønnsfastsatt ->
            PensjonsgivendeInntektRequest.Skjønnsfastsatt(
                årsinntekt = InntektbeløpDto.Årlig(årsinntekt),
                årsak = årsak.tilPensjonsgivendeSkjønnsfastsettelseÅrsak(),
                begrunnelse = begrunnelse,
            )
    }

fun FrilanserInntektRequestDto.tilFrilanserInntektRequest(): FrilanserInntektRequest =
    when (this) {
        is FrilanserInntektRequestDto.Ainntekt ->
            FrilanserInntektRequest.Ainntekt(
                begrunnelse = begrunnelse,
            )

        is FrilanserInntektRequestDto.Skjønnsfastsatt ->
            FrilanserInntektRequest.Skjønnsfastsatt(
                årsinntekt = InntektbeløpDto.Årlig(årsinntekt),
                årsak = årsak.tilFrilanserSkjønnsfastsettelseÅrsak(),
                begrunnelse = begrunnelse,
            )
    }

fun ArbeidsledigInntektRequestDto.tilArbeidsledigInntektRequest(): ArbeidsledigInntektRequest =
    when (this) {
        is ArbeidsledigInntektRequestDto.Dagpenger ->
            ArbeidsledigInntektRequest.Dagpenger(
                dagbeløp = InntektbeløpDto.DagligInt(dagbeløp),
                begrunnelse = begrunnelse,
            )

        is ArbeidsledigInntektRequestDto.Ventelønn ->
            ArbeidsledigInntektRequest.Ventelønn(
                årsinntekt = InntektbeløpDto.Årlig(årsinntekt),
                begrunnelse = begrunnelse,
            )

        is ArbeidsledigInntektRequestDto.Vartpenger ->
            ArbeidsledigInntektRequest.Vartpenger(
                årsinntekt = InntektbeløpDto.Årlig(årsinntekt),
                begrunnelse = begrunnelse,
            )
    }

fun ArbeidstakerSkjønnsfastsettelseÅrsakDto.tilArbeidstakerSkjønnsfastsettelseÅrsak(): ArbeidstakerSkjønnsfastsettelseÅrsak =
    when (this) {
        ArbeidstakerSkjønnsfastsettelseÅrsakDto.AVVIK_25_PROSENT -> ArbeidstakerSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT
        ArbeidstakerSkjønnsfastsettelseÅrsakDto.MANGELFULL_RAPPORTERING -> ArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING
        ArbeidstakerSkjønnsfastsettelseÅrsakDto.TIDSAVGRENSET -> ArbeidstakerSkjønnsfastsettelseÅrsak.TIDSAVGRENSET
    }

fun PensjonsgivendeSkjønnsfastsettelseÅrsakDto.tilPensjonsgivendeSkjønnsfastsettelseÅrsak(): PensjonsgivendeSkjønnsfastsettelseÅrsak =
    when (this) {
        PensjonsgivendeSkjønnsfastsettelseÅrsakDto.AVVIK_25_PROSENT_VARIG_ENDRING -> PensjonsgivendeSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT_VARIG_ENDRING
        PensjonsgivendeSkjønnsfastsettelseÅrsakDto.SISTE_TRE_YRKESAKTIV -> PensjonsgivendeSkjønnsfastsettelseÅrsak.SISTE_TRE_YRKESAKTIV
    }

fun FrilanserSkjønnsfastsettelseÅrsakDto.tilFrilanserSkjønnsfastsettelseÅrsak(): FrilanserSkjønnsfastsettelseÅrsak =
    when (this) {
        FrilanserSkjønnsfastsettelseÅrsakDto.AVVIK_25_PROSENT -> FrilanserSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT
        FrilanserSkjønnsfastsettelseÅrsakDto.MANGELFULL_RAPPORTERING -> FrilanserSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING
    }

fun RefusjonsperiodeDto.tilRefusjonsperiode(): Refusjonsperiode =
    Refusjonsperiode(
        fom = fom,
        tom = tom,
        beløp = InntektbeløpDto.MånedligDouble(beløp),
    )

fun PerioderDto.tilPerioder(): Perioder =
    Perioder(
        type = type.tilPeriodetype(),
        perioder = perioder.map { it.tilSpleisPeriodeDto() },
    )

fun PeriodetypeDto.tilPeriodetype(): Periodetype =
    when (this) {
        PeriodetypeDto.ARBEIDSGIVERPERIODE -> Periodetype.ARBEIDSGIVERPERIODE
        PeriodetypeDto.VENTETID -> Periodetype.VENTETID
        PeriodetypeDto.VENTETID_INAKTIV -> Periodetype.VENTETID_INAKTIV
    }

fun PeriodeDto.tilSpleisPeriodeDto(): SpleisPeriodeDto =
    SpleisPeriodeDto(
        fom = LocalDate.parse(fom),
        tom = LocalDate.parse(tom),
    )

fun List<DagDto>.tilJsonNode(): JsonNode = objectMapper.valueToTree(this)
