package no.nav.helse.bakrommet.tidslinje

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDao
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDbRecord
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektYrkesaktivitetType
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetForenkletDbRecord
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.maybeOrgnummer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.Organisasjon
import no.nav.helse.bakrommet.infrastruktur.provider.OrganisasjonsnavnProvider
import no.nav.helse.bakrommet.logg

interface TidslinjeServiceDaoer {
    val behandlingDao: BehandlingDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val tilkommenInntektDao: TilkommenInntektDao
}

data class TidslinjeData(
    val behandlinger: List<BehandlingDbRecord>,
    val yrkesaktiviteter: List<YrkesaktivitetForenkletDbRecord>,
    val tilkommen: List<TilkommenInntektDbRecord>,
    val organisasjonsnavnMap: Map<String, Organisasjon?>,
)

class TidslinjeService(
    private val db: DbDaoer<TidslinjeServiceDaoer>,
    private val organisasjonsnavnProvider: OrganisasjonsnavnProvider,
) {
    suspend fun hentTidslinjeData(
        naturligIdent: NaturligIdent,
    ): TidslinjeData =
        db.nonTransactional {
            val behandlinger = behandlingDao.finnBehandlingerForNaturligIdent(naturligIdent)
            val yrkesaktivteter = yrkesaktivitetDao.finnYrkesaktiviteterForBehandlinger(behandlinger.map { it.id })
            val tilkommen = tilkommenInntektDao.finnTilkommenInntektForBehandlinger(behandlinger.map { it.id })

            val alleOrgnummer =
                (
                    yrkesaktivteter.mapNotNull { it.kategorisering.maybeOrgnummer() } +
                        tilkommen
                            .filter { it.tilkommenInntekt.yrkesaktivitetType != TilkommenInntektYrkesaktivitetType.PRIVATPERSON }
                            .map { it.tilkommenInntekt.ident }
                ).toSet()

            val organisasjonsnavnMap =
                coroutineScope {
                    alleOrgnummer
                        .associateWith { orgnummer ->
                            async {
                                withTimeoutOrNull(3_000) {
                                    try {
                                        organisasjonsnavnProvider.hentOrganisasjonsnavn(orgnummer)
                                    } catch (e: Exception) {
                                        logg.warn("Kall mot Ereg feilet for orgnummer $orgnummer", e)
                                        null
                                    }
                                }
                            }
                        }.mapValues { (_, deferred) -> deferred.await() }
                }

            TidslinjeData(behandlinger, yrkesaktivteter, tilkommen, organisasjonsnavnMap)
        }
}
