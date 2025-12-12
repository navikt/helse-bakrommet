package no.nav.helse.bakrommet.aareg

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Builder-funksjoner for å lage AA-Reg arbeidsforhold på en enklere måte.
 */

private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

// Helper-funksjoner for vanlige typer
fun ordinaertArbeidsforhold() = ArbeidsforholdType("ordinaertArbeidsforhold", "Ordinært arbeidsforhold")

fun maritimtArbeidsforhold() = ArbeidsforholdType("maritimtArbeidsforhold", "Maritimt arbeidsforhold")

fun forenkletOppgjoersordning() = ArbeidsforholdType("forenkletOppgjoersordning", "Forenklet oppgjørsordning")

fun frilanserOppdragstaker() = ArbeidsforholdType("frilanserOppdragstakerHonorarPersonerMm", "Frilansere/oppdragstakere, styremedlemmer, folkevalgte, personer som innehar tillitsverv, fosterforelder, støttekontakter, avlastere og personer med omsorgslønn")

// Helper-funksjoner for identer
fun fnrIdent(
    fnr: String,
    gjeldende: Boolean = true,
) = Ident("FOLKEREGISTERIDENT", fnr, gjeldende)

fun aktorIdIdent(
    aktorId: String,
    gjeldende: Boolean = true,
) = Ident("AKTORID", aktorId, gjeldende)

fun orgnummerIdent(orgnummer: String) = Ident("ORGANISASJONSNUMMER", orgnummer)

// Helper-funksjoner for arbeidssted
fun arbeidsstedUnderenhet(orgnummer: String) = Arbeidssted("Underenhet", listOf(orgnummerIdent(orgnummer)))

fun arbeidsstedHovedenhet(orgnummer: String) = Arbeidssted("Hovedenhet", listOf(orgnummerIdent(orgnummer)))

fun arbeidsstedPerson(identer: List<Ident>) = Arbeidssted("Person", identer)

// Helper-funksjoner for ansettelsesform
fun fastAnsettelse() = KodeBeskrivelse("fast", "Fast ansettelse")

fun midlertidigAnsettelse() = KodeBeskrivelse("midlertidig", "Midlertidig ansettelse")

// Helper-funksjoner for rapporteringsordning
fun aOrdningen() = Rapporteringsordning("a-ordningen", "Rapportert via a-ordningen (2015-d.d.)")

// Helper-funksjoner for dato-formatering
private fun LocalDate.tilDatoString(): String = format(dateFormatter)

private fun LocalDate.tilDateTimeString(): String = atStartOfDay().format(dateTimeFormatter)

/**
 * Builder-funksjon for å lage et ordinært arbeidsforhold på en enklere måte.
 */
fun arbeidsforhold(
    fnr: String,
    orgnummer: String,
    startdato: LocalDate,
    sluttdato: LocalDate? = null,
    id: String = UUID.randomUUID().toString(),
    stillingsprosent: Double = 100.0,
    timerPrUke: Double? = null,
    ansettelsesform: KodeBeskrivelse = fastAnsettelse(),
    navArbeidsforholdId: Int = (10000..99999).random(),
    block: ArbeidsforholdBuilder.() -> Unit = {},
): Arbeidsforhold {
    val builder =
        ArbeidsforholdBuilder(
            fnr = fnr,
            orgnummer = orgnummer,
            startdato = startdato,
            sluttdato = sluttdato,
            id = id,
            stillingsprosent = stillingsprosent,
            timerPrUke = timerPrUke ?: (stillingsprosent / 100.0 * 37.5),
            ansettelsesform = ansettelsesform,
            navArbeidsforholdId = navArbeidsforholdId,
        )
    builder.block()
    return builder.build()
}

