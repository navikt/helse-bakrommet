package no.nav.helse.bakrommet.behandling

import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.person.NaturligIdent
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

val enkelBehandling =
    Behandling(
        id = UUID.randomUUID(),
        naturligIdent = NaturligIdent("01010199999"),
        opprettet = OffsetDateTime.now(),
        opprettetAvNavIdent = "A001122",
        opprettetAvNavn = "A",
        fom = LocalDate.now().minusMonths(1),
        tom = LocalDate.now().minusDays(1),
        skjæringstidspunkt = LocalDate.now().minusMonths(1),
    )

val enkelYrkesaktivitet =
    Yrkesaktivitet(
        id = UUID.randomUUID(),
        kategorisering = YrkesaktivitetKategorisering.Inaktiv(),
        kategoriseringGenerert = null,
        dagoversikt = emptyList(),
        dagoversiktGenerert = null,
        saksbehandlingsperiodeId = UUID.randomUUID(),
        opprettet = OffsetDateTime.now(),
        generertFraDokumenter = emptyList(),
    )
