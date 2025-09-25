package no.nav.helse.hendelser

import no.nav.helse.dto.HendelseskildeDto
import kotlin.reflect.KClass

internal typealias Melding = KClass<out Any>

data class Hendelseskilde(
    val type: String,
) {
    fun dto(): HendelseskildeDto {
        TODO("Not yet implemented")
    }

    companion object {
        fun gjenopprett(kilde: HendelseskildeDto): Hendelseskilde {
            return Hendelseskilde(kilde.type)
        }

        val INGEN = Hendelseskilde("SykdomshistorikkHendelse")
    }
}
