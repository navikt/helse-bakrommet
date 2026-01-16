package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.dagoversikt.Dag
import no.nav.helse.bakrommet.behandling.dagoversikt.Dagtype
import no.nav.helse.bakrommet.behandling.enkelBehandlingDbRecord
import no.nav.helse.bakrommet.behandling.enkelLegacyYrkesaktivitet
import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Dagoversikt
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Vilkårskode
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingUnderspørsmål
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class AvslåttBegrunnelseUtenVilkårsvurderingTest {
    @Test
    fun `Har en avslått dag som er vurdert, Det er ok`() {
        val data =
            ValideringData(
                beregningData = null,
                behandlingDbRecord = enkelBehandlingDbRecord,
                yrkesaktiviteter =
                    listOf(
                        enkelLegacyYrkesaktivitet.copy(
                            dagoversikt =
                                Dagoversikt(
                                    listOf(
                                        Dag(
                                            dato = LocalDate.now(),
                                            dagtype = Dagtype.Syk,
                                            grad = null,
                                            kilde = null,
                                            avslåttBegrunnelse = null,
                                        ),
                                    ),
                                    listOf(
                                        Dag(
                                            dato = LocalDate.now(),
                                            dagtype = Dagtype.Avslått,
                                            grad = null,
                                            kilde = null,
                                            avslåttBegrunnelse = listOf("AAP_FØR_FORELDREPENGER"),
                                        ),
                                    ),
                                ),
                        ),
                    ),
                sykepengegrunnlag = null,
                vurderteVilkår =
                    listOf(
                        VurdertVilkår(
                            id =
                                VilkårsvurderingId(
                                    behandlingId = BehandlingId(UUID.randomUUID()),
                                    vilkårskode = Vilkårskode("OPPTJENING"),
                                ),
                            vurdering =
                                VurdertVilkår.Vurdering(
                                    utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
                                    underspørsmål =
                                        listOf(
                                            VilkårsvurderingUnderspørsmål(
                                                spørsmål = UUID.randomUUID().toString(),
                                                svar = "AAP_FØR_FORELDREPENGER",
                                            ),
                                        ),
                                    notat = "",
                                ),
                        ),
                    ),
            )
        assertFalse(AvslåttBegrunnelseUtenVilkårsvurdering.harInkonsistens(data))
    }

    @Test
    fun `Har en avslått dag som ikke er vurdert, Det er ikke ok`() {
        val data =
            ValideringData(
                beregningData = null,
                behandlingDbRecord = enkelBehandlingDbRecord,
                yrkesaktiviteter =
                    listOf(
                        enkelLegacyYrkesaktivitet.copy(
                            dagoversikt =
                                Dagoversikt(
                                    listOf(
                                        Dag(
                                            dato = LocalDate.now(),
                                            dagtype = Dagtype.Syk,
                                            grad = null,
                                            kilde = null,
                                            avslåttBegrunnelse = null,
                                        ),
                                    ),
                                    listOf(
                                        Dag(
                                            dato = LocalDate.now(),
                                            dagtype = Dagtype.Avslått,
                                            grad = null,
                                            kilde = null,
                                            avslåttBegrunnelse = listOf("AAP_FØR_FORELDREPENGER"),
                                        ),
                                    ),
                                ),
                        ),
                    ),
                sykepengegrunnlag = null,
                vurderteVilkår = emptyList(),
            )
        assertTrue(AvslåttBegrunnelseUtenVilkårsvurdering.harInkonsistens(data))
    }
}
