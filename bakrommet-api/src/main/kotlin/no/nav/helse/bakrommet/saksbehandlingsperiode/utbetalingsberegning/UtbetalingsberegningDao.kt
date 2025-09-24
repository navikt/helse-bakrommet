package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.util.objectMapper
import java.util.UUID
import javax.sql.DataSource

class UtbetalingsberegningDao private constructor(private val db: QueryRunner) {
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
                UPDATE utbetalingsberegning 
                SET 
                    utbetalingsberegning_data = :utbetalingsberegning_data,
                    opprettet_av_nav_ident = :opprettet_av_nav_ident,
                    sist_oppdatert = NOW()
                WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
                """.trimIndent(),
                "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
                "utbetalingsberegning_data" to beregningJson,
                "opprettet_av_nav_ident" to saksbehandler.navIdent,
            )
        } else {
            // Opprett nytt
            db.update(
                """
                INSERT INTO utbetalingsberegning 
                    (id, saksbehandlingsperiode_id, utbetalingsberegning_data, opprettet, opprettet_av_nav_ident, sist_oppdatert)
                VALUES 
                    (:id, :saksbehandlingsperiode_id, :utbetalingsberegning_data, NOW(), :opprettet_av_nav_ident, NOW())
                """.trimIndent(),
                "id" to beregning.id,
                "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
                "utbetalingsberegning_data" to beregningJson,
                "opprettet_av_nav_ident" to saksbehandler.navIdent,
            )
        }

        return hentBeregning(saksbehandlingsperiodeId)!!
    }

    fun hentBeregning(saksbehandlingsperiodeId: UUID): BeregningResponse? =
        db.single(
            """
            SELECT * FROM utbetalingsberegning 
            WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
            mapper = ::beregningFraRow,
        )

    fun slettBeregning(saksbehandlingsperiodeId: UUID) {
        db.update(
            """
            DELETE FROM utbetalingsberegning 
            WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
        )
    }

    private fun beregningFraRow(row: Row): BeregningResponse {
        val beregningJson = row.string("utbetalingsberegning_data")
        val beregningData = objectMapper.readValue(beregningJson, UtbetalingsberegningData::class.java)

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
