package no.nav.helse.bakrommet.db.repository

import kotliquery.Session
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.db.dto.yrkesaktivitet.DbYrkesaktivitet
import no.nav.helse.bakrommet.db.tilPgJson
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.repository.YrkesaktivitetRepository

class PgYrkesaktivitetRepository private constructor(
    private val queryRunner: QueryRunner,
) : YrkesaktivitetRepository {
    constructor(session: Session) : this(MedSession(session))

    override fun finn(behandlingId: BehandlingId): List<Yrkesaktivitet> {
        TODO("Not yet implemented")
    }

    override fun lagre(yrkesaktivitet: Yrkesaktivitet) {
        val record: DbYrkesaktivitet = yrkesaktivitet.toDbRecord()
        queryRunner.update(
            """
            insert into yrkesaktivitet
                (id, kategorisering, kategorisering_generert,
                dagoversikt, dagoversikt_generert,
                behandling_id, opprettet, generert_fra_dokumenter, perioder, inntekt_data, refusjon)
            values(
                :id, :kategorisering, :kategorisering_generert,
                :dagoversikt, :dagoversikt_generert,
                :behandling_id, :opprettet, :generert_fra_dokumenter, :perioder, :inntekt_data, :refusjon
            )
            on conflict (id) do update
                set kategorisering = excluded.kategorisering,
                    kategorisering_generert = excluded.kategorisering_generert,
                    dagoversikt = excluded.dagoversikt,
                    dagoversikt_generert = excluded.dagoversikt_generert,
                    perioder = excluded.perioder,
                    inntekt_data = excluded.inntekt_data,
                    refusjon = excluded.refusjon
            """.trimIndent(),
            "id" to record.id,
            "kategorisering" to record.kategorisering.tilPgJson(),
            "kategorisering_generert" to record.kategoriseringGenerert?.tilPgJson(),
            "dagoversikt" to record.dagoversikt?.tilPgJson(),
            "dagoversikt_generert" to record.dagoversiktGenerert?.tilPgJson(),
            "behandling_id" to record.behandlingId,
            "opprettet" to record.opprettet,
            "generert_fra_dokumenter" to record.generertFraDokumenter.tilPgJson(),
            "perioder" to record.perioder?.tilPgJson(),
            "inntekt_data" to record.inntektData?.tilPgJson(),
            "refusjon" to record.refusjon?.tilPgJson(),
        )
    }

    private fun Yrkesaktivitet.toDbRecord(): DbYrkesaktivitet = TODO()
}
