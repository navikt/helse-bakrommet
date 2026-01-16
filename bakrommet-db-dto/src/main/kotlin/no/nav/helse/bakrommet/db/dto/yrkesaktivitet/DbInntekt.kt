package no.nav.helse.bakrommet.db.dto.yrkesaktivitet

sealed class DbInntekt {
    abstract val beløp: Number

    data class Årlig(
        override val beløp: Double,
    ) : DbInntekt()

    data class Månedlig(
        override val beløp: Double,
    ) : DbInntekt()

    data class DagligDouble(
        override val beløp: Double,
    ) : DbInntekt()

    data class Daglig(
        override val beløp: Int,
    ) : DbInntekt()
}
