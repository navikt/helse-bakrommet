package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.auth.brukerPrincipal
import no.nav.helse.bakrommet.errorhandling.ForbiddenException
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.SaksbehandlingsperiodeIkkeFunnetException
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.skapDagoversiktFraSoknader
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Inntektsforhold
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Kategorisering
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.bakrommet.util.somGyldigUUID
import no.nav.helse.bakrommet.util.tilJsonNode
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

internal suspend inline fun ApplicationCall.medBehandlingsperiode(
    personDao: PersonDao,
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    crossinline block: suspend (saksbehandlingsperiode: Saksbehandlingsperiode) -> Unit,
) {
    this.medIdent(personDao) { fnr, spilleromPersonId ->
        val periodeId = parameters[PARAM_PERIODEUUID].somGyldigUUID()
        val periode =
            saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(periodeId)
                ?: throw SaksbehandlingsperiodeIkkeFunnetException()
        if (periode.spilleromPersonId != spilleromPersonId) {
            throw InputValideringException("Ugyldig saksbehandlingsperiode")
        }
        block(periode)
    }
}

internal fun Route.saksbehandlingsperiodeRoute(
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    personDao: PersonDao,
    dokumentDao: DokumentDao,
    saksbehandlingsperiodeService: SaksbehandlingsperiodeService,
    dokumentRoutes: List<Route.() -> Unit> = emptyList(),
) {
    route("/v1/saksbehandlingsperioder") {
        get {
            val perioder = saksbehandlingsperiodeDao.hentAlleSaksbehandlingsperioder()
            call.respondText(perioder.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder") {
        data class CreatePeriodeRequest(
            val fom: String,
            val tom: String,
            val søknader: List<UUID>? = null,
        )

        /** Opprett en ny periode */
        post {
            call.medIdent(personDao) { fnr, spilleromPersonId ->
                val body = call.receive<CreatePeriodeRequest>()
                val fom = LocalDate.parse(body.fom)
                val tom = LocalDate.parse(body.tom)
                val saksbehandler =
                    BrukerOgToken(
                        bruker = call.brukerPrincipal()!!,
                        token = call.request.bearerToken(),
                    )
                val nyPeriode =
                    saksbehandlingsperiodeService.opprettNySaksbehandlingsperiode(
                        spilleromPersonId = SpilleromPersonId(spilleromPersonId),
                        fom = fom,
                        tom = tom,
                        søknader = body.søknader?.toSet() ?: emptySet(),
                        saksbehandler = saksbehandler,
                    )
                call.respondText(nyPeriode.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.Created)
            }
        }

        /** Hent alle perioder for en person */
        get {
            call.medIdent(personDao) { fnr, spilleromPersonId ->
                val perioder = saksbehandlingsperiodeDao.finnPerioderForPerson(spilleromPersonId)
                call.respondText(perioder.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}") {
        get {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                call.respondText(periode.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/sendtilbeslutning") {
        post {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val bruker = call.brukerPrincipal()!!
                krevAtBrukerErSaksbehandlerFor(bruker, periode)
                val nyStatus = SaksbehandlingsperiodeStatus.TIL_BESLUTNING
                periode.verifiserNyStatusGyldighet(nyStatus)
                saksbehandlingsperiodeDao.endreStatus(periode, nyStatus = nyStatus)
                val oppdatertPeriode = saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(periode.id)!!
                call.respondText(oppdatertPeriode.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/tatilbeslutning") {
        post {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val bruker = call.brukerPrincipal()!!
                val nyStatus = SaksbehandlingsperiodeStatus.UNDER_BESLUTNING
                periode.verifiserNyStatusGyldighet(nyStatus)
                saksbehandlingsperiodeDao.endreStatusOgBeslutter(
                    periode,
                    nyStatus = nyStatus,
                    beslutterNavIdent = bruker.navIdent,
                )
                val oppdatertPeriode = saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(periode.id)!!
                call.respondText(oppdatertPeriode.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/sendtilbake") {
        post {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val bruker = call.brukerPrincipal()!!
                krevAtBrukerErBeslutterFor(bruker, periode)

                val nyStatus = SaksbehandlingsperiodeStatus.UNDER_BEHANDLING
                periode.verifiserNyStatusGyldighet(nyStatus)
                saksbehandlingsperiodeDao.endreStatusOgBeslutter(
                    periode,
                    nyStatus = nyStatus,
                    beslutterNavIdent = null,
                ) // TODO: Eller skal beslutter beholdes ?
                val oppdatertPeriode = saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(periode.id)!!
                call.respondText(oppdatertPeriode.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/godkjenn") {
        post {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val bruker = call.brukerPrincipal()!!
                krevAtBrukerErBeslutterFor(bruker, periode)

                val nyStatus = SaksbehandlingsperiodeStatus.GODKJENT
                periode.verifiserNyStatusGyldighet(nyStatus)
                saksbehandlingsperiodeDao.endreStatusOgBeslutter(
                    periode,
                    nyStatus = nyStatus,
                    beslutterNavIdent = bruker.navIdent,
                )
                val oppdatertPeriode = saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(periode.id)!!
                call.respondText(oppdatertPeriode.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/dokumenter") {
        get {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val dokumenter = dokumentDao.hentDokumenterFor(periode.id)
                val dokumenterDto = dokumenter.map { it.tilDto() }
                call.respondText(dokumenterDto.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }

        route("/{dokumentUUID}") {
            get {
                call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                    val dokumentId = call.parameters["dokumentUUID"].somGyldigUUID()
                    val dok = dokumentDao.hentDokument(dokumentId)
                    if (dok == null || (dok.opprettetForBehandling != periode.id)) {
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        call.respondText(dok.tilDto().serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
                    }
                }
            }
        }

        dokumentRoutes.forEach { dokRoute ->
            dokRoute(this)
        }
    }
}

fun krevAtBrukerErBeslutterFor(
    bruker: Bruker,
    periode: Saksbehandlingsperiode,
) {
    fun Bruker.erBeslutterFor(periode: Saksbehandlingsperiode): Boolean {
        return periode.beslutterNavIdent == this.navIdent
    }

    if (!bruker.erBeslutterFor(periode)) {
        throw ForbiddenException("Ikke beslutter for periode")
    }
}

fun krevAtBrukerErSaksbehandlerFor(
    bruker: Bruker,
    periode: Saksbehandlingsperiode,
) {
    fun Bruker.erSaksbehandlerFor(periode: Saksbehandlingsperiode): Boolean {
        return periode.opprettetAvNavIdent == this.navIdent
    }

    if (!bruker.erSaksbehandlerFor(periode)) {
        throw ForbiddenException("Ikke saksbehandler for periode")
    }
}

fun Saksbehandlingsperiode.verifiserNyStatusGyldighet(nyStatus: SaksbehandlingsperiodeStatus) {
    if (!SaksbehandlingsperiodeStatus.erGyldigEndring(status to nyStatus)) {
        throw InputValideringException("Ugyldig statusendring: $status til $nyStatus")
    }
}

fun SykepengesoknadDTO.kategorisering(): Kategorisering {
    return objectMapper.createObjectNode().apply {
        val soknad = this@kategorisering
        put("INNTEKTSKATEGORI", soknad.bestemInntektskategori())
        val orgnummer = soknad.arbeidsgiver?.orgnummer
        if (orgnummer != null) {
            put("ORGNUMMER", orgnummer)
        }
    }
}

fun lagInntektsforholdFraSøknader(
    sykepengesoknader: Iterable<Dokument>,
    saksbehandlingsperiode: Saksbehandlingsperiode,
): List<Inntektsforhold> {
    val kategorierOgSøknader =
        sykepengesoknader
            .groupBy { dokument -> dokument.somSøknad().kategorisering() }
    return kategorierOgSøknader.map { (kategorisering, dok) ->
        val dagoversikt = skapDagoversiktFraSoknader(dok.map { it.somSøknad() }, saksbehandlingsperiode.fom, saksbehandlingsperiode.tom)
        Inntektsforhold(
            id = UUID.randomUUID(),
            kategorisering = kategorisering,
            kategoriseringGenerert = kategorisering,
            dagoversikt = dagoversikt.tilJsonNode(),
            dagoversiktGenerert = dagoversikt.tilJsonNode(),
            saksbehandlingsperiodeId = saksbehandlingsperiode.id,
            opprettet = OffsetDateTime.now(),
            generertFraDokumenter = dok.map { it.id },
        )
    }
}

private fun SykepengesoknadDTO.bestemInntektskategori() =
    when (arbeidssituasjon) {
        ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE -> InntektsforholdType.SELVSTENDIG_NÆRINGSDRIVENDE
        ArbeidssituasjonDTO.FISKER -> InntektsforholdType.SELVSTENDIG_NÆRINGSDRIVENDE
        ArbeidssituasjonDTO.JORDBRUKER -> InntektsforholdType.SELVSTENDIG_NÆRINGSDRIVENDE
        ArbeidssituasjonDTO.FRILANSER -> InntektsforholdType.FRILANSER
        ArbeidssituasjonDTO.ARBEIDSTAKER -> InntektsforholdType.ARBEIDSTAKER
        ArbeidssituasjonDTO.ARBEIDSLEDIG -> InntektsforholdType.INAKTIV
        ArbeidssituasjonDTO.ANNET -> InntektsforholdType.ANNET
        null -> {
            logg.warn("'null'-verdi for arbeidssituasjon for søknad med id={}", id)
            "IKKE SATT"
        }
    }.toString()

// kopiert fra frontend:
private enum class InntektsforholdType {
    ARBEIDSTAKER,
    FRILANSER,
    SELVSTENDIG_NÆRINGSDRIVENDE,
    INAKTIV,
    ANNET,
}

fun RoutingContext.dokumentUriFor(dokument: Dokument): String {
    val periodeId = call.parameters[PARAM_PERIODEUUID].somGyldigUUID()
    val personId = call.parameters[PARAM_PERSONID]!!
    val dokUri = "/v1/$personId/saksbehandlingsperioder/$periodeId/dokumenter"
    check(call.request.uri.startsWith(dokUri)) {
        "Forventet å være i kontekst av /dokumenter for å kunne resolve dokument-uri"
    }
    return "$dokUri/${dokument.id}"
}
