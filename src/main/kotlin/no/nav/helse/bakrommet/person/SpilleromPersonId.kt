package no.nav.helse.bakrommet.person

import java.util.UUID

data class SpilleromPersonId(val personId: String) {
    companion object {
        fun lagNy() = SpilleromPersonId(personId = lagNySpilleromId())
    }

    override fun toString() = personId
}

private fun lagNySpilleromId(): String = UUID.randomUUID().toString().replace("-", "").substring(0, 5)
