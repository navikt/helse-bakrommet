package no.nav.helse.bakrommet

import io.ktor.server.application.ApplicationCall
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.bakrommet.util.somGyldigUUID
import java.util.UUID

const val PARAM_PERIODEUUID = "periodeUUID"
const val PARAM_PERSONID = "personId"
const val PARAM_YRKESAKTIVITETUUID = "yrkesaktivitetUUID"

fun ApplicationCall.personId(): SpilleromPersonId =
    parameters[PARAM_PERSONID]?.let { SpilleromPersonId(it) }
        ?: throw InputValideringException("Mangler personId i path")

fun ApplicationCall.periodeUUID(): UUID =
    parameters[PARAM_PERIODEUUID]?.somGyldigUUID()
        ?: throw InputValideringException("Mangler periodeUUID i path")
