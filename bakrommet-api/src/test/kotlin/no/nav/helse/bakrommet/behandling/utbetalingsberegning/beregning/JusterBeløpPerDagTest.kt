package no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JusterBeløpPerDagTest {

    @Test
    fun `skal ikke justere beløp når summen er under sykepengegrunnlagDaglig`() {
        val yrkesaktivitetId1 = UUID.randomUUID()
        val yrkesaktivitetId2 = UUID.randomUUID()
        val dato = LocalDate.of(2024, 1, 1)
        val periode = PeriodeDto(dato, dato)

        // Sykepengegrunnlag: 240000 kr/år = 923.08 kr/dag
        val sykepengegrunnlagDaglig = 240000.0 / 260.0 // ca 923.08

        // Totalt beløp: 800 kr/dag (under grensen)
        val beløp1 = InntektbeløpDto.DagligDouble(400.0).tilInntekt()
        val beløp2 = InntektbeløpDto.DagligDouble(400.0).tilInntekt()

        val maksInntektTilFordelingPerDagMap = mapOf(
            yrkesaktivitetId1 to Beløpstidslinje(
                listOf(
                    Beløpsdag(
                        dato = dato,
                        beløp = beløp1,
                        kilde = Kilde(
                            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                            avsender = Avsender.SYKMELDT,
                            tidsstempel = LocalDateTime.now(),
                        ),
                    ),
                ),
            ),
            yrkesaktivitetId2 to Beløpstidslinje(
                listOf(
                    Beløpsdag(
                        dato = dato,
                        beløp = beløp2,
                        kilde = Kilde(
                            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                            avsender = Avsender.SYKMELDT,
                            tidsstempel = LocalDateTime.now(),
                        ),
                    ),
                ),
            ),
        )

        val resultat = justerBeløpPerDag(
            maksInntektTilFordelingPerDagMap = maksInntektTilFordelingPerDagMap,
            sykepengegrunnlagDaglig = sykepengegrunnlagDaglig,
            saksbehandlingsperiode = periode,
        )

        assertEquals(beløp1, resultat[dato to yrkesaktivitetId1]!!)
        assertEquals(beløp2, resultat[dato to yrkesaktivitetId2]!!)
    }

    @Test
    fun `skal justere beløp proporsjonalt når summen overstiger sykepengegrunnlagDaglig`() {
        val yrkesaktivitetId1 = UUID.randomUUID()
        val yrkesaktivitetId2 = UUID.randomUUID()
        val dato = LocalDate.of(2024, 1, 1)
        val periode = PeriodeDto(dato, dato)

        // Sykepengegrunnlag: 240000 kr/år = 923.08 kr/dag
        val sykepengegrunnlagDaglig = 240000.0 / 260.0 // ca 923.08

        // Totalt beløp: 1500 kr/dag (over grensen)
        val beløp1 = InntektbeløpDto.DagligDouble(900.0).tilInntekt() // 60% av totalt
        val beløp2 = InntektbeløpDto.DagligDouble(600.0).tilInntekt() // 40% av totalt

        val maksInntektTilFordelingPerDagMap = mapOf(
            yrkesaktivitetId1 to Beløpstidslinje(
                listOf(
                    Beløpsdag(
                        dato = dato,
                        beløp = beløp1,
                        kilde = Kilde(
                            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                            avsender = Avsender.SYKMELDT,
                            tidsstempel = LocalDateTime.now(),
                        ),
                    ),
                ),
            ),
            yrkesaktivitetId2 to Beløpstidslinje(
                listOf(
                    Beløpsdag(
                        dato = dato,
                        beløp = beløp2,
                        kilde = Kilde(
                            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                            avsender = Avsender.SYKMELDT,
                            tidsstempel = LocalDateTime.now(),
                        ),
                    ),
                ),
            ),
        )

        val resultat = justerBeløpPerDag(
            maksInntektTilFordelingPerDagMap = maksInntektTilFordelingPerDagMap,
            sykepengegrunnlagDaglig = sykepengegrunnlagDaglig,
            saksbehandlingsperiode = periode,
        )

        // Verifiser at summen er nøyaktig lik sykepengegrunnlagDaglig
        val sum = resultat[dato to yrkesaktivitetId1]!!.daglig + resultat[dato to yrkesaktivitetId2]!!.daglig
        assertEquals(sykepengegrunnlagDaglig, sum, absoluteTolerance = 0.01)

        // Verifiser proporsjonal fordeling
        // Yrkesaktivitet 1 skal ha 60% av sykepengegrunnlagDaglig
        val forventetBeløp1 = sykepengegrunnlagDaglig * 0.6
        assertEquals(forventetBeløp1, resultat[dato to yrkesaktivitetId1]!!.daglig, absoluteTolerance = 0.01)

        // Yrkesaktivitet 2 skal ha 40% av sykepengegrunnlagDaglig
        val forventetBeløp2 = sykepengegrunnlagDaglig * 0.4
        assertEquals(forventetBeløp2, resultat[dato to yrkesaktivitetId2]!!.daglig, absoluteTolerance = 0.01)
    }

    @Test
    fun `skal justere beløp for flere dager`() {
        val yrkesaktivitetId1 = UUID.randomUUID()
        val yrkesaktivitetId2 = UUID.randomUUID()
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 3)
        val periode = PeriodeDto(fom, tom)

        val sykepengegrunnlagDaglig = 240000.0 / 260.0 // ca 923.08

        // Dag 1: 1500 kr (over grensen)
        // Dag 2: 800 kr (under grensen)
        // Dag 3: 2000 kr (over grensen)
        val maksInntektTilFordelingPerDagMap = mapOf(
            yrkesaktivitetId1 to Beløpstidslinje(
                listOf(
                    Beløpsdag(fom, InntektbeløpDto.DagligDouble(900.0).tilInntekt(), kilde()),
                    Beløpsdag(fom.plusDays(1), InntektbeløpDto.DagligDouble(400.0).tilInntekt(), kilde()),
                    Beløpsdag(fom.plusDays(2), InntektbeløpDto.DagligDouble(1200.0).tilInntekt(), kilde()),
                ),
            ),
            yrkesaktivitetId2 to Beløpstidslinje(
                listOf(
                    Beløpsdag(fom, InntektbeløpDto.DagligDouble(600.0).tilInntekt(), kilde()),
                    Beløpsdag(fom.plusDays(1), InntektbeløpDto.DagligDouble(400.0).tilInntekt(), kilde()),
                    Beløpsdag(fom.plusDays(2), InntektbeløpDto.DagligDouble(800.0).tilInntekt(), kilde()),
                ),
            ),
        )

        val resultat = justerBeløpPerDag(
            maksInntektTilFordelingPerDagMap = maksInntektTilFordelingPerDagMap,
            sykepengegrunnlagDaglig = sykepengegrunnlagDaglig,
            saksbehandlingsperiode = periode,
        )

        // Dag 1: skal være justert til sykepengegrunnlagDaglig
        val sumDag1 = resultat[fom to yrkesaktivitetId1]!!.daglig + resultat[fom to yrkesaktivitetId2]!!.daglig
        assertEquals(sykepengegrunnlagDaglig, sumDag1, absoluteTolerance = 0.01)

        // Dag 2: skal være uendret (under grensen)
        val sumDag2 = resultat[fom.plusDays(1) to yrkesaktivitetId1]!!.daglig + resultat[fom.plusDays(1) to yrkesaktivitetId2]!!.daglig
        assertEquals(800.0, sumDag2, absoluteTolerance = 0.01)

        // Dag 3: skal være justert til sykepengegrunnlagDaglig
        val sumDag3 = resultat[fom.plusDays(2) to yrkesaktivitetId1]!!.daglig + resultat[fom.plusDays(2) to yrkesaktivitetId2]!!.daglig
        assertEquals(sykepengegrunnlagDaglig, sumDag3, absoluteTolerance = 0.01)
    }

    @Test
    fun `skal håndtere tre yrkesaktiviteter med proporsjonal fordeling`() {
        val yrkesaktivitetId1 = UUID.randomUUID()
        val yrkesaktivitetId2 = UUID.randomUUID()
        val yrkesaktivitetId3 = UUID.randomUUID()
        val dato = LocalDate.of(2024, 1, 1)
        val periode = PeriodeDto(dato, dato)

        val sykepengegrunnlagDaglig = 240000.0 / 260.0 // ca 923.08

        // Totalt: 1500 kr (over grensen)
        // Yrkesaktivitet 1: 600 kr (40%)
        // Yrkesaktivitet 2: 600 kr (40%)
        // Yrkesaktivitet 3: 300 kr (20%)
        val maksInntektTilFordelingPerDagMap = mapOf(
            yrkesaktivitetId1 to Beløpstidslinje(
                listOf(Beløpsdag(dato, InntektbeløpDto.DagligDouble(600.0).tilInntekt(), kilde())),
            ),
            yrkesaktivitetId2 to Beløpstidslinje(
                listOf(Beløpsdag(dato, InntektbeløpDto.DagligDouble(600.0).tilInntekt(), kilde())),
            ),
            yrkesaktivitetId3 to Beløpstidslinje(
                listOf(Beløpsdag(dato, InntektbeløpDto.DagligDouble(300.0).tilInntekt(), kilde())),
            ),
        )

        val resultat = justerBeløpPerDag(
            maksInntektTilFordelingPerDagMap = maksInntektTilFordelingPerDagMap,
            sykepengegrunnlagDaglig = sykepengegrunnlagDaglig,
            saksbehandlingsperiode = periode,
        )

        val sum = resultat[dato to yrkesaktivitetId1]!!.daglig +
            resultat[dato to yrkesaktivitetId2]!!.daglig +
            resultat[dato to yrkesaktivitetId3]!!.daglig

        assertEquals(sykepengegrunnlagDaglig, sum, absoluteTolerance = 0.01)

        // Verifiser proporsjonal fordeling
        val forventetBeløp1 = sykepengegrunnlagDaglig * (600.0 / 1500.0)
        val forventetBeløp2 = sykepengegrunnlagDaglig * (600.0 / 1500.0)
        val forventetBeløp3 = sykepengegrunnlagDaglig * (300.0 / 1500.0)

        assertEquals(forventetBeløp1, resultat[dato to yrkesaktivitetId1]!!.daglig, absoluteTolerance = 0.01)
        assertEquals(forventetBeløp2, resultat[dato to yrkesaktivitetId2]!!.daglig, absoluteTolerance = 0.01)
        assertEquals(forventetBeløp3, resultat[dato to yrkesaktivitetId3]!!.daglig, absoluteTolerance = 0.01)
    }

    @Test
    fun `skal håndtere avrundingsfeil ved å legge differansen på største yrkesaktivitet`() {
        val yrkesaktivitetId1 = UUID.randomUUID()
        val yrkesaktivitetId2 = UUID.randomUUID()
        val dato = LocalDate.of(2024, 1, 1)
        val periode = PeriodeDto(dato, dato)

        // Bruk et beløp som gir avrundingsfeil
        val sykepengegrunnlagDaglig = 1000.0 / 3.0 // ca 333.33...

        // Totalt: 500 kr (over grensen)
        val beløp1 = InntektbeløpDto.DagligDouble(300.0).tilInntekt()
        val beløp2 = InntektbeløpDto.DagligDouble(200.0).tilInntekt()

        val maksInntektTilFordelingPerDagMap = mapOf(
            yrkesaktivitetId1 to Beløpstidslinje(
                listOf(Beløpsdag(dato, beløp1, kilde())),
            ),
            yrkesaktivitetId2 to Beløpstidslinje(
                listOf(Beløpsdag(dato, beløp2, kilde())),
            ),
        )

        val resultat = justerBeløpPerDag(
            maksInntektTilFordelingPerDagMap = maksInntektTilFordelingPerDagMap,
            sykepengegrunnlagDaglig = sykepengegrunnlagDaglig,
            saksbehandlingsperiode = periode,
        )

        // Summen skal være nøyaktig lik sykepengegrunnlagDaglig (differansen skal være lagt til på største yrkesaktivitet)
        val sum = resultat[dato to yrkesaktivitetId1]!!.daglig + resultat[dato to yrkesaktivitetId2]!!.daglig
        assertEquals(sykepengegrunnlagDaglig, sum, absoluteTolerance = 0.01)
    }

    private fun kilde() = Kilde(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        avsender = Avsender.SYKMELDT,
        tidsstempel = LocalDateTime.now(),
    )
}

