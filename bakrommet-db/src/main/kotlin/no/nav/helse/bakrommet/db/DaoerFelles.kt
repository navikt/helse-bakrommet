package no.nav.helse.bakrommet.db

import kotliquery.Session
import no.nav.helse.bakrommet.db.dao.*
import no.nav.helse.bakrommet.db.repository.PgBehandlingRepository
import no.nav.helse.bakrommet.db.repository.PgSykefraværstilfelleRepository
import no.nav.helse.bakrommet.db.repository.PgTilkommenInntektRepository
import no.nav.helse.bakrommet.db.repository.PgVilkårsvurderingRepository
import no.nav.helse.bakrommet.db.repository.PgYrkesaktivitetsperiodeRepository
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.repository.BehandlingRepository
import no.nav.helse.bakrommet.repository.SykefraværstilfelleRepository
import no.nav.helse.bakrommet.repository.TilkommenInntektRepository
import no.nav.helse.bakrommet.repository.VilkårsvurderingRepository
import javax.sql.DataSource

class DaoerFelles(
    dataSource: DataSource,
) : AlleDaoer {
    override val behandlingDao = BehandlingDaoPg(dataSource)
    override val behandlingRepository: BehandlingRepository get() = error("Ikke tilgjengelig utenfor transaksjon")
    override val vilkårsvurderingRepository: VilkårsvurderingRepository get() = error("Ikke tilgjengelig utenfor transaksjon")
    override val tilkommenInntektRepository: TilkommenInntektRepository get() = error("Ikke tilgjengelig utenfor transaksjon")
    override val sykefraværstilfelleRepository: SykefraværstilfelleRepository get() = error("Ikke tilgjengelig utenfor transaksjon")
    override val yrkesaktivitetsperiodeRepository = PgYrkesaktivitetsperiodeRepository(dataSource)
    override val behandlingEndringerDao = BehandlingEndringerDaoPg(dataSource)
    override val personPseudoIdDao = PersonPseudoIdDaoPg(dataSource)
    override val dokumentDao = DokumentDaoPg(dataSource)
    override val yrkesaktivitetDao = YrkesaktivitetDaoOverRepository(yrkesaktivitetsperiodeRepository)
    override val sykepengegrunnlagDao = SykepengegrunnlagDaoPg(dataSource)
    override val beregningDao = UtbetalingsberegningDaoPg(dataSource)
    override val outboxDao = OutboxDaoPg(dataSource)
}

class SessionDaoerFelles(
    session: Session,
) : AlleDaoer {
    override val behandlingDao = BehandlingDaoPg(session)
    override val behandlingEndringerDao = BehandlingEndringerDaoPg(session)
    override val personPseudoIdDao = PersonPseudoIdDaoPg(session)
    override val dokumentDao = DokumentDaoPg(session)
    override val sykepengegrunnlagDao = SykepengegrunnlagDaoPg(session)
    override val beregningDao = UtbetalingsberegningDaoPg(session)
    override val outboxDao = OutboxDaoPg(session)

    override val yrkesaktivitetsperiodeRepository = PgYrkesaktivitetsperiodeRepository(session)
    override val behandlingRepository = PgBehandlingRepository(session)
    override val tilkommenInntektRepository = PgTilkommenInntektRepository(session)
    override val vilkårsvurderingRepository = PgVilkårsvurderingRepository(session)
    override val sykefraværstilfelleRepository = PgSykefraværstilfelleRepository(session)
    override val yrkesaktivitetDao = YrkesaktivitetDaoOverRepository(yrkesaktivitetsperiodeRepository)
}
