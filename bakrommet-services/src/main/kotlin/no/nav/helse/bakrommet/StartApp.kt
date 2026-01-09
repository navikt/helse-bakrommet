package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.aareg.AARegClient
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.OboClient
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
import no.nav.helse.bakrommet.bruker.BrukerService
import no.nav.helse.bakrommet.ereg.EregClient
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import no.nav.helse.bakrommet.infrastruktur.db.DaoerFelles
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoerImpl
import no.nav.helse.bakrommet.infrastruktur.db.SessionDaoerFelles
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactoryPg
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
import javax.sql.DataSource

// App-oppstarten må definere egen logger her, siden den (per nå) ikke skjer inne i en klasse
val appLogger: Logger = LoggerFactory.getLogger("bakrommet")

fun instansierDatabase(configuration: Configuration.DB) = DBModule(configuration = configuration).also { it.migrate() }.dataSource

fun skapDbDaoer(dataSource: DataSource) =
    DbDaoerImpl(
        DaoerFelles(dataSource),
        TransactionalSessionFactoryPg(dataSource) { session ->
            SessionDaoerFelles(session)
        },
    )

class Clienter(
    val pdlClient: PdlClient,
    val sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    val aInntektClient: AInntektClient,
    val aaRegClient: AARegClient,
    val eregClient: EregClient,
    val inntektsmeldingClient: InntektsmeldingClient,
    val sigrunClient: SigrunClient,
)

fun createClients(configuration: Configuration): Clienter {
    val oboClient = OboClient(configuration.obo)
    val pdlClient = PdlClient(configuration.pdl, oboClient)
    val sykepengesoknadBackendClient =
        SykepengesoknadBackendClient(
            configuration.sykepengesoknadBackend,
            oboClient,
        )

    val aaRegClient = AARegClient(configuration.aareg, oboClient)
    val aInntektClient = AInntektClient(configuration.ainntekt, oboClient)
    val eregClient = EregClient(configuration.ereg)
    val inntektsmeldingClient = InntektsmeldingClient(configuration.inntektsmelding, oboClient)
    val sigrunClient = SigrunClient(configuration.sigrun, oboClient)

    return Clienter(
        pdlClient = pdlClient,
        sykepengesoknadBackendClient = sykepengesoknadBackendClient,
        aInntektClient = aInntektClient,
        aaRegClient = aaRegClient,
        eregClient = eregClient,
        inntektsmeldingClient = inntektsmeldingClient,
        sigrunClient = sigrunClient,
    )
}

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
    val brukerService: BrukerService,
    val soknaderService: SoknaderService,
    val valideringService: ValideringService,
)

fun createServices(
    clienter: Clienter,
    db: DbDaoer<AlleDaoer>,
): Services {
    val dokumentHenter =
        DokumentHenter(
            db = db,
            soknadClient = clienter.sykepengesoknadBackendClient,
            aInntektClient = clienter.aInntektClient,
            aaRegClient = clienter.aaRegClient,
            sigrunClient = clienter.sigrunClient,
        )
    val personService = PersonService(db, clienter.pdlClient)
    val yrkesaktivitetService = YrkesaktivitetService(db, clienter.eregClient)
    return Services(
        personsøkService =
            PersonsøkService(
                db = db,
                pdlClient = clienter.pdlClient,
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
                clienter.inntektsmeldingClient,
                clienter.sigrunClient,
                clienter.aInntektClient,
            ),
        inntektsmeldingMatcherService =
            InntektsmeldingMatcherService(
                db = db,
                clienter.inntektsmeldingClient,
            ),
        utbetalingsberegningService = UtbetalingsberegningService(db),
        personService = personService,
        organisasjonService = OrganisasjonService(clienter.eregClient),
        tilkommenInntektService = TilkommenInntektService(db),
        tidslinjeService = TidslinjeService(db, clienter.eregClient),
        brukerService = BrukerService(),
        soknaderService =
            SoknaderService(
                sykepengesoknadBackendClient = clienter.sykepengesoknadBackendClient,
                personService = personService,
            ),
        valideringService = ValideringService(db),
    )
}
