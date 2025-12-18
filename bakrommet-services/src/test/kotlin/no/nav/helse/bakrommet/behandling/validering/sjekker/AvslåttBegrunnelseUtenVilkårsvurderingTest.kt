package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.dagoversikt.Dag
import no.nav.helse.bakrommet.behandling.dagoversikt.Dagtype
import no.nav.helse.bakrommet.behandling.enkelBehandling
import no.nav.helse.bakrommet.behandling.enkelYrkesaktivitet
import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.vilkaar.Vilkaarsvurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VilkaarsvurderingUnderspørsmål
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Dagoversikt
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
                behandling = enkelBehandling,
                yrkesaktiviteter =
                    listOf(
                        enkelYrkesaktivitet.copy(
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
                            kode = "1",
                            vurdering =
                                Vilkaarsvurdering(
                                    vilkårskode = "OPPTJENING",
                                    hovedspørsmål = "1",
                                    vurdering = Vurdering.IKKE_OPPFYLT,
                                    underspørsmål = listOf(VilkaarsvurderingUnderspørsmål(UUID.randomUUID().toString(), "AAP_FØR_FORELDREPENGER")),
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
                behandling = enkelBehandling,
                yrkesaktiviteter =
                    listOf(
                        enkelYrkesaktivitet.copy(
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
