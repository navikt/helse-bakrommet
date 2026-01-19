package no.nav.helse.bakrommet.db.repository

import no.nav.helse.bakrommet.db.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.domain.sykepenger.*
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.*
import no.nav.helse.bakrommet.serialisertTilString
import no.nav.helse.bakrommet.toJsonNode
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt

internal fun YrkesaktivitetKategorisering.toDb(): DbYrkesaktivitetKategorisering =
    when (this) {
        is YrkesaktivitetKategorisering.Arbeidstaker -> {
            DbYrkesaktivitetKategorisering.Arbeidstaker(
                sykmeldt = sykmeldt,
                typeArbeidstaker = typeArbeidstaker.toDb(),
            )
        }

        is YrkesaktivitetKategorisering.Frilanser -> {
            DbYrkesaktivitetKategorisering.Frilanser(
                sykmeldt = sykmeldt,
                orgnummer = orgnummer,
                forsikring = forsikring.toDb(),
            )
        }

        is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> {
            DbYrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = sykmeldt,
                typeSelvstendigNæringsdrivende = typeSelvstendigNæringsdrivende.toDb(),
            )
        }

        is YrkesaktivitetKategorisering.Inaktiv -> {
            DbYrkesaktivitetKategorisering.Inaktiv(
                sykmeldt = sykmeldt,
            )
        }

        is YrkesaktivitetKategorisering.Arbeidsledig -> {
            DbYrkesaktivitetKategorisering.Arbeidsledig(
                sykmeldt = sykmeldt,
            )
        }
    }

internal fun TypeArbeidstaker.toDb(): DbTypeArbeidstaker =
    when (this) {
        is TypeArbeidstaker.Ordinær -> {
            DbTypeArbeidstaker.Ordinær(orgnummer = orgnummer)
        }

        is TypeArbeidstaker.Maritim -> {
            DbTypeArbeidstaker.Maritim(orgnummer = orgnummer)
        }

        is TypeArbeidstaker.Fisker -> {
            DbTypeArbeidstaker.Fisker(orgnummer = orgnummer)
        }

        is TypeArbeidstaker.DimmitertVernepliktig -> {
            DbTypeArbeidstaker.DimmitertVernepliktig()
        }

        is TypeArbeidstaker.PrivatArbeidsgiver -> {
            DbTypeArbeidstaker.PrivatArbeidsgiver(arbeidsgiverFnr = arbeidsgiverFnr)
        }
    }

internal fun TypeSelvstendigNæringsdrivende.toDb(): DbTypeSelvstendigNæringsdrivende =
    when (this) {
        is TypeSelvstendigNæringsdrivende.Ordinær -> {
            DbTypeSelvstendigNæringsdrivende.Ordinær(forsikring = forsikring.toDb())
        }

        is TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem -> {
            DbTypeSelvstendigNæringsdrivende.BarnepasserEgetHjem(forsikring = forsikring.toDb())
        }

        is TypeSelvstendigNæringsdrivende.Fisker -> {
            DbTypeSelvstendigNæringsdrivende.Fisker(forsikring = forsikring.toDb())
        }

        is TypeSelvstendigNæringsdrivende.Jordbruker -> {
            DbTypeSelvstendigNæringsdrivende.Jordbruker(forsikring = forsikring.toDb())
        }

        is TypeSelvstendigNæringsdrivende.Reindrift -> {
            DbTypeSelvstendigNæringsdrivende.Reindrift(forsikring = forsikring.toDb())
        }
    }

internal fun FrilanserForsikring.toDb(): DbFrilanserForsikring =
    when (this) {
        FrilanserForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG -> {
            DbFrilanserForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG
        }

        FrilanserForsikring.INGEN_FORSIKRING -> {
            DbFrilanserForsikring.INGEN_FORSIKRING
        }
    }

internal fun SelvstendigForsikring.toDb(): DbSelvstendigForsikring =
    when (this) {
        SelvstendigForsikring.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG -> {
            DbSelvstendigForsikring.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG
        }

        SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG -> {
            DbSelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG
        }

        SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG -> {
            DbSelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG
        }

        SelvstendigForsikring.INGEN_FORSIKRING -> {
            DbSelvstendigForsikring.INGEN_FORSIKRING
        }
    }

