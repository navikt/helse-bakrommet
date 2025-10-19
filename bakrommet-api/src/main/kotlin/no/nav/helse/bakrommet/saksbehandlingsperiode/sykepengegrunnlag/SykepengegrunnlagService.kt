package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.*
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao

interface SykepengegrunnlagServiceDaoer {
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val personDao: PersonDao
}

class SykepengegrunnlagService(
    daoer: SykepengegrunnlagServiceDaoer,
    sessionFactory: TransactionalSessionFactory<SykepengegrunnlagServiceDaoer>,
) {
    private val db = DbDaoer(daoer, sessionFactory)

    fun hentSykepengegrunnlag(referanse: SaksbehandlingsperiodeReferanse): Sykepengegrunnlag? =
        db.nonTransactional {
            saksbehandlingsperiodeDao.hentPeriode(referanse, krav = null).sykepengegrunnlagId?.let {
                sykepengegrunnlagDao.hentSykepengegrunnlag(it)?.sykepengegrunnlag
            }
        }
}
