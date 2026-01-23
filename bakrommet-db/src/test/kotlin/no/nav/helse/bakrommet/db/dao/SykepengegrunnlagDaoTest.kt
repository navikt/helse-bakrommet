package no.nav.helse.bakrommet.db.dao

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.db.DBTestFixture
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.enNavIdent
import no.nav.helse.bakrommet.errorhandling.KunneIkkeOppdatereDbException
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.dto.InntektbeløpDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SykepengegrunnlagDaoTest {
    private val dataSource = DBTestFixture.module.dataSource
    private val saksbehandler = Bruker("ABC", enNavIdent(), "ola@nav.no", emptySet())

    private val behandlingId = UUID.randomUUID()
    private val naturligIdent = enNaturligIdent()
    private val pseudoId = UUID.nameUUIDFromBytes(naturligIdent.value.toByteArray())
    private val dao = SykepengegrunnlagDaoPg(dataSource)

    init {
        PersonPseudoIdDaoPg(dataSource).opprettPseudoId(pseudoId, naturligIdent)
        BehandlingDaoPg(dataSource).opprettPeriode(
            BehandlingDbRecord(
                id = behandlingId,
                naturligIdent = naturligIdent,
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
        val sykepengegrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(124028.0),
                beregningsgrunnlag = InntektbeløpDto.Årlig(744168.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(540000.0),
                seksG = InntektbeløpDto.Årlig(744168.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val lagretGrunnlag = dao.lagreSykepengegrunnlag(sykepengegrunnlag, saksbehandler, behandlingId)

        assertEquals(540000.0, lagretGrunnlag.sykepengegrunnlag!!.sykepengegrunnlag.beløp)
        assertEquals(744168.0, lagretGrunnlag.sykepengegrunnlag!!.seksG.beløp)
        assertEquals(false, lagretGrunnlag.sykepengegrunnlag!!.begrensetTil6G)
        assertEquals(saksbehandler.navIdent, lagretGrunnlag.opprettetAv)

        // Verifiser at opprettet og oppdatert er like ved opprettelse
        assertEquals(lagretGrunnlag.opprettet, lagretGrunnlag.oppdatert)

        val hentetGrunnlag = dao.finnSykepengegrunnlag(lagretGrunnlag.id)
        assertEquals(lagretGrunnlag.id, hentetGrunnlag!!.id)
        assertEquals(
            lagretGrunnlag.sykepengegrunnlag!!.sykepengegrunnlag.beløp,
            hentetGrunnlag.sykepengegrunnlag!!.sykepengegrunnlag.beløp,
        )
    }

    @Test
    fun `oppretter sykepengegrunnlag med 6G-begrensning`() {
        val sykepengegrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(130160.0),
                beregningsgrunnlag = InntektbeløpDto.Årlig(900000.0), // Høyere enn 6G
                sykepengegrunnlag = InntektbeløpDto.Årlig(780960.0), // Begrenset til 6G
                seksG = InntektbeløpDto.Årlig(780960.0),
                begrensetTil6G = true,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val lagretGrunnlag = dao.lagreSykepengegrunnlag(sykepengegrunnlag, saksbehandler, behandlingId)

        assertEquals(780960.0, lagretGrunnlag.sykepengegrunnlag!!.sykepengegrunnlag.beløp)
        assertEquals(true, lagretGrunnlag.sykepengegrunnlag!!.begrensetTil6G)
        assertEquals(
            lagretGrunnlag.sykepengegrunnlag!!.seksG.beløp,
            lagretGrunnlag.sykepengegrunnlag!!.sykepengegrunnlag.beløp,
        )
    }

    @Test
    fun `oppdaterer eksisterende sykepengegrunnlag`() {
        val opprinneligGrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(130160.0),
                beregningsgrunnlag = InntektbeløpDto.Årlig(480000.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(480000.0),
                seksG = InntektbeløpDto.Årlig(780960.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val lagretGrunnlag = dao.lagreSykepengegrunnlag(opprinneligGrunnlag, saksbehandler, behandlingId)

        val oppdatertGrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(130160.0),
                beregningsgrunnlag = InntektbeløpDto.Årlig(660000.0),
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
        val sykepengegrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(130160.0),
                beregningsgrunnlag = InntektbeløpDto.Årlig(540000.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(540000.0),
                seksG = InntektbeløpDto.Årlig(780960.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val lagretGrunnlag = dao.lagreSykepengegrunnlag(sykepengegrunnlag, saksbehandler, behandlingId)

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
        val ikkeEksisterendeId = UUID.randomUUID()

        val grunnlag = dao.finnSykepengegrunnlag(ikkeEksisterendeId)

        assertNull(grunnlag)
    }

    @Test
    fun `serialiserer og deserialiserer sykepengegrunnlag korrekt`() {
        val sykepengegrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(124028.0),
                beregningsgrunnlag = InntektbeløpDto.Årlig(744168.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(540000.0),
                seksG = InntektbeløpDto.Årlig(744168.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val lagretGrunnlag = dao.lagreSykepengegrunnlag(sykepengegrunnlag, saksbehandler, behandlingId)
        val hentetGrunnlag = dao.finnSykepengegrunnlag(lagretGrunnlag.id)

        // Verifiser at alle felter er korrekt deserialisert
        assertEquals(sykepengegrunnlag.grunnbeløp.beløp, hentetGrunnlag!!.sykepengegrunnlag!!.grunnbeløp.beløp)
        assertEquals(
            sykepengegrunnlag.beregningsgrunnlag.beløp,
            hentetGrunnlag.sykepengegrunnlag!!.beregningsgrunnlag.beløp,
        )
        assertEquals(
            sykepengegrunnlag.sykepengegrunnlag.beløp,
            hentetGrunnlag.sykepengegrunnlag!!.sykepengegrunnlag.beløp,
        )
        assertEquals(sykepengegrunnlag.seksG.beløp, hentetGrunnlag.sykepengegrunnlag!!.seksG.beløp)
        assertEquals(sykepengegrunnlag.begrensetTil6G, hentetGrunnlag.sykepengegrunnlag!!.begrensetTil6G)
        assertEquals(
            sykepengegrunnlag.grunnbeløpVirkningstidspunkt,
            hentetGrunnlag.sykepengegrunnlag!!.grunnbeløpVirkningstidspunkt,
        )
        assertIs<Sykepengegrunnlag>(hentetGrunnlag.sykepengegrunnlag)
        assertEquals(sykepengegrunnlag.næringsdel, (hentetGrunnlag.sykepengegrunnlag as Sykepengegrunnlag).næringsdel)
    }

    @Test
    fun `låste grunnlag kan ikke endres`() {
        val sykepengegrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(124028.0),
                beregningsgrunnlag = InntektbeløpDto.Årlig(744168.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(540000.0),
                seksG = InntektbeløpDto.Årlig(744168.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val lagretGrunnlag = dao.lagreSykepengegrunnlag(sykepengegrunnlag, saksbehandler, behandlingId)

        dao.settLåst(lagretGrunnlag.id)
        assertThrows<KunneIkkeOppdatereDbException> {
            dao.oppdaterSykepengegrunnlag(
                lagretGrunnlag.id,
                sykepengegrunnlag.copy(sykepengegrunnlag = InntektbeløpDto.Årlig(600000.0)),
            )
        }.also { it.message `should equal` "Sykepengegrunnlag kunne ikke oppdateres" }

        assertThrows<KunneIkkeOppdatereDbException> {
            dao.slettSykepengegrunnlag(lagretGrunnlag.id)
        }.also { it.message `should equal` "Sykepengegrunnlag kunne ikke oppdateres" }

        assertThrows<KunneIkkeOppdatereDbException> {
            dao.settLåst(lagretGrunnlag.id)
        }.also { it.message `should equal` "Sykepengegrunnlag kunne ikke oppdateres" }

        assertThrows<KunneIkkeOppdatereDbException> {
            dao.oppdaterSammenlikningsgrunnlag(lagretGrunnlag.id, null)
        }.also { it.message `should equal` "Sykepengegrunnlag kunne ikke oppdateres" }
    }
}