internal fun Dagoversikt.toDb(): DbDagoversikt =
    DbDagoversikt(
        sykdomstidlinje = sykdomstidlinje.map { it.toDb() },
        avslagsdager = avslagsdager.map { it.toDb() },
    )

internal fun Dag.toDb(): DbDag =
    DbDag(
        dato = dato,
        dagtype = dagtype.toDb(),
        grad = grad,
        avslåttBegrunnelse = avslåttBegrunnelse,
        andreYtelserBegrunnelse = andreYtelserBegrunnelse,
        kilde = kilde?.toDb(),
    )

internal fun Dagtype.toDb(): DbDagtype =
    when (this) {
        Dagtype.Syk -> DbDagtype.Syk
        Dagtype.SykNav -> DbDagtype.SykNav
        Dagtype.Arbeidsdag -> DbDagtype.Arbeidsdag
        Dagtype.Ferie -> DbDagtype.Ferie
        Dagtype.Permisjon -> DbDagtype.Permisjon
        Dagtype.Avslått -> DbDagtype.Avslått
        Dagtype.AndreYtelser -> DbDagtype.AndreYtelser
        Dagtype.Behandlingsdag -> DbDagtype.Behandlingsdag
    }

internal fun Kilde.toDb(): DbKilde =
    when (this) {
        Kilde.Søknad -> DbKilde.Søknad
        Kilde.Saksbehandler -> DbKilde.Saksbehandler
    }

internal fun Perioder.toDb(): DbPerioder =
    DbPerioder(
        type = type.toDb(),
        perioder = perioder.map { it.toDb() },
    )

internal fun Periodetype.toDb(): DbPeriodetype =
    when (this) {
        Periodetype.ARBEIDSGIVERPERIODE -> DbPeriodetype.ARBEIDSGIVERPERIODE
        Periodetype.VENTETID -> DbPeriodetype.VENTETID
        Periodetype.VENTETID_INAKTIV -> DbPeriodetype.VENTETID_INAKTIV
    }

internal fun Periode.toDb(): DbPeriode =
    DbPeriode(
        fom = fom,
        tom = tom,
    )

internal fun Refusjonsperiode.toDb(): DbRefusjonsperiode =
    DbRefusjonsperiode(
        fom = fom,
        tom = tom,
        beløp = beløp.toDbInntektMånedlig(),
    )

internal fun InntektData.toDb(): DbInntektData =
    when (this) {
        is InntektData.ArbeidstakerInntektsmelding -> {
            DbInntektData.ArbeidstakerInntektsmelding(
                inntektsmeldingId = inntektsmeldingId,
                inntektsmelding = inntektsmelding.toJsonNode(),
                omregnetÅrsinntekt = omregnetÅrsinntekt.toDbInntektÅrlig(),
                sporing = sporing.toDb(),
            )
        }

        is InntektData.ArbeidstakerAinntekt -> {
            DbInntektData.ArbeidstakerAinntekt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.toDbInntektÅrlig(),
                sporing = sporing.toDb(),
                kildedata = kildedata.mapValues { it.value.toDbInntektMånedlig() },
            )
        }

        is InntektData.ArbeidstakerSkjønnsfastsatt -> {
            DbInntektData.ArbeidstakerSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.toDbInntektÅrlig(),
                sporing = sporing.toDb(),
            )
        }

        is InntektData.FrilanserAinntekt -> {
            DbInntektData.FrilanserAinntekt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.toDbInntektÅrlig(),
                sporing = sporing.toDb(),
                kildedata = kildedata.mapValues { it.value.toDbInntektMånedlig() },
            )
        }

        is InntektData.FrilanserSkjønnsfastsatt -> {
            DbInntektData.FrilanserSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.toDbInntektÅrlig(),
                sporing = sporing.toDb(),
            )
        }

        is InntektData.Arbeidsledig -> {
            DbInntektData.Arbeidsledig(
                omregnetÅrsinntekt = omregnetÅrsinntekt.toDbInntektÅrlig(),
                sporing = sporing.toDb(),
            )
        }

        is InntektData.InaktivSkjønnsfastsatt -> {
            DbInntektData.InaktivSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.toDbInntektÅrlig(),
                sporing = sporing.toDb(),
            )
        }

        is InntektData.InaktivPensjonsgivende -> {
            DbInntektData.InaktivPensjonsgivende(
                omregnetÅrsinntekt = omregnetÅrsinntekt.toDbInntektÅrlig(),
                sporing = sporing.toDb(),
                pensjonsgivendeInntekt = pensjonsgivendeInntekt.toDb(),
            )
        }

        is InntektData.SelvstendigNæringsdrivendePensjonsgivende -> {
            DbInntektData.SelvstendigNæringsdrivendePensjonsgivende(
                omregnetÅrsinntekt = omregnetÅrsinntekt.toDbInntektÅrlig(),
                sporing = sporing.toDb(),
                pensjonsgivendeInntekt = pensjonsgivendeInntekt.toDb(),
            )
        }

        is InntektData.SelvstendigNæringsdrivendeSkjønnsfastsatt -> {
            DbInntektData.SelvstendigNæringsdrivendeSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.toDbInntektÅrlig(),
                sporing = sporing.toDb(),
            )
        }
    }

