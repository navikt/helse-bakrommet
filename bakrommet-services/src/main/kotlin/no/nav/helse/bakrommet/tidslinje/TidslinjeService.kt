package no.nav.helse.bakrommet.tidslinje

import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetForenkletDbRecord
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntekt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntektYrkesaktivitetType.PRIVATPERSON
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.maybeOrgnummer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.Organisasjon
import no.nav.helse.bakrommet.infrastruktur.provider.OrganisasjonsnavnProvider
import no.nav.helse.bakrommet.repository.TilkommenInntektRepository

interface TidslinjeServiceDaoer {
    val behandlingDao: BehandlingDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val tilkommenInntektRepository: TilkommenInntektRepository
}

data class TidslinjeData(
    val behandlinger: List<BehandlingDbRecord>,
    val yrkesaktiviteter: List<YrkesaktivitetForenkletDbRecord>,
    val tilkommen: List<TilkommenInntekt>,
    val organisasjonsnavnMap: Map<String, Organisasjon?>,
)

class TidslinjeService(
    private val db: DbDaoer<TidslinjeServiceDaoer>,
    private val organisasjonsnavnProvider: OrganisasjonsnavnProvider,
) {
    suspend fun hentTidslinjeData(
        naturligIdent: NaturligIdent,
    ): TidslinjeData =
        db.transactional {
            val behandlinger = behandlingDao.finnBehandlingerForNaturligIdent(naturligIdent)
            val yrkesaktivteter = yrkesaktivitetDao.finnYrkesaktiviteterForBehandlinger(behandlinger.map { it.id })
            val tilkommen =
                behandlinger
                    .map { tilkommenInntektRepository.finnFor(BehandlingId(it.id)) }
                    .flatten()

            val alleOrgnummer =
                (
                    yrkesaktivteter.mapNotNull { it.kategorisering.maybeOrgnummer() } +
                        tilkommen
                            .filter { it.yrkesaktivitetType != PRIVATPERSON }
                            .map { it.ident }
                ).toSet()

            val organisasjonsnavnMap = organisasjonsnavnProvider.hentFlereOrganisasjonsnavn(alleOrgnummer)
            TidslinjeData(behandlinger, yrkesaktivteter, tilkommen, organisasjonsnavnMap)
        }
}
