package no.nav.helse.bakrommet.domain

import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Vilkårskode
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingUnderspørsmål
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

fun enNaturligIdent() = NaturligIdent(Random.nextLong(10000000000L, 100000000000L).toString())

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
    revurdererSaksbehandlingsperiodeId = revurdererSaksbehandlingsperiodeId,
    revurdertAvBehandlingId = revurdertAvBehandlingId,
)

fun etVurdertVilkår(
    behandlingId: BehandlingId,
    vilkårskode: Vilkårskode = Vilkårskode(UUID.randomUUID().toString()),
    hovedspørsmål: String = "Er vilkåret oppfylt?",
    underspørsmål: List<VilkårsvurderingUnderspørsmål> = emptyList(),
    vurdering: VurdertVilkår.Vurdering = VurdertVilkår.Vurdering.OPPFYLT,
    notat: String? = null,
) = VurdertVilkår(
    id =
        VilkårsvurderingId(
            behandlingId = behandlingId,
            vilkårskode = vilkårskode,
        ),
    hovedspørsmål = hovedspørsmål,
    underspørsmål = underspørsmål,
    vurdering = vurdering,
    notat = notat,
)