internal fun InntektRequest.toDbInntektRequest() =
    when (this) {
        is InntektRequest.Arbeidstaker -> {
            DbInntektRequest.Arbeidstaker(
                data = data.toDb(),
            )
        }

        is InntektRequest.SelvstendigNæringsdrivende -> {
            DbInntektRequest.SelvstendigNæringsdrivende(
                data = data.toDb(),
            )
        }

        is InntektRequest.Inaktiv -> {
            DbInntektRequest.Inaktiv(
                data = data.toDb(),
            )
        }

        is InntektRequest.Frilanser -> {
            DbInntektRequest.Frilanser(
                data = data.toDb(),
            )
        }

        is InntektRequest.Arbeidsledig -> {
            DbInntektRequest.Arbeidsledig(
                data = data.toDb(),
            )
        }
    }

internal fun ArbeidstakerInntektRequest.toDb(): DbArbeidstakerInntektRequest =
    when (this) {
        is ArbeidstakerInntektRequest.Inntektsmelding -> {
            DbArbeidstakerInntektRequest.Inntektsmelding(
                inntektsmeldingId = inntektsmeldingId,
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.toDb() },
            )
        }

        is ArbeidstakerInntektRequest.Ainntekt -> {
            DbArbeidstakerInntektRequest.Ainntekt(
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.toDb() },
            )
        }

        is ArbeidstakerInntektRequest.Skjønnsfastsatt -> {
            DbArbeidstakerInntektRequest.Skjønnsfastsatt(
                årsinntekt = årsinntekt.toDbInntektÅrlig(),
                årsak = årsak.toDb(),
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.toDb() },
            )
        }
    }

internal fun ArbeidstakerSkjønnsfastsettelseÅrsak.toDb(): DbArbeidstakerSkjønnsfastsettelseÅrsak =
    when (this) {
        ArbeidstakerSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT -> {
            DbArbeidstakerSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT
        }

        ArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING -> {
            DbArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING
        }

        ArbeidstakerSkjønnsfastsettelseÅrsak.TIDSAVGRENSET -> {
            DbArbeidstakerSkjønnsfastsettelseÅrsak.TIDSAVGRENSET
        }
    }

internal fun PensjonsgivendeInntektRequest.toDb(): DbPensjonsgivendeInntektRequest =
    when (this) {
        is PensjonsgivendeInntektRequest.PensjonsgivendeInntekt -> {
            DbPensjonsgivendeInntektRequest.PensjonsgivendeInntekt(
                begrunnelse = begrunnelse,
            )
        }

        is PensjonsgivendeInntektRequest.Skjønnsfastsatt -> {
            DbPensjonsgivendeInntektRequest.Skjønnsfastsatt(
                årsinntekt = årsinntekt.toDbInntektÅrlig(),
                årsak = årsak.toDb(),
                begrunnelse = begrunnelse,
            )
        }
    }

