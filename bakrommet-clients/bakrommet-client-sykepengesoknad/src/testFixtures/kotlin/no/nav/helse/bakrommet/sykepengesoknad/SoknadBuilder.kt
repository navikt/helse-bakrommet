package no.nav.helse.bakrommet.sykepengesoknad

import no.nav.helse.bakrommet.ereg.Organisasjon
import no.nav.helse.flex.sykepengesoknad.kafka.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SoknadDsl(
    var id: UUID = UUID.randomUUID(),
    var fnr: String,
    var type: SoknadstypeDTO = SoknadstypeDTO.ARBEIDSTAKERE,
    var status: SoknadsstatusDTO = SoknadsstatusDTO.SENDT,
    var fom: LocalDate,
    var tom: LocalDate,
) {
    var arbeidsgiverNavn: String? = null
    var arbeidsgiverOrgnummer: String? = null
    var arbeidssituasjon: ArbeidssituasjonDTO? = null
    var sykmeldingSkrevet: LocalDate? = null
    var startSyketilfelle: LocalDate? = null
    var opprettet: LocalDate? = null
    var sporsmal: List<SporsmalDTO> = emptyList()
    var sendtNav: LocalDate? = null
    var sendtArbeidsgiver: LocalDate? = null
    var sykmeldingstype: SykmeldingstypeDTO = SykmeldingstypeDTO.AKTIVITET_IKKE_MULIG
    var grad: Int = 100
    var sykmeldingsgrad: Int? = null
    var valgteBehandlingsdager: List<LocalDate>? = null

    fun arbeidstaker(organisasjon: Organisasjon) {
        arbeidsgiverOrgnummer = organisasjon.orgnummer
        arbeidsgiverNavn = organisasjon.navn
        arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
        type = SoknadstypeDTO.ARBEIDSTAKERE
    }

    fun medBehandlingsdager(vararg dager: LocalDate) {
        valgteBehandlingsdager = dager.toList()
    }

    fun medBehandlingsdager(dager: List<LocalDate>) {
        valgteBehandlingsdager = dager
    }

    fun medBehandlingsdagerSporsmal() {
        val behandlingsdagerPerioder =
            listOf(
                SoknadsperiodeDTO(
                    fom = fom,
                    tom = tom,
                    grad = grad,
                    sykmeldingsgrad = sykmeldingsgrad ?: grad,
                    faktiskGrad = null,
                    sykmeldingstype = sykmeldingstype,
                    avtaltTimer = null,
                    faktiskTimer = null,
                ),
            ).filter { it.sykmeldingstype == SykmeldingstypeDTO.BEHANDLINGSDAGER }
                .sortedBy { it.fom }

        if (behandlingsdagerPerioder.isEmpty()) return

        val behandlingsdagerSporsmal =
            behandlingsdagerPerioder
                .mapIndexed { index, periode ->
                    val uker = splittPeriodeIUker(periode.fom!!, periode.tom!!)
                    val sporsmalstekst =
                        if (arbeidssituasjon == ArbeidssituasjonDTO.ARBEIDSLEDIG) {
                            "Hvilke dager kunne du ikke være arbeidssøker på grunn av behandling mellom ${formatterPeriode(periode.fom!!, periode.tom!!)}?"
                        } else {
                            "Hvilke dager måtte du være helt borte fra jobben på grunn av behandling mellom ${formatterPeriode(periode.fom!!, periode.tom!!)}?"
                        }

                    SporsmalDTO(
                        tag = "ENKELTSTAENDE_BEHANDLINGSDAGER_$index",
                        sporsmalstekst = sporsmalstekst,
                        svartype = SvartypeDTO.INFO_BEHANDLINGSDAGER,
                        undersporsmal = skapUndersporsmalUke(uker, index, valgteBehandlingsdager),
                    )
                }

        sporsmal = sporsmal + behandlingsdagerSporsmal
    }

    fun build(): SykepengesoknadDTO {
        // Legg automatisk til behandlingsdager-spørsmål hvis det finnes behandlingsdager-perioder
        if (sykmeldingstype == SykmeldingstypeDTO.BEHANDLINGSDAGER) {
            medBehandlingsdagerSporsmal()
        }

        val sykmeldingSkrevetDato = sykmeldingSkrevet ?: fom
        val startSyketilfelleDato = startSyketilfelle ?: fom
        val opprettetDato = opprettet ?: fom
        val sendtNavDato = sendtNav ?: tom.plusDays(1)

        return SykepengesoknadDTO(
            id = id.toString(),
            fnr = fnr,
            type = type,
            status = status,
            fom = fom,
            sporsmal = sporsmal,
            tom = tom,
            arbeidsgiver =
                arbeidsgiverNavn?.let { navn ->
                    arbeidsgiverOrgnummer?.let { orgnummer ->
                        ArbeidsgiverDTO(
                            navn = navn,
                            orgnummer = orgnummer,
                        )
                    }
                },
            arbeidssituasjon = arbeidssituasjon,
            sykmeldingSkrevet = LocalDateTime.of(sykmeldingSkrevetDato, java.time.LocalTime.of(2, 0)),
            startSyketilfelle = startSyketilfelleDato,
            arbeidGjenopptatt = null,
            opprettet = opprettetDato.atStartOfDay(),
            sendtNav = sendtNavDato.atStartOfDay(),
            sendtArbeidsgiver = sendtArbeidsgiver?.atStartOfDay(),
            soknadsperioder =
                listOf(
                    SoknadsperiodeDTO(
                        fom = fom,
                        tom = tom,
                        grad = grad,
                        sykmeldingsgrad = sykmeldingsgrad ?: grad,
                        faktiskGrad = null,
                        sykmeldingstype = sykmeldingstype,
                        avtaltTimer = null,
                        faktiskTimer = null,
                    ),
                ),
            fravar = emptyList(),
            egenmeldinger = null,
            fravarForSykmeldingen = emptyList(),
            papirsykmeldinger = emptyList(),
            andreInntektskilder = emptyList(),
            korrigerer = null,
            korrigertAv = null,
            soktUtenlandsopphold = false,
            arbeidsgiverForskutterer = null,
            dodsdato = null,
            friskmeldt = null,
            opprinneligSendt = null,
            behandlingsdager = valgteBehandlingsdager,
        )
    }
}

fun soknad(
    fnr: String,
    fom: LocalDate,
    tom: LocalDate,
    block: SoknadDsl.() -> Unit = {},
): SykepengesoknadDTO = SoknadDsl(fnr = fnr, fom = fom, tom = tom).apply(block).build()
