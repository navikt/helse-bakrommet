package no.nav.helse.utbetalingslinjer

import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.FagområdeDto
import no.nav.helse.dto.OppdragstatusDto
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.dto.deserialisering.OppdragInnDto
import no.nav.helse.dto.serialisering.OppdragUtDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.utbetalingslinjer.Oppdragstatus.AKSEPTERT
import no.nav.helse.utbetalingslinjer.Oppdragstatus.AKSEPTERT_MED_FEIL
import no.nav.helse.utbetalingslinjer.Oppdragstatus.AVVIST
import no.nav.helse.utbetalingslinjer.Oppdragstatus.FEIL
import no.nav.helse.utbetalingslinjer.Oppdragstatus.OVERFØRT
import no.nav.helse.utbetalingslinjer.Utbetalingslinje.Companion.kjedeSammenLinjer
import no.nav.helse.utbetalingslinjer.Utbetalingslinje.Companion.kobleTil
import no.nav.helse.utbetalingslinjer.Utbetalingslinje.Companion.normaliserLinjer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Oppdrag private constructor(
    val mottaker: String,
    val fagområde: Fagområde,
    val linjer: MutableList<Utbetalingslinje>,
    val fagsystemId: String,
    val endringskode: Endringskode,
    nettoBeløp: Int = linjer.sumOf { it.totalbeløp() },
    overføringstidspunkt: LocalDateTime? = null,
    avstemmingsnøkkel: Long? = null,
    status: Oppdragstatus? = null,
    val tidsstempel: LocalDateTime,
    erSimulert: Boolean = false,
    simuleringsResultat: SimuleringResultatDto? = null,
) : List<Utbetalingslinje> by linjer {
    var nettoBeløp: Int = nettoBeløp
        private set
    var overføringstidspunkt: LocalDateTime? = overføringstidspunkt
        private set
    var avstemmingsnøkkel: Long? = avstemmingsnøkkel
        private set
    var status: Oppdragstatus? = status
        private set
    var erSimulert: Boolean = erSimulert
        private set
    var simuleringsResultat: SimuleringResultatDto? = simuleringsResultat
        private set

    companion object {
        fun periode(vararg oppdrag: Oppdrag): Periode? {
            return oppdrag
                .mapNotNull { it.linjeperiode }
                .takeIf(List<*>::isNotEmpty)
                ?.reduce(Periode::plus)
        }

        fun List<Oppdrag>.trekkerTilbakePenger() = sumOf { it.nettoBeløp() } < 0

        fun stønadsdager(vararg oppdrag: Oppdrag): Int {
            return Utbetalingslinje.stønadsdager(oppdrag.toList().flatten())
        }

        fun synkronisert(vararg oppdrag: Oppdrag): Boolean {
            val endrede = oppdrag.filter { it.harUtbetalinger() }
            return endrede.all { it.status == endrede.first().status }
        }

        fun ingenFeil(vararg oppdrag: Oppdrag) = oppdrag.none { it.status in listOf(AVVIST, FEIL) }

        fun harFeil(vararg oppdrag: Oppdrag) = oppdrag.any { it.status in listOf(AVVIST, FEIL) }

        fun kanIkkeForsøkesPåNy(vararg oppdrag: Oppdrag) = oppdrag.any { it.status == AVVIST }

        fun gjenopprett(dto: OppdragInnDto): Oppdrag {
            return Oppdrag(
                mottaker = dto.mottaker,
                fagområde =
                    when (dto.fagområde) {
                        FagområdeDto.SP -> Fagområde.Sykepenger
                        FagområdeDto.SPREF -> Fagområde.SykepengerRefusjon
                    },
                linjer = dto.linjer.map { Utbetalingslinje.gjenopprett(it) }.toMutableList(),
                fagsystemId = dto.fagsystemId,
                endringskode = Endringskode.gjenopprett(dto.endringskode),
                nettoBeløp = dto.nettoBeløp,
                overføringstidspunkt = dto.overføringstidspunkt,
                avstemmingsnøkkel = dto.avstemmingsnøkkel,
                status =
                    when (dto.status) {
                        OppdragstatusDto.AKSEPTERT -> AKSEPTERT
                        OppdragstatusDto.AKSEPTERT_MED_FEIL -> AKSEPTERT_MED_FEIL
                        OppdragstatusDto.AVVIST -> AVVIST
                        OppdragstatusDto.FEIL -> FEIL
                        OppdragstatusDto.OVERFØRT -> OVERFØRT
                        null -> null
                    },
                tidsstempel = dto.tidsstempel,
                erSimulert = dto.erSimulert,
                simuleringsResultat = dto.simuleringsResultat,
            )
        }
    }

    val linjeperiode get() = firstOrNull()?.let { (it.datoStatusFom ?: it.fom) til last().tom }

    constructor(
        mottaker: String,
        fagområde: Fagområde,
        linjer: List<Utbetalingslinje> = listOf(),
        fagsystemId: String = genererUtbetalingsreferanse(UUID.randomUUID()),
    ) : this(
        mottaker,
        fagområde,
        normaliserLinjer(fagsystemId, linjer).toMutableList(),
        fagsystemId,
        Endringskode.NY,
        tidsstempel = LocalDateTime.now(),
    )

    fun detaljer(): OppdragDetaljer {
        val linjene = map { it.detaljer() }
        return OppdragDetaljer(
            fagsystemId = fagsystemId,
            fagområde = fagområde.verdi,
            mottaker = mottaker,
            nettoBeløp = nettoBeløp,
            stønadsdager = stønadsdager(),
            fom = linjene.firstOrNull()?.fom ?: LocalDate.MIN,
            tom = linjene.lastOrNull()?.tom ?: LocalDate.MIN,
            linjer = linjene,
        )
    }



    private fun behovdetaljer(
        saksbehandler: String,
        maksdato: LocalDate?,
    ): MutableMap<String, Any> {
        return mutableMapOf(
            "mottaker" to mottaker,
            "fagområde" to "$fagområde",
            "linjer" to kopierKunLinjerMedEndring().map(Utbetalingslinje::behovdetaljer),
            "fagsystemId" to fagsystemId,
            "endringskode" to "$endringskode",
            "saksbehandler" to saksbehandler,
        ).apply {
            maksdato?.let {
                put("maksdato", maksdato.toString())
            }
        }
    }

    fun totalbeløp() = linjerUtenOpphør().sumOf { it.totalbeløp() }

    fun stønadsdager() = sumOf { it.stønadsdager() }

    fun nettoBeløp() = nettoBeløp

    private fun nettoBeløp(tidligere: Oppdrag) {
        nettoBeløp = this.totalbeløp() - tidligere.totalbeløp()
    }

    fun harUtbetalinger() = any(Utbetalingslinje::erForskjell)

    fun erRelevant(
        fagsystemId: String,
        fagområde: Fagområde,
    ) = this.fagsystemId == fagsystemId && this.fagområde == fagområde

    private fun kopierKunLinjerMedEndring() = kopierMed(filter(Utbetalingslinje::erForskjell))

    private fun kopierUtenOpphørslinjer() = kopierMed(linjerUtenOpphør())

    fun linjerUtenOpphør() = filter { !it.erOpphør() }



    private fun tomtOppdrag(): Oppdrag =
        Oppdrag(
            mottaker = mottaker,
            fagområde = fagområde,
            fagsystemId = fagsystemId,
        )

    fun begrensFra(førsteDag: LocalDate): Oppdrag {
        val (senereLinjer, tidligereLinjer) =
            this.linjer
                .filterNot { it.erOpphør() }
                .partition { it.fom >= førsteDag }
        val delvisOverlappendeFørsteLinje =
            tidligereLinjer
                .lastOrNull()
                ?.takeIf { it.tom >= førsteDag }
                ?.begrensFra(førsteDag)
        return kopierMed(listOfNotNull(delvisOverlappendeFørsteLinje) + senereLinjer)
    }

    fun begrensTil(
        sisteDato: LocalDate,
        other: Oppdrag? = null,
    ): Oppdrag {
        val (tidligereLinjer, senereLinjer) =
            this.linjer
                .filterNot { it.erOpphør() }
                .partition { it.tom <= sisteDato }
        val delvisOverlappendeSisteLinje =
            senereLinjer
                .firstOrNull()
                ?.takeIf { it.fom <= sisteDato }
                ?.begrensTil(sisteDato)
        other?.also { kobleTil(it) }
        return kopierMed(tidligereLinjer + listOfNotNull(delvisOverlappendeSisteLinje))
    }

    operator fun plus(other: Oppdrag): Oppdrag {
        check(none { linje -> other.any { it.periode.overlapperMed(linje.periode) } }) {
            "ikke støttet: kan ikke overlappe med annet oppdrag"
        }
        if (this.isNotEmpty() && other.isNotEmpty() && this.fomHarFlyttetSegFremover(other)) return other + this
        return kopierMed((slåSammenOppdrag(other)))
    }

    private fun slåSammenOppdrag(other: Oppdrag): List<Utbetalingslinje> {
        if (this.isEmpty()) return other.linjer
        if (other.isEmpty()) return this.linjer
        val sisteLinje = this.last()
        val første = other.first()
        val mellomlinje = sisteLinje.slåSammenLinje(første) ?: return this.linjer + other.linjer
        return this.linjer.dropLast(1) + listOf(mellomlinje) + other.linjer.drop(1)
    }


    private fun ingenUtbetalteDager() = linjerUtenOpphør().isEmpty()

    private fun erTomt() = this.isEmpty()

    // Vi har oppdaget utbetalingsdager tidligere i tidslinjen
    private fun fomHarFlyttetSegBakover(eldre: Oppdrag) = this.first().fom < eldre.first().fom

    // Vi har endret tidligere utbetalte dager til ikke-utbetalte dager i starten av tidslinjen
    private fun fomHarFlyttetSegFremover(eldre: Oppdrag) = this.first().fom > eldre.first().fom

    // man opphører (annullerer) et annet oppdrag ved å lage en opphørslinje som dekker hele perioden som er utbetalt
    // om det forrige oppdraget også var et opphør så kopieres siste linje for å bevare
    // delytelseId-rekkefølgen slik at det nye oppdraget kan bygges videre på
    private fun annulleringsoppdrag(tidligere: Oppdrag) =
        if (tidligere.kopierUtenOpphørslinjer().erTomt()) {
            kopierMed(
                linjer = listOf(tidligere.last().markerUendret(tidligere.last())),
                fagsystemId = tidligere.fagsystemId,
                endringskode = Endringskode.UEND,
            )
        } else {
            kopierMed(
                linjer = listOf(tidligere.last().opphørslinje(tidligere.kopierUtenOpphørslinjer().first().fom)),
                fagsystemId = tidligere.fagsystemId,
                endringskode = Endringskode.ENDR,
            )
        }

    // når man oppretter en NY linje med dato-intervall "(a, b)" vil oppdragsystemet
    // automatisk opphøre alle eventuelle linjer med fom > b.
    //
    // Eksempel:
    // Oppdrag 1: 5. januar til 31. januar (linje 1)
    // Oppdrag 2: 1. januar til 10. januar
    // Fordi linje "1. januar - 10. januar" opprettes som NY, medfører dette at oppdragsystemet opphører 11. januar til 31. januar automatisk
    private fun kjørFrem(tidligere: Oppdrag): Oppdrag {
        val sammenkoblet = this.kobleTil(tidligere)
        val linjer = kjedeSammenLinjer(sammenkoblet, tidligere.last())
        return sammenkoblet.kopierMed(linjer)
    }


    private fun opphørOppdrag(tidligere: Oppdrag) = tidligere.last().opphørslinje(tidligere.first().fom)

    private fun medFagsystemId(other: Oppdrag) = kopierMed(this.linjer, fagsystemId = other.fagsystemId)

    private fun kopierMed(
        linjer: List<Utbetalingslinje>,
        fagsystemId: String = this.fagsystemId,
        endringskode: Endringskode = this.endringskode,
    ) = Oppdrag(
        mottaker = mottaker,
        fagområde = fagområde,
        linjer = linjer.map { it.kopier() }.toMutableList(),
        fagsystemId = fagsystemId,
        endringskode = endringskode,
        overføringstidspunkt = overføringstidspunkt,
        avstemmingsnøkkel = avstemmingsnøkkel,
        status = status,
        tidsstempel = tidsstempel,
        erSimulert = erSimulert,
        simuleringsResultat = simuleringsResultat,
    )

    private fun kobleTil(tidligere: Oppdrag) =
        kopierMed(
            linjer.kobleTil(tidligere.fagsystemId),
            tidligere.fagsystemId,
            Endringskode.ENDR,
        )


    fun erKlarForGodkjenning() = !harUtbetalinger() || erSimulert


    fun dto() =
        OppdragUtDto(
            mottaker = mottaker,
            fagområde =
                when (fagområde) {
                    Fagområde.SykepengerRefusjon -> FagområdeDto.SPREF
                    Fagområde.Sykepenger -> FagområdeDto.SP
                },
            linjer = linjer.map { it.dto() },
            fagsystemId = fagsystemId,
            endringskode =
                when (endringskode) {
                    Endringskode.NY -> EndringskodeDto.NY
                    Endringskode.UEND -> EndringskodeDto.UEND
                    Endringskode.ENDR -> EndringskodeDto.ENDR
                },
            nettoBeløp = nettoBeløp,
            totalbeløp = this.totalbeløp(),
            stønadsdager = this.stønadsdager(),
            overføringstidspunkt = overføringstidspunkt,
            avstemmingsnøkkel = avstemmingsnøkkel,
            status =
                when (status) {
                    OVERFØRT -> OppdragstatusDto.OVERFØRT
                    AKSEPTERT -> OppdragstatusDto.AKSEPTERT
                    AKSEPTERT_MED_FEIL -> OppdragstatusDto.AKSEPTERT_MED_FEIL
                    AVVIST -> OppdragstatusDto.AVVIST
                    FEIL -> OppdragstatusDto.FEIL
                    null -> null
                },
            tidsstempel = tidsstempel,
            erSimulert = erSimulert,
            simuleringsResultat = simuleringsResultat,
        )
}

enum class Oppdragstatus { OVERFØRT, AKSEPTERT, AKSEPTERT_MED_FEIL, AVVIST, FEIL }