internal fun PensjonsgivendeSkjønnsfastsettelseÅrsak.toDb(): DbPensjonsgivendeSkjønnsfastsettelseÅrsak =
    when (this) {
        PensjonsgivendeSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT_VARIG_ENDRING -> {
            DbPensjonsgivendeSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT_VARIG_ENDRING
        }

        PensjonsgivendeSkjønnsfastsettelseÅrsak.SISTE_TRE_YRKESAKTIV -> {
            DbPensjonsgivendeSkjønnsfastsettelseÅrsak.SISTE_TRE_YRKESAKTIV
        }
    }

internal fun FrilanserInntektRequest.toDb(): DbFrilanserInntektRequest =
    when (this) {
        is FrilanserInntektRequest.Ainntekt -> {
            DbFrilanserInntektRequest.Ainntekt(
                begrunnelse = begrunnelse,
            )
        }

        is FrilanserInntektRequest.Skjønnsfastsatt -> {
            DbFrilanserInntektRequest.Skjønnsfastsatt(
                årsinntekt = årsinntekt.toDbInntektÅrlig(),
                årsak = årsak.toDb(),
                begrunnelse = begrunnelse,
            )
        }
    }

internal fun FrilanserSkjønnsfastsettelseÅrsak.toDb(): DbFrilanserSkjønnsfastsettelseÅrsak =
    when (this) {
        FrilanserSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT -> {
            DbFrilanserSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT
        }

        FrilanserSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING -> {
            DbFrilanserSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING
        }
    }

internal fun ArbeidsledigInntektRequest.toDb(): DbArbeidsledigInntektRequest =
    when (this) {
        is ArbeidsledigInntektRequest.Dagpenger -> {
            DbArbeidsledigInntektRequest.Dagpenger(
                dagbeløp = dagbeløp.toDbInntektDaglig(),
                begrunnelse = begrunnelse,
            )
        }

        is ArbeidsledigInntektRequest.Ventelønn -> {
            DbArbeidsledigInntektRequest.Ventelønn(
                årsinntekt = årsinntekt.toDbInntektÅrlig(),
                begrunnelse = begrunnelse,
            )
        }

        is ArbeidsledigInntektRequest.Vartpenger -> {
            DbArbeidsledigInntektRequest.Vartpenger(
                årsinntekt = årsinntekt.toDbInntektÅrlig(),
                begrunnelse = begrunnelse,
            )
        }
    }

internal fun Inntekt.toDbInntektÅrlig(): DbInntekt.Årlig = DbInntekt.Årlig(beløp = this.årlig)

internal fun Inntekt.toDbInntektMånedlig(): DbInntekt.Månedlig = DbInntekt.Månedlig(beløp = this.månedlig)

internal fun Inntekt.toDbInntektDaglig(): DbInntekt.Daglig = DbInntekt.Daglig(beløp = this.dagligInt)

internal fun InntektData.PensjonsgivendeInntekt.toDb(): DbInntektData.PensjonsgivendeInntekt =
    DbInntektData.PensjonsgivendeInntekt(
        omregnetÅrsinntekt = omregnetÅrsinntekt.toDbInntektÅrlig(),
        pensjonsgivendeInntekt = pensjonsgivendeInntekt.map { it.toDb() },
        anvendtGrunnbeløp = anvendtGrunnbeløp.toDbInntektÅrlig(),
    )

internal fun InntektAar.toDb(): DbInntektÅr =
    DbInntektÅr(
        år = år,
        rapportertinntekt = rapportertinntekt.toDbInntektÅrlig(),
        justertÅrsgrunnlag = justertÅrsgrunnlag.toDbInntektÅrlig(),
        antallGKompensert = antallGKompensert,
        snittG = snittG.toDbInntektÅrlig(),
    )

internal fun BeregningskoderSykepengegrunnlag.toDb(): DbBeregningskoderSykepengegrunnlag =
    when (this) {
        BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL -> {
            DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL
        }

        BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK -> {
            DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
        }

        BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG -> {
            DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
        }

        BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL -> {
            DbBeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL
        }

        BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK -> {
            DbBeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
        }

        BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG -> {
            DbBeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
        }

        BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL -> {
            DbBeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL
        }

        BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING -> {
            DbBeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
        }

        BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB -> {
            DbBeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
        }

        BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL -> {
            DbBeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL
        }

        BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING -> {
            DbBeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
        }

        BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB -> {
            DbBeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
        }

        BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER -> {
            DbBeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER
        }

        BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN -> {
            DbBeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN
        }

        BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER -> {
            DbBeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER
        }

        BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO -> {
            DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO
        }
    }

