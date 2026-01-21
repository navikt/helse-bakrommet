package no.nav.helse.bakrommet.db.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.db.dto.tilkommeninntekt.TilkommenInntektYrkesaktivitetType
import no.nav.helse.bakrommet.db.tilPgJson
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntekt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntektId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntektYrkesaktivitetType.*
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.repository.TilkommenInntektRepository

class PgTilkommenInntektRepository private constructor(
    private val queryRunner: QueryRunner,
) : TilkommenInntektRepository {
    constructor(session: Session) : this(MedSession(session))

    override fun lagre(tilkommenInntekt: TilkommenInntekt) {
        queryRunner.update(
            """
            insert into tilkommen_inntekt
                (id, behandling_id, tilkommen_inntekt, opprettet, opprettet_av_nav_ident)
            values
                (:id, :behandling_id, :tilkommen_inntekt, :opprettet, :opprettet_av_nav_ident)
            on conflict (id) do update
            set
                tilkommen_inntekt = excluded.tilkommen_inntekt
            """.trimIndent(),
            "id" to tilkommenInntekt.id.value,
            "behandling_id" to tilkommenInntekt.behandlingId.value,
            "tilkommen_inntekt" to
                no.nav.helse.bakrommet.db.dto.tilkommeninntekt
                    .TilkommenInntekt(
                        ident = tilkommenInntekt.ident,
                        yrkesaktivitetType =
                            when (tilkommenInntekt.yrkesaktivitetType) {
                                VIRKSOMHET -> TilkommenInntektYrkesaktivitetType.VIRKSOMHET
                                PRIVATPERSON -> TilkommenInntektYrkesaktivitetType.PRIVATPERSON
                                NÆRINGSDRIVENDE -> TilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE
                            },
                        fom = tilkommenInntekt.fom,
                        tom = tilkommenInntekt.tom,
                        inntektForPerioden = tilkommenInntekt.inntektForPerioden,
                        notatTilBeslutter = tilkommenInntekt.notatTilBeslutter,
                        ekskluderteDager = tilkommenInntekt.ekskluderteDager,
                    ).tilPgJson(),
            "opprettet" to tilkommenInntekt.opprettet,
            "opprettet_av_nav_ident" to tilkommenInntekt.opprettetAvNavIdent,
        )
    }

    override fun finn(tilkommenInntektId: TilkommenInntektId): TilkommenInntekt? =
        queryRunner.single(
            """
            select id, behandling_id, tilkommen_inntekt, opprettet, opprettet_av_nav_ident
            from tilkommen_inntekt
            where id = :id
            """.trimIndent(),
            "id" to tilkommenInntektId.value,
        ) {
            rowTilTilkommenInntekt(it)
        }

    override fun slett(tilkommenInntektId: TilkommenInntektId) {
        queryRunner.update(
            """
            DELETE FROM tilkommen_inntekt WHERE id = :id
            """.trimIndent(),
            "id" to tilkommenInntektId.value,
        )
    }

    override fun finnFor(behandlingId: BehandlingId): List<TilkommenInntekt> =
        queryRunner.list(
            """
            select id, behandling_id, tilkommen_inntekt, opprettet, opprettet_av_nav_ident
            from tilkommen_inntekt
            where behandling_id = :behandlingId
            """.trimIndent(),
            "behandlingId" to behandlingId.value,
        ) {
            rowTilTilkommenInntekt(it)
        }

    private fun rowTilTilkommenInntekt(row: Row): TilkommenInntekt {
        val dbTilkommenInntektJsonParsed =
            objectMapper.readValue<no.nav.helse.bakrommet.db.dto.tilkommeninntekt.TilkommenInntekt>(row.string("tilkommen_inntekt"))

        return TilkommenInntekt.fraLagring(
            id = TilkommenInntektId(row.uuid("id")),
            behandlingId = BehandlingId(row.uuid("behandling_id")),
            ident = dbTilkommenInntektJsonParsed.ident,
            yrkesaktivitetType =
                when (dbTilkommenInntektJsonParsed.yrkesaktivitetType) {
                    TilkommenInntektYrkesaktivitetType.VIRKSOMHET -> VIRKSOMHET
                    TilkommenInntektYrkesaktivitetType.PRIVATPERSON -> PRIVATPERSON
                    TilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE -> NÆRINGSDRIVENDE
                },
            fom = dbTilkommenInntektJsonParsed.fom,
            tom = dbTilkommenInntektJsonParsed.tom,
            inntektForPerioden = dbTilkommenInntektJsonParsed.inntektForPerioden,
            notatTilBeslutter = dbTilkommenInntektJsonParsed.notatTilBeslutter,
            ekskluderteDager = dbTilkommenInntektJsonParsed.ekskluderteDager,
            opprettet = row.offsetDateTime("opprettet"),
            opprettetAvNavIdent = row.string("opprettet_av_nav_ident"),
        )
    }
}
