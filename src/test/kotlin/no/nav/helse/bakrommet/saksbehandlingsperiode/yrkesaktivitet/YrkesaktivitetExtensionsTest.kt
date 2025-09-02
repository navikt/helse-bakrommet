package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.bakrommet.testutils.`should equal`
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*

class YrkesaktivitetExtensionsTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun `skal returnere Prosentdel for selvstendig næringsdrivende`() {
        val kategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "SELVSTENDIG_NÆRINGSDRIVENDE")
            }

        val yrkesaktivitet =
            Yrkesaktivitet(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitet.hentDekningsgrad()

        dekningsgrad.toDouble() `should equal` 80.0
    }

    @Test
    fun `skal returnere Prosentdel for arbeidstaker`() {
        val kategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "ARBEIDSTAKER")
            }

        val yrkesaktivitet =
            Yrkesaktivitet(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitet.hentDekningsgrad()

        dekningsgrad.toDouble() `should equal` 100.0
    }

    @Test
    fun `skal returnere Prosentdel for frilanser`() {
        val kategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "FRILANSER")
            }

        val yrkesaktivitet =
            Yrkesaktivitet(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitet.hentDekningsgrad()

        dekningsgrad.toDouble() `should equal` 100.0
    }

    @Test
    fun `skal returnere Prosentdel når INNTEKTSKATEGORI ikke er satt`() {
        val kategorisering = objectMapper.createObjectNode()

        val yrkesaktivitet =
            Yrkesaktivitet(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitet.hentDekningsgrad()
        dekningsgrad.toDouble() `should equal` 100.0
    }

    @Test
    fun `skal returnere forskjellige Prosentdel-objekter for ulike kategorier`() {
        val selvstendigKategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "SELVSTENDIG_NÆRINGSDRIVENDE")
            }
        val arbeidstakerKategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "ARBEIDSTAKER")
            }

        val selvstendigYrkesaktivitet =
            Yrkesaktivitet(
                id = UUID.randomUUID(),
                kategorisering = selvstendigKategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val arbeidstakerYrkesaktivitet =
            Yrkesaktivitet(
                id = UUID.randomUUID(),
                kategorisering = arbeidstakerKategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val selvstendigDekningsgrad = selvstendigYrkesaktivitet.hentDekningsgrad()
        val arbeidstakerDekningsgrad = arbeidstakerYrkesaktivitet.hentDekningsgrad()

        selvstendigDekningsgrad.toDouble() `should equal` 80.0
        arbeidstakerDekningsgrad.toDouble() `should equal` 100.0
    }
}
