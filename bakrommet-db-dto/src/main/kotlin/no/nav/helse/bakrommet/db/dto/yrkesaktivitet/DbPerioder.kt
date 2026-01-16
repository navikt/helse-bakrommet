package no.nav.helse.bakrommet.db.dto.yrkesaktivitet

data class DbPerioder(
    val type: DbPeriodetype,
    val perioder: List<DbPeriode>,
)