// ============= Reverse mappings: DB DTO -> Domain =============

internal fun DbYrkesaktivitetKategorisering.toDomain(): YrkesaktivitetKategorisering =
    when (this) {
        is DbYrkesaktivitetKategorisering.Arbeidstaker -> {
            YrkesaktivitetKategorisering.Arbeidstaker(
                sykmeldt = sykmeldt,
                typeArbeidstaker = typeArbeidstaker.toDomain(),
            )
        }

        is DbYrkesaktivitetKategorisering.Frilanser -> {
            YrkesaktivitetKategorisering.Frilanser(
                sykmeldt = sykmeldt,
                orgnummer = orgnummer,
                forsikring = forsikring.toDomain(),
            )
        }

        is DbYrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> {
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = sykmeldt,
                typeSelvstendigNæringsdrivende = typeSelvstendigNæringsdrivende.toDomain(),
            )
        }

        is DbYrkesaktivitetKategorisering.Inaktiv -> {
            YrkesaktivitetKategorisering.Inaktiv(
                sykmeldt = sykmeldt,
            )
        }

        is DbYrkesaktivitetKategorisering.Arbeidsledig -> {
            YrkesaktivitetKategorisering.Arbeidsledig(
                sykmeldt = sykmeldt,
            )
        }
    }

internal fun DbTypeArbeidstaker.toDomain(): TypeArbeidstaker =
    when (this) {
        is DbTypeArbeidstaker.Ordinær -> {
            TypeArbeidstaker.Ordinær(orgnummer = orgnummer)
        }

        is DbTypeArbeidstaker.Maritim -> {
            TypeArbeidstaker.Maritim(orgnummer = orgnummer)
        }

        is DbTypeArbeidstaker.Fisker -> {
            TypeArbeidstaker.Fisker(orgnummer = orgnummer)
        }

        is DbTypeArbeidstaker.DimmitertVernepliktig -> {
            TypeArbeidstaker.DimmitertVernepliktig()
        }

        is DbTypeArbeidstaker.PrivatArbeidsgiver -> {
            TypeArbeidstaker.PrivatArbeidsgiver(arbeidsgiverFnr = arbeidsgiverFnr)
        }
    }

internal fun DbTypeSelvstendigNæringsdrivende.toDomain(): TypeSelvstendigNæringsdrivende =
    when (this) {
        is DbTypeSelvstendigNæringsdrivende.Ordinær -> {
            TypeSelvstendigNæringsdrivende.Ordinær(forsikring = forsikring.toDomain())
        }

        is DbTypeSelvstendigNæringsdrivende.BarnepasserEgetHjem -> {
            TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem(forsikring = forsikring.toDomain())
        }

        is DbTypeSelvstendigNæringsdrivende.Fisker -> {
            TypeSelvstendigNæringsdrivende.Fisker(forsikring = forsikring.toDomain())
        }

        is DbTypeSelvstendigNæringsdrivende.Jordbruker -> {
            TypeSelvstendigNæringsdrivende.Jordbruker(forsikring = forsikring.toDomain())
        }

        is DbTypeSelvstendigNæringsdrivende.Reindrift -> {
            TypeSelvstendigNæringsdrivende.Reindrift(forsikring = forsikring.toDomain())
        }
    }

internal fun DbFrilanserForsikring.toDomain(): FrilanserForsikring =
    when (this) {
        DbFrilanserForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG -> {
            FrilanserForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG
        }

        DbFrilanserForsikring.INGEN_FORSIKRING -> {
            FrilanserForsikring.INGEN_FORSIKRING
        }
    }

internal fun DbSelvstendigForsikring.toDomain(): SelvstendigForsikring =
    when (this) {
        DbSelvstendigForsikring.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG -> {
            SelvstendigForsikring.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG
        }

        DbSelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG -> {
            SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG
        }

        DbSelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG -> {
            SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG
        }

        DbSelvstendigForsikring.INGEN_FORSIKRING -> {
            SelvstendigForsikring.INGEN_FORSIKRING
        }
    }

