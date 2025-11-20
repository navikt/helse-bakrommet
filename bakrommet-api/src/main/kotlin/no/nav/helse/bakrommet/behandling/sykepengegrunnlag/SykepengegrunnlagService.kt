package no.nav.helse.bakrommet.behandling.sykepengegrunnlag

import no.nav.helse.bakrommet.behandling.*
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.person.PersonDao

interface SykepengegrunnlagServiceDaoer {
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val behandlingDao: BehandlingDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val personDao: PersonDao
}

class SykepengegrunnlagService(
    private val db: DbDaoer<SykepengegrunnlagServiceDaoer>,
) {
    suspend fun hentSykepengegrunnlag(referanse: SaksbehandlingsperiodeReferanse): SykepengegrunnlagResponse? =
        db.nonTransactional {
            behandlingDao.hentPeriode(referanse, krav = null, måVæreUnderBehandling = false).sykepengegrunnlagId?.let { sykepengegrunnlagId ->
                sykepengegrunnlagDao.finnSykepengegrunnlag(sykepengegrunnlagId)?.let { record ->
                    SykepengegrunnlagResponse(
                        sykepengegrunnlag = record.sykepengegrunnlag,
                        sammenlikningsgrunnlag = record.sammenlikningsgrunnlag,
                        opprettetForBehandling = record.opprettetForBehandling,
                    )
                }
            }
        }
}