class ArbeidsforholdBuilder(
    private val fnr: String,
    private val orgnummer: String,
    private val startdato: LocalDate,
    private val sluttdato: LocalDate?,
    private val id: String?,
    private val stillingsprosent: Double,
    private val timerPrUke: Double,
    private val ansettelsesform: KodeBeskrivelse,
    private val navArbeidsforholdId: Int,
) {
    private var type: ArbeidsforholdType = ordinaertArbeidsforhold()
    private var aktorId: String? = null
    private var yrke: KodeBeskrivelse? = null
    private var arbeidstidsordning: KodeBeskrivelse? = null
    private var rapporteringsmaaneder: Rapporteringsmaaneder? = null
    private var opplysningspliktigOrgnummer: String? = null
    private var navVersjon: Int = 1
    private var navUuid: String = UUID.randomUUID().toString()
    private var sistEndret: String? = null
    private var permisjoner: List<Permisjon>? = null
    private var permitteringer: List<Permittering>? = null
    private var innrapportertEtterAOrdningen: Boolean? = null
    private var idHistorikk: List<IdHistorikk>? = null
    private var varsler: List<Varsel>? = null
    private var utenlandsopphold: List<Utenlandsopphold>? = null
    private var timerMedTimeloenn: List<TimerMedTimeloenn>? = null

    fun type(type: ArbeidsforholdType) = apply { this.type = type }

    fun aktorId(aktorId: String) = apply { this.aktorId = aktorId }

    fun yrke(
        kode: String,
        beskrivelse: String,
    ) = apply { this.yrke = KodeBeskrivelse(kode, beskrivelse) }

    fun arbeidstidsordning(
        kode: String,
        beskrivelse: String,
    ) = apply { this.arbeidstidsordning = KodeBeskrivelse(kode, beskrivelse) }

    fun rapporteringsmaaneder(
        fra: String,
        til: String? = null,
    ) = apply { this.rapporteringsmaaneder = Rapporteringsmaaneder(fra, til) }

    fun opplysningspliktig(orgnummer: String) = apply { this.opplysningspliktigOrgnummer = orgnummer }

    fun navVersjon(versjon: Int) = apply { this.navVersjon = versjon }

    fun navUuid(uuid: String) = apply { this.navUuid = uuid }

    fun sistEndret(dato: LocalDate) = apply { this.sistEndret = dato.tilDateTimeString() }

    fun permisjoner(permisjoner: List<Permisjon>) = apply { this.permisjoner = permisjoner }

    fun permitteringer(permitteringer: List<Permittering>) = apply { this.permitteringer = permitteringer }

    fun innrapportertEtterAOrdningen(verdi: Boolean) = apply { this.innrapportertEtterAOrdningen = verdi }

    fun idHistorikk(idHistorikk: List<IdHistorikk>) = apply { this.idHistorikk = idHistorikk }

    fun varsler(varsler: List<Varsel>) = apply { this.varsler = varsler }

    fun utenlandsopphold(utenlandsopphold: List<Utenlandsopphold>) = apply { this.utenlandsopphold = utenlandsopphold }

    fun timerMedTimeloenn(timerMedTimeloenn: List<TimerMedTimeloenn>) = apply { this.timerMedTimeloenn = timerMedTimeloenn }

    fun build(): Arbeidsforhold {
        val identer = mutableListOf(fnrIdent(fnr))
        aktorId?.let { identer.add(aktorIdIdent(it)) }

        val opplysningspliktigOrgnr = opplysningspliktigOrgnummer ?: orgnummer

        return Arbeidsforhold(
            id = id,
            type = type,
            arbeidstaker = Arbeidstaker(identer),
            arbeidssted = arbeidsstedUnderenhet(orgnummer),
            opplysningspliktig = arbeidsstedHovedenhet(opplysningspliktigOrgnr),
            ansettelsesperiode = Ansettelsesperiode(startdato.tilDatoString(), sluttdato?.tilDatoString()),
            ansettelsesdetaljer =
                listOf(
                    Ansettelsesdetaljer(
                        type = "Ordinaer",
                        arbeidstidsordning = arbeidstidsordning,
                        ansettelsesform = ansettelsesform,
                        yrke = yrke,
                        antallTimerPrUke = timerPrUke,
                        avtaltStillingsprosent = stillingsprosent,
                        rapporteringsmaaneder = rapporteringsmaaneder,
                    ),
                ),
            rapporteringsordning = aOrdningen(),
            navArbeidsforholdId = navArbeidsforholdId,
            navVersjon = navVersjon,
            navUuid = navUuid,
            opprettet = startdato.tilDateTimeString(),
            sistBekreftet = startdato.tilDateTimeString(),
            bruksperiode = Bruksperiode(startdato.tilDateTimeString(), sluttdato?.let { it.tilDateTimeString() }),
            sistEndret = sistEndret,
            permisjoner = permisjoner,
            permitteringer = permitteringer,
            innrapportertEtterAOrdningen = innrapportertEtterAOrdningen,
            idHistorikk = idHistorikk,
            varsler = varsler,
            utenlandsopphold = utenlandsopphold,
            timerMedTimeloenn = timerMedTimeloenn,
        )
    }
}
