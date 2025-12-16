@file:Suppress("ktlint:standard:filename", "ktlint:standard:class-naming")

package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.vilkaar.Vilkaarsvurdering
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.person.NaturligIdent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class IkkeOppfylt8_2IkkeVurdert8_47Test {
    @Test
    fun `8-2 ikke oppfylt, 8-47 ikke vurdert, Det er ikke OK`() {
        val data =
            ValideringData(
                behandling = behandling,
                yrkesaktiviteter = emptyList(),
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
                                    underspørsmål = listOf(),
                                    notat = "",
                                ),
                        ),
                    ),
            )
        assertTrue(IkkeOppfylt8_2IkkeVurdert8_47.harInkonsistens(data))
    }

    @Test
    fun `8-2 ikke oppfylt, 8-47 ikke oppfylt, Det er helt OK`() {
        val data =
            ValideringData(
                behandling = behandling,
                yrkesaktiviteter = emptyList(),
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
                                    underspørsmål = listOf(),
                                    notat = "",
                                ),
                        ),
                        VurdertVilkår(
                            kode = "1",
                            vurdering =
                                Vilkaarsvurdering(
                                    vilkårskode = "SYK_INAKTIV",
                                    hovedspørsmål = "1",
                                    vurdering = Vurdering.IKKE_OPPFYLT,
                                    underspørsmål = listOf(),
                                    notat = "",
                                ),
                        ),
                    ),
            )
        assertFalse(IkkeOppfylt8_2IkkeVurdert8_47.harInkonsistens(data))
    }

    val behandling =
        Behandling(
            id = UUID.randomUUID(),
            naturligIdent = NaturligIdent("01010199999"),
            opprettet = OffsetDateTime.now(),
            opprettetAvNavIdent = "A001122",
            opprettetAvNavn = "A",
            fom = LocalDate.now().minusMonths(1),
            tom = LocalDate.now().minusDays(1),
            skjæringstidspunkt = LocalDate.now().minusMonths(1),
        )
}
