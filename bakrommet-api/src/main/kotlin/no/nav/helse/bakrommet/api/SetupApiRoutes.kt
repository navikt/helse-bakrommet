package no.nav.helse.bakrommet.api

import io.ktor.server.routing.*
import no.nav.helse.bakrommet.Providers
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
    providers: Providers,
) {
    behandlingRoute(services.behandlingService, services.personService)
    brukerRoute()
    tidslinjeRoute(providers.organisasjonsnavnProvider, db)
    vilkårRoute(db)
    dokumentRoute(services.dokumentHenter, services.personService)
    tilkommenInntektRoute(db)
    organisasjonRoute(providers.organisasjonsnavnProvider)
    personsøkRoute(services.personsøkService)
    personinfoRoute(services.personService)
    soknaderRoute(services.soknaderService, services.personService)
    sykepengegrunnlagRoute(services.sykepengegrunnlagService, services.personService)
    beregningRoute(services.utbetalingsberegningService, services.personService)
    yrkesaktivitetRoute(
        db = db,
        inntektservice = services.inntektService,
        organisasjonsnavnProvider = providers.organisasjonsnavnProvider,
        pensjonsgivendeInntektProvider = providers.pensjonsgivendeInntektProvider,
        inntektProvider = providers.inntekterProvider,
        inntektsmeldingProvider = providers.inntektsmeldingProvider,
    )
    valideringRoute(services.valideringService, services.personService)
}