internal fun DbDagoversikt.toDomain(): Dagoversikt =
    Dagoversikt(
        sykdomstidlinje = sykdomstidlinje.map { it.toDomain() },
        avslagsdager = avslagsdager.map { it.toDomain() },
    )

internal fun DbDag.toDomain(): Dag =
    Dag(
        dato = dato,
        dagtype = dagtype.toDomain(),
        grad = grad,
        avslåttBegrunnelse = avslåttBegrunnelse,
        andreYtelserBegrunnelse = andreYtelserBegrunnelse,
        kilde = kilde?.toDomain(),
    )

internal fun DbDagtype.toDomain(): Dagtype =
    when (this) {
        DbDagtype.Syk -> Dagtype.Syk
        DbDagtype.SykNav -> Dagtype.SykNav
        DbDagtype.Arbeidsdag -> Dagtype.Arbeidsdag
        DbDagtype.Ferie -> Dagtype.Ferie
        DbDagtype.Permisjon -> Dagtype.Permisjon
        DbDagtype.Avslått -> Dagtype.Avslått
        DbDagtype.AndreYtelser -> Dagtype.AndreYtelser
        DbDagtype.Behandlingsdag -> Dagtype.Behandlingsdag
    }

internal fun DbInntekt.tilInntekt() =
    when (this) {
        is DbInntekt.Daglig -> Inntekt.gjenopprett(InntektbeløpDto.DagligInt(this.beløp))
        is DbInntekt.DagligDouble -> Inntekt.gjenopprett(InntektbeløpDto.DagligDouble(this.beløp))
        is DbInntekt.Månedlig -> Inntekt.gjenopprett(InntektbeløpDto.MånedligDouble(this.beløp))
        is DbInntekt.Årlig -> Inntekt.gjenopprett(InntektbeløpDto.Årlig(this.beløp))
    }

internal fun DbKilde.toDomain(): Kilde =
    when (this) {
        DbKilde.Søknad -> Kilde.Søknad
        DbKilde.Saksbehandler -> Kilde.Saksbehandler
    }

internal fun DbPerioder.toDomain(): Perioder =
    Perioder(
        type = type.toDomain(),
        perioder = perioder.map { it.toDomain() },
    )

internal fun DbPeriodetype.toDomain(): Periodetype =
    when (this) {
        DbPeriodetype.ARBEIDSGIVERPERIODE -> Periodetype.ARBEIDSGIVERPERIODE
        DbPeriodetype.VENTETID -> Periodetype.VENTETID
        DbPeriodetype.VENTETID_INAKTIV -> Periodetype.VENTETID_INAKTIV
    }

internal fun DbPeriode.toDomain(): Periode =
    Periode(
        fom = fom,
        tom = tom,
    )

internal fun DbRefusjonsperiode.toDomain(): Refusjonsperiode =
    Refusjonsperiode(
        fom = fom,
        tom = tom,
        beløp = beløp.tilInntekt(),
    )

