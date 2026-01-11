package no.nav.helse.bakrommet.behandling

import java.time.OffsetDateTime
import java.util.UUID

enum class SaksbehandlingsperiodeEndringType {
    STARTET,
    SENDT_TIL_BESLUTNING,
    TATT_TIL_BESLUTNING,
    SENDT_I_RETUR,
    GODKJENT,
    OPPDATERT_INDIVIDUELL_BEGRUNNELSE,
    OPPDATERT_SKJÆRINGSTIDSPUNKT,
    OPPDATERT_YRKESAKTIVITET_KATEGORISERING,
    REVURDERING_STARTET,
}

data class SaksbehandlingsperiodeEndring(
    val behandlingId: UUID,
    // //
    val status: BehandlingStatus,
    val beslutterNavIdent: String?,
    // //
    val endretTidspunkt: OffsetDateTime,
    val endretAvNavIdent: String,
    val endringType: SaksbehandlingsperiodeEndringType,
    val endringKommentar: String? = null,
)

interface BehandlingEndringerDao {
    fun leggTilEndring(hist: SaksbehandlingsperiodeEndring)

    fun hentEndringerFor(behandlingId: UUID): List<SaksbehandlingsperiodeEndring>
}
