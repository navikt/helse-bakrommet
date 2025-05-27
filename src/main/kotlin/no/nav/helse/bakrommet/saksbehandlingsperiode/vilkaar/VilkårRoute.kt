package no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar

import com.fasterxml.jackson.annotation.JsonValue
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.medInputvalidering
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import java.util.*

fun String.erGyldigSomKode(): Boolean {
    val regex = "^[A-ZÆØÅ0-9_]*$".toRegex()
    return regex.matches(this)
}

class Kode(
    @JsonValue private val kode: String,
) {
    init {
        if (!kode.erGyldigSomKode()) {
            throw InputValideringException("Ugyldig format på Kode")
        }
    }

    override fun toString(): String = kode
}

fun String.somGyldigUUID(): UUID =
    try {
        UUID.fromString(this)
    } catch (ex: IllegalArgumentException) {
        throw InputValideringException("Ugyldig periodeUUID. Forventet UUID-format")
    }

enum class VilkårStatus {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_RELEVANT,
    IKKE_VURDERT,
}

data class VurdertVilkår(
    val vilkårKode: Kode,
    val status: VilkårStatus,
    val fordi: String,
)

internal fun Route.saksbehandlingsperiodeVilkårRoute(
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    personDao: PersonDao,
) {
    route("/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/vilkår") {
        post {
            call.medIdent(personDao) { fnr, spilleromPersonId ->
                val periodeId = call.parameters["periodeUUID"]!!.somGyldigUUID()

                val vilkår = medInputvalidering { call.receive<List<VurdertVilkår>>() }

                val periode = saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(periodeId)!!
                require(periode.spilleromPersonId == spilleromPersonId)

                vilkår.forEach {
                    saksbehandlingsperiodeDao.lagreVilkårsvurdering(
                        periode = periode,
                        vilkårsKode = it.vilkårKode,
                        status = it.status,
                        fordi = it.fordi,
                    )
                }

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
