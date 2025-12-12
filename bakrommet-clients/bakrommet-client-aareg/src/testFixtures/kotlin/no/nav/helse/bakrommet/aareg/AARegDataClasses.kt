package no.nav.helse.bakrommet.aareg

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Dataklasser for AA-Reg arbeidsforhold basert på Zod skjemaet i spillerom.
 */

data class ArbeidsforholdType(
    val kode: String,
    val beskrivelse: String,
)

data class Ident(
    val type: String,
    val ident: String,
    val gjeldende: Boolean? = null,
)

data class Arbeidssted(
    val type: String,
    val identer: List<Ident>,
)

data class KodeBeskrivelse(
    val kode: String,
    val beskrivelse: String,
)

data class Rapporteringsmaaneder(
    val fra: String,
    val til: String? = null,
)

data class Ansettelsesdetaljer(
    val type: String,
    val arbeidstidsordning: KodeBeskrivelse? = null,
    val ansettelsesform: KodeBeskrivelse? = null,
    val yrke: KodeBeskrivelse? = null,
    val antallTimerPrUke: Double? = null,
    val avtaltStillingsprosent: Double? = null,
    val rapporteringsmaaneder: Rapporteringsmaaneder? = null,
    val fartsomraade: KodeBeskrivelse? = null,
    val skipsregister: KodeBeskrivelse? = null,
    val fartoeystype: KodeBeskrivelse? = null,
    @param:JsonProperty("sisteStillingsprosentendring") val sisteStillingsprosentendring: String? = null,
    @param:JsonProperty("sisteLoennsendring") val sisteLoennsendring: String? = null,
)

data class Rapporteringsordning(
    val kode: String,
    val beskrivelse: String,
)

data class Bruksperiode(
    val fom: String,
    val tom: String? = null,
)

data class Ansettelsesperiode(
    val startdato: String,
    val sluttdato: String? = null,
)

data class Arbeidstaker(
    val identer: List<Ident>,
)

data class Arbeidsforhold(
    val id: String? = null,
    val type: ArbeidsforholdType,
    val arbeidstaker: Arbeidstaker,
    val arbeidssted: Arbeidssted,
    val opplysningspliktig: Arbeidssted,
    val ansettelsesperiode: Ansettelsesperiode,
    val ansettelsesdetaljer: List<Ansettelsesdetaljer>,
    val rapporteringsordning: Rapporteringsordning,
    val navArbeidsforholdId: Int,
    val navVersjon: Int,
    val navUuid: String,
    val opprettet: String,
    val sistBekreftet: String,
    val bruksperiode: Bruksperiode,
    val sistEndret: String? = null,
    val permisjoner: List<Permisjon>? = null,
    val permitteringer: List<Permittering>? = null,
    val innrapportertEtterAOrdningen: Boolean? = null,
    val idHistorikk: List<IdHistorikk>? = null,
    val varsler: List<Varsel>? = null,
    val utenlandsopphold: List<Utenlandsopphold>? = null,
    val timerMedTimeloenn: List<TimerMedTimeloenn>? = null,
)

data class Permisjon(
    val id: String,
    val type: ArbeidsforholdType,
    val startdato: String,
    val prosent: Double? = null,
)

data class Permittering(
    val id: String,
    val type: ArbeidsforholdType,
    val startdato: String,
    val prosent: Double? = null,
)

data class IdHistorikk(
    val id: String,
    val bruksperiode: Bruksperiode,
)

data class Varsel(
    val entitet: String,
    val varslingskode: KodeBeskrivelse,
)

data class Utenlandsopphold(
    val landkode: KodeBeskrivelse,
    val startdato: String,
    val sluttdato: String,
    val rapporteringsmaaned: String,
)

data class TimerMedTimeloenn(
    val antall: Int,
    val startdato: String,
    val sluttdato: String,
    val rapporteringsmaaned: String,
)
