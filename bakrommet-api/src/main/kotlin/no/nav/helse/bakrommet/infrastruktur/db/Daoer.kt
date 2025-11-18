package no.nav.helse.bakrommet.infrastruktur.db

import kotliquery.Session
import no.nav.helse.bakrommet.behandling.BehandlingDaoPg
import no.nav.helse.bakrommet.behandling.BehandlingEndringerDaoPg
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeServiceDaoer
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDaoPg
import no.nav.helse.bakrommet.behandling.inntekter.InntektServiceDaoer
import no.nav.helse.bakrommet.behandling.inntekter.InntektsmeldingMatcherDaoer
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDaoPg
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagServiceDaoer
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDaoPg
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDaoer
import no.nav.helse.bakrommet.behandling.vilkaar.VilkårServiceDaoer
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkårDaoPg
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDaoPg
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetServiceDaoer
import no.nav.helse.bakrommet.kafka.OutboxDaoPg
import no.nav.helse.bakrommet.person.PersonDaoPg
import no.nav.helse.bakrommet.person.PersonIdServiceDaoer
import no.nav.helse.bakrommet.person.PersonsokDaoer
import javax.sql.DataSource

interface AlleDaoer :
    SaksbehandlingsperiodeServiceDaoer,
    YrkesaktivitetServiceDaoer,
    SykepengegrunnlagServiceDaoer,
    InntektsmeldingMatcherDaoer,
    InntektServiceDaoer,
    VilkårServiceDaoer,
    PersonsokDaoer,
    PersonIdServiceDaoer,
    UtbetalingsberegningDaoer

class DaoerFelles(
    dataSource: DataSource,
) : AlleDaoer {
    override val behandlingDao = BehandlingDaoPg(dataSource)
    override val behandlingEndringerDao = BehandlingEndringerDaoPg(dataSource)
    override val personDao = PersonDaoPg(dataSource)
    override val dokumentDao = DokumentDaoPg(dataSource)
    override val yrkesaktivitetDao = YrkesaktivitetDaoPg(dataSource)
    override val vurdertVilkårDao = VurdertVilkårDaoPg(dataSource)
    override val sykepengegrunnlagDao = SykepengegrunnlagDaoPg(dataSource)
    override val beregningDao = UtbetalingsberegningDaoPg(dataSource)
    override val outboxDao = OutboxDaoPg(dataSource)
}

class SessionDaoerFelles(
    session: Session,
) : AlleDaoer {
    override val behandlingDao = BehandlingDaoPg(session)
    override val behandlingEndringerDao = BehandlingEndringerDaoPg(session)
    override val personDao = PersonDaoPg(session)
    override val dokumentDao = DokumentDaoPg(session)
    override val yrkesaktivitetDao = YrkesaktivitetDaoPg(session)
    override val vurdertVilkårDao = VurdertVilkårDaoPg(session)
    override val sykepengegrunnlagDao = SykepengegrunnlagDaoPg(session)
    override val beregningDao = UtbetalingsberegningDaoPg(session)
    override val outboxDao = OutboxDaoPg(session)
}
