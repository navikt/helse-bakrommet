package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

data class FaktiskInntektEntity(
    val id: UUID,
    val inntektsforholdId: UUID,
    val beløpPerMånedØre: Long,
    val kilde: Inntektskilde,
    val erSkjønnsfastsatt: Boolean,
    val skjønnsfastsettelseBegrunnelse: String?,
    val refusjonsbeløpPerMånedØre: Long?,
    val refusjonsgrad: Int?,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
)

class FaktiskInntektDao private constructor(private val db: QueryRunner) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun opprettFaktiskInntekt(faktiskInntekt: FaktiskInntekt): FaktiskInntekt {
        val id = UUID.randomUUID()
        db.update(
            """
            INSERT INTO faktisk_inntekt 
                (id, inntektsforhold_id, belop_per_maned_ore, kilde, er_skjonnsfastsatt,
                 skjonnsfastsettelse_begrunnelse, refusjon_belop_per_maned_ore, refusjon_grad,
                 opprettet, opprettet_av_nav_ident)
            VALUES 
                (:id, :inntektsforhold_id, :belop_per_maned_ore, :kilde, :er_skjonnsfastsatt,
                 :skjonnsfastsettelse_begrunnelse, :refusjon_belop_per_maned_ore, :refusjon_grad,
                 NOW(), :opprettet_av_nav_ident)
            """.trimIndent(),
            "id" to id,
            "inntektsforhold_id" to faktiskInntekt.inntektsforholdId,
            "belop_per_maned_ore" to faktiskInntekt.beløpPerMånedØre,
            "kilde" to faktiskInntekt.kilde.name,
            "er_skjonnsfastsatt" to faktiskInntekt.erSkjønnsfastsatt,
            "skjonnsfastsettelse_begrunnelse" to faktiskInntekt.skjønnsfastsettelseBegrunnelse,
            "refusjon_belop_per_maned_ore" to faktiskInntekt.refusjon?.refusjonsbeløpPerMånedØre,
            "refusjon_grad" to faktiskInntekt.refusjon?.refusjonsgrad,
            "opprettet_av_nav_ident" to faktiskInntekt.opprettetAv,
        )
        return hentFaktiskInntekt(id)!!
    }

    fun hentFaktiskInntekt(id: UUID): FaktiskInntekt? =
        db.single(
            """
            SELECT * FROM faktisk_inntekt WHERE id = :id
            """.trimIndent(),
            "id" to id,
            mapper = ::faktiskInntektFraRow,
        )

    fun hentFaktiskeInntekterFor(saksbehandlingsperiodeId: UUID): List<FaktiskInntekt> =
        db.list(
            """
            SELECT fi.* FROM faktisk_inntekt fi
            JOIN inntektsforhold i ON fi.inntektsforhold_id = i.id
            WHERE i.saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            ORDER BY fi.opprettet DESC
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
            mapper = ::faktiskInntektFraRow,
        )

    fun slettFaktiskeInntekterFor(saksbehandlingsperiodeId: UUID) {
        db.update(
            """
            DELETE FROM faktisk_inntekt 
            WHERE inntektsforhold_id IN (
                SELECT id FROM inntektsforhold 
                WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            )
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
        )
    }

    private fun faktiskInntektFraRow(row: Row): FaktiskInntekt {
        val refusjon =
            if (row.longOrNull("refusjon_belop_per_maned_ore") != null &&
                row.intOrNull("refusjon_grad") != null
            ) {
                Refusjonsforhold(
                    refusjonsbeløpPerMånedØre = row.long("refusjon_belop_per_maned_ore"),
                    refusjonsgrad = row.int("refusjon_grad"),
                )
            } else {
                null
            }

        return FaktiskInntekt(
            id = row.uuid("id"),
            inntektsforholdId = row.uuid("inntektsforhold_id"),
            beløpPerMånedØre = row.long("belop_per_maned_ore"),
            kilde = Inntektskilde.valueOf(row.string("kilde")),
            erSkjønnsfastsatt = row.boolean("er_skjonnsfastsatt"),
            skjønnsfastsettelseBegrunnelse = row.stringOrNull("skjonnsfastsettelse_begrunnelse"),
            refusjon = refusjon,
            opprettetAv = row.string("opprettet_av_nav_ident"),
        )
    }
}