internal fun DbInntektData.toDomain(): InntektData =
    when (this) {
        is DbInntektData.ArbeidstakerInntektsmelding -> {
            InntektData.ArbeidstakerInntektsmelding(
                inntektsmeldingId = inntektsmeldingId,
                inntektsmelding = inntektsmelding.serialisertTilString(),
                omregnetÅrsinntekt = omregnetÅrsinntekt.tilInntekt(),
                sporing = sporing.toDomain(),
            )
        }

        is DbInntektData.ArbeidstakerAinntekt -> {
            InntektData.ArbeidstakerAinntekt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.tilInntekt(),
                sporing = sporing.toDomain(),
                kildedata = kildedata.mapValues { it.value.tilInntekt() },
            )
        }

        is DbInntektData.ArbeidstakerSkjønnsfastsatt -> {
            InntektData.ArbeidstakerSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.tilInntekt(),
                sporing = sporing.toDomain(),
            )
        }

        is DbInntektData.FrilanserAinntekt -> {
            InntektData.FrilanserAinntekt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.tilInntekt(),
                sporing = sporing.toDomain(),
                kildedata = kildedata.mapValues { it.value.tilInntekt() },
            )
        }

        is DbInntektData.FrilanserSkjønnsfastsatt -> {
            InntektData.FrilanserSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.tilInntekt(),
                sporing = sporing.toDomain(),
            )
        }

        is DbInntektData.Arbeidsledig -> {
            InntektData.Arbeidsledig(
                omregnetÅrsinntekt = omregnetÅrsinntekt.tilInntekt(),
                sporing = sporing.toDomain(),
            )
        }

        is DbInntektData.InaktivSkjønnsfastsatt -> {
            InntektData.InaktivSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.tilInntekt(),
                sporing = sporing.toDomain(),
            )
        }

        is DbInntektData.InaktivPensjonsgivende -> {
            InntektData.InaktivPensjonsgivende(
                omregnetÅrsinntekt = omregnetÅrsinntekt.tilInntekt(),
                sporing = sporing.toDomain(),
                pensjonsgivendeInntekt = pensjonsgivendeInntekt.toDomain(),
            )
        }

        is DbInntektData.SelvstendigNæringsdrivendePensjonsgivende -> {
            InntektData.SelvstendigNæringsdrivendePensjonsgivende(
                omregnetÅrsinntekt = omregnetÅrsinntekt.tilInntekt(),
                sporing = sporing.toDomain(),
                pensjonsgivendeInntekt = pensjonsgivendeInntekt.toDomain(),
            )
        }

        is DbInntektData.SelvstendigNæringsdrivendeSkjønnsfastsatt -> {
            InntektData.SelvstendigNæringsdrivendeSkjønnsfastsatt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.tilInntekt(),
                sporing = sporing.toDomain(),
            )
        }
    }

internal fun DbInntektData.PensjonsgivendeInntekt.toDomain(): InntektData.PensjonsgivendeInntekt =
    InntektData.PensjonsgivendeInntekt(
        omregnetÅrsinntekt = omregnetÅrsinntekt.tilInntekt(),
        pensjonsgivendeInntekt = pensjonsgivendeInntekt.map { it.toDomain() },
        anvendtGrunnbeløp = anvendtGrunnbeløp.tilInntekt(),
    )

internal fun DbInntektÅr.toDomain(): InntektAar =
    InntektAar(
        år = år,
        rapportertinntekt = rapportertinntekt.tilInntekt(),
        justertÅrsgrunnlag = justertÅrsgrunnlag.tilInntekt(),
        antallGKompensert = antallGKompensert,
        snittG = snittG.tilInntekt(),
    )

internal fun DbBeregningskoderSykepengegrunnlag.toDomain(): BeregningskoderSykepengegrunnlag =
    when (this) {
        DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL -> {
            BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL
        }

        DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK -> {
            BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
        }

        DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG -> {
            BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
        }

        DbBeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL -> {
            BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL
        }

        DbBeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK -> {
            BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
        }

        DbBeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG -> {
            BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
        }

        DbBeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL -> {
            BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL
        }

        DbBeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING -> {
            BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
        }

        DbBeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB -> {
            BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
        }

        DbBeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL -> {
            BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL
        }

        DbBeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING -> {
            BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
        }

        DbBeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB -> {
            BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
        }

        DbBeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER -> {
            BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER
        }

        DbBeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN -> {
            BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN
        }

        DbBeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER -> {
            BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER
        }

        DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO -> {
            BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO
        }
    }

internal fun DbInntektRequest.toDomain(): InntektRequest =
    when (this) {
        is DbInntektRequest.Arbeidstaker -> {
            InntektRequest.Arbeidstaker(
                data = data.toDomain(),
            )
        }

        is DbInntektRequest.SelvstendigNæringsdrivende -> {
            InntektRequest.SelvstendigNæringsdrivende(
                data = data.toDomain(),
            )
        }

        is DbInntektRequest.Inaktiv -> {
            InntektRequest.Inaktiv(
                data = data.toDomain(),
            )
        }

        is DbInntektRequest.Frilanser -> {
            InntektRequest.Frilanser(
                data = data.toDomain(),
            )
        }

        is DbInntektRequest.Arbeidsledig -> {
            InntektRequest.Arbeidsledig(
                data = data.toDomain(),
            )
        }
    }

