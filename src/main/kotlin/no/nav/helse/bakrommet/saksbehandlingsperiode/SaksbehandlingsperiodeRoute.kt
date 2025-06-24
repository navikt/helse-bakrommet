package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.SaksbehandlingsperiodeIkkeFunnetException
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.skapDagoversiktFraSoknader
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Inntektsforhold
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Kategorisering
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.saksbehandler
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
        val periodeId = parameters["periodeUUID"].somGyldigUUID()
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
    dokumentHenter: DokumentHenter,
    dokumentDao: DokumentDao,
    inntektsforholdDao: InntektsforholdDao,
) {
    route("/v1/{personId}/saksbehandlingsperioder") {
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
                if (fom.isAfter(tom)) throw InputValideringException("Fom-dato kan ikke være etter tom-dato")
                val saksbehandler = call.saksbehandler()
                val nyPeriode =
                    Saksbehandlingsperiode(
                        id = UUID.randomUUID(),
                        spilleromPersonId = spilleromPersonId,
                        opprettet = OffsetDateTime.now(),
                        opprettetAvNavIdent = saksbehandler.navIdent,
                        opprettetAvNavn = saksbehandler.navn,
                        fom = fom,
                        tom = tom,
                    )
                saksbehandlingsperiodeDao.opprettPeriode(nyPeriode)
                val innhentedeDokumenter =
                    if (body.søknader != null && body.søknader.isNotEmpty()) {
                        dokumentHenter.hentOgLagreSøknaderOgInntekter(
                            nyPeriode.id,
                            body.søknader,
                            call.request.bearerToken(),
                        )
                    } else {
                        emptyList()
                    }
                val søknader =
                    innhentedeDokumenter.filter { it.dokumentType == DokumentType.søknad }
                lagInntektsforholdFraSøknader(søknader, nyPeriode).forEach(inntektsforholdDao::opprettInntektsforhold)

                // TODO: Returner også innhentede dokumenter?

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

    route("/v1/{personId}/saksbehandlingsperioder/{periodeUUID}") {
        get {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                call.respondText(periode.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }

    route("/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/dokumenter") {
        get {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val dokumenter = dokumentDao.hentDokumenterFor(periode.id)
                val dokumenterDto = dokumenter.map { it.tilDto() }
                call.respondText(dokumenterDto.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }
}

fun SykepengesoknadDTO.kategorisering(): Kategorisering {
    return objectMapper.createObjectNode().apply {
        val soknad = this@kategorisering
        put("INNTEKTSKATEGORI", soknad.bestemInntektskategori())
        val orgnummer = soknad.arbeidsgiver?.orgnummer
        val orgnavn = soknad.arbeidsgiver?.navn
        if (orgnummer != null) {
            put("ORGNUMMER", orgnummer)
            if (orgnavn != null) {
                put("ORGNAVN", orgnavn)
            }
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
