package no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.medBehandlingsperiode
import no.nav.helse.bakrommet.util.serialisertTilString

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

interface VilkårRouteSessionDaoer {
    val vurdertVilkårDao: VurdertVilkårDao
}

fun VurdertVilkår.tilApiSvar(): JsonNode {
    val kopiert = vurdering.deepCopy<ObjectNode>()
    kopiert.put("kode", kode)
    return kopiert
}

private enum class OpprettetEllerEndret {
    OPPRETTET,
    ENDRET,
}

internal fun Route.saksbehandlingsperiodeVilkårRoute(
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    personDao: PersonDao,
    vurdertVikårDao: VurdertVilkårDao,
    sessionFactory: TransactionalSessionFactory<VilkårRouteSessionDaoer>,
) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/vilkaar") {
        get {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val vurderteVilkår =
                    vurdertVikårDao.hentVilkårsvurderinger(periode.id).map {
                        it.tilApiSvar()
                    }
                call.respondText(vurderteVilkår.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/vilkaar/{kode}") {
        put {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val vilkårsKode = Kode(call.parameters["kode"]!!)
                val vurdertVilkår = call.receive<JsonNode>()

                val opprettetEllerEndret =
                    sessionFactory.transactionalSessionScope { session ->
                        session.vurdertVilkårDao.let { dao ->
                            val finnesFraFør = dao.eksisterer(periode, vilkårsKode)
                            if (finnesFraFør) {
                                dao.oppdater(periode, vilkårsKode, vurdertVilkår)
                                OpprettetEllerEndret.ENDRET
                            } else {
                                dao.leggTil(periode, vilkårsKode, vurdertVilkår)
                                OpprettetEllerEndret.OPPRETTET
                            }
                        }
                    }

                val lagretVurdering = vurdertVikårDao.hentVilkårsvurdering(periode.id, vilkårsKode.kode)
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
                val numAffectedRows = vurdertVikårDao.slettVilkårsvurdering(periode.id, vilkårsKode.kode)
                if (numAffectedRows == 0) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
