package no.nav.helse.bakrommet.db

import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import javax.sql.DataSource

fun skapDbDaoer(dataSource: DataSource): DbDaoer<AlleDaoer> =
    DbDaoerImpl(
        DaoerFelles(dataSource),
        TransactionalSessionFactoryPg(dataSource) { session ->
            SessionDaoerFelles(session)
        },
    )
