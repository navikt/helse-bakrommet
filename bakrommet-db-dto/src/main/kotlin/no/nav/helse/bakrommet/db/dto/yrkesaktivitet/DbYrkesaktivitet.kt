package no.nav.helse.bakrommet.db.dto.yrkesaktivitet

import java.time.OffsetDateTime
import java.util.UUID

data class DbYrkesaktivitet(
    val id: UUID,
    val kategorisering: DbYrkesaktivitetKategorisering,
    val kategoriseringGenerert: DbYrkesaktivitetKategorisering?,
    val dagoversikt: DbDagoversikt?,
    val dagoversiktGenerert: DbDagoversikt?,
    val behandlingId: UUID,
    val opprettet: OffsetDateTime,
    val generertFraDokumenter: List<UUID>,
    val perioder: DbPerioder? = null,
    val inntektRequest: DbInntektRequest? = null,
    val inntektData: DbInntektData? = null,
    val refusjon: List<DbRefusjonsperiode>? = null,
)
