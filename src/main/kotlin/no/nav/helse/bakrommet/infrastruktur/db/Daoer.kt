package no.nav.helse.bakrommet.infrastruktur.db

import kotliquery.Session
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeEndringerDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeServiceDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagServiceDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VilkårServiceDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VurdertVilkårDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.InntektsforholdServiceDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import javax.sql.DataSource

class DaoerFelles(dataSource: DataSource) :
    SaksbehandlingsperiodeServiceDaoer,
    InntektsforholdServiceDaoer,
    SykepengegrunnlagServiceDaoer,
    VilkårServiceDaoer {
    override val saksbehandlingsperiodeDao = SaksbehandlingsperiodeDao(dataSource)
    override val saksbehandlingsperiodeEndringerDao = SaksbehandlingsperiodeEndringerDao(dataSource)
    override val personDao = PersonDao(dataSource)
    override val dokumentDao = DokumentDao(dataSource)
    override val yrkesaktivitetDao = YrkesaktivitetDao(dataSource)
    override val vurdertVilkårDao = VurdertVilkårDao(dataSource)
    override val sykepengegrunnlagDao = SykepengegrunnlagDao(dataSource)
    override val beregningDao = UtbetalingsberegningDao(dataSource)
}

class SessionDaoerFelles(session: Session) :
    SaksbehandlingsperiodeServiceDaoer,
    InntektsforholdServiceDaoer,
    SykepengegrunnlagServiceDaoer,
    VilkårServiceDaoer {
    override val saksbehandlingsperiodeDao = SaksbehandlingsperiodeDao(session)
    override val saksbehandlingsperiodeEndringerDao = SaksbehandlingsperiodeEndringerDao(session)
    override val personDao = PersonDao(session)
    override val dokumentDao = DokumentDao(session)
    override val yrkesaktivitetDao = YrkesaktivitetDao(session)
    override val vurdertVilkårDao = VurdertVilkårDao(session)
    override val sykepengegrunnlagDao = SykepengegrunnlagDao(session)
    override val beregningDao = UtbetalingsberegningDao(session)
}
