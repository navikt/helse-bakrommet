package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.Beregningssporing
import no.nav.helse.bakrommet.testutils.`should equal`
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import java.util.*

class YrkesaktivitetExtensionsTest {
    @Test
    fun `skal kaste feil for selvstendig næringsdrivende uten forsikring`() {
        val kategorisering =
            HashMap<String, String>().apply {
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

        val exception =
            assertThrows<InputValideringException> {
                yrkesaktivitet.hentDekningsgrad()
            }

        assertEquals("ER_SYKMELDT mangler for SELVSTENDIG_NÆRINGSDRIVENDE", exception.message)
    }

    @Test
    fun `skal returnere 100 prosent for selvstendig næringsdrivende med 100 prosent fra første sykedag`() {
        val kategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
                forsikring = "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG",
            )

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

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` Beregningssporing.ORDINAER_SELVSTENDIG_NAVFORSIKRING_100
    }

    @Test
    fun `skal returnere 100 prosent for selvstendig næringsdrivende med 100 prosent fra 17 sykedag`() {
        val kategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
                forsikring = "FORSIKRING_100_PROSENT_FRA_17_SYKEDAG",
            )

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

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` Beregningssporing.ORDINAER_SELVSTENDIG_NAVFORSIKRING_100
    }

    @Test
    fun `skal returnere 80 prosent for selvstendig næringsdrivende med 80 prosent fra første sykedag`() {
        val kategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
                forsikring = "FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG",
            )

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

        dekningsgrad.verdi.prosentDesimal `should equal` 0.8
        dekningsgrad.sporing `should equal` Beregningssporing.ORDINAER_SELVSTENDIG_80
    }

    @Test
    fun `skal returnere 80 prosent for selvstendig næringsdrivende med ingen forsikring`() {
        val kategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
                forsikring = "INGEN_FORSIKRING",
            )

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

        dekningsgrad.verdi.prosentDesimal `should equal` 0.8
        dekningsgrad.sporing `should equal` Beregningssporing.ORDINAER_SELVSTENDIG_80
    }

    @Test
    fun `skal returnere 100 prosent for fisker på blad b`() {
        val kategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "FISKER",
            )

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

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` Beregningssporing.SELVSTENDIG_KOLLEKTIVFORSIKRING_100
    }

    @Test
    fun `skal kaste feil for inaktiv uten variant`() {
        val kategorisering =
            HashMap<String, String>().apply {
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

        val exception =
            assertThrows<InputValideringException> {
                yrkesaktivitet.hentDekningsgrad()
            }

        assertEquals("VARIANT_AV_INAKTIV mangler for INAKTIV", exception.message)
    }

    @Test
    fun `skal returnere 65 prosent for inaktiv variant A`() {
        val kategorisering = inaktivKategorisering(variant = "INAKTIV_VARIANT_A")

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

        dekningsgrad.verdi.prosentDesimal `should equal` 0.65
        dekningsgrad.sporing `should equal` Beregningssporing.INAKTIV_65
    }

    @Test
    fun `skal returnere 100 prosent for inaktiv variant B`() {
        val kategorisering = inaktivKategorisering(variant = "INAKTIV_VARIANT_B")

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

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` Beregningssporing.INAKTIV_100
    }

    @Test
    fun `skal returnere Prosentdel for arbeidstaker`() {
        val kategorisering = arbeidstakerKategorisering()

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

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` Beregningssporing.ARBEIDSTAKER_100
    }

    @Test
    fun `skal returnere Prosentdel for frilanser`() {
        val kategorisering = frilanserKategorisering()

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

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` Beregningssporing.FRILANSER_100
    }

    @Test
    fun `skal returnere 100 prosent for arbeidsledig`() {
        val kategorisering =
            HashMap<String, String>().apply {
                put("INNTEKTSKATEGORI", "ARBEIDSLEDIG")
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

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` Beregningssporing.DAGPENGEMOTTAKER_100
    }

    @Test
    fun `skal kaste feil for ukjent forsikringstype for selvstendig næringsdrivende`() {
        val kategorisering =
            HashMap<String, String>().apply {
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

        val exception =
            assertThrows<InputValideringException> {
                yrkesaktivitet.hentDekningsgrad()
            }

        assertEquals("ER_SYKMELDT mangler for SELVSTENDIG_NÆRINGSDRIVENDE", exception.message)
    }

    @Test
    fun `skal kaste feil for ukjent variant for inaktiv`() {
        val kategorisering =
            HashMap<String, String>().apply {
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

        val exception =
            assertThrows<InputValideringException> {
                yrkesaktivitet.hentDekningsgrad()
            }

        assertEquals("Ugyldig VARIANT_AV_INAKTIV: UKJENT_VARIANT", exception.message)
    }

    @Test
    fun `skal kaste feil for ukjent inntektskategori`() {
        val kategorisering =
            HashMap<String, String>().apply {
                put("INNTEKTSKATEGORI", "UKJENT_KATEGORI")
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

        val exception =
            assertThrows<InputValideringException> {
                yrkesaktivitet.hentDekningsgrad()
            }

        assertEquals("Ugyldig INNTEKTSKATEGORI: UKJENT_KATEGORI", exception.message)
    }

    @Test
    fun `skal kaste feil når INNTEKTSKATEGORI ikke er satt`() {
        val kategorisering = HashMap<String, String>()

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

        val exception =
            assertThrows<InputValideringException> {
                yrkesaktivitet.hentDekningsgrad()
            }

        assertEquals("INNTEKTSKATEGORI mangler", exception.message)
    }

    @Test
    fun `skal returnere forskjellige Prosentdel-objekter for ulike kategorier`() {
        val selvstendigKategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
                forsikring = "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG",
            )
        val arbeidstakerKategorisering = arbeidstakerKategorisering()

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

        selvstendigDekningsgrad.verdi.prosentDesimal `should equal` 1.0
        selvstendigDekningsgrad.sporing `should equal` Beregningssporing.ORDINAER_SELVSTENDIG_NAVFORSIKRING_100

        arbeidstakerDekningsgrad.verdi.prosentDesimal `should equal` 1.0
        arbeidstakerDekningsgrad.sporing `should equal` Beregningssporing.ARBEIDSTAKER_100
    }
}
