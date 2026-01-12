package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.behandling.BehandlingService
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentHenter
import no.nav.helse.bakrommet.behandling.inntekter.InntektService
import no.nav.helse.bakrommet.behandling.inntekter.InntektsmeldingMatcherService
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagService
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektService
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningService
import no.nav.helse.bakrommet.behandling.validering.ValideringService
import no.nav.helse.bakrommet.behandling.vilkaar.VilkårService
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetService
import no.nav.helse.bakrommet.ereg.EregClient
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.ArbeidsforholdProvider
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.organisasjon.OrganisasjonService
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.bakrommet.person.PersonsøkService
import no.nav.helse.bakrommet.sigrun.SigrunClient
import no.nav.helse.bakrommet.sykepengesoknad.SoknaderService
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.tidslinje.TidslinjeService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// App-oppstarten må definere egen logger her, siden den (per nå) ikke skjer inne i en klasse
val appLogger: Logger = LoggerFactory.getLogger("bakrommet")

class Providers(
    val pdlClient: PdlClient,
    val sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    val inntekterProvider: InntekterProvider,
    val arbeidsforholdProvider: ArbeidsforholdProvider,
    val eregClient: EregClient,
    val inntektsmeldingClient: InntektsmeldingClient,
    val sigrunClient: SigrunClient,
)

data class Services(
    val personsøkService: PersonsøkService,
    val sykepengegrunnlagService: SykepengegrunnlagService,
    val behandlingService: BehandlingService,
    val dokumentHenter: DokumentHenter,
    val vilkårService: VilkårService,
    val yrkesaktivitetService: YrkesaktivitetService,
    val inntektService: InntektService,
    val inntektsmeldingMatcherService: InntektsmeldingMatcherService,
    val utbetalingsberegningService: UtbetalingsberegningService,
    val personService: PersonService,
    val organisasjonService: OrganisasjonService,
    val tilkommenInntektService: TilkommenInntektService,
    val tidslinjeService: TidslinjeService,
    val soknaderService: SoknaderService,
    val valideringService: ValideringService,
)

fun createServices(
    providers: Providers,
    db: DbDaoer<AlleDaoer>,
): Services {
    val dokumentHenter =
        DokumentHenter(
            db = db,
            soknadClient = providers.sykepengesoknadBackendClient,
            inntekterProvider = providers.inntekterProvider,
            arbeidsforholdProvider = providers.arbeidsforholdProvider,
            sigrunClient = providers.sigrunClient,
        )
    val personService = PersonService(db, providers.pdlClient)
    val yrkesaktivitetService = YrkesaktivitetService(db, providers.eregClient)
    return Services(
        personsøkService =
            PersonsøkService(
                db = db,
                pdlClient = providers.pdlClient,
            ),
        sykepengegrunnlagService = SykepengegrunnlagService(db),
        behandlingService =
            BehandlingService(
                db = db,
                dokumentHenter = dokumentHenter,
            ),
        dokumentHenter = dokumentHenter,
        yrkesaktivitetService = yrkesaktivitetService,
        vilkårService = VilkårService(db, yrkesaktivitetService),
        inntektService =
            InntektService(
                db,
                providers.inntektsmeldingClient,
                providers.sigrunClient,
                providers.inntekterProvider,
            ),
        inntektsmeldingMatcherService =
            InntektsmeldingMatcherService(
                db = db,
                providers.inntektsmeldingClient,
            ),
        utbetalingsberegningService = UtbetalingsberegningService(db),
        personService = personService,
        organisasjonService = OrganisasjonService(providers.eregClient),
        tilkommenInntektService = TilkommenInntektService(db),
        tidslinjeService = TidslinjeService(db, providers.eregClient),
        soknaderService =
            SoknaderService(
                sykepengesoknadBackendClient = providers.sykepengesoknadBackendClient,
                personService = personService,
            ),
        valideringService = ValideringService(db),
    )
}
