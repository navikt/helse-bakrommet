package no.nav.helse.bakrommet.infrastruktur.db

import no.nav.helse.bakrommet.behandling.BehandlingServiceDaoer
import no.nav.helse.bakrommet.behandling.inntekter.InntektServiceDaoer
import no.nav.helse.bakrommet.behandling.inntekter.InntektsmeldingMatcherDaoer
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagServiceDaoer
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektServiceDaoer
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDaoer
import no.nav.helse.bakrommet.behandling.vilkaar.VilkårServiceDaoer
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetServiceDaoer
import no.nav.helse.bakrommet.person.PersonServiceDaoer
import no.nav.helse.bakrommet.person.PersonsokDaoer
import no.nav.helse.bakrommet.tidslinje.TidslinjeServiceDaoer

interface AlleDaoer :
    BehandlingServiceDaoer,
    YrkesaktivitetServiceDaoer,
    SykepengegrunnlagServiceDaoer,
    InntektsmeldingMatcherDaoer,
    InntektServiceDaoer,
    VilkårServiceDaoer,
    PersonsokDaoer,
    PersonServiceDaoer,
    UtbetalingsberegningDaoer,
    TilkommenInntektServiceDaoer,
    TidslinjeServiceDaoer,
    Repositories
