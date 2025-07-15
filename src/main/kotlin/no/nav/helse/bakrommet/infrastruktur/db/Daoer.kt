package no.nav.helse.bakrommet.infrastruktur.db

import kotliquery.Session
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeEndringerDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeServiceDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VilkårRouteSessionDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VurdertVilkårDao
import javax.sql.DataSource

class DaoerFelles(dataSource: DataSource) :
    SaksbehandlingsperiodeServiceDaoer,
    VilkårRouteSessionDaoer {
    override val saksbehandlingsperiodeDao = SaksbehandlingsperiodeDao(dataSource)
    override val saksbehandlingsperiodeEndringerDao = SaksbehandlingsperiodeEndringerDao(dataSource)
    override val personDao = PersonDao(dataSource)
    override val dokumentDao = DokumentDao(dataSource)
    override val inntektsforholdDao = InntektsforholdDao(dataSource)
    override val vurdertVilkårDao = VurdertVilkårDao(dataSource)
}

class SessionDaoerFelles(session: Session) :
    SaksbehandlingsperiodeServiceDaoer,
    VilkårRouteSessionDaoer {
    override val saksbehandlingsperiodeDao = SaksbehandlingsperiodeDao(session)
    override val saksbehandlingsperiodeEndringerDao = SaksbehandlingsperiodeEndringerDao(session)
    override val personDao = PersonDao(session)
    override val dokumentDao = DokumentDao(session)
    override val inntektsforholdDao = InntektsforholdDao(session)
    override val vurdertVilkårDao = VurdertVilkårDao(session)
}
