package no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.medInputvalidering
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.util.serialisertTilString
import java.util.*

fun String.erGyldigSomKode(): Boolean {
    val regex = "^[A-ZÆØÅ0-9_]*$".toRegex()
    return regex.matches(this)
}

class Kode(
    @JsonValue val kode: String,
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

data class VurdertVilkårBody(
    val vurdering: JsonNode,
)

private suspend inline fun ApplicationCall.medBehandlingsperiode(
    personDao: PersonDao,
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    crossinline block: suspend (saksbehandlingsperiode: Saksbehandlingsperiode) -> Unit,
) {
    this.medIdent(personDao) { fnr, spilleromPersonId ->
        val periodeId = parameters["periodeUUID"]!!.somGyldigUUID()
        val periode = saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(periodeId)!!
        if (periode.spilleromPersonId != spilleromPersonId) {
            throw InputValideringException("Ugyldig saksbehandlingsperiode")
        }
        block(periode)
    }
}

internal fun Route.saksbehandlingsperiodeVilkårRoute(
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    personDao: PersonDao,
) {
    route("/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/vilkaar") {
        get {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val vurderteVilkår =
                    saksbehandlingsperiodeDao.hentVurderteVilkårFor(periode.id).map {
                        it.tilApiSvar()
                    }
                call.respondText(vurderteVilkår.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }

    route("/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/vilkaar/{kode}") {
        put {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val vilkårsKode = Kode(call.parameters["kode"]!!)
                val vurdertVilkår = medInputvalidering { call.receive<JsonNode>() }

                val opprettetEllerEndret =
                    saksbehandlingsperiodeDao.lagreVilkårsvurdering(
                        periode = periode,
                        vilkårsKode = vilkårsKode,
                        vurdering = vurdertVilkår,
                    )
                val lagretVurdering = saksbehandlingsperiodeDao.hentVurdertVilkårFor(periode.id, vilkårsKode.kode)
                call.respondText(
                    lagretVurdering!!.tilApiSvar().serialisertTilString(),
                    ContentType.Application.Json,
                    if (opprettetEllerEndret == OpprettetEllerEndret.OPPRETTET) HttpStatusCode.Created else HttpStatusCode.OK,
                )
            }
        }

        delete {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val vilkårsKode = Kode(call.parameters["kode"]!!)
                val numAffectedRows = saksbehandlingsperiodeDao.slettVilkårsvurdering(periode.id, vilkårsKode.kode)
                if (numAffectedRows == 0) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
