package no.nav.helse.bakrommet.domain

import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Vilkårskode
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingUnderspørsmål
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektData
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Perioder
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntekt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntektId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntektYrkesaktivitetType
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitetsperiode
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetsperiodeId
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.random.Random

fun enNaturligIdent() = NaturligIdent(Random.nextLong(100000_00000L, 1000000_00000L).toString())

fun etOrganisasjonsnummer() = Random.nextLong(800_000_000, 1000_000_000).toString()

fun enNavIdent(): String {
    val letter = ('A'..'Z').random()
    val digits = Random.nextInt(1000000).toString().padStart(6, '0')
    return "$letter$digits"
}

fun enBehandlingId() = BehandlingId(UUID.randomUUID())

fun enBruker(
    navn: String = "Test Testesen",
    navIdent: String = enNavIdent(),
    preferredUsername: String = "test.testesen@nav.no",
    roller: Set<Rolle> = setOf(Rolle.SAKSBEHANDLER),
) = Bruker(
    navn = navn,
    navIdent = navIdent,
    preferredUsername = preferredUsername,
    roller = roller,
)

fun enBehandling(
    id: BehandlingId = enBehandlingId(),
    naturligIdent: NaturligIdent = enNaturligIdent(),
    opprettet: Instant = Instant.now(),
    opprettetAvNavIdent: String = "Z999999",
    opprettetAvNavn: String = "Test Testesen",
    fom: LocalDate = LocalDate.now().minusMonths(1),
    tom: LocalDate = LocalDate.now(),
    status: BehandlingStatus = BehandlingStatus.UNDER_BEHANDLING,
    beslutterNavIdent: String? = null,
    skjæringstidspunkt: LocalDate = LocalDate.now().minusMonths(1),
    individuellBegrunnelse: String? = null,
    sykepengegrunnlagId: UUID? = null,
    revurdererSaksbehandlingsperiodeId: BehandlingId? = null,
    revurdertAvBehandlingId: BehandlingId? = null,
) = Behandling.fraLagring(
    id = id,
    naturligIdent = naturligIdent,
    opprettet = opprettet,
    opprettetAvNavIdent = opprettetAvNavIdent,
    opprettetAvNavn = opprettetAvNavn,
    fom = fom,
    tom = tom,
    status = status,
    beslutterNavIdent = beslutterNavIdent,
    skjæringstidspunkt = skjæringstidspunkt,
    individuellBegrunnelse = individuellBegrunnelse,
    sykepengegrunnlagId = sykepengegrunnlagId,
    revurdererBehandlingId = revurdererSaksbehandlingsperiodeId,
    revurdertAvBehandlingId = revurdertAvBehandlingId,
)

fun etVurdertVilkår(
    behandlingId: BehandlingId,
    vilkårskode: Vilkårskode = Vilkårskode(UUID.randomUUID().toString()),
    underspørsmål: List<VilkårsvurderingUnderspørsmål> = emptyList(),
    utfall: VurdertVilkår.Utfall = VurdertVilkår.Utfall.OPPFYLT,
    notat: String? = null,
) = VurdertVilkår(
    id =
        VilkårsvurderingId(
            behandlingId = behandlingId,
            vilkårskode = vilkårskode,
        ),
    vurdering =
        VurdertVilkår.Vurdering(
            underspørsmål = underspørsmål,
            utfall = utfall,
            notat = notat,
        ),
)

fun enYrkesaktivitet(
    id: YrkesaktivitetsperiodeId = YrkesaktivitetsperiodeId(UUID.randomUUID()),
    kategorisering: YrkesaktivitetKategorisering = YrkesaktivitetKategorisering.Inaktiv(),
    kategoriseringGenerert: YrkesaktivitetKategorisering? = null,
    dagoversikt: Dagoversikt? = null,
    dagoversiktGenerert: Dagoversikt? = null,
    behandlingId: BehandlingId,
    opprettet: OffsetDateTime = OffsetDateTime.now(),
    generertFraDokumenter: List<UUID> = emptyList(),
    perioder: Perioder? = null,
    inntektRequest: InntektRequest? = null,
    inntektData: InntektData? = null,
    refusjon: List<Refusjonsperiode>? = null,
) = Yrkesaktivitetsperiode(
    id = id,
    kategorisering = kategorisering,
    kategoriseringGenerert = kategoriseringGenerert,
    dagoversikt = dagoversikt,
    dagoversiktGenerert = dagoversiktGenerert,
    behandlingId = behandlingId,
    opprettet = opprettet,
    generertFraDokumenter = generertFraDokumenter,
    perioder = perioder,
    inntektRequest = inntektRequest,
    inntektData = inntektData,
    refusjon = refusjon,
)

fun enTilkommenInntekt(
    id: TilkommenInntektId = TilkommenInntektId(UUID.randomUUID()),
    behandlingId: BehandlingId = enBehandlingId(),
    ident: String = etOrganisasjonsnummer(),
    yrkesaktivitetType: TilkommenInntektYrkesaktivitetType = TilkommenInntektYrkesaktivitetType.VIRKSOMHET,
    fom: LocalDate = LocalDate.now().minusMonths(1),
    tom: LocalDate = LocalDate.now(),
    inntektForPerioden: BigDecimal = BigDecimal.valueOf(50000),
    notatTilBeslutter: String = "Test notat",
    ekskluderteDager: List<LocalDate> = emptyList(),
    opprettet: OffsetDateTime = OffsetDateTime.now(),
    opprettetAvNavIdent: String = "Z999999",
) = TilkommenInntekt.fraLagring(
    id = id,
    behandlingId = behandlingId,
    ident = ident,
    yrkesaktivitetType = yrkesaktivitetType,
    fom = fom,
    tom = tom,
    inntektForPerioden = inntektForPerioden,
    notatTilBeslutter = notatTilBeslutter,
    ekskluderteDager = ekskluderteDager,
    opprettet = opprettet,
    opprettetAvNavIdent = opprettetAvNavIdent,
)
