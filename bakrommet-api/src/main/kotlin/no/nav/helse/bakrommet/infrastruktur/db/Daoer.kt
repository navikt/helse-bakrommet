package no.nav.helse.bakrommet.infrastruktur.db

import kotliquery.Session
import no.nav.helse.bakrommet.kafka.OutboxDaoPg
import no.nav.helse.bakrommet.person.PersonDaoPg
import no.nav.helse.bakrommet.person.PersonIdServiceDaoer
import no.nav.helse.bakrommet.person.PersonsokDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDaoPg
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeEndringerDaoPg
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeServiceDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDaoPg
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektServiceDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektsmeldingMatcherDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDaoPg
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagServiceDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDaoPg
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VilkårServiceDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VurdertVilkårDaoPg
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDaoPg
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetServiceDaoer
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
    override val saksbehandlingsperiodeDao = SaksbehandlingsperiodeDaoPg(dataSource)
    override val saksbehandlingsperiodeEndringerDao = SaksbehandlingsperiodeEndringerDaoPg(dataSource)
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
    override val saksbehandlingsperiodeDao = SaksbehandlingsperiodeDaoPg(session)
    override val saksbehandlingsperiodeEndringerDao = SaksbehandlingsperiodeEndringerDaoPg(session)
    override val personDao = PersonDaoPg(session)
    override val dokumentDao = DokumentDaoPg(session)
    override val yrkesaktivitetDao = YrkesaktivitetDaoPg(session)
    override val vurdertVilkårDao = VurdertVilkårDaoPg(session)
    override val sykepengegrunnlagDao = SykepengegrunnlagDaoPg(session)
    override val beregningDao = UtbetalingsberegningDaoPg(session)
    override val outboxDao = OutboxDaoPg(session)
}
