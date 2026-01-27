package no.nav.helse.bakrommet.db.dao

import kotliquery.sessionOf
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.db.DBTestFixture
import no.nav.helse.bakrommet.db.repository.PgBehandlingRepository
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.enNavIdent
import no.nav.helse.bakrommet.testutils.truncateTidspunkt
import no.nav.helse.dto.InntektbeløpDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SaksbehandlingsperiodeDaoTest {
    val dataSource = DBTestFixture.module.dataSource

    private val dao = BehandlingDaoPg(dataSource)
    private val personPseudoIdDao = PersonPseudoIdDaoPg(dataSource)
    private val session = sessionOf(dataSource)
    private val behandlingRepository = PgBehandlingRepository(session)

    private val naturligIdent = enNaturligIdent()
    private val pseudoId = UUID.nameUUIDFromBytes(naturligIdent.value.toByteArray())

    init {
        personPseudoIdDao.opprettPseudoId(pseudoId, naturligIdent)
    }

    @AfterEach
    fun tearDown() {
        session.close()
    }

    @Test
    fun `ukjent id gir null`() {
        assertNull(dao.finnBehandling(UUID.randomUUID()))
    }

    @Test
    fun `kan opprette og hente periode`() {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        val saksbehandler = Bruker("Ola Nordmann", enNavIdent(), "ola@nav.no", emptySet())
        val fom = LocalDate.of(2021, 1, 1)
        val tom = LocalDate.of(2021, 1, 31)

        val periode =
            BehandlingDbRecord(
                id = id,
                naturligIdent = naturligIdent,
                opprettet = now,
                opprettetAvNavIdent = saksbehandler.navIdent,
                opprettetAvNavn = saksbehandler.navn,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = fom,
                individuellBegrunnelse = null,
                sykepengegrunnlagId = null,
            ).truncateTidspunkt()
        dao.opprettPeriode(periode)

        val hentet = dao.finnBehandling(id)!!
        assertEquals(periode, hentet)

        // Sjekk at perioden finnes i listen over alle perioder for personen
        val perioder = behandlingRepository.finnFor(naturligIdent)
        assertTrue(perioder.any { it.id.value == id })
    }

    @Test
    fun `kan finne perioder som overlapper angitt periode`() {
        fun String.toLocalDate() = LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)

        fun opprettPeriode(
            fom: String,
            tom: String,
        ): BehandlingDbRecord {
            val id = UUID.randomUUID()
            val now = OffsetDateTime.now()
            val saksbehandler = Bruker("Z12345", "Ola Nordmann", "ola@nav.no", emptySet())
            val periode =
                BehandlingDbRecord(
                    id = id,
                    naturligIdent = naturligIdent,
                    opprettet = now,
                    opprettetAvNavIdent = saksbehandler.navIdent,
                    opprettetAvNavn = saksbehandler.navn,
                    fom = fom.toLocalDate(),
                    tom = tom.toLocalDate(),
                    skjæringstidspunkt = fom.toLocalDate(),
                    sykepengegrunnlagId = null,
                ).truncateTidspunkt()
            dao.opprettPeriode(periode)
            return dao.finnBehandling(id)!!
        }

        fun finnOverlappende(
            fom: String,
            tom: String,
        ): Set<BehandlingDbRecord> =
            dao
                .finnBehandlingerForNaturligIdentSomOverlapper(
                    naturligIdent,
                    fom.toLocalDate(),
                    tom.toLocalDate(),
                ).toSet()

        val p1 = opprettPeriode("2024-01-01", "2024-02-01")
        val p2 = opprettPeriode("2024-02-15", "2024-02-25")

        assertEquals(setOf(p1), finnOverlappende("2023-12-15", "2024-01-01"))
        assertEquals(emptySet(), finnOverlappende("2023-12-15", "2023-12-31"))
        assertEquals(setOf(p1, p2), finnOverlappende("2023-12-15", "2024-02-15"))
        assertEquals(setOf(p1), finnOverlappende("2023-12-15", "2024-02-14"))
        assertEquals(emptySet(), finnOverlappende("2024-02-02", "2024-02-14"))
        assertEquals(setOf(p1), finnOverlappende("2024-02-01", "2024-02-14"))
        assertEquals(setOf(p1, p2), finnOverlappende("2024-02-01", "2024-02-15"))
        assertEquals(setOf(p2), finnOverlappende("2024-02-25", "2024-03-15"))
        assertEquals(emptySet(), finnOverlappende("2024-02-26", "2024-03-15"))
    }

    @Test
    fun `kan oppdatere skjæringstidspunkt`() {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        val saksbehandler = Bruker("Z12345", "Ola Nordmann", "ola@nav.no", emptySet())
        val fom = LocalDate.of(2021, 1, 1)
        val tom = LocalDate.of(2021, 1, 31)

        val periode =
            BehandlingDbRecord(
                id = id,
                naturligIdent = naturligIdent,
                opprettet = now,
                opprettetAvNavIdent = saksbehandler.navIdent,
                opprettetAvNavn = saksbehandler.navn,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = fom,
                individuellBegrunnelse = null,
                sykepengegrunnlagId = null,
            ).truncateTidspunkt()
        dao.opprettPeriode(periode)

        // Oppdater skjæringstidspunkt
        val nyttSkjæringstidspunkt = LocalDate.of(2021, 1, 15)
        dao.oppdaterSkjæringstidspunkt(id, nyttSkjæringstidspunkt)

        val oppdatertPeriode = dao.finnBehandling(id)!!
        assertEquals(nyttSkjæringstidspunkt, oppdatertPeriode.skjæringstidspunkt)
    }

    @Test
    fun `kan oppdatere sykepengegrunnlag_id`() {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        val saksbehandler = Bruker("Z12345", "Ola Nordmann", "ola@nav.no", emptySet())
        val fom = LocalDate.of(2021, 1, 1)
        val tom = LocalDate.of(2021, 1, 31)

        val periode =
            BehandlingDbRecord(
                id = id,
                naturligIdent = naturligIdent,
                opprettet = now,
                opprettetAvNavIdent = saksbehandler.navIdent,
                opprettetAvNavn = saksbehandler.navn,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = fom,
                individuellBegrunnelse = null,
                sykepengegrunnlagId = null,
            ).truncateTidspunkt()
        dao.opprettPeriode(periode)

        // Opprett et gyldig sykepengegrunnlag først
        val sykepengegrunnlagDao = SykepengegrunnlagDaoPg(dataSource)
        val sykepengegrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(124028.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(540000.0),
                seksG = InntektbeløpDto.Årlig(744168.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                beregningsgrunnlag = InntektbeløpDto.Årlig(540000.0),
                næringsdel = null,
            )
        val lagretGrunnlag = sykepengegrunnlagDao.lagreSykepengegrunnlag(sykepengegrunnlag, saksbehandler, id)

        // Oppdater sykepengegrunnlag_id med gyldig ID
        dao.oppdaterSykepengegrunnlagId(id, lagretGrunnlag.id)

        val oppdatertPeriode = dao.finnBehandling(id)!!
        assertEquals(lagretGrunnlag.id, oppdatertPeriode.sykepengegrunnlagId)

        // Nullstill sykepengegrunnlag_id
        dao.oppdaterSykepengegrunnlagId(id, null)

        val nullstiltPeriode = dao.finnBehandling(id)!!
        assertNull(nullstiltPeriode.sykepengegrunnlagId)
    }
}
