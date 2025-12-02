package no.nav.helse.bakrommet.behandling.utbetalingsberegning

import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.TypeSelvstendigNæringsdrivende
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.utbetalingslinjer.Klassekode

fun YrkesaktivitetKategorisering.tilKlassekode(): Klassekode {
    return when (this) {
        is YrkesaktivitetKategorisering.Arbeidstaker -> Klassekode.SykepengerArbeidstakerOrdinær
        is YrkesaktivitetKategorisering.Arbeidsledig -> Klassekode.SykepengerArbeidstakerOrdinær // TODO denne har vi ikke klassekode for enda
        is YrkesaktivitetKategorisering.Frilanser -> Klassekode.SykepengerArbeidstakerOrdinær // TODO denne har vi ikke klassekode for enda
        is YrkesaktivitetKategorisering.Inaktiv -> Klassekode.SykepengerArbeidstakerOrdinær // TODO denne har vi ikke klassekode for enda
        is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> {
            return when (this.typeSelvstendigNæringsdrivende) {
                is TypeSelvstendigNæringsdrivende.Ordinær -> Klassekode.SelvstendigNæringsdrivendeOppgavepliktig
                is TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem -> Klassekode.SelvstendigNæringsdrivendeBarnepasserOppgavepliktig
                is TypeSelvstendigNæringsdrivende.Fisker -> Klassekode.SelvstendigNæringsdrivendeFisker
                is TypeSelvstendigNæringsdrivende.Jordbruker -> Klassekode.SelvstendigNæringsdrivendeJordbrukOgSkogbruk
                is TypeSelvstendigNæringsdrivende.Reindrift -> Klassekode.SelvstendigNæringsdrivendeJordbrukOgSkogbruk // TODO er denne riktig?
            }
        }
    }
}
