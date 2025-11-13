package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.person.PersonDaoPg
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDaoPg
import no.nav.helse.dto.InntektbeløpDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SykepengegrunnlagDaoTest {
    val dataSource = TestDataSource.dbModule.dataSource
    val saksbehandler = Bruker("ABC", "A. B. C", "ola@nav.no", emptySet())

    val saksbehandlingsperiodeId = UUID.randomUUID()

    @BeforeEach
    fun setOpp() {
        TestDataSource.resetDatasource()
        PersonDaoPg(TestDataSource.dbModule.dataSource).opprettPerson("01019012345", "6512a")
        SaksbehandlingsperiodeDaoPg(TestDataSource.dbModule.dataSource).opprettPeriode(
            Saksbehandlingsperiode(
                id = saksbehandlingsperiodeId,
                spilleromPersonId = "6512a",
                opprettet = OffsetDateTime.now(),
                opprettetAvNavIdent = saksbehandler.navIdent,
                opprettetAvNavn = saksbehandler.navn,
                fom = LocalDate.of(2021, 1, 1),
                tom = LocalDate.of(2021, 1, 31),
                skjæringstidspunkt = LocalDate.of(2021, 1, 1),
            ),
        )
    }

    @Test
    fun `oppretter og henter sykepengegrunnlag`() {
        val dao = SykepengegrunnlagDaoPg(dataSource)

        val sykepengegrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(124028.0),
                totaltInntektsgrunnlag = InntektbeløpDto.Årlig(744168.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(540000.0),
                seksG = InntektbeløpDto.Årlig(744168.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val lagretGrunnlag = dao.lagreSykepengegrunnlag(sykepengegrunnlag, saksbehandler, saksbehandlingsperiodeId)

        assertEquals(540000.0, lagretGrunnlag.sykepengegrunnlag!!.sykepengegrunnlag.beløp)
        assertEquals(744168.0, lagretGrunnlag.sykepengegrunnlag!!.seksG.beløp)
        assertEquals(false, lagretGrunnlag.sykepengegrunnlag!!.begrensetTil6G)
        assertEquals(saksbehandler.navIdent, lagretGrunnlag.opprettetAv)

        // Verifiser at opprettet og oppdatert er like ved opprettelse
        assertEquals(lagretGrunnlag.opprettet, lagretGrunnlag.oppdatert)

        val hentetGrunnlag = dao.finnSykepengegrunnlag(lagretGrunnlag.id)
        assertEquals(lagretGrunnlag.id, hentetGrunnlag!!.id)
        assertEquals(lagretGrunnlag.sykepengegrunnlag!!.sykepengegrunnlag.beløp, hentetGrunnlag.sykepengegrunnlag!!.sykepengegrunnlag.beløp)
    }

    @Test
    fun `oppretter sykepengegrunnlag med 6G-begrensning`() {
        val dao = SykepengegrunnlagDaoPg(dataSource)

        val sykepengegrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(130160.0),
                totaltInntektsgrunnlag = InntektbeløpDto.Årlig(900000.0), // Høyere enn 6G
                sykepengegrunnlag = InntektbeløpDto.Årlig(780960.0), // Begrenset til 6G
                seksG = InntektbeløpDto.Årlig(780960.0),
                begrensetTil6G = true,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val lagretGrunnlag = dao.lagreSykepengegrunnlag(sykepengegrunnlag, saksbehandler, saksbehandlingsperiodeId)

        assertEquals(780960.0, lagretGrunnlag.sykepengegrunnlag!!.sykepengegrunnlag.beløp)
        assertEquals(true, lagretGrunnlag.sykepengegrunnlag!!.begrensetTil6G)
        assertEquals(lagretGrunnlag.sykepengegrunnlag!!.seksG.beløp, lagretGrunnlag.sykepengegrunnlag!!.sykepengegrunnlag.beløp)
    }

    @Test
    fun `oppdaterer eksisterende sykepengegrunnlag`() {
        val dao = SykepengegrunnlagDaoPg(dataSource)

        val opprinneligGrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(130160.0),
                totaltInntektsgrunnlag = InntektbeløpDto.Årlig(480000.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(480000.0),
                seksG = InntektbeløpDto.Årlig(780960.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val lagretGrunnlag = dao.lagreSykepengegrunnlag(opprinneligGrunnlag, saksbehandler, saksbehandlingsperiodeId)

        val oppdatertGrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(130160.0),
                totaltInntektsgrunnlag = InntektbeløpDto.Årlig(660000.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(660000.0),
                seksG = InntektbeløpDto.Årlig(780960.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val oppdatertResultat = dao.oppdaterSykepengegrunnlag(lagretGrunnlag.id, oppdatertGrunnlag)

        assertEquals(660000.0, oppdatertResultat.sykepengegrunnlag!!.sykepengegrunnlag.beløp)
        assertEquals(lagretGrunnlag.id, oppdatertResultat.id)

        // Verifiser at oppdatert tidspunkt er endret
        assertTrue(oppdatertResultat.oppdatert.isAfter(lagretGrunnlag.oppdatert))
    }

    @Test
    fun `sletter sykepengegrunnlag`() {
        val dao = SykepengegrunnlagDaoPg(dataSource)

        val sykepengegrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(130160.0),
                totaltInntektsgrunnlag = InntektbeløpDto.Årlig(540000.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(540000.0),
                seksG = InntektbeløpDto.Årlig(780960.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val lagretGrunnlag = dao.lagreSykepengegrunnlag(sykepengegrunnlag, saksbehandler, saksbehandlingsperiodeId)

        // Verifiser at grunnlaget finnes
        val hentetFørSletting = dao.finnSykepengegrunnlag(lagretGrunnlag.id)
        assertEquals(540000.0, hentetFørSletting!!.sykepengegrunnlag!!.sykepengegrunnlag.beløp)

        // Slett grunnlaget
        dao.slettSykepengegrunnlag(lagretGrunnlag.id)

        // Verifiser at grunnlaget er slettet
        val hentetEtterSletting = dao.finnSykepengegrunnlag(lagretGrunnlag.id)
        assertNull(hentetEtterSletting)
    }

    @Test
    fun `returnerer null for ikke-eksisterende sykepengegrunnlag`() {
        val dao = SykepengegrunnlagDaoPg(dataSource)
        val ikkeEksisterendeId = UUID.randomUUID()

        val grunnlag = dao.finnSykepengegrunnlag(ikkeEksisterendeId)

        assertNull(grunnlag)
    }

    @Test
    fun `serialiserer og deserialiserer sykepengegrunnlag korrekt`() {
        val dao = SykepengegrunnlagDaoPg(dataSource)

        val sykepengegrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(124028.0),
                totaltInntektsgrunnlag = InntektbeløpDto.Årlig(744168.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(540000.0),
                seksG = InntektbeløpDto.Årlig(744168.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val lagretGrunnlag = dao.lagreSykepengegrunnlag(sykepengegrunnlag, saksbehandler, saksbehandlingsperiodeId)
        val hentetGrunnlag = dao.finnSykepengegrunnlag(lagretGrunnlag.id)

        // Verifiser at alle felter er korrekt deserialisert
        assertEquals(sykepengegrunnlag.grunnbeløp.beløp, hentetGrunnlag!!.sykepengegrunnlag!!.grunnbeløp.beløp)
        assertEquals(sykepengegrunnlag.totaltInntektsgrunnlag.beløp, hentetGrunnlag.sykepengegrunnlag!!.totaltInntektsgrunnlag.beløp)
        assertEquals(sykepengegrunnlag.sykepengegrunnlag.beløp, hentetGrunnlag.sykepengegrunnlag!!.sykepengegrunnlag.beløp)
        assertEquals(sykepengegrunnlag.seksG.beløp, hentetGrunnlag.sykepengegrunnlag!!.seksG.beløp)
        assertEquals(sykepengegrunnlag.begrensetTil6G, hentetGrunnlag.sykepengegrunnlag!!.begrensetTil6G)
        assertEquals(sykepengegrunnlag.grunnbeløpVirkningstidspunkt, hentetGrunnlag.sykepengegrunnlag!!.grunnbeløpVirkningstidspunkt)
        assertEquals(sykepengegrunnlag.næringsdel, hentetGrunnlag.sykepengegrunnlag!!.næringsdel)
    }
}
