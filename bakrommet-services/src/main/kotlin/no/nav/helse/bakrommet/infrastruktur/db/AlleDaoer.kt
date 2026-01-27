package no.nav.helse.bakrommet.infrastruktur.db

import no.nav.helse.bakrommet.behandling.BehandlingServiceDaoer
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagServiceDaoer
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDaoer
import no.nav.helse.bakrommet.person.PersonServiceDaoer
import no.nav.helse.bakrommet.person.PersonsokDaoer

interface AlleDaoer :
    BehandlingServiceDaoer,
    SykepengegrunnlagServiceDaoer,
    PersonsokDaoer,
    PersonServiceDaoer,
    UtbetalingsberegningDaoer,
    Repositories
