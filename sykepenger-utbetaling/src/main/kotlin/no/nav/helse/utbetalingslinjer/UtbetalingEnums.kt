package no.nav.helse.utbetalingslinjer

import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.KlassekodeDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.util.UUID


enum class Utbetalingstatus {
    NY,
    IKKE_UTBETALT,
    IKKE_GODKJENT,
    OVERFØRT,
    UTBETALT,
    GODKJENT,
    GODKJENT_UTEN_UTBETALING,
    ANNULLERT,
    FORKASTET,
}

enum class Utbetalingtype { UTBETALING, ETTERUTBETALING, ANNULLERING, REVURDERING }

enum class Endringskode {
    NY,
    UEND,
    ENDR,
    ;

    companion object {
        fun gjenopprett(dto: EndringskodeDto) =
            when (dto) {
                EndringskodeDto.ENDR -> ENDR
                EndringskodeDto.NY -> NY
                EndringskodeDto.UEND -> UEND
            }
    }
}

enum class Klassekode(val verdi: String) {
    RefusjonIkkeOpplysningspliktig(verdi = "SPREFAG-IOP"),
    SykepengerArbeidstakerOrdinær(verdi = "SPATORD"),
    SelvstendigNæringsdrivendeOppgavepliktig(verdi = "SPSND-OP"),
    SelvstendigNæringsdrivendeFisker(verdi = "SPSNDFISK"),
    SelvstendigNæringsdrivendeJordbrukOgSkogbruk(verdi = "SPSNDJORD"),
    SelvstendigNæringsdrivendeBarnepasserOppgavepliktig(verdi = "SPSNDDM-OP"),
    ;

    companion object {
        private val map = entries.associateBy(Klassekode::verdi)

        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }

        fun gjenopprett(dto: KlassekodeDto) =
            when (dto) {
                KlassekodeDto.RefusjonIkkeOpplysningspliktig -> RefusjonIkkeOpplysningspliktig
                KlassekodeDto.SykepengerArbeidstakerOrdinær -> SykepengerArbeidstakerOrdinær
                KlassekodeDto.SelvstendigNæringsdrivendeOppgavepliktig -> SelvstendigNæringsdrivendeOppgavepliktig
                KlassekodeDto.SelvstendigNæringsdrivendeBarnepasserOppgavepliktig -> SelvstendigNæringsdrivendeBarnepasserOppgavepliktig
                KlassekodeDto.SelvstendigNæringsdrivendeFisker -> SelvstendigNæringsdrivendeFisker
                KlassekodeDto.SelvstendigNæringsdrivendeJordbrukOgSkogbruk -> SelvstendigNæringsdrivendeJordbrukOgSkogbruk
            }
    }
}

data class UtbetalingView(
    val id: UUID,
    val korrelasjonsId: UUID,
    val periode: Periode,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val arbeidsgiverOppdrag: Oppdrag,
    val personOppdrag: Oppdrag,
    val status: Utbetalingstatus,
    val type: Utbetalingtype,
    val annulleringer: List<UUID>,
    val erAvsluttet: Boolean,
)
