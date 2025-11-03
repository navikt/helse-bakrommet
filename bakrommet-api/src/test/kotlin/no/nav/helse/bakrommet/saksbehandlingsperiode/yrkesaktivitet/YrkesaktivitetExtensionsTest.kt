package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.bakrommet.BeregningskoderDekningsgrad
import no.nav.helse.bakrommet.testutils.`should equal`
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*

class YrkesaktivitetExtensionsTest {
    @Test
    fun `skal returnere 100 prosent for selvstendig næringsdrivende med 100 prosent fra første sykedag`() {
        val kategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
                forsikring = "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG",
            )

        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitetDbRecord.hentDekningsgrad()

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_100
    }

    @Test
    fun `skal returnere 100 prosent for selvstendig næringsdrivende med 100 prosent fra 17 sykedag`() {
        val kategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
                forsikring = "FORSIKRING_100_PROSENT_FRA_17_SYKEDAG",
            )

        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitetDbRecord.hentDekningsgrad()

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_100
    }

    @Test
    fun `skal returnere 80 prosent for selvstendig næringsdrivende med 80 prosent fra første sykedag`() {
        val kategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
                forsikring = "FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG",
            )

        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitetDbRecord.hentDekningsgrad()

        dekningsgrad.verdi.prosentDesimal `should equal` 0.8
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_80
    }

    @Test
    fun `skal returnere 80 prosent for selvstendig næringsdrivende med ingen forsikring`() {
        val kategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
                forsikring = "INGEN_FORSIKRING",
            )

        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitetDbRecord.hentDekningsgrad()

        dekningsgrad.verdi.prosentDesimal `should equal` 0.8
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.SELVSTENDIG_DEKNINGSGRAD_80
    }

    @Test
    fun `skal returnere 100 prosent for fisker på blad b`() {
        val kategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "FISKER",
            )

        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitetDbRecord.hentDekningsgrad()

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.SELVSTENDIG_KOLLEKTIVFORSIKRING_DEKNINGSGRAD_100
    }

    @Test
    fun `skal returnere 65 prosent for inaktiv variant A`() {
        val kategorisering = inaktivKategorisering(variant = "INAKTIV_VARIANT_A")

        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitetDbRecord.hentDekningsgrad()

        dekningsgrad.verdi.prosentDesimal `should equal` 0.65
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.INAKTIV_DEKNINGSGRAD_65
    }

    @Test
    fun `skal returnere 100 prosent for inaktiv variant B`() {
        val kategorisering = inaktivKategorisering(variant = "INAKTIV_VARIANT_B")

        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitetDbRecord.hentDekningsgrad()

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.INAKTIV_DEKNINGSGRAD_100
    }

    @Test
    fun `skal returnere Prosentdel for arbeidstaker`() {
        val kategorisering = arbeidstakerKategorisering()

        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitetDbRecord.hentDekningsgrad()

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.ARBEIDSTAKER_DEKNINGSGRAD_100
    }

    @Test
    fun `skal returnere Prosentdel for frilanser`() {
        val kategorisering = frilanserKategorisering()

        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitetDbRecord.hentDekningsgrad()

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.FRILANSER_DEKNINGSGRAD_100
    }

    @Test
    fun `skal returnere 100 prosent for arbeidsledig`() {
        val kategorisering =
            HashMap<String, String>().apply {
                put("INNTEKTSKATEGORI", "ARBEIDSLEDIG")
            }

        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = kategorisering.fromMap(),
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val dekningsgrad = yrkesaktivitetDbRecord.hentDekningsgrad()

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.ARBEIDSLEDIG_DEKNINGSGRAD_100
    }

    @Test
    fun `skal returnere forskjellige Prosentdel-objekter for ulike kategorier`() {
        val selvstendigKategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
                forsikring = "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG",
            )
        val arbeidstakerKategorisering = arbeidstakerKategorisering()

        val selvstendigYrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = selvstendigKategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val arbeidstakerYrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = arbeidstakerKategorisering,
                kategoriseringGenerert = null,
                dagoversikt = null,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        val selvstendigDekningsgrad = selvstendigYrkesaktivitetDbRecord.hentDekningsgrad()
        val arbeidstakerDekningsgrad = arbeidstakerYrkesaktivitetDbRecord.hentDekningsgrad()

        selvstendigDekningsgrad.verdi.prosentDesimal `should equal` 1.0
        selvstendigDekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_100

        arbeidstakerDekningsgrad.verdi.prosentDesimal `should equal` 1.0
        arbeidstakerDekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.ARBEIDSTAKER_DEKNINGSGRAD_100
    }
}
