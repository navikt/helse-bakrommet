package no.nav.helse.bakrommet.person

data class NaturligIdent(
    val naturligIdent: String,
) {
    init {
        require(naturligIdent.matches(Regex("""^[0-9]{11}$"""))) {
            "Naturlig ident må være 11 sifre"
        }
    }

    override fun toString() = naturligIdent
}

fun String.somNaturligIdent(): NaturligIdent = NaturligIdent(this)
