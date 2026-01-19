package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.enkelBehandlingDbRecord
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDbRecord
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.BeregningData
import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.validering.ValideringSjekk
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.LegacyYrkesaktivitet
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingUnderspørsmål
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TypeArbeidstaker
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.kafka.dto.oppdrag.OppdragDto
import no.nav.helse.bakrommet.kafka.dto.oppdrag.SpilleromOppdragDto
import no.nav.helse.bakrommet.kodeverk.Vilkårskode
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Vilkårskode as DomainVilkårskode

internal infix fun ValideringSjekk.`skal ha inkonsistens med`(data: ValideringData) {
    val inkonsistens = harInkonsistens(data)
    assertTrue(inkonsistens, "Forventet at sjekken $id skulle være inkonsistent, men var konsistent")
}

internal infix fun ValideringSjekk.`skal ha konsistens med`(data: ValideringData) {
    val inkonsistens = harInkonsistens(data)
    assertFalse(inkonsistens, "Forventet at sjekken $id skulle være konsistent, men var inkonsistent")
}

fun vurdertVilkårMedBegrunnelse(b: VilkårskodeBegrunnelse): List<VurdertVilkår> =
    listOf(
        VurdertVilkår(
            id =
                VilkårsvurderingId(
                    behandlingId = BehandlingId(UUID.randomUUID()),
                    vilkårskode = DomainVilkårskode("WHATEVER"),
                ),
            vurdering =
                VurdertVilkår.Vurdering(
                    utfall = VurdertVilkår.Utfall.OPPFYLT,
                    underspørsmål =
                        listOf(
                            VilkårsvurderingUnderspørsmål(
                                spørsmål = "SPM_1",
                                svar = b.name,
                            ),
                        ),
                    notat = "",
                ),
        ),
    )

fun vilkårVurdertSom(
    vilkar: Vilkårskode,
    utfall: VurdertVilkår.Utfall,
): VurdertVilkår =
    VurdertVilkår(
        id =
            VilkårsvurderingId(
                behandlingId = BehandlingId(UUID.randomUUID()),
                vilkårskode = DomainVilkårskode(vilkar.name),
            ),
        vurdering =
            VurdertVilkår.Vurdering(
                utfall = utfall,
                underspørsmål = emptyList(),
                notat = "",
            ),
    )

fun data(
    sykepengegrunnlag: SykepengegrunnlagDbRecord? = null,
    behandlingDbRecord: BehandlingDbRecord = enkelBehandlingDbRecord,
    yrkesaktiviteter: List<LegacyYrkesaktivitet> = emptyList(),
    vurderteVilkår: List<VurdertVilkår> = emptyList(),
    beregningData: BeregningData? = null,
): ValideringData =
    ValideringData(
        behandlingDbRecord = behandlingDbRecord,
        yrkesaktiviteter = yrkesaktiviteter,
        sykepengegrunnlag = sykepengegrunnlag,
        vurderteVilkår = vurderteVilkår,
        beregningData = beregningData,
    )

fun arbeidstaker(): List<LegacyYrkesaktivitet> =
    listOf(
        LegacyYrkesaktivitet(
            id = UUID.randomUUID(),
            kategorisering =
                YrkesaktivitetKategorisering.Arbeidstaker(
                    sykmeldt = true,
                    typeArbeidstaker =
                        TypeArbeidstaker.Ordinær(
                            "123456798",
                        ),
                ),
            kategoriseringGenerert = null,
            dagoversikt = Dagoversikt(emptyList(), emptyList()),
            dagoversiktGenerert = Dagoversikt(emptyList(), emptyList()),
            behandlingId = UUID.randomUUID(),
            opprettet = OffsetDateTime.now(),
            generertFraDokumenter = emptyList(),
        ),
    )

fun skapUtbetaling(totalbeløp: Int): BeregningData =
    BeregningData(
        yrkesaktiviteter = emptyList(),
        spilleromOppdrag =
            SpilleromOppdragDto(
                spilleromUtbetalingId = UUID.randomUUID().toString(),
                fnr = "sdf",
                oppdrag =
                    listOf(
                        OppdragDto(
                            mottaker = "whatever",
                            fagområde = "whatever",
                            linjer = emptyList(),
                            totalbeløp = totalbeløp,
                        ),
                    ),
                maksdato = LocalDate.now(),
            ),
    )
