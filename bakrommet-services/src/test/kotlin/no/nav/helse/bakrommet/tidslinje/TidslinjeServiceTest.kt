package no.nav.helse.bakrommet.tidslinje

import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetForenkletDbRecord
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.TypeArbeidstaker
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.ereg.Organisasjon
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TidslinjeServiceTest {
    private val revurdertBehandlingId = UUID.fromString("45b8a5a5-abf0-4edc-be9a-94ad0475c6f4")
    private val revurdererBehandlingId = UUID.fromString("c859e52b-6ed7-47fe-9c6a-61b98a6eb446")
    private val yrkesaktivitet1Id = UUID.fromString("7a9186bb-ee78-4a43-a5c7-441ea22ab14a")
    private val yrkesaktivitet2Id = UUID.fromString("5cf02940-f728-4170-96c3-a379c424c01b")
    private val orgnummer = "834567890"

    @Test
    fun `revurdert behandling skal ligge i historiske listen til den som revurderer den`() {
        // Gitt: En behandling som revurderer en annen behandling
        val revurdertBehandling =
            Behandling(
                id = revurdertBehandlingId,
                spilleromPersonId = "5433d",
                opprettet = OffsetDateTime.parse("2025-11-20T20:12:45.629967+01:00"),
                opprettetAvNavIdent = "Z123456",
                opprettetAvNavn = "Saks McBehandlersen",
                fom = LocalDate.parse("2025-09-29"),
                tom = LocalDate.parse("2025-10-26"),
                status = BehandlingStatus.GODKJENT,
                beslutterNavIdent = "K111222",
                skjæringstidspunkt = LocalDate.parse("2025-09-29"),
                revurdererSaksbehandlingsperiodeId = null,
                revurdertAvBehandlingId = null,
            )

        val revurdererBehandling =
            Behandling(
                id = revurdererBehandlingId,
                spilleromPersonId = "5433d",
                opprettet = OffsetDateTime.parse("2025-11-20T20:13:05.868922+01:00"),
                opprettetAvNavIdent = "K111222",
                opprettetAvNavn = "Kai Kombinator",
                fom = LocalDate.parse("2025-09-29"),
                tom = LocalDate.parse("2025-10-26"),
                status = BehandlingStatus.UNDER_BEHANDLING,
                beslutterNavIdent = "K111222",
                skjæringstidspunkt = LocalDate.parse("2025-09-29"),
                revurdererSaksbehandlingsperiodeId = revurdertBehandlingId,
                revurdertAvBehandlingId = null,
            )

        val kategorisering =
            YrkesaktivitetKategorisering.Arbeidstaker(
                sykmeldt = true,
                typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = orgnummer),
            )

        val yrkesaktivitet1 =
            YrkesaktivitetForenkletDbRecord(
                id = yrkesaktivitet1Id,
                kategorisering = kategorisering,
                behandlingId = revurdertBehandlingId,
            )

        val yrkesaktivitet2 =
            YrkesaktivitetForenkletDbRecord(
                id = yrkesaktivitet2Id,
                kategorisering = kategorisering,
                behandlingId = revurdererBehandlingId,
            )

        val organisasjonsnavnMap = mapOf(orgnummer to Organisasjon(navn = "Test Arbeidsgiver AS", orgnummer = orgnummer))

        val tidslinjeData =
            TidslinjeData(
                behandlinger = listOf(revurdertBehandling, revurdererBehandling),
                yrkesaktiviteter = listOf(yrkesaktivitet1, yrkesaktivitet2),
                tilkommen = emptyList(),
                organisasjonsnavnMap = organisasjonsnavnMap,
            )

        // Når: Vi kaller tilTidslinje() direkte på dataene
        val tidslinje = tidslinjeData.tilTidslinje()

        // Så: Den revurderte behandlingen skal ligge i historiske-listen til den som revurderer den
        val sykmeldtYrkesaktivitetRad =
            tidslinje.find { it is TidslinjeRad.SykmeldtYrkesaktivitet && it.id == orgnummer }
        assertNotNull(sykmeldtYrkesaktivitetRad, "Skal finne SykmeldtYrkesaktivitet-rad")

        val sykmeldtYrkesaktivitet = sykmeldtYrkesaktivitetRad as TidslinjeRad.SykmeldtYrkesaktivitet
        assertEquals(1, sykmeldtYrkesaktivitet.tidslinjeElementer.size, "Skal ha ett hovedelement")

        val hovedElement = sykmeldtYrkesaktivitet.tidslinjeElementer.first()
        assertEquals(revurdererBehandlingId, hovedElement.behandlingId, "Hovedelementet skal være den som revurderer")
        assertEquals(revurdertBehandlingId, hovedElement.revurdererBehandlingId, "Hovedelementet skal revurdere den revurderte behandlingen")

        assertTrue(
            hovedElement.historiske.isNotEmpty(),
            "Historiske-listen skal ikke være tom",
        )

        val historiskElement =
            hovedElement.historiske.find { it.behandlingId == revurdertBehandlingId }
        assertNotNull(
            historiskElement,
            "Den revurderte behandlingen skal ligge i historiske-listen",
        )
        assertEquals(
            revurdertBehandlingId,
            historiskElement.behandlingId,
            "Historisk element skal ha riktig behandlingId",
        )
        assertTrue(
            historiskElement.historisk,
            "Historisk element skal være markert som historisk",
        )
    }
}
