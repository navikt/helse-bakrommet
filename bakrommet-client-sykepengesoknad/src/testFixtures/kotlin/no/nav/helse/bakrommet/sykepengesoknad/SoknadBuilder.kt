package no.nav.helse.bakrommet.sykepengesoknad

import no.nav.helse.flex.sykepengesoknad.kafka.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SoknadBuilder(
    private var id: String = UUID.randomUUID().toString(),
    private var fnr: String,
    private var type: SoknadstypeDTO = SoknadstypeDTO.ANNET_ARBEIDSFORHOLD,
    private var status: SoknadsstatusDTO = SoknadsstatusDTO.NY,
    private var fom: LocalDate,
    private var tom: LocalDate,
) {
    private var arbeidsgiverNavn: String? = null
    private var arbeidsgiverOrgnummer: String? = null
    private var arbeidssituasjon: ArbeidssituasjonDTO? = null
    private var sykmeldingSkrevet: LocalDate? = null
    private var startSyketilfelle: LocalDate? = null
    private var opprettet: LocalDate? = null
    private var sporsmal: List<SporsmalDTO>? = null
    private var sendtNav: LocalDate? = null
    private var sendtArbeidsgiver: LocalDate? = null
    private var sykmeldingstype: SykmeldingstypeDTO = SykmeldingstypeDTO.AKTIVITET_IKKE_MULIG
    private var grad: Int = 100
    private var sykmeldingsgrad: Int? = null

    fun id(id: String) = apply { this.id = id }

    fun type(type: SoknadstypeDTO) = apply { this.type = type }

    fun status(status: SoknadsstatusDTO) = apply { this.status = status }

    fun arbeidsgiver(
        navn: String,
        orgnummer: String,
    ) = apply {
        this.arbeidsgiverNavn = navn
        this.arbeidsgiverOrgnummer = orgnummer
    }

    fun arbeidssituasjon(arbeidssituasjon: ArbeidssituasjonDTO) =
        apply {
            this.arbeidssituasjon = arbeidssituasjon
        }

    fun sykmeldingSkrevet(dato: LocalDate) =
        apply {
            this.sykmeldingSkrevet = dato
        }

    fun startSyketilfelle(dato: LocalDate) =
        apply {
            this.startSyketilfelle = dato
        }

    fun sporsmal(sporsmal: List<SporsmalDTO>) =
        apply {
            this.sporsmal = sporsmal
        }

    fun opprettet(dato: LocalDate) =
        apply {
            this.opprettet = dato
        }

    fun sendtNav(dato: LocalDate) =
        apply {
            this.sendtNav = dato
        }

    fun sendtArbeidsgiver(dato: LocalDate) =
        apply {
            this.sendtArbeidsgiver = dato
        }

    fun sykmeldingstype(type: SykmeldingstypeDTO) =
        apply {
            this.sykmeldingstype = type
        }

    fun grad(grad: Int) =
        apply {
            this.grad = grad
        }

    fun sykmeldingsgrad(grad: Int) =
        apply {
            this.sykmeldingsgrad = grad
        }

    fun build(): SykepengesoknadDTO =
        SykepengesoknadDTO(
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
            sykmeldingSkrevet = sykmeldingSkrevet?.let { LocalDateTime.of(it, java.time.LocalTime.of(2, 0)) },
            startSyketilfelle = startSyketilfelle,
            arbeidGjenopptatt = null,
            opprettet = opprettet?.atStartOfDay(),
            sendtNav = sendtNav?.atStartOfDay(),
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

fun soknad(
    fnr: String,
    fom: LocalDate,
    tom: LocalDate,
    builder: SoknadBuilder.() -> Unit = {},
): SykepengesoknadDTO = SoknadBuilder(fnr = fnr, fom = fom, tom = tom).apply(builder).build()
