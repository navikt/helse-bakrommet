package no.nav.helse.bakrommet.behandling

import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDbRecord
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Dagoversikt
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.person.NaturligIdent
import no.nav.helse.dto.InntektbeløpDto
import java.time.Instant
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
        dagoversikt = Dagoversikt(),
        dagoversiktGenerert = null,
        behandlingId = UUID.randomUUID(),
        opprettet = OffsetDateTime.now(),
        generertFraDokumenter = emptyList(),
    )

fun skapSykepengegrunnlag(
    grunnlag: Double,
    g: Double,
): SykepengegrunnlagDbRecord =
    SykepengegrunnlagDbRecord(
        sykepengegrunnlag =
            Sykepengegrunnlag(
                sykepengegrunnlag = InntektbeløpDto.Årlig(grunnlag),
                grunnbeløp = InntektbeløpDto.Årlig(g),
                seksG = InntektbeløpDto.Årlig(6 * g),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.now().minusYears(1),
                beregningsgrunnlag = InntektbeløpDto.Årlig(grunnlag),
                næringsdel = null,
                kombinertBeregningskode = null,
            ),
        sammenlikningsgrunnlag = null,
        id = UUID.randomUUID(),
        opprettetAv = "T",
        opprettet = Instant.now(),
        oppdatert = Instant.now(),
        opprettetForBehandling = UUID.randomUUID(),
        låst = false,
    )
