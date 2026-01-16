package no.nav.helse.bakrommet.db.dto.yrkesaktivitet

data class DbDagoversikt(
    val sykdomstidlinje: List<DbDag> = emptyList(),
    val avslagsdager: List<DbDag> = emptyList(),
)
