package no.nav.helse.bakrommet.sykepengesoknad

import no.nav.helse.flex.sykepengesoknad.kafka.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SoknadDsl(
    var id: String = UUID.randomUUID().toString(),
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

    fun arbeidstaker(organisasjon: Pair<String, String>) {
        arbeidsgiverOrgnummer = organisasjon.first
        arbeidsgiverNavn = organisasjon.second
        arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
        type = SoknadstypeDTO.ARBEIDSTAKERE
    }

    fun build(): SykepengesoknadDTO {
        val sykmeldingSkrevetDato = sykmeldingSkrevet ?: fom
        val startSyketilfelleDato = startSyketilfelle ?: fom
        val opprettetDato = opprettet ?: fom
        val sendtNavDato = sendtNav ?: tom.plusDays(1)

        return SykepengesoknadDTO(
            id = id,
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
        )
    }
}

fun soknad(
    fnr: String,
    fom: LocalDate,
    tom: LocalDate,
    block: SoknadDsl.() -> Unit = {},
): SykepengesoknadDTO = SoknadDsl(fnr = fnr, fom = fom, tom = tom).apply(block).build()
