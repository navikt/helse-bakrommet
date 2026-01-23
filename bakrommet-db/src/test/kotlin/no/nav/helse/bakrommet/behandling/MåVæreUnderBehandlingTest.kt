package no.nav.helse.bakrommet.behandling

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentHenter
import no.nav.helse.bakrommet.db.DBTestFixture
import no.nav.helse.bakrommet.db.skapDbDaoer
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.domain.Rolle
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.enNavIdent
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class MåVæreUnderBehandlingTest {
    private val dataSource = DBTestFixture.module.dataSource
    private val db: DbDaoer<AlleDaoer> = skapDbDaoer(dataSource)
    private val dokHenter =
        DokumentHenter(
            db = db,
            soknadClient = SykepengesoknadMock.sykepengersoknadBackendClientMock(),
            inntekterProvider = AInntektMock.aInntektClientMock(fnrTilAInntektResponse = emptyMap()),
            arbeidsforholdProvider = AARegMock.aaRegClientMock(),
            pensjonsgivendeInntektProvider = SigrunMock.sigrunMockClient(),
        )
    private val behandlingService: BehandlingService = BehandlingService(db, dokHenter)

    private fun brukerOgToken() =
        BrukerOgToken(
            bruker =
                Bruker(
                    navn = "BasisBruker",
                    navIdent = enNavIdent(),
                    preferredUsername = "basis bruker",
                    roller =
                        setOf(
                            Rolle.SAKSBEHANDLER,
                        ),
                ),
            token = AccessToken("tulletoken"),
        )

    @Test
    fun `hentPeriode sin måVæreUnderBehandling-parameter sjekker om er under behandling`() =
        runBlocking {
            val naturligIdent = enNaturligIdent()
            val fom = LocalDate.now().minusDays(14)
            val tom = LocalDate.now()
            val saksbehandler = brukerOgToken()
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
