package no.nav.helse.bakrommet.behandling

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.auth.Rolle
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentHenter
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.db.skapDbDaoer
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.person.NaturligIdent
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class MåVæreUnderBehandlingTest {
    private val dataSource = TestDataSource.dbModule.dataSource
    private lateinit var behandlingService: BehandlingService
    private lateinit var db: DbDaoer<AlleDaoer>

    @BeforeEach
    fun setup() {
        TestDataSource.resetDatasource()
        db = skapDbDaoer(dataSource)
        val dokHenter =
            DokumentHenter(
                db = db,
                soknadClient = SykepengesoknadMock.sykepengersoknadBackendClientMock(),
                inntekterProvider = AInntektMock.aInntektClientMock(fnrTilAInntektResponse = emptyMap()),
                arbeidsforholdProvider = AARegMock.aaRegClientMock(),
                sigrunClient = SigrunMock.sigrunMockClient(),
            )
        behandlingService = BehandlingService(db, dokHenter)
    }

    private val baseBruker =
        Bruker(
            navn = "BasisBruker",
            navIdent = "B000001",
            preferredUsername = "basis bruker",
            roller =
                setOf(
                    Rolle.SAKSBEHANDLER,
                ),
        )

    private fun brukerOgToken(rolle: Rolle = Rolle.SAKSBEHANDLER) =
        BrukerOgToken(
            bruker = baseBruker.copy(roller = setOf(rolle)),
            token = SpilleromBearerToken("tulletoken"),
        )

    @Test
    fun `hentPeriode sin måVæreUnderBehandling-parameter sjekker om er under behandling`() =
        runBlocking {
            val naturligIdent = NaturligIdent("01010199999")
            val fom = LocalDate.now().minusDays(14)
            val tom = LocalDate.now()
            val saksbehandler = brukerOgToken(rolle = Rolle.SAKSBEHANDLER)
            val behandling =
                behandlingService.opprettNyBehandling(
                    naturligIdent = naturligIdent,
                    fom = fom,
                    tom = tom,
                    søknader = setOf(),
                    saksbehandler = saksbehandler,
                )

            val ref = BehandlingReferanse(naturligIdent, behandling.id)

            db.nonTransactional {
                assertNotNull(behandlingDao.hentPeriode(ref, krav = null))
                assertNotNull(behandlingDao.hentPeriode(ref, krav = null, måVæreUnderBehandling = true))
                assertNotNull(behandlingDao.hentPeriode(ref, krav = null, måVæreUnderBehandling = false))
            }

            behandlingService.sendTilBeslutning(ref, "LGTM", saksbehandler.bruker)

            db.nonTransactional {
                assertThrows<InputValideringException> { (behandlingDao.hentPeriode(ref, krav = null)) }
                assertThrows<InputValideringException> {
                    (
                        behandlingDao.hentPeriode(
                            ref,
                            krav = null,
                            måVæreUnderBehandling = true,
                        )
                    )
                }
                assertNotNull(behandlingDao.hentPeriode(ref, krav = null, måVæreUnderBehandling = false))
            }
        }
}
