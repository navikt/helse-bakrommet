package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.behandling.BehandlingService
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentHenter
import no.nav.helse.bakrommet.behandling.inntekter.InntektService
import no.nav.helse.bakrommet.behandling.inntekter.InntektsmeldingMatcherService
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagService
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektService
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningService
import no.nav.helse.bakrommet.behandling.validering.ValideringService
import no.nav.helse.bakrommet.behandling.vilkaar.VilkårServiceOld
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetService
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.ArbeidsforholdProvider
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.infrastruktur.provider.InntektsmeldingProvider
import no.nav.helse.bakrommet.infrastruktur.provider.OrganisasjonsnavnProvider
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektProvider
import no.nav.helse.bakrommet.infrastruktur.provider.PersoninfoProvider
import no.nav.helse.bakrommet.infrastruktur.provider.SykepengesøknadProvider
import no.nav.helse.bakrommet.organisasjon.OrganisasjonService
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.bakrommet.person.PersonsøkService
import no.nav.helse.bakrommet.sykepengesoknad.SoknaderService
import no.nav.helse.bakrommet.tidslinje.TidslinjeService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// App-oppstarten må definere egen logger her, siden den (per nå) ikke skjer inne i en klasse
val appLogger: Logger = LoggerFactory.getLogger("bakrommet")

class Providers(
    val personinfoProvider: PersoninfoProvider,
    val sykepengesøknadProvider: SykepengesøknadProvider,
    val inntekterProvider: InntekterProvider,
    val arbeidsforholdProvider: ArbeidsforholdProvider,
    val organisasjonsnavnProvider: OrganisasjonsnavnProvider,
    val inntektsmeldingProvider: InntektsmeldingProvider,
    val pensjonsgivendeInntektProvider: PensjonsgivendeInntektProvider,
)

data class Services(
    val personsøkService: PersonsøkService,
    val sykepengegrunnlagService: SykepengegrunnlagService,
    val behandlingService: BehandlingService,
    val dokumentHenter: DokumentHenter,
    val vilkårServiceOld: VilkårServiceOld,
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
            soknadClient = providers.sykepengesøknadProvider,
            inntekterProvider = providers.inntekterProvider,
            arbeidsforholdProvider = providers.arbeidsforholdProvider,
            pensjonsgivendeInntektProvider = providers.pensjonsgivendeInntektProvider,
        )
    val personService = PersonService(db, providers.personinfoProvider)
    val yrkesaktivitetService = YrkesaktivitetService(db, providers.organisasjonsnavnProvider)
    return Services(
        personsøkService =
            PersonsøkService(
                db = db,
                personinfoProvider = providers.personinfoProvider,
            ),
        sykepengegrunnlagService = SykepengegrunnlagService(db),
        behandlingService =
            BehandlingService(
                db = db,
                dokumentHenter = dokumentHenter,
            ),
        dokumentHenter = dokumentHenter,
        yrkesaktivitetService = yrkesaktivitetService,
        vilkårServiceOld = VilkårServiceOld(db, yrkesaktivitetService),
        inntektService =
            InntektService(
                db,
                providers.inntektsmeldingProvider,
                providers.pensjonsgivendeInntektProvider,
                providers.inntekterProvider,
            ),
        inntektsmeldingMatcherService =
            InntektsmeldingMatcherService(
                db = db,
                providers.inntektsmeldingProvider,
            ),
        utbetalingsberegningService = UtbetalingsberegningService(db),
        personService = personService,
        organisasjonService = OrganisasjonService(providers.organisasjonsnavnProvider),
        tilkommenInntektService = TilkommenInntektService(db),
        tidslinjeService = TidslinjeService(db, providers.organisasjonsnavnProvider),
        soknaderService =
            SoknaderService(
                sykepengesøknadProvider = providers.sykepengesøknadProvider,
            ),
        valideringService = ValideringService(db),
    )
}
