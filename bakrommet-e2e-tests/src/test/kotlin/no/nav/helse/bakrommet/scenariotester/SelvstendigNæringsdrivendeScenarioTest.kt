package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.SelvstendigForsikring
import no.nav.helse.bakrommet.testutils.*
import kotlin.test.Test

class SelvstendigNæringsdrivendeScenarioTest {
    @Test
    fun `enkel selvstendig næringsdrivende`() {
        Scenario(
            listOf(
                Selvstendig(inntekt = SigrunInntekt(700000, 900000, 1000000), dagoversikt = SykAlleDager()),
            ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha direkteutbetaling`(22900)
            `skal ikke ha sammenlikningsgrunnlag`()
        }
    }

    @Test
    fun `enkel selvstendig næringsdrivende 100prosemt forsikring`() {
        Scenario(
            listOf(
                Selvstendig(forsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG, inntekt = SigrunInntekt(700000, 900000, 1000000), dagoversikt = SykAlleDager()),
            ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha direkteutbetaling`(28620)
            `skal ikke ha sammenlikningsgrunnlag`()
        }
    }

    @Test
    fun `enkel selvstendig næringsdrivende alle år under 6g`() {
        Scenario(
            listOf(
                Selvstendig(inntekt = SigrunInntekt(400000, 400000, 400000), dagoversikt = SykAlleDager()),
            ),
        ).run {
            `skal ha sykepengegrunnlag`(450823.0)
            `skal ha direkteutbetaling`(13870)
            `skal ikke ha sammenlikningsgrunnlag`()
        }
    }

    @Test
    fun `enkel selvstendig næringsdrivende alle år under 6g 4 år rapportert`() {
        Scenario(
            listOf(
                Selvstendig(inntekt = SigrunInntekt(400000, 400000, 400000, 400000), dagoversikt = SykAlleDager()),
            ),
        ).run {
            `skal ha sykepengegrunnlag`(450823.0)
            `skal ha direkteutbetaling`(13870)
            `skal ikke ha sammenlikningsgrunnlag`()
        }
    }

    @Test
    fun `Fjoråret er ikke liknet, men har 3 år før det`() {
        Scenario(
            listOf(
                Selvstendig(inntekt = SigrunInntekt(400000, 400000, 400000, null), dagoversikt = SykAlleDager()),
            ),
        ).run {
            `skal ha sykepengegrunnlag`(472527.0)
            `skal ha direkteutbetaling`(14540)
        }
    }

    @Test
    fun `enkel selvstendig næringsdrivende alle år under 6g 4 år rapportert, 0 for 2 år siden`() {
        Scenario(
            listOf(
                Selvstendig(inntekt = SigrunInntekt(400000, 400000, 0, 400000), dagoversikt = SykAlleDager()),
            ),
        ).run {
            `skal ha sykepengegrunnlag`(300190.0)
            `skal ha direkteutbetaling`(9240)
        }
    }
}
