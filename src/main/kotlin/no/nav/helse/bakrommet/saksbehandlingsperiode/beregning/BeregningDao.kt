package no.nav.helse.bakrommet.saksbehandlingsperiode.beregning

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.util.objectMapper
import java.util.UUID
import javax.sql.DataSource

class BeregningDao private constructor(private val db: QueryRunner) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun settBeregning(
        saksbehandlingsperiodeId: UUID,
        beregning: BeregningResponse,
        saksbehandler: Bruker,
    ): BeregningResponse {
        val beregningJson = objectMapper.writeValueAsString(beregning.beregningData)

        // Sjekk om det finnes fra før
        val eksisterende = hentBeregning(saksbehandlingsperiodeId)

        if (eksisterende != null) {
            // Oppdater eksisterende
            db.update(
                """
                UPDATE beregning 
                SET 
                    beregning_data = :beregning_data,
                    opprettet_av_nav_ident = :opprettet_av_nav_ident,
                    sist_oppdatert = NOW()
                WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
                """.trimIndent(),
                "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
                "beregning_data" to beregningJson,
                "opprettet_av_nav_ident" to saksbehandler.navIdent,
            )
        } else {
            // Opprett nytt
            db.update(
                """
                INSERT INTO beregning 
                    (id, saksbehandlingsperiode_id, beregning_data, opprettet, opprettet_av_nav_ident, sist_oppdatert)
                VALUES 
                    (:id, :saksbehandlingsperiode_id, :beregning_data, NOW(), :opprettet_av_nav_ident, NOW())
                """.trimIndent(),
                "id" to beregning.id,
                "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
                "beregning_data" to beregningJson,
                "opprettet_av_nav_ident" to saksbehandler.navIdent,
            )
        }

        return hentBeregning(saksbehandlingsperiodeId)!!
    }

    fun hentBeregning(saksbehandlingsperiodeId: UUID): BeregningResponse? =
        db.single(
            """
            SELECT * FROM beregning 
            WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
            mapper = ::beregningFraRow,
        )

    fun slettBeregning(saksbehandlingsperiodeId: UUID) {
        db.update(
            """
            DELETE FROM beregning 
            WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
        )
    }

    private fun beregningFraRow(row: Row): BeregningResponse {
        val beregningJson = row.string("beregning_data")
        val beregningData = objectMapper.readValue(beregningJson, BeregningData::class.java)

        return BeregningResponse(
            id = row.uuid("id"),
            saksbehandlingsperiodeId = row.uuid("saksbehandlingsperiode_id"),
            beregningData = beregningData,
            opprettet = row.offsetDateTime("opprettet").toString(),
            opprettetAv = row.string("opprettet_av_nav_ident"),
            sistOppdatert = row.offsetDateTime("sist_oppdatert").toString(),
        )
    }
}
