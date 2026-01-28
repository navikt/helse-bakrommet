package no.nav.helse.bakrommet.db.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.db.dto.sykepengegrunnlag.*
import no.nav.helse.bakrommet.db.tilPgJson
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderKombinasjonerSykepengegrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.sykepengegrunnlag.*
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Sykefraværstilfelle
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.SykefraværstilfelleId
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.repository.SykefraværstilfelleRepository
import java.util.*

class PgSykefraværstilfelleRepository private constructor(
    private val queryRunner: QueryRunner,
) : SykefraværstilfelleRepository {
    constructor(session: Session) : this(MedSession(session))

    override fun lagre(sykefraværstilfelle: Sykefraværstilfelle) {
        val id = sykefraværstilfelle.id.tilUUID()
        queryRunner.update(
            """
            INSERT INTO sykepengegrunnlag (id, sykepengegrunnlag, sammenlikningsgrunnlag, opprettet_av_nav_ident, opprettet, oppdatert, opprettet_for_behandling, laast)
            VALUES(:id, :sykepengegrunnlag, :sammenlikningsgrunnlag, :opprettet_av_nav_ident, :opprettet, :oppdatert, :opprettet_for_behandling, :laast)
            ON CONFLICT (id) DO UPDATE SET
                sykepengegrunnlag = excluded.sykepengegrunnlag,
                sammenlikningsgrunnlag = excluded.sammenlikningsgrunnlag,
                oppdatert = excluded.oppdatert,
                laast = excluded.laast
            """.trimIndent(),
            "id" to id,
            "sykepengegrunnlag" to sykefraværstilfelle.sykepengegrunnlag.tilDbRecord().tilPgJson(),
            "sammenlikningsgrunnlag" to sykefraværstilfelle.sammenlikningsgrunnlag.tilDbRecord().tilPgJson(),
            "opprettet_av_nav_ident" to sykefraværstilfelle.opprettetAv,
            "opprettet" to sykefraværstilfelle.opprettet,
            "oppdatert" to sykefraværstilfelle.oppdatert,
            "opprettet_for_behandling" to sykefraværstilfelle.opprettetForBehandling.value,
            "laast" to sykefraværstilfelle.låst,
        )
    }

    override fun finn(sykefraværstilfelleId: SykefraværstilfelleId): Sykefraværstilfelle? {
        val id = sykefraværstilfelleId.tilUUID()
        return queryRunner.single(
            """SELECT * FROM sykepengegrunnlag WHERE id = :id""",
            "id" to id,
        ) {
            Sykefraværstilfelle.fraLagring(
                id = sykefraværstilfelleId,
                sykepengegrunnlag = objectMapper.readValue<DbSykepengegrunnlagBase>(it.string("sykepengegrunnlag")).tilDomene(),
                sammenlikningsgrunnlag = objectMapper.readValue<DbSammenlikningsgrunnlag>(it.string("sammenlikningsgrunnlag")).tilDomene(),
                opprettetAv = it.string("opprettet_av_nav_ident"),
                opprettet = it.instant("opprettet"),
                oppdatert = it.instant("oppdatert"),
                opprettetForBehandling = BehandlingId(it.uuid("opprettet_for_behandling")),
                låst = it.boolean("laast"),
            )
        }
    }

    private fun Sammenlikningsgrunnlag.tilDbRecord(): DbSammenlikningsgrunnlag =
        DbSammenlikningsgrunnlag(
            totaltSammenlikningsgrunnlag = totaltSammenlikningsgrunnlag.toDbInntektÅrlig(),
            avvikProsent = avvikProsent,
            avvikMotInntektsgrunnlag = avvikMotInntektsgrunnlag.toDbInntektÅrlig(),
            basertPåDokumentId = basertPåDokumentId,
        )

    private fun DbSammenlikningsgrunnlag.tilDomene(): Sammenlikningsgrunnlag =
        Sammenlikningsgrunnlag(
            totaltSammenlikningsgrunnlag = totaltSammenlikningsgrunnlag.tilInntekt(),
            avvikProsent = avvikProsent,
            avvikMotInntektsgrunnlag = avvikMotInntektsgrunnlag.tilInntekt(),
            basertPåDokumentId = basertPåDokumentId,
        )

    private fun SykepengegrunnlagBase.tilDbRecord() =
        when (this) {
            is FrihåndSykepengegrunnlag -> {
                DbFrihåndSykepengegrunnlag(
                    grunnbeløp = grunnbeløp.toDbInntektÅrlig(),
                    sykepengegrunnlag = sykepengegrunnlag.toDbInntektÅrlig(),
                    seksG = seksG.toDbInntektÅrlig(),
                    begrensetTil6G = begrensetTil6G,
                    grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
                    beregningsgrunnlag = beregningsgrunnlag.toDbInntektÅrlig(),
                    begrunnelse = begrunnelse,
                    beregningskoder = beregningskoder.tilDbRecord(),
                )
            }

            is Sykepengegrunnlag -> {
                DbSykepengegrunnlag(
                    grunnbeløp = grunnbeløp.toDbInntektÅrlig(),
                    sykepengegrunnlag = sykepengegrunnlag.toDbInntektÅrlig(),
                    seksG = seksG.toDbInntektÅrlig(),
                    begrensetTil6G = begrensetTil6G,
                    grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
                    beregningsgrunnlag = beregningsgrunnlag.toDbInntektÅrlig(),
                    næringsdel = næringsdel?.tilDbRecord(),
                    kombinertBeregningskode = kombinertBeregningskode?.tilDbRecord(),
                )
            }
        }

    private fun DbSykepengegrunnlagBase.tilDomene(): SykepengegrunnlagBase =
        when (this) {
            is DbFrihåndSykepengegrunnlag -> {
                FrihåndSykepengegrunnlag(
                    grunnbeløp = grunnbeløp.tilInntekt(),
                    sykepengegrunnlag = sykepengegrunnlag.tilInntekt(),
                    seksG = seksG.tilInntekt(),
                    begrensetTil6G = begrensetTil6G,
                    grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
                    beregningsgrunnlag = beregningsgrunnlag.tilInntekt(),
                    begrunnelse = begrunnelse,
                    beregningskoder = beregningskoder.tilDomene(),
                )
            }

            is DbSykepengegrunnlag -> {
                Sykepengegrunnlag(
                    grunnbeløp = grunnbeløp.tilInntekt(),
                    sykepengegrunnlag = sykepengegrunnlag.tilInntekt(),
                    seksG = seksG.tilInntekt(),
                    begrensetTil6G = begrensetTil6G,
                    grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
                    beregningsgrunnlag = beregningsgrunnlag.tilInntekt(),
                    næringsdel = næringsdel?.tilDomene(),
                    kombinertBeregningskode = kombinertBeregningskode?.tilDomene(),
                )
            }
        }

    private fun Næringsdel.tilDbRecord(): DbNæringsdel =
        DbNæringsdel(
            pensjonsgivendeÅrsinntekt = pensjonsgivendeÅrsinntekt.toDbInntektÅrlig(),
            pensjonsgivendeÅrsinntekt6GBegrenset = pensjonsgivendeÅrsinntekt6GBegrenset.toDbInntektÅrlig(),
            pensjonsgivendeÅrsinntektBegrensetTil6G = pensjonsgivendeÅrsinntektBegrensetTil6G,
            næringsdel = næringsdel.toDbInntektÅrlig(),
            sumAvArbeidsinntekt = sumAvArbeidsinntekt.toDbInntektÅrlig(),
        )

    private fun DbNæringsdel.tilDomene(): Næringsdel =
        Næringsdel(
            pensjonsgivendeÅrsinntekt = pensjonsgivendeÅrsinntekt.tilInntekt(),
            pensjonsgivendeÅrsinntekt6GBegrenset = pensjonsgivendeÅrsinntekt6GBegrenset.tilInntekt(),
            pensjonsgivendeÅrsinntektBegrensetTil6G = pensjonsgivendeÅrsinntektBegrensetTil6G,
            næringsdel = næringsdel.tilInntekt(),
            sumAvArbeidsinntekt = sumAvArbeidsinntekt.tilInntekt(),
        )

    private fun List<BeregningskoderSykepengegrunnlag>.tilDbRecord(): List<DbBeregningskoderSykepengegrunnlag> =
        map {
            when (it) {
                BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL -> DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL
                BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK -> DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
                BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG -> DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
                BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL -> DbBeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL
                BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK -> DbBeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
                BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG -> DbBeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
                BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL -> DbBeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL
                BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING -> DbBeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
                BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB -> DbBeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
                BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL -> DbBeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL
                BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING -> DbBeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
                BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB -> DbBeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
                BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER -> DbBeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER
                BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN -> DbBeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN
                BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER -> DbBeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER
                BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO -> DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO
            }
        }

    private fun List<DbBeregningskoderSykepengegrunnlag>.tilDomene(): List<BeregningskoderSykepengegrunnlag> =
        map {
            when (it) {
                DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL
                DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
                DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
                DbBeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL -> BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL
                DbBeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK -> BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
                DbBeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG -> BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
                DbBeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL -> BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL
                DbBeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING -> BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
                DbBeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB -> BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
                DbBeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL -> BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL
                DbBeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING -> BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
                DbBeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB -> BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
                DbBeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER -> BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER
                DbBeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN -> BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN
                DbBeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER -> BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER
                DbBeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO
            }
        }

    private fun BeregningskoderKombinasjonerSykepengegrunnlag.tilDbRecord(): DbBeregningskoderKombinasjonerSykepengegrunnlag =
        when (this) {
            BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_SELVSTENDIG_SYKEPENGEGRUNNLAG -> DbBeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_SELVSTENDIG_SYKEPENGEGRUNNLAG
            BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SYKEPENGEGRUNNLAG -> DbBeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SYKEPENGEGRUNNLAG
            BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SELVSTENDIG_SYKEPENGEGRUNNLAG -> DbBeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SELVSTENDIG_SYKEPENGEGRUNNLAG
            BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_SELVSTENDIG_FRILANSER_SYKEPENGEGRUNNLAG -> DbBeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_SELVSTENDIG_FRILANSER_SYKEPENGEGRUNNLAG
        }

    private fun DbBeregningskoderKombinasjonerSykepengegrunnlag.tilDomene(): BeregningskoderKombinasjonerSykepengegrunnlag =
        when (this) {
            DbBeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_SELVSTENDIG_SYKEPENGEGRUNNLAG -> BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_SELVSTENDIG_SYKEPENGEGRUNNLAG
            DbBeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SYKEPENGEGRUNNLAG -> BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SYKEPENGEGRUNNLAG
            DbBeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SELVSTENDIG_SYKEPENGEGRUNNLAG -> BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SELVSTENDIG_SYKEPENGEGRUNNLAG
            DbBeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_SELVSTENDIG_FRILANSER_SYKEPENGEGRUNNLAG -> BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_SELVSTENDIG_FRILANSER_SYKEPENGEGRUNNLAG
        }

    private fun SykefraværstilfelleId.tilUUID() = UUID.nameUUIDFromBytes((naturligIdent.value + skjæringstidspunkt.toString()).toByteArray())
}
