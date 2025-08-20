package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import com.fasterxml.jackson.core.type.TypeReference
import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.util.objectMapper
import java.util.UUID
import javax.sql.DataSource

class SykepengegrunnlagDao private constructor(private val db: QueryRunner) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun opprettSykepengegrunnlag(
        saksbehandlingsperiodeId: UUID,
        beregning: SykepengegrunnlagResponse,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse {
        val inntekterJson = objectMapper.writeValueAsString(beregning.inntekter)

        db.update(
            """
            INSERT INTO sykepengegrunnlag 
                (id, saksbehandlingsperiode_id, total_inntekt_ore, grunnbelop_6g_ore, 
                 begrenset_til_6g, sykepengegrunnlag_ore, begrunnelse, inntekter,
                 opprettet, opprettet_av_nav_ident, sist_oppdatert)
            VALUES 
                (:id, :saksbehandlingsperiode_id, :total_inntekt_ore, :grunnbelop_6g_ore,
                 :begrenset_til_6g, :sykepengegrunnlag_ore, :begrunnelse, :inntekter,
                 NOW(), :opprettet_av_nav_ident, NOW())
            """.trimIndent(),
            "id" to beregning.id,
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
            "total_inntekt_ore" to beregning.totalInntektØre,
            "grunnbelop_6g_ore" to beregning.grunnbeløp6GØre,
            "begrenset_til_6g" to beregning.begrensetTil6G,
            "sykepengegrunnlag_ore" to beregning.sykepengegrunnlagØre,
            "begrunnelse" to beregning.begrunnelse,
            "inntekter" to inntekterJson,
            "opprettet_av_nav_ident" to saksbehandler.navIdent,
        )
        return hentSykepengegrunnlag(saksbehandlingsperiodeId)!!
    }

    fun hentSykepengegrunnlag(saksbehandlingsperiodeId: UUID): SykepengegrunnlagResponse? =
        db.single(
            """
            SELECT * FROM sykepengegrunnlag 
            WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
            mapper = ::sykepengegrunnlagFraRow,
        )

    fun oppdaterSykepengegrunnlag(
        saksbehandlingsperiodeId: UUID,
        beregning: SykepengegrunnlagResponse,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse {
        val inntekterJson = objectMapper.writeValueAsString(beregning.inntekter)

        db.update(
            """
            UPDATE sykepengegrunnlag 
            SET 
                total_inntekt_ore = :total_inntekt_ore,
                grunnbelop_6g_ore = :grunnbelop_6g_ore,
                begrenset_til_6g = :begrenset_til_6g,
                sykepengegrunnlag_ore = :sykepengegrunnlag_ore,
                begrunnelse = :begrunnelse,
                inntekter = :inntekter,
                sist_oppdatert = NOW()
            WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
            "total_inntekt_ore" to beregning.totalInntektØre,
            "grunnbelop_6g_ore" to beregning.grunnbeløp6GØre,
            "begrenset_til_6g" to beregning.begrensetTil6G,
            "sykepengegrunnlag_ore" to beregning.sykepengegrunnlagØre,
            "begrunnelse" to beregning.begrunnelse,
            "inntekter" to inntekterJson,
        )
        return hentSykepengegrunnlag(saksbehandlingsperiodeId)!!
    }

    fun slettSykepengegrunnlag(saksbehandlingsperiodeId: UUID) {
        db.update(
            """
            DELETE FROM sykepengegrunnlag 
            WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
        )
    }

    private fun sykepengegrunnlagFraRow(row: Row): SykepengegrunnlagResponse {
        val inntekterJson = row.string("inntekter")
        val inntekter =
            objectMapper.readValue(
                inntekterJson,
                object : TypeReference<List<Inntekt>>() {},
            )

        return SykepengegrunnlagResponse(
            id = row.uuid("id"),
            saksbehandlingsperiodeId = row.uuid("saksbehandlingsperiode_id"),
            inntekter = inntekter,
            totalInntektØre = row.long("total_inntekt_ore"),
            grunnbeløp6GØre = row.long("grunnbelop_6g_ore"),
            begrensetTil6G = row.boolean("begrenset_til_6g"),
            sykepengegrunnlagØre = row.long("sykepengegrunnlag_ore"),
            begrunnelse = row.stringOrNull("begrunnelse"),
            opprettet = row.offsetDateTime("opprettet").toString(),
            opprettetAv = row.string("opprettet_av_nav_ident"),
            sistOppdatert = row.offsetDateTime("sist_oppdatert").toString(),
        )
    }
}
