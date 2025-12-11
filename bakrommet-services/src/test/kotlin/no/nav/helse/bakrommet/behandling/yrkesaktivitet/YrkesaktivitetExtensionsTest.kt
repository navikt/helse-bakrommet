package no.nav.helse.bakrommet.behandling.yrkesaktivitet

import no.nav.helse.bakrommet.BeregningskoderDekningsgrad
import no.nav.helse.bakrommet.behandling.vilkaar.Vilkaarsvurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VilkaarsvurderingUnderspørsmål
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.testutils.`should equal`
import org.junit.jupiter.api.Test
import java.util.*

class YrkesaktivitetExtensionsTest {
    @Test
    fun `skal returnere 100 prosent for selvstendig næringsdrivende med 100 prosent fra første sykedag`() {
        val kategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE",
                forsikring = "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG",
            )

        val dekningsgrad = kategorisering.hentDekningsgrad(emptyList())

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

        val dekningsgrad = kategorisering.hentDekningsgrad(emptyList())

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

        val dekningsgrad = kategorisering.hentDekningsgrad(emptyList())

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

        val dekningsgrad = kategorisering.hentDekningsgrad(emptyList())

        dekningsgrad.verdi.prosentDesimal `should equal` 0.8
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.SELVSTENDIG_DEKNINGSGRAD_80
    }

    @Test
    fun `skal returnere 100 prosent for fisker på blad b`() {
        val kategorisering =
            selvstendigNæringsdrivendeKategorisering(
                type = "FISKER",
            )

        val dekningsgrad = kategorisering.hentDekningsgrad(emptyList())

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.SELVSTENDIG_KOLLEKTIVFORSIKRING_DEKNINGSGRAD_100
    }

    @Test
    fun `skal returnere 65 prosent for inaktiv variant A`() {
        val kategorisering = inaktivKategorisering()

        val dekningsgrad = kategorisering.hentDekningsgrad(emptyList())

        dekningsgrad.verdi.prosentDesimal `should equal` 0.65
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.INAKTIV_DEKNINGSGRAD_65
    }

    @Test
    fun `skal returnere 100 prosent for inaktiv variant B`() {
        val kategorisering = inaktivKategorisering()

        val vilkår =
            listOf(
                VurdertVilkår(
                    kode = "123",
                    vurdering =
                        Vilkaarsvurdering(
                            hovedspørsmål = "1",
                            underspørsmål =
                                listOf(
                                    VilkaarsvurderingUnderspørsmål(
                                        spørsmål = "2",
                                        svar = "UTE_AV_ARBEID_HOVED",
                                    ),
                                ),
                            vurdering = Vurdering.OPPFYLT,
                            notat = "3",
                        ),
                ),
            )
        val dekningsgrad = kategorisering.hentDekningsgrad(vilkår)

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.INAKTIV_DEKNINGSGRAD_100
    }

    @Test
    fun `skal returnere Prosentdel for arbeidstaker`() {
        val kategorisering = arbeidstakerKategorisering()

        val dekningsgrad = kategorisering.hentDekningsgrad(emptyList())

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.ARBEIDSTAKER_DEKNINGSGRAD_100
    }

    @Test
    fun `skal returnere Prosentdel for frilanser`() {
        val kategorisering = frilanserKategorisering()

        val dekningsgrad = kategorisering.hentDekningsgrad(emptyList())

        dekningsgrad.verdi.prosentDesimal `should equal` 1.0
        dekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.FRILANSER_DEKNINGSGRAD_100
    }

    @Test
    fun `skal returnere 100 prosent for arbeidsledig`() {
        val kategorisering = YrkesaktivitetKategorisering.Arbeidsledig()

        val dekningsgrad = kategorisering.hentDekningsgrad(emptyList())

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

        val selvstendigDekningsgrad = selvstendigKategorisering.hentDekningsgrad(emptyList())
        val arbeidstakerDekningsgrad = arbeidstakerKategorisering.hentDekningsgrad(emptyList())

        selvstendigDekningsgrad.verdi.prosentDesimal `should equal` 1.0
        selvstendigDekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_100

        arbeidstakerDekningsgrad.verdi.prosentDesimal `should equal` 1.0
        arbeidstakerDekningsgrad.sporing `should equal` BeregningskoderDekningsgrad.ARBEIDSTAKER_DEKNINGSGRAD_100
    }
}
