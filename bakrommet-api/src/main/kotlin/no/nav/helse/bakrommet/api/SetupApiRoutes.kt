package no.nav.helse.bakrommet.api

import io.ktor.server.routing.Route
import no.nav.helse.bakrommet.Services
import no.nav.helse.bakrommet.api.behandling.behandlingRoute
import no.nav.helse.bakrommet.api.bruker.brukerRoute
import no.nav.helse.bakrommet.api.dokumenter.dokumentRoute
import no.nav.helse.bakrommet.api.organisasjon.organisasjonRoute
import no.nav.helse.bakrommet.api.person.personinfoRoute
import no.nav.helse.bakrommet.api.person.personsøkRoute
import no.nav.helse.bakrommet.api.soknader.soknaderRoute
import no.nav.helse.bakrommet.api.sykepengegrunnlag.sykepengegrunnlagRoute
import no.nav.helse.bakrommet.api.tidslinje.tidslinjeRoute
import no.nav.helse.bakrommet.api.tilkommen.tilkommenInntektRoute
import no.nav.helse.bakrommet.api.utbetalingsberegning.beregningRoute
import no.nav.helse.bakrommet.api.validering.valideringRoute
import no.nav.helse.bakrommet.api.vilkaar.vilkårRoute
import no.nav.helse.bakrommet.api.yrkesaktivitet.yrkesaktivitetRoute
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer

fun Route.setupApiRoutes(
    services: Services,
    db: DbDaoer<AlleDaoer>,
) {
    behandlingRoute(services.behandlingService, services.personService)
    brukerRoute()
    tidslinjeRoute(services.tidslinjeService, services.personService)
    vilkårRoute(services.vilkårServiceOld, services.personService, db)
    dokumentRoute(services.dokumentHenter, services.personService)
    tilkommenInntektRoute(services.tilkommenInntektService, services.personService)
    organisasjonRoute(services.organisasjonService)
    personsøkRoute(services.personsøkService)
    personinfoRoute(services.personService)
    soknaderRoute(services.soknaderService, services.personService)
    sykepengegrunnlagRoute(services.sykepengegrunnlagService, services.personService)
    beregningRoute(services.utbetalingsberegningService, services.personService)
    yrkesaktivitetRoute(
        yrkesaktivitetService = services.yrkesaktivitetService,
        inntektservice = services.inntektService,
        inntektsmeldingMatcherService = services.inntektsmeldingMatcherService,
        personService = services.personService,
    )
    valideringRoute(services.valideringService, services.personService)
}
