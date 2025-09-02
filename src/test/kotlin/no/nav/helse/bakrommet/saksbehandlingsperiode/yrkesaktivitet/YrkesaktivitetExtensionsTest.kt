package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.bakrommet.testutils.`should equal`
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*

class YrkesaktivitetExtensionsTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun `skal returnere 80 prosent for selvstendig næringsdrivende uten forsikring`() {
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
    fun `skal returnere 100 prosent for selvstendig næringsdrivende med 100 prosent fra første sykedag`() {
        val kategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "SELVSTENDIG_NÆRINGSDRIVENDE")
                put("SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING", "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG")
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
    fun `skal returnere 100 prosent for selvstendig næringsdrivende med 100 prosent fra 17 sykedag`() {
        val kategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "SELVSTENDIG_NÆRINGSDRIVENDE")
                put("SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING", "FORSIKRING_100_PROSENT_FRA_17_SYKEDAG")
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
    fun `skal returnere 80 prosent for selvstendig næringsdrivende med 80 prosent fra første sykedag`() {
        val kategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "SELVSTENDIG_NÆRINGSDRIVENDE")
                put("SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING", "FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG")
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
    fun `skal returnere 80 prosent for selvstendig næringsdrivende med ingen forsikring`() {
        val kategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "SELVSTENDIG_NÆRINGSDRIVENDE")
                put("SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING", "INGEN_FORSIKRING")
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
    fun `skal returnere 80 prosent for selvstendig næringsdrivende med ukjent forsikringstype`() {
        val kategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "SELVSTENDIG_NÆRINGSDRIVENDE")
                put("SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING", "UKJENT_FORSIKRING")
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
    fun `skal returnere 100 prosent for inaktiv uten variant`() {
        val kategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "INAKTIV")
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
    fun `skal returnere 65 prosent for inaktiv variant A`() {
        val kategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "INAKTIV")
                put("VARIANT_AV_INAKTIV", "INAKTIV_VARIANT_A")
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

        dekningsgrad.toDouble() `should equal` 65.0
    }

    @Test
    fun `skal returnere 100 prosent for inaktiv variant B`() {
        val kategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "INAKTIV")
                put("VARIANT_AV_INAKTIV", "INAKTIV_VARIANT_B")
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
    fun `skal returnere 100 prosent for inaktiv med ukjent variant`() {
        val kategorisering =
            objectMapper.createObjectNode().apply {
                put("INNTEKTSKATEGORI", "INAKTIV")
                put("VARIANT_AV_INAKTIV", "UKJENT_VARIANT")
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
                put("SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING", "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG")
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

        selvstendigDekningsgrad.toDouble() `should equal` 100.0
        arbeidstakerDekningsgrad.toDouble() `should equal` 100.0
    }
}
