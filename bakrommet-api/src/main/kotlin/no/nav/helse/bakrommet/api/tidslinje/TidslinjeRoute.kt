package no.nav.helse.bakrommet.api.tidslinje

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helse.bakrommet.api.PARAM_PSEUDO_ID
import no.nav.helse.bakrommet.api.naturligIdent
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntektYrkesaktivitetType.PRIVATPERSON
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.maybeOrgnummer
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.OrganisasjonsnavnProvider

fun Route.tidslinjeRoute(
    organisasjonsnavnProvider: OrganisasjonsnavnProvider,
    db: DbDaoer<AlleDaoer>,
) {
    route("/v2/{$PARAM_PSEUDO_ID}/tidslinje") {
        get {
            val response =
                db.transactional {
                    val naturligIdent = naturligIdent(call)
                    val behandlinger = this.behandlingRepository.finnFor(naturligIdent)
                    val yrkesaktivteter = behandlinger.flatMap { yrkesaktivitetRepository.finn(it.id) }
                    val tilkommen = behandlinger.flatMap { tilkommenInntektRepository.finnFor(it.id) }

                    val alleOrgnummer =
                        (
                            yrkesaktivteter.mapNotNull { it.kategorisering.maybeOrgnummer() } +
                                tilkommen
                                    .filter { it.yrkesaktivitetType != PRIVATPERSON }
                                    .map { it.ident }
                        ).toSet()

                    val organisasjonsnavnMap = organisasjonsnavnProvider.hentFlereOrganisasjonsnavn(alleOrgnummer)
                    TidslinjeData(behandlinger, yrkesaktivteter, tilkommen, organisasjonsnavnMap).tilTidslinjeDto()
                }

            call.respondJson(response)
        }
    }
}
