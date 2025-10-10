package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.bakrommet.errorhandling.InputValideringException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class YrkesaktivitetKategoriseringMapperTest {
    @Test
    fun `arbeidstaker round-trip mapping bevarer alle felter`() {
        val original =
            YrkesaktivitetKategorisering.Arbeidstaker(
                orgnummer = "999888777",
                sykmeldt = true,
                typeArbeidstaker = TypeArbeidstaker.ORDINÆRT_ARBEIDSFORHOLD,
            )

        val map = YrkesaktivitetKategoriseringMapper.toMap(original)
        val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

        assertEquals(original, resultat)
    }

    @Test
    fun `arbeidstaker round-trip med sykmeldt=false`() {
        val original =
            YrkesaktivitetKategorisering.Arbeidstaker(
                orgnummer = "123456789",
                sykmeldt = false,
                typeArbeidstaker = TypeArbeidstaker.MARITIMT_ARBEIDSFORHOLD,
            )

        val map = YrkesaktivitetKategoriseringMapper.toMap(original)
        val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

        assertEquals(original, resultat)
    }

    @Test
    fun `frilanser round-trip mapping bevarer alle felter`() {
        val original =
            YrkesaktivitetKategorisering.Frilanser(
                orgnummer = "555444333",
                sykmeldt = true,
                forsikring = FrilanserForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
            )

        val map = YrkesaktivitetKategoriseringMapper.toMap(original)
        val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

        assertEquals(original, resultat)
    }

    @Test
    fun `frilanser round-trip med ulike forsikringer`() {
        FrilanserForsikring.entries.forEach { forsikring ->
            val original =
                YrkesaktivitetKategorisering.Frilanser(
                    orgnummer = "123456789",
                    sykmeldt = true,
                    forsikring = forsikring,
                )

            val map = YrkesaktivitetKategoriseringMapper.toMap(original)
            val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

            assertEquals(original, resultat, "Forsikring $forsikring skal bevares")
        }
    }

    @Test
    fun `selvstendig næringsdrivende ordinær round-trip`() {
        val original =
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = true,
                type =
                    TypeSelvstendigNæringsdrivende.Ordinær(
                        forsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
                    ),
            )

        val map = YrkesaktivitetKategoriseringMapper.toMap(original)
        val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

        assertEquals(original, resultat)
    }

    @Test
    fun `selvstendig næringsdrivende fisker round-trip`() {
        val original =
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = true,
                type = TypeSelvstendigNæringsdrivende.Fisker(),
            )

        val map = YrkesaktivitetKategoriseringMapper.toMap(original)
        val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

        assertEquals(original, resultat)
    }

    @Test
    fun `selvstendig næringsdrivende jordbruker round-trip med alle forsikringer`() {
        SelvstendigForsikring.entries.forEach { forsikring ->
            val original =
                YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                    sykmeldt = true,
                    type =
                        TypeSelvstendigNæringsdrivende.Jordbruker(
                            forsikring = forsikring,
                        ),
                )

            val map = YrkesaktivitetKategoriseringMapper.toMap(original)
            val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

            assertEquals(original, resultat, "Jordbruker forsikring $forsikring skal bevares")
        }
    }

    @Test
    fun `selvstendig næringsdrivende reindrift round-trip`() {
        val original =
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = false,
                type =
                    TypeSelvstendigNæringsdrivende.Reindrift(
                        forsikring = SelvstendigForsikring.INGEN_FORSIKRING,
                    ),
            )

        val map = YrkesaktivitetKategoriseringMapper.toMap(original)
        val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

        assertEquals(original, resultat)
    }

    @Test
    fun `selvstendig næringsdrivende barnepasser round-trip`() {
        val original =
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = true,
                type =
                    TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem(
                        forsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
                    ),
            )

        val map = YrkesaktivitetKategoriseringMapper.toMap(original)
        val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

        assertEquals(original, resultat)
    }

    @Test
    fun `inaktiv round-trip med begge varianter`() {
        listOf(VariantAvInaktiv.INAKTIV_VARIANT_A, VariantAvInaktiv.INAKTIV_VARIANT_B).forEach { variant ->
            val original =
                YrkesaktivitetKategorisering.Inaktiv(
                    variant = variant,
                )

            val map = YrkesaktivitetKategoriseringMapper.toMap(original)
            val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

            assertEquals(original, resultat, "Variant $variant skal bevares")
            assertEquals(true, resultat.sykmeldt, "Inaktiv skal alltid være sykmeldt")
        }
    }

    @Test
    fun `arbeidsledig round-trip`() {
        val original = YrkesaktivitetKategorisering.Arbeidsledig()

        val map = YrkesaktivitetKategoriseringMapper.toMap(original)
        val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

        assertEquals(original, resultat)
        assertEquals(true, resultat.sykmeldt, "Arbeidsledig skal alltid være sykmeldt")
    }

    @Test
    fun `validering feiler når INNTEKTSKATEGORI mangler`() {
        val map = mapOf<String, String>()

        val exception =
            assertThrows<InputValideringException> {
                YrkesaktivitetKategoriseringMapper.fromMap(map)
            }

        assertEquals("INNTEKTSKATEGORI mangler", exception.message)
    }

    @Test
    fun `validering feiler når INNTEKTSKATEGORI er ugyldig`() {
        val map = mapOf("INNTEKTSKATEGORI" to "UGYLDIG_KATEGORI")

        val exception =
            assertThrows<InputValideringException> {
                YrkesaktivitetKategoriseringMapper.fromMap(map)
            }

        assertEquals("Ugyldig INNTEKTSKATEGORI: UGYLDIG_KATEGORI", exception.message)
    }

    @Test
    fun `validering feiler når ORGNUMMER mangler for arbeidstaker`() {
        val map =
            mapOf(
                "INNTEKTSKATEGORI" to "ARBEIDSTAKER",
                "ER_SYKMELDT" to "ER_SYKMELDT_JA",
                "TYPE_ARBEIDSTAKER" to "ORDINÆRT_ARBEIDSFORHOLD",
            )

        val exception =
            assertThrows<InputValideringException> {
                YrkesaktivitetKategoriseringMapper.fromMap(map)
            }

        assertEquals("ORGNUMMER mangler for ARBEIDSTAKER", exception.message)
    }

    @Test
    fun `validering feiler når TYPE_ARBEIDSTAKER mangler`() {
        val map =
            mapOf(
                "INNTEKTSKATEGORI" to "ARBEIDSTAKER",
                "ORGNUMMER" to "123456789",
                "ER_SYKMELDT" to "ER_SYKMELDT_JA",
            )

        val exception =
            assertThrows<InputValideringException> {
                YrkesaktivitetKategoriseringMapper.fromMap(map)
            }

        assertEquals("TYPE_ARBEIDSTAKER mangler for ARBEIDSTAKER", exception.message)
    }

    @Test
    fun `validering feiler når ER_SYKMELDT mangler for arbeidstaker`() {
        val map =
            mapOf(
                "INNTEKTSKATEGORI" to "ARBEIDSTAKER",
                "ORGNUMMER" to "123456789",
                "TYPE_ARBEIDSTAKER" to "ORDINÆRT_ARBEIDSFORHOLD",
            )

        val exception =
            assertThrows<InputValideringException> {
                YrkesaktivitetKategoriseringMapper.fromMap(map)
            }

        assertEquals("ER_SYKMELDT mangler for ARBEIDSTAKER", exception.message)
    }

    @Test
    fun `validering feiler når ORGNUMMER mangler for frilanser`() {
        val map =
            mapOf(
                "INNTEKTSKATEGORI" to "FRILANSER",
                "ER_SYKMELDT" to "ER_SYKMELDT_JA",
                "FRILANSER_FORSIKRING" to "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG",
            )

        val exception =
            assertThrows<InputValideringException> {
                YrkesaktivitetKategoriseringMapper.fromMap(map)
            }

        assertEquals("ORGNUMMER mangler for FRILANSER", exception.message)
    }

    @Test
    fun `validering feiler når FRILANSER_FORSIKRING mangler`() {
        val map =
            mapOf(
                "INNTEKTSKATEGORI" to "FRILANSER",
                "ORGNUMMER" to "123456789",
                "ER_SYKMELDT" to "ER_SYKMELDT_JA",
            )

        val exception =
            assertThrows<InputValideringException> {
                YrkesaktivitetKategoriseringMapper.fromMap(map)
            }

        assertEquals("FRILANSER_FORSIKRING mangler for FRILANSER", exception.message)
    }

    @Test
    fun `validering feiler når TYPE_SELVSTENDIG_NÆRINGSDRIVENDE mangler`() {
        val map =
            mapOf(
                "INNTEKTSKATEGORI" to "SELVSTENDIG_NÆRINGSDRIVENDE",
                "ER_SYKMELDT" to "ER_SYKMELDT_JA",
                "SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING" to "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG",
            )

        val exception =
            assertThrows<InputValideringException> {
                YrkesaktivitetKategoriseringMapper.fromMap(map)
            }

        assertEquals("TYPE_SELVSTENDIG_NÆRINGSDRIVENDE mangler", exception.message)
    }

    @Test
    fun `validering feiler når VARIANT_AV_INAKTIV mangler`() {
        val map = mapOf("INNTEKTSKATEGORI" to "INAKTIV")

        val exception =
            assertThrows<InputValideringException> {
                YrkesaktivitetKategoriseringMapper.fromMap(map)
            }

        assertEquals("VARIANT_AV_INAKTIV mangler for INAKTIV", exception.message)
    }

    @Test
    fun `validering feiler når VARIANT_AV_INAKTIV er ugyldig`() {
        val map =
            mapOf(
                "INNTEKTSKATEGORI" to "INAKTIV",
                "VARIANT_AV_INAKTIV" to "UGYLDIG_VARIANT",
            )

        val exception =
            assertThrows<InputValideringException> {
                YrkesaktivitetKategoriseringMapper.fromMap(map)
            }

        assertEquals("Ugyldig VARIANT_AV_INAKTIV: UGYLDIG_VARIANT", exception.message)
    }

    @Test
    fun `toMap inkluderer alle nødvendige felter for arbeidstaker`() {
        val kategorisering =
            YrkesaktivitetKategorisering.Arbeidstaker(
                orgnummer = "123456789",
                sykmeldt = true,
                typeArbeidstaker = TypeArbeidstaker.ORDINÆRT_ARBEIDSFORHOLD,
            )

        val map = YrkesaktivitetKategoriseringMapper.toMap(kategorisering)

        assertEquals("ARBEIDSTAKER", map["INNTEKTSKATEGORI"])
        assertEquals("123456789", map["ORGNUMMER"])
        assertEquals("ER_SYKMELDT_JA", map["ER_SYKMELDT"])
        assertEquals("ORDINÆRT_ARBEIDSFORHOLD", map["TYPE_ARBEIDSTAKER"])
    }

    @Test
    fun `toMap inkluderer alle nødvendige felter for frilanser`() {
        val kategorisering =
            YrkesaktivitetKategorisering.Frilanser(
                orgnummer = "987654321",
                sykmeldt = false,
                forsikring = FrilanserForsikring.INGEN_FORSIKRING,
            )

        val map = YrkesaktivitetKategoriseringMapper.toMap(kategorisering)

        assertEquals("FRILANSER", map["INNTEKTSKATEGORI"])
        assertEquals("987654321", map["ORGNUMMER"])
        assertEquals("ER_SYKMELDT_NEI", map["ER_SYKMELDT"])
        assertEquals("INGEN_FORSIKRING", map["FRILANSER_FORSIKRING"])
    }

    @Test
    fun `toMap inkluderer alle nødvendige felter for selvstendig næringsdrivende ordinær`() {
        val kategorisering =
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = true,
                type =
                    TypeSelvstendigNæringsdrivende.Ordinær(
                        forsikring = SelvstendigForsikring.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG,
                    ),
            )

        val map = YrkesaktivitetKategoriseringMapper.toMap(kategorisering)

        assertEquals("SELVSTENDIG_NÆRINGSDRIVENDE", map["INNTEKTSKATEGORI"])
        assertEquals("ER_SYKMELDT_JA", map["ER_SYKMELDT"])
        assertEquals("ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE", map["TYPE_SELVSTENDIG_NÆRINGSDRIVENDE"])
        assertEquals("FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG", map["SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING"])
    }

    @Test
    fun `toMap inkluderer alle nødvendige felter for inaktiv`() {
        val kategorisering =
            YrkesaktivitetKategorisering.Inaktiv(
                variant = VariantAvInaktiv.INAKTIV_VARIANT_B,
            )

        val map = YrkesaktivitetKategoriseringMapper.toMap(kategorisering)

        assertEquals("INAKTIV", map["INNTEKTSKATEGORI"])
        assertEquals("INAKTIV_VARIANT_B", map["VARIANT_AV_INAKTIV"])
        // Inaktiv har alltid sykmeldt=true som default, men vi lagrer det ikke nødvendigvis
    }

    @Test
    fun `toMap inkluderer alle nødvendige felter for arbeidsledig`() {
        val kategorisering = YrkesaktivitetKategorisering.Arbeidsledig()

        val map = YrkesaktivitetKategoriseringMapper.toMap(kategorisering)

        assertEquals("ARBEIDSLEDIG", map["INNTEKTSKATEGORI"])
        // Arbeidsledig har alltid sykmeldt=true som default
    }

    @Test
    fun `alle TypeArbeidstaker verdier kan round-trippe`() {
        TypeArbeidstaker.entries.forEach { type ->
            val original =
                YrkesaktivitetKategorisering.Arbeidstaker(
                    orgnummer = "123456789",
                    sykmeldt = true,
                    typeArbeidstaker = type,
                )

            val map = YrkesaktivitetKategoriseringMapper.toMap(original)
            val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

            assertEquals(original, resultat, "TypeArbeidstaker $type skal bevares")
        }
    }

    @Test
    fun `alle SelvstendigForsikring verdier kan round-trippe`() {
        SelvstendigForsikring.entries.forEach { forsikring ->
            val original =
                YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                    sykmeldt = true,
                    type =
                        TypeSelvstendigNæringsdrivende.Ordinær(
                            forsikring = forsikring,
                        ),
                )

            val map = YrkesaktivitetKategoriseringMapper.toMap(original)
            val resultat = YrkesaktivitetKategoriseringMapper.fromMap(map)

            assertEquals(original, resultat, "SelvstendigForsikring $forsikring skal bevares")
        }
    }
}
