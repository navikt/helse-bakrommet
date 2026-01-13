package no.nav.helse.bakrommet.domain.person

@JvmInline
value class NaturligIdent(
    val value: String,
) {
    init {
        require(value.matches(Regex("""^[0-9]{11}$"""))) {
            "Naturlig ident må være 11 sifre"
        }
    }

    override fun toString() = value
}

fun String.somNaturligIdent(): NaturligIdent = NaturligIdent(this)
