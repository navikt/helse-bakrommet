package no.nav.helse.bakrommet.db.repository

import kotliquery.sessionOf
import no.nav.helse.bakrommet.assertOffsetDateTimeEquals
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.domain.enBehandling
import no.nav.helse.bakrommet.domain.enYrkesaktivitet
import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.*
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.AfterEach
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PgYrkesaktivitetRepositoryTest {
    private val dataSource = TestDataSource.dbModule.dataSource
    private val session = sessionOf(dataSource)
    private val yrkesaktivitetRepository = PgYrkesaktivitetRepository(session)
    private val behandlingRepository = PgBehandlingRepository(session)

    @AfterEach
    fun tearDown() {
        session.close()
    }

    @Test
    fun `lagre og finn yrkesaktivitet med minimal data`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val yrkesaktivitet = enYrkesaktivitet(behandlingId = behandling.id)
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(1, funnet.size)

        val første = funnet.first()
        assertEquals(yrkesaktivitet.id, første.id)
        assertEquals(yrkesaktivitet.behandlingId, første.behandlingId)
        assertOffsetDateTimeEquals(yrkesaktivitet.opprettet, første.opprettet)
        assertEquals(yrkesaktivitet.generertFraDokumenter, første.generertFraDokumenter)
        assertNull(første.dagoversikt)
        assertNull(første.dagoversiktGenerert)
        assertNull(første.kategoriseringGenerert)
        assertNull(første.perioder)
        assertNull(første.inntektRequest)
        assertNull(første.inntektData)
        assertNull(første.refusjon)
    }

    @Test
    fun `lagre og finn yrkesaktivitet med arbeidstaker kategorisering`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val yrkesaktivitet =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "123456789"),
                    ),
            )
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(1, funnet.size)

        val første = funnet.first()
        val kategorisering = første.kategorisering as YrkesaktivitetKategorisering.Arbeidstaker
        assertEquals(true, kategorisering.sykmeldt)
        assertEquals("123456789", (kategorisering.typeArbeidstaker as TypeArbeidstaker.Ordinær).orgnummer)
    }

    @Test
    fun `lagre og finn yrkesaktivitet med frilanser kategorisering`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val yrkesaktivitet =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.Frilanser(
                        sykmeldt = true,
                        orgnummer = "987654321",
                        forsikring = FrilanserForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
                    ),
            )
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(1, funnet.size)

        val første = funnet.first()
        val kategorisering = første.kategorisering as YrkesaktivitetKategorisering.Frilanser
        assertEquals(true, kategorisering.sykmeldt)
        assertEquals("987654321", kategorisering.orgnummer)
        assertEquals(FrilanserForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG, kategorisering.forsikring)
    }

    @Test
    fun `lagre og finn yrkesaktivitet med selvstendig næringsdrivende kategorisering`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val yrkesaktivitet =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                        sykmeldt = true,
                        typeSelvstendigNæringsdrivende =
                            TypeSelvstendigNæringsdrivende.Ordinær(
                                forsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
                            ),
                    ),
            )
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(1, funnet.size)

        val første = funnet.first()
        val kategorisering = første.kategorisering as YrkesaktivitetKategorisering.SelvstendigNæringsdrivende
        assertEquals(true, kategorisering.sykmeldt)
        val type = kategorisering.typeSelvstendigNæringsdrivende as TypeSelvstendigNæringsdrivende.Ordinær
        assertEquals(SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG, type.forsikring)
    }

    @Test
    fun `lagre og finn yrkesaktivitet med inaktiv kategorisering`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val yrkesaktivitet =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering = YrkesaktivitetKategorisering.Inaktiv(),
            )
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(1, funnet.size)

        val første = funnet.first()
        val kategorisering = første.kategorisering as YrkesaktivitetKategorisering.Inaktiv
        assertEquals(true, kategorisering.sykmeldt)
    }

    @Test
    fun `lagre og finn yrkesaktivitet med arbeidsledig kategorisering`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val yrkesaktivitet =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering = YrkesaktivitetKategorisering.Arbeidsledig(),
            )
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(1, funnet.size)

        val første = funnet.first()
        val kategorisering = første.kategorisering as YrkesaktivitetKategorisering.Arbeidsledig
        assertEquals(true, kategorisering.sykmeldt)
    }

    @Test
    fun `oppdater eksisterende yrkesaktivitet`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val yrkesaktivitet = enYrkesaktivitet(behandlingId = behandling.id)
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        yrkesaktivitet.nyKategorisering(
            YrkesaktivitetKategorisering.Arbeidstaker(
                sykmeldt = false,
                typeArbeidstaker = TypeArbeidstaker.Maritim(orgnummer = "999888777"),
            ),
        )
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(1, funnet.size)

        val første = funnet.first()
        assertEquals(yrkesaktivitet.id, første.id)
        val kategorisering = første.kategorisering as YrkesaktivitetKategorisering.Arbeidstaker
        assertEquals(false, kategorisering.sykmeldt)
        assertEquals("999888777", (kategorisering.typeArbeidstaker as TypeArbeidstaker.Maritim).orgnummer)
    }

    @Test
    fun `lagre og finn flere yrkesaktiviteter for samme behandling`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val yrkesaktivitet1 =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "111111111"),
                    ),
            )
        val yrkesaktivitet2 =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.Frilanser(
                        sykmeldt = true,
                        orgnummer = "222222222",
                        forsikring = FrilanserForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
                    ),
            )

        yrkesaktivitetRepository.lagre(yrkesaktivitet1)
        yrkesaktivitetRepository.lagre(yrkesaktivitet2)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(2, funnet.size)

        assertTrue(funnet.any { it.id == yrkesaktivitet1.id })
        assertTrue(funnet.any { it.id == yrkesaktivitet2.id })
    }

    @Test
    fun `finn returnerer tom liste for behandling uten yrkesaktiviteter`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(0, funnet.size)
    }

    @Test
    fun `lagre og finn yrkesaktivitet med generert fra dokumenter`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val dokumentId1 = UUID.randomUUID()
        val dokumentId2 = UUID.randomUUID()
        val yrkesaktivitet =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                generertFraDokumenter = listOf(dokumentId1, dokumentId2),
            )
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(1, funnet.size)

        val første = funnet.first()
        assertEquals(2, første.generertFraDokumenter.size)
        assertTrue(første.generertFraDokumenter.contains(dokumentId1))
        assertTrue(første.generertFraDokumenter.contains(dokumentId2))
    }

    @Test
    fun `lagre og finn yrkesaktivitet med dagoversikt`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val dagoversikt = Dagoversikt(sykdomstidlinje = emptyList(), avslagsdager = emptyList())
        val yrkesaktivitet =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                dagoversikt = dagoversikt,
            )
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(1, funnet.size)

        val første = funnet.first()
        assertNotNull(første.dagoversikt)
        assertEquals(0, første.dagoversikt!!.sykdomstidlinje.size)
        assertEquals(0, første.dagoversikt!!.avslagsdager.size)
    }

    @Test
    fun `lagre og finn yrkesaktivitet med alle typer arbeidstakere`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val ordinær =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "111111111"),
                    ),
            )
        val maritim =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstaker.Maritim(orgnummer = "222222222"),
                    ),
            )
        val fisker =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstaker.Fisker(orgnummer = "333333333"),
                    ),
            )
        val vernepliktig =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstaker.DimmitertVernepliktig(),
                    ),
            )
        val privatArbeidsgiver =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstaker.PrivatArbeidsgiver(arbeidsgiverFnr = "12345678901"),
                    ),
            )

        listOf(ordinær, maritim, fisker, vernepliktig, privatArbeidsgiver).forEach {
            yrkesaktivitetRepository.lagre(it)
        }

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(5, funnet.size)
    }

    @Test
    fun `lagre og finn yrkesaktivitet med alle typer selvstendig næringsdrivende`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val ordinær =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                        sykmeldt = true,
                        typeSelvstendigNæringsdrivende =
                            TypeSelvstendigNæringsdrivende.Ordinær(
                                forsikring = SelvstendigForsikring.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG,
                            ),
                    ),
            )
        val barnepasser =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                        sykmeldt = true,
                        typeSelvstendigNæringsdrivende =
                            TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem(
                                forsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG,
                            ),
                    ),
            )
        val fisker =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                        sykmeldt = true,
                        typeSelvstendigNæringsdrivende = TypeSelvstendigNæringsdrivende.Fisker(),
                    ),
            )
        val jordbruker =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                        sykmeldt = true,
                        typeSelvstendigNæringsdrivende =
                            TypeSelvstendigNæringsdrivende.Jordbruker(
                                forsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
                            ),
                    ),
            )
        val reindrift =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                kategorisering =
                    YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                        sykmeldt = true,
                        typeSelvstendigNæringsdrivende =
                            TypeSelvstendigNæringsdrivende.Reindrift(
                                forsikring = SelvstendigForsikring.INGEN_FORSIKRING,
                            ),
                    ),
            )

        listOf(ordinær, barnepasser, fisker, jordbruker, reindrift).forEach {
            yrkesaktivitetRepository.lagre(it)
        }

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(5, funnet.size)
    }

    @Test
    fun `lagre og finn yrkesaktivitet med perioder`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val perioder =
            Perioder(
                type = Periodetype.ARBEIDSGIVERPERIODE,
                perioder =
                    listOf(
                        no.nav.helse.bakrommet.domain.sykepenger.Periode(
                            fom = java.time.LocalDate.of(2024, 1, 1),
                            tom = java.time.LocalDate.of(2024, 1, 16),
                        ),
                        no.nav.helse.bakrommet.domain.sykepenger.Periode(
                            fom = java.time.LocalDate.of(2024, 2, 1),
                            tom = java.time.LocalDate.of(2024, 2, 16),
                        ),
                    ),
            )

        val yrkesaktivitet =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                perioder = perioder,
            )
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(1, funnet.size)

        val første = funnet.first()
        assertNotNull(første.perioder)
        assertEquals(Periodetype.ARBEIDSGIVERPERIODE, første.perioder!!.type)
        assertEquals(2, første.perioder!!.perioder.size)
        assertEquals(java.time.LocalDate.of(2024, 1, 1), første.perioder!!.perioder[0].fom)
        assertEquals(java.time.LocalDate.of(2024, 1, 16), første.perioder!!.perioder[0].tom)
    }

    @Test
    fun `lagre og finn yrkesaktivitet med refusjon`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val refusjonsperioder =
            listOf(
                Refusjonsperiode(
                    fom = java.time.LocalDate.of(2024, 1, 1),
                    tom = java.time.LocalDate.of(2024, 1, 31),
                    beløp = 500000.årlig,
                ),
                Refusjonsperiode(
                    fom = java.time.LocalDate.of(2024, 2, 1),
                    tom = null,
                    beløp = 600000.årlig,
                ),
            )

        val yrkesaktivitet =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                refusjon = refusjonsperioder,
            )
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(1, funnet.size)

        val første = funnet.first()
        assertNotNull(første.refusjon)
        assertEquals(2, første.refusjon!!.size)
        assertEquals(java.time.LocalDate.of(2024, 1, 1), første.refusjon!![0].fom)
        assertEquals(java.time.LocalDate.of(2024, 1, 31), første.refusjon!![0].tom)
        assertNull(første.refusjon!![1].tom)
    }

    @Test
    fun `lagre yrkesaktivitet med både dagoversikt og dagoversikt generert`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val dagoversikt1 = Dagoversikt(sykdomstidlinje = emptyList(), avslagsdager = emptyList())
        val dagoversikt2 = Dagoversikt(sykdomstidlinje = emptyList(), avslagsdager = emptyList())

        val yrkesaktivitet =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                dagoversikt = dagoversikt1,
                dagoversiktGenerert = dagoversikt2,
            )
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(1, funnet.size)

        val første = funnet.first()
        assertNotNull(første.dagoversikt)
        assertNotNull(første.dagoversiktGenerert)
    }

    @Test
    fun `lagre med ulike periodetype verdier`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val arbeidsgiverperiode =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                perioder =
                    Perioder(
                        type = Periodetype.ARBEIDSGIVERPERIODE,
                        perioder = emptyList(),
                    ),
            )
        val ventetid =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                perioder =
                    Perioder(
                        type = Periodetype.VENTETID,
                        perioder = emptyList(),
                    ),
            )
        val ventetidInaktiv =
            enYrkesaktivitet(
                behandlingId = behandling.id,
                perioder =
                    Perioder(
                        type = Periodetype.VENTETID_INAKTIV,
                        perioder = emptyList(),
                    ),
            )

        listOf(arbeidsgiverperiode, ventetid, ventetidInaktiv).forEach {
            yrkesaktivitetRepository.lagre(it)
        }

        val funnet = yrkesaktivitetRepository.finn(behandling.id)
        assertEquals(3, funnet.size)

        val typer = funnet.mapNotNull { it.perioder?.type }.toSet()
        assertEquals(3, typer.size)
        assertTrue(typer.contains(Periodetype.ARBEIDSGIVERPERIODE))
        assertTrue(typer.contains(Periodetype.VENTETID))
        assertTrue(typer.contains(Periodetype.VENTETID_INAKTIV))
    }

    @Test
    fun `finn yrkesaktivitet by id returnerer yrkesaktivitet`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val yrkesaktivitet = enYrkesaktivitet(behandlingId = behandling.id)
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnet = yrkesaktivitetRepository.finn(yrkesaktivitet.id)
        assertNotNull(funnet)
        assertEquals(yrkesaktivitet.id, funnet.id)
        assertEquals(yrkesaktivitet.behandlingId, funnet.behandlingId)
    }

    @Test
    fun `finn yrkesaktivitet by id returnerer null når den ikke finnes`() {
        val funnet = yrkesaktivitetRepository.finn(YrkesaktivitetId(UUID.randomUUID()))
        assertNull(funnet)
    }

    @Test
    fun `slett yrkesaktivitet by id fjerner yrkesaktiviteten`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val yrkesaktivitet = enYrkesaktivitet(behandlingId = behandling.id)
        yrkesaktivitetRepository.lagre(yrkesaktivitet)

        val funnetFør = yrkesaktivitetRepository.finn(yrkesaktivitet.id)
        assertNotNull(funnetFør)

        yrkesaktivitetRepository.slett(yrkesaktivitet.id)

        val funnetEtter = yrkesaktivitetRepository.finn(yrkesaktivitet.id)
        assertNull(funnetEtter)
    }
}
