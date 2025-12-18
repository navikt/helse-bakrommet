package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.enkelBehandling
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDbRecord
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.BeregningData
import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.validering.ValideringSjekk
import no.nav.helse.bakrommet.behandling.vilkaar.Vilkaarsvurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VilkaarsvurderingUnderspørsmål
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Dagoversikt
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.TypeArbeidstaker
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.kafka.dto.oppdrag.OppdragDto
import no.nav.helse.bakrommet.kafka.dto.oppdrag.SpilleromOppdragDto
import no.nav.helse.bakrommet.kodeverk.Vilkårskode
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

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
            kode = "1",
            vurdering =
                Vilkaarsvurdering(
                    vilkårskode = "WHATEVER",
                    hovedspørsmål = "1",
                    vurdering = Vurdering.OPPFYLT,
                    underspørsmål =
                        listOf(
                            VilkaarsvurderingUnderspørsmål(
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
    vurdering: Vurdering,
): VurdertVilkår =
    VurdertVilkår(
        kode = "1",
        vurdering =
            Vilkaarsvurdering(
                vilkårskode = vilkar.name,
                hovedspørsmål = "1",
                vurdering = vurdering,
                underspørsmål = emptyList(),
                notat = "",
            ),
    )

fun data(
    sykepengegrunnlag: SykepengegrunnlagDbRecord? = null,
    behandling: Behandling = enkelBehandling,
    yrkesaktiviteter: List<Yrkesaktivitet> = emptyList(),
    vurderteVilkår: List<VurdertVilkår> = emptyList(),
    beregningData: BeregningData? = null,
): ValideringData =
    ValideringData(
        behandling = behandling,
        yrkesaktiviteter = yrkesaktiviteter,
        sykepengegrunnlag = sykepengegrunnlag,
        vurderteVilkår = vurderteVilkår,
        beregningData = beregningData,
    )

fun arbeidstaker(): List<Yrkesaktivitet> =
    listOf(
        Yrkesaktivitet(
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
            dagoversikt = Dagoversikt(),
            dagoversiktGenerert = Dagoversikt(),
            saksbehandlingsperiodeId = UUID.randomUUID(),
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