internal fun DbArbeidstakerInntektRequest.toDomain(): ArbeidstakerInntektRequest =
    when (this) {
        is DbArbeidstakerInntektRequest.Inntektsmelding -> {
            ArbeidstakerInntektRequest.Inntektsmelding(
                inntektsmeldingId = inntektsmeldingId,
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.toDomain() },
            )
        }

        is DbArbeidstakerInntektRequest.Ainntekt -> {
            ArbeidstakerInntektRequest.Ainntekt(
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.toDomain() },
            )
        }

        is DbArbeidstakerInntektRequest.Skjønnsfastsatt -> {
            ArbeidstakerInntektRequest.Skjønnsfastsatt(
                årsinntekt = årsinntekt.tilInntekt(),
                årsak = årsak.toDomain(),
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.toDomain() },
            )
        }
    }

internal fun DbArbeidstakerSkjønnsfastsettelseÅrsak.toDomain(): ArbeidstakerSkjønnsfastsettelseÅrsak =
    when (this) {
        DbArbeidstakerSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT -> {
            ArbeidstakerSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT
        }

        DbArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING -> {
            ArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING
        }

        DbArbeidstakerSkjønnsfastsettelseÅrsak.TIDSAVGRENSET -> {
            ArbeidstakerSkjønnsfastsettelseÅrsak.TIDSAVGRENSET
        }
    }

internal fun DbPensjonsgivendeInntektRequest.toDomain(): PensjonsgivendeInntektRequest =
    when (this) {
        is DbPensjonsgivendeInntektRequest.PensjonsgivendeInntekt -> {
            PensjonsgivendeInntektRequest.PensjonsgivendeInntekt(
                begrunnelse = begrunnelse,
            )
        }

        is DbPensjonsgivendeInntektRequest.Skjønnsfastsatt -> {
            PensjonsgivendeInntektRequest.Skjønnsfastsatt(
                årsinntekt = årsinntekt.tilInntekt(),
                årsak = årsak.toDomain(),
                begrunnelse = begrunnelse,
            )
        }
    }

internal fun DbPensjonsgivendeSkjønnsfastsettelseÅrsak.toDomain(): PensjonsgivendeSkjønnsfastsettelseÅrsak =
    when (this) {
        DbPensjonsgivendeSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT_VARIG_ENDRING -> {
            PensjonsgivendeSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT_VARIG_ENDRING
        }

        DbPensjonsgivendeSkjønnsfastsettelseÅrsak.SISTE_TRE_YRKESAKTIV -> {
            PensjonsgivendeSkjønnsfastsettelseÅrsak.SISTE_TRE_YRKESAKTIV
        }
    }

internal fun DbFrilanserInntektRequest.toDomain(): FrilanserInntektRequest =
    when (this) {
        is DbFrilanserInntektRequest.Ainntekt -> {
            FrilanserInntektRequest.Ainntekt(
                begrunnelse = begrunnelse,
            )
        }

        is DbFrilanserInntektRequest.Skjønnsfastsatt -> {
            FrilanserInntektRequest.Skjønnsfastsatt(
                årsinntekt = årsinntekt.tilInntekt(),
                årsak = årsak.toDomain(),
                begrunnelse = begrunnelse,
            )
        }
    }

internal fun DbFrilanserSkjønnsfastsettelseÅrsak.toDomain(): FrilanserSkjønnsfastsettelseÅrsak =
    when (this) {
        DbFrilanserSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT -> {
            FrilanserSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT
        }

        DbFrilanserSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING -> {
            FrilanserSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING
        }
    }

internal fun DbArbeidsledigInntektRequest.toDomain(): ArbeidsledigInntektRequest =
    when (this) {
        is DbArbeidsledigInntektRequest.Dagpenger -> {
            ArbeidsledigInntektRequest.Dagpenger(
                dagbeløp = dagbeløp.tilInntekt(),
                begrunnelse = begrunnelse,
            )
        }

        is DbArbeidsledigInntektRequest.Ventelønn -> {
            ArbeidsledigInntektRequest.Ventelønn(
                årsinntekt = årsinntekt.tilInntekt(),
                begrunnelse = begrunnelse,
            )
        }

        is DbArbeidsledigInntektRequest.Vartpenger -> {
            ArbeidsledigInntektRequest.Vartpenger(
                årsinntekt = årsinntekt.tilInntekt(),
                begrunnelse = begrunnelse,
            )
        }
    }
