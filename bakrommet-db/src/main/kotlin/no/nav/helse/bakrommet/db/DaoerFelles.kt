package no.nav.helse.bakrommet.db

import kotliquery.Session
import no.nav.helse.bakrommet.db.dao.BehandlingDaoPg
import no.nav.helse.bakrommet.db.dao.BehandlingEndringerDaoPg
import no.nav.helse.bakrommet.db.dao.DokumentDaoPg
import no.nav.helse.bakrommet.db.dao.OutboxDaoPg
import no.nav.helse.bakrommet.db.dao.PersonPseudoIdDaoPg
import no.nav.helse.bakrommet.db.dao.SykepengegrunnlagDaoPg
import no.nav.helse.bakrommet.db.dao.TilkommenInntektDaoPg
import no.nav.helse.bakrommet.db.dao.UtbetalingsberegningDaoPg
import no.nav.helse.bakrommet.db.dao.`VurdertVilkårDaoPg`
import no.nav.helse.bakrommet.db.dao.YrkesaktivitetDaoPg
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import javax.sql.DataSource

class DaoerFelles(
    dataSource: DataSource,
) : AlleDaoer {
    override val behandlingDao = BehandlingDaoPg(dataSource)
    override val behandlingEndringerDao = BehandlingEndringerDaoPg(dataSource)
    override val personPseudoIdDao = PersonPseudoIdDaoPg(dataSource)
    override val dokumentDao = DokumentDaoPg(dataSource)
    override val yrkesaktivitetDao = YrkesaktivitetDaoPg(dataSource)
    override val vurdertVilkårDao = `VurdertVilkårDaoPg`(dataSource)
    override val sykepengegrunnlagDao = SykepengegrunnlagDaoPg(dataSource)
    override val beregningDao = UtbetalingsberegningDaoPg(dataSource)
    override val outboxDao = OutboxDaoPg(dataSource)
    override val tilkommenInntektDao = TilkommenInntektDaoPg(dataSource)
}

class SessionDaoerFelles(
    session: Session,
) : AlleDaoer {
    override val behandlingDao = BehandlingDaoPg(session)
    override val behandlingEndringerDao = BehandlingEndringerDaoPg(session)
    override val personPseudoIdDao = PersonPseudoIdDaoPg(session)
    override val dokumentDao = DokumentDaoPg(session)
    override val yrkesaktivitetDao = YrkesaktivitetDaoPg(session)
    override val vurdertVilkårDao = `VurdertVilkårDaoPg`(session)
    override val sykepengegrunnlagDao = SykepengegrunnlagDaoPg(session)
    override val beregningDao = UtbetalingsberegningDaoPg(session)
    override val outboxDao = OutboxDaoPg(session)
    override val tilkommenInntektDao = TilkommenInntektDaoPg(session)
}
