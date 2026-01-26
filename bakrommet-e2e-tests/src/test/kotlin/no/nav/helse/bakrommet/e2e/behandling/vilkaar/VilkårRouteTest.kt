package no.nav.helse.bakrommet.e2e.behandling.vilkaar

import forventCreated
import forventNoContent
import forventOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VurderingDto
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.deleteVilkårsvurdering
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.getVilkårsvurderinger
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettBehandlingOgForventOk
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.personsøk
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.putVilkårsvurdering
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class VilkårRouteTest {
    private val naturligIdent = enNaturligIdent()

    fun vilkårAppTest(testBlock: suspend ApplicationTestBuilder.(BehandlingDto) -> Unit) =
        runApplicationTest {
            val personPseudoId = personsøk(naturligIdent)
            val behandlingDto = opprettBehandlingOgForventOk(personPseudoId, fom = 1.januar(2023), tom = 31.januar(2023))
            this.testBlock(behandlingDto)
        }

    @Test
    fun `oppretter et vurdert vilkår på saksbehandlingsperiode`() =
        vilkårAppTest { behandling ->
            val hovedspørsmål = "BOR_I_NORGE"
            val personPseudoId = personsøk(naturligIdent)
            putVilkårsvurdering(personPseudoId, behandling.id, hovedspørsmål, VurderingDto.OPPFYLT, "derfor")
                .forventCreated()

            val vurderinger =
                getVilkårsvurderinger(personPseudoId, behandling.id)
                    .forventOk()

            assertEquals(1, vurderinger.size)
            assertEquals(VurderingDto.OPPFYLT, vurderinger[0].vurdering)
            assertEquals(hovedspørsmål, vurderinger[0].hovedspørsmål)
            assertEquals("derfor", vurderinger[0].notat)
        }

    @Test
    fun `oppretter, endrer, legger til, henter og sletter vurdert vilkår på saksbehandlingsperiode`() =
        vilkårAppTest { behandling ->
            val hovedspørsmål1 = "BOR_I_NORGE"
            val personPseudoId = personsøk(naturligIdent)
            putVilkårsvurdering(personPseudoId, behandling.id, hovedspørsmål1, VurderingDto.OPPFYLT, "derfor")
                .forventCreated()

            getVilkårsvurderinger(personPseudoId, behandling.id)
                .forventOk()
                .apply {
                    assertEquals(1, this.size)
                    assertEquals(VurderingDto.OPPFYLT, this[0].vurdering)
                    assertEquals(hovedspørsmål1, this[0].hovedspørsmål)
                    assertEquals("derfor", this[0].notat)
                }

            putVilkårsvurdering(
                personPseudoId,
                behandling.id,
                hovedspørsmål1,
                VurderingDto.IKKE_OPPFYLT,
                "Bor ikke i Norge",
            ).forventOk()

            getVilkårsvurderinger(personPseudoId, behandling.id)
                .forventOk()
                .apply {
                    assertEquals(1, this.size)
                    assertEquals(VurderingDto.IKKE_OPPFYLT, this[0].vurdering)
                    assertEquals(hovedspørsmål1, this[0].hovedspørsmål)
                    assertEquals("Bor ikke i Norge", this[0].notat)
                }

            val hovedspørsmål2 = "ET_VILKÅR_TIL"
            putVilkårsvurdering(personPseudoId, behandling.id, hovedspørsmål2, VurderingDto.IKKE_RELEVANT, null)
                .forventCreated()

            getVilkårsvurderinger(personPseudoId, behandling.id)
                .forventOk()
                .apply {
                    assertEquals(2, this.size)
                    assertEquals(VurderingDto.IKKE_RELEVANT, this[1].vurdering)
                    assertEquals(hovedspørsmål2, this[1].hovedspørsmål)
                    assertEquals(null, this[1].notat)
                }

            deleteVilkårsvurdering(personPseudoId, behandling.id, hovedspørsmål1)
                .forventNoContent()

            deleteVilkårsvurdering(personPseudoId, behandling.id, hovedspørsmål1)
                .forventNoContent() // gir fortsatt no content, selv når koden ikke finnes lenger

            getVilkårsvurderinger(personPseudoId, behandling.id)
                .forventOk()
                .apply {
                    assertEquals(1, this.size)
                    assertEquals(VurderingDto.IKKE_RELEVANT, this[0].vurdering)
                    assertEquals(hovedspørsmål2, this[0].hovedspørsmål)
                    assertEquals(null, this[0].notat)
                }
        }

    @Test
    fun `ugyldig kode-format gir 400 med beskrivelse`() =
        vilkårAppTest { behandling ->
            behandling.id
            val personPseudoId = personsøk(naturligIdent)
            val result =
                putVilkårsvurdering(
                    personPseudoId = personPseudoId,
                    behandlingId = behandling.id,
                    hovedspørsmål = "ugyldig-KODE",
                    vurdering = VurderingDto.OPPFYLT,
                    notat = null,
                )
            assertIs<ApiResult.Error>(result)
            assertEquals(HttpStatusCode.BadRequest.value, result.problemDetails.status)
            assertEquals("Ugyldig forespørsel", result.problemDetails.title)
            assertEquals("Ugyldig format på Kode", result.problemDetails.detail)
            assertEquals("https://spillerom.ansatt.nav.no/validation/request", result.problemDetails.type)
            assertEquals("/v1/$personPseudoId/behandlinger/${behandling.id}/vilkaarsvurdering/ugyldig-KODE", result.problemDetails.instance)
        }

    @Test
    fun `Feil person+periode-kombo gir 400 for både GET,PUT og DELETE`() =
        vilkårAppTest { behandling ->
            val dennePersonPseudoId = personsøk(naturligIdent)
            val denneBehandlingId = behandling.id

            val annenPersonPseudoId = personsøk(enNaturligIdent())

            val leggTilResult =
                putVilkårsvurdering(
                    personPseudoId = annenPersonPseudoId,
                    behandlingId = denneBehandlingId,
                    hovedspørsmål = "BOR_I_NORGE",
                    vurdering = VurderingDto.IKKE_OPPFYLT,
                    notat = "BOR_IKKE_I_NORGE",
                )

            assertIs<ApiResult.Error>(leggTilResult)
            assertEquals(HttpStatusCode.BadRequest.value, leggTilResult.problemDetails.status)

            putVilkårsvurdering(
                dennePersonPseudoId,
                denneBehandlingId,
                "BOR_I_NORGE",
                VurderingDto.IKKE_OPPFYLT,
                "BOR_IKKE_I_NORGE",
            ).forventCreated()

            val oppdaterResult =
                putVilkårsvurdering(
                    personPseudoId = annenPersonPseudoId,
                    behandlingId = denneBehandlingId,
                    hovedspørsmål = "BOR_I_NORGE",
                    vurdering = VurderingDto.IKKE_OPPFYLT,
                    notat = "BOR_IKKE_I_NORGE",
                )

            assertIs<ApiResult.Error>(oppdaterResult)
            assertEquals(HttpStatusCode.BadRequest.value, oppdaterResult.problemDetails.status)

            val getFeilPersonBehandlingKomboResult = getVilkårsvurderinger(annenPersonPseudoId, behandling.id)
            assertIs<ApiResult.Error>(getFeilPersonBehandlingKomboResult)
            assertEquals(HttpStatusCode.BadRequest.value, getFeilPersonBehandlingKomboResult.problemDetails.status)

            val deleteResult = deleteVilkårsvurdering(annenPersonPseudoId, denneBehandlingId, "BOR_I_NORGE")
            assertIs<ApiResult.Error>(deleteResult)
            assertEquals(HttpStatusCode.BadRequest.value, deleteResult.problemDetails.status)

            val vilkårsvurderinger =
                getVilkårsvurderinger(dennePersonPseudoId, behandling.id)
                    .forventOk()

            assertEquals(1, vilkårsvurderinger.size)
            assertEquals(VurderingDto.IKKE_OPPFYLT, vilkårsvurderinger[0].vurdering)
            assertEquals("BOR_I_NORGE", vilkårsvurderinger[0].hovedspørsmål)
            assertEquals("BOR_IKKE_I_NORGE", vilkårsvurderinger[0].notat)

            deleteVilkårsvurdering(dennePersonPseudoId, denneBehandlingId, "BOR_I_NORGE")
                .forventNoContent()
        }
}
