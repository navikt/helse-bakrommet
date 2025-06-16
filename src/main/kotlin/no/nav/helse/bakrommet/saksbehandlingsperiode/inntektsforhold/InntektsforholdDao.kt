package no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold

import kotliquery.Row
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.util.insert
import no.nav.helse.bakrommet.util.list
import no.nav.helse.bakrommet.util.single
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

data class Inntektsforhold(
    val id: UUID,
    val inntektsforholdType: String,
    val sykmeldtFraForholdet: Boolean,
    val orgnummer: String?,
    val orgnavn: String?,
    val dagoversikt: String,
    val saksbehandlingsperiodeId: UUID,
    val opprettet: OffsetDateTime,
)

class InntektsforholdDao(private val dataSource: DataSource) {
    fun opprettInntektsforhold(inntektsforhold: Inntektsforhold): Inntektsforhold {
        dataSource.insert(
            """
            insert into inntektsforhold
                (id, inntektsforhold_type, sykmeldt_fra_forholdet, 
                orgnummer, orgnavn, dagoversikt, saksbehandlingsperiode_id, opprettet)
            values
                (:id, :inntektsforhold_type, :sykmeldt_fra_forholdet, 
                :orgnummer, :orgnavn, :dagoversikt, :saksbehandlingsperiode_id, :opprettet)
            """.trimIndent(),
            "id" to inntektsforhold.id,
            "inntektsforhold_type" to inntektsforhold.inntektsforholdType,
            "sykmeldt_fra_forholdet" to inntektsforhold.sykmeldtFraForholdet,
            "orgnummer" to inntektsforhold.orgnummer,
            "orgnavn" to inntektsforhold.orgnavn,
            "dagoversikt" to inntektsforhold.dagoversikt,
            "saksbehandlingsperiode_id" to inntektsforhold.saksbehandlingsperiodeId,
            "opprettet" to inntektsforhold.opprettet,
        )
        return hentInntektsforhold(inntektsforhold.id)!!
    }

    fun hentInntektsforhold(id: UUID): Inntektsforhold? =
        dataSource.single(
            """
            select * from inntektsforhold where id = :id
            """.trimIndent(),
            "id" to id,
            mapper = ::inntektsforholdFraRow,
        )

    fun hentInntektsforholdFor(periode: Saksbehandlingsperiode): List<Inntektsforhold> =
        dataSource.list(
            """
            select * from inntektsforhold where saksbehandlingsperiode_id = :behandling_id
            """.trimIndent(),
            "behandling_id" to periode.id,
            mapper = ::inntektsforholdFraRow,
        )

    private fun inntektsforholdFraRow(row: Row) =
        Inntektsforhold(
            id = row.uuid("id"),
            inntektsforholdType = row.string("inntektsforhold_type"),
            sykmeldtFraForholdet = row.boolean("sykmeldt_fra_forholdet"),
            orgnummer = row.stringOrNull("orgnummer"),
            orgnavn = row.stringOrNull("orgnavn"),
            dagoversikt = row.string("dagoversikt"),
            saksbehandlingsperiodeId = row.uuid("saksbehandlingsperiode_id"),
            opprettet = row.offsetDateTime("opprettet"),
        )
}
