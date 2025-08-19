package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SykepengegrunnlagDaoTest {
    val dataSource = TestDataSource.dbModule.dataSource
    val fnr = "01019012345"
    val personId = "0h0a1"
    val saksbehandler = Bruker("ABC", "A. B. C", "ola@nav.no", emptySet())
    val periode =
        Saksbehandlingsperiode(
            id = UUID.randomUUID(),
            spilleromPersonId = personId,
            opprettet = OffsetDateTime.now(),
            opprettetAvNavIdent = saksbehandler.navIdent,
            opprettetAvNavn = saksbehandler.navn,
            fom = LocalDate.now().minusMonths(1),
            tom = LocalDate.now().minusDays(1),
        )

    @BeforeEach
    fun setOpp() {
        TestDataSource.resetDatasource()
        val personDao = PersonDao(dataSource)
        personDao.opprettPerson(fnr, personId)
        val behandlingDao = SaksbehandlingsperiodeDao(dataSource)
        behandlingDao.opprettPeriode(periode)
    }

    @Test
    fun `oppretter og henter sykepengegrunnlag`() {
        val dao = SykepengegrunnlagDao(dataSource)

        val beregning =
            SykepengegrunnlagResponse(
                id = UUID.randomUUID(),
                saksbehandlingsperiodeId = periode.id,
                faktiskeInntekter = emptyList(),
                totalInntektØre = 54000000L,
                grunnbeløp6GØre = 74416800L,
                begrensetTil6G = false,
                sykepengegrunnlagØre = 54000000L,
                begrunnelse = "Test beregning",
                opprettet = LocalDateTime.now().toString(),
                opprettetAv = saksbehandler.navIdent,
                sistOppdatert = LocalDateTime.now().toString(),
                versjon = 1,
            )

        val lagretGrunnlag = dao.opprettSykepengegrunnlag(periode.id, beregning, saksbehandler)

        assertEquals(periode.id, lagretGrunnlag.saksbehandlingsperiodeId)
        assertEquals(54000000L, lagretGrunnlag.totalInntektØre)
        assertEquals(74416800L, lagretGrunnlag.grunnbeløp6GØre)
        assertEquals(false, lagretGrunnlag.begrensetTil6G)
        assertEquals(54000000L, lagretGrunnlag.sykepengegrunnlagØre)
        assertEquals("Test beregning", lagretGrunnlag.begrunnelse)
        assertEquals(saksbehandler.navIdent, lagretGrunnlag.opprettetAv)
        assertEquals(1, lagretGrunnlag.versjon)

        val hentetGrunnlag = dao.hentSykepengegrunnlag(periode.id)
        assertEquals(lagretGrunnlag.id, hentetGrunnlag!!.id)
        assertEquals(lagretGrunnlag.totalInntektØre, hentetGrunnlag.totalInntektØre)
    }

    @Test
    fun `oppretter sykepengegrunnlag med 6G-begrensning`() {
        val dao = SykepengegrunnlagDao(dataSource)

        val beregning =
            SykepengegrunnlagResponse(
                id = UUID.randomUUID(),
                saksbehandlingsperiodeId = periode.id,
                faktiskeInntekter = emptyList(),
                // Over 6G
                totalInntektØre = 96000000L,
                grunnbeløp6GØre = 74416800L,
                begrensetTil6G = true,
                // Begrenset til 6G
                sykepengegrunnlagØre = 74416800L,
                begrunnelse = null,
                opprettet = LocalDateTime.now().toString(),
                opprettetAv = saksbehandler.navIdent,
                sistOppdatert = LocalDateTime.now().toString(),
                versjon = 1,
            )

        val lagretGrunnlag = dao.opprettSykepengegrunnlag(periode.id, beregning, saksbehandler)

        assertEquals(96000000L, lagretGrunnlag.totalInntektØre)
        assertEquals(true, lagretGrunnlag.begrensetTil6G)
        assertEquals(74416800L, lagretGrunnlag.sykepengegrunnlagØre)
    }

    @Test
    fun `oppdaterer eksisterende sykepengegrunnlag`() {
        val dao = SykepengegrunnlagDao(dataSource)

        val opprinneligBeregning =
            SykepengegrunnlagResponse(
                id = UUID.randomUUID(),
                saksbehandlingsperiodeId = periode.id,
                faktiskeInntekter = emptyList(),
                totalInntektØre = 48000000L,
                grunnbeløp6GØre = 74416800L,
                begrensetTil6G = false,
                sykepengegrunnlagØre = 48000000L,
                begrunnelse = "Opprinnelig beregning",
                opprettet = LocalDateTime.now().toString(),
                opprettetAv = saksbehandler.navIdent,
                sistOppdatert = LocalDateTime.now().toString(),
                versjon = 1,
            )

        val lagretGrunnlag = dao.opprettSykepengegrunnlag(periode.id, opprinneligBeregning, saksbehandler)

        val oppdatertBeregning =
            SykepengegrunnlagResponse(
                id = lagretGrunnlag.id,
                saksbehandlingsperiodeId = periode.id,
                faktiskeInntekter = emptyList(),
                totalInntektØre = 66000000L,
                grunnbeløp6GØre = 74416800L,
                begrensetTil6G = false,
                sykepengegrunnlagØre = 66000000L,
                begrunnelse = "Oppdatert beregning",
                opprettet = lagretGrunnlag.opprettet,
                opprettetAv = saksbehandler.navIdent,
                sistOppdatert = LocalDateTime.now().toString(),
                versjon = 2,
            )

        val oppdatertGrunnlag =
            dao.oppdaterSykepengegrunnlag(
                lagretGrunnlag.id,
                periode.id,
                oppdatertBeregning,
                saksbehandler,
                2,
            )

        assertEquals(66000000L, oppdatertGrunnlag.totalInntektØre)
        assertEquals("Oppdatert beregning", oppdatertGrunnlag.begrunnelse)
        assertEquals(2, oppdatertGrunnlag.versjon)

        // Verifiser at sistOppdatert er endret
        assertTrue(oppdatertGrunnlag.sistOppdatert >= lagretGrunnlag.sistOppdatert)
    }

    @Test
    fun `sletter sykepengegrunnlag`() {
        val dao = SykepengegrunnlagDao(dataSource)

        val beregning =
            SykepengegrunnlagResponse(
                id = UUID.randomUUID(),
                saksbehandlingsperiodeId = periode.id,
                faktiskeInntekter = emptyList(),
                totalInntektØre = 54000000L,
                grunnbeløp6GØre = 74416800L,
                begrensetTil6G = false,
                sykepengegrunnlagØre = 54000000L,
                begrunnelse = "Test beregning",
                opprettet = LocalDateTime.now().toString(),
                opprettetAv = saksbehandler.navIdent,
                sistOppdatert = LocalDateTime.now().toString(),
                versjon = 1,
            )

        dao.opprettSykepengegrunnlag(periode.id, beregning, saksbehandler)

        // Verifiser at grunnlaget finnes
        val hentetFørSletting = dao.hentSykepengegrunnlag(periode.id)
        assertEquals(54000000L, hentetFørSletting!!.totalInntektØre)

        // Slett grunnlaget
        dao.slettSykepengegrunnlag(periode.id)

        // Verifiser at grunnlaget er slettet
        val hentetEtterSletting = dao.hentSykepengegrunnlag(periode.id)
        assertNull(hentetEtterSletting)
    }

    @Test
    fun `returnerer null for periode uten sykepengegrunnlag`() {
        val dao = SykepengegrunnlagDao(dataSource)
        val annenPeriode = UUID.randomUUID()

        val grunnlag = dao.hentSykepengegrunnlag(annenPeriode)

        assertNull(grunnlag)
    }

    @Test
    fun `henter siste versjon ved flere versjoner`() {
        val dao = SykepengegrunnlagDao(dataSource)

        // Opprett første versjon
        val versjon1 =
            SykepengegrunnlagResponse(
                id = UUID.randomUUID(),
                saksbehandlingsperiodeId = periode.id,
                faktiskeInntekter = emptyList(),
                totalInntektØre = 48000000L,
                grunnbeløp6GØre = 74416800L,
                begrensetTil6G = false,
                sykepengegrunnlagØre = 48000000L,
                begrunnelse = "Versjon 1",
                opprettet = LocalDateTime.now().toString(),
                opprettetAv = saksbehandler.navIdent,
                sistOppdatert = LocalDateTime.now().toString(),
                versjon = 1,
            )

        val lagret1 = dao.opprettSykepengegrunnlag(periode.id, versjon1, saksbehandler)

        // Oppdater til andre versjon
        val versjon2 =
            SykepengegrunnlagResponse(
                // Bruk samme ID
                id = lagret1.id,
                saksbehandlingsperiodeId = periode.id,
                faktiskeInntekter = emptyList(),
                totalInntektØre = 60000000L,
                grunnbeløp6GØre = 74416800L,
                begrensetTil6G = false,
                sykepengegrunnlagØre = 60000000L,
                begrunnelse = "Versjon 2",
                // Behold opprinnelig opprettelsestidspunkt
                opprettet = lagret1.opprettet,
                opprettetAv = saksbehandler.navIdent,
                sistOppdatert = LocalDateTime.now().toString(),
                versjon = 2,
            )

        dao.oppdaterSykepengegrunnlag(lagret1.id, periode.id, versjon2, saksbehandler, 2)

        // Hent grunnlag - skal returnere siste versjon
        val hentetGrunnlag = dao.hentSykepengegrunnlag(periode.id)

        assertEquals(60000000L, hentetGrunnlag!!.totalInntektØre)
        assertEquals("Versjon 2", hentetGrunnlag.begrunnelse)
        assertEquals(2, hentetGrunnlag.versjon)
    }
}
