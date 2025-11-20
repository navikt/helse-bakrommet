package no.nav.helse.bakrommet.tidslinje

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDao
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDbRecord
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektYrkesaktivitetType
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetForenkletDbRecord
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.maybeOrgnummer
import no.nav.helse.bakrommet.ereg.EregClient
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.bakrommet.tidslinje.TidslinjeRad.OpprettetBehandling
import no.nav.helse.bakrommet.tidslinje.TidslinjeRad.SykmeldtYrkesaktivitet
import no.nav.helse.bakrommet.tidslinje.TidslinjeRad.TilkommenInntekt
import no.nav.helse.bakrommet.util.logg
import java.time.LocalDate
import java.util.*

interface TidslinjeServiceDaoer {
    val behandlingDao: BehandlingDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val tilkommenInntektDao: TilkommenInntektDao
}

data class TidslinjeData(
    val behandlinger: List<Behandling>,
    val yrkesaktiviteter: List<YrkesaktivitetForenkletDbRecord>,
    val tilkommen: List<TilkommenInntektDbRecord>,
    val organisasjonsnavnMap: Map<String, no.nav.helse.bakrommet.ereg.Organisasjon?>,
)

data class Tidslinje(
    val rader: List<TidslinjeRad>,
)

class TidslinjeService(
    private val db: DbDaoer<TidslinjeServiceDaoer>,
    private val eregClient: EregClient,
) {
    suspend fun hentTidslinje(
        personid: SpilleromPersonId,
    ): List<TidslinjeRad> =
        db.nonTransactional {
            val behandlinger = behandlingDao.finnBehandlingerForPerson(personid.personId)
            val yrkesaktivteter = yrkesaktivitetDao.finnYrkesaktiviteterForBehandlinger(behandlinger.map { it.id })
            val tilkommen = tilkommenInntektDao.finnTilkommenInntektForBehandlinger(behandlinger.map { it.id })
            val alleOrgnummer =
                (
                    yrkesaktivteter.mapNotNull { it.kategorisering.maybeOrgnummer() } +
                        tilkommen
                            .filter { it.tilkommenInntekt.yrkesaktivitetType != TilkommenInntektYrkesaktivitetType.PRIVATPERSON }
                            .map { it.tilkommenInntekt.ident }
                ).toSet()

            val organisasjonsnavnMap =
                coroutineScope {
                    alleOrgnummer
                        .associateWith { orgnummer ->
                            async {
                                withTimeoutOrNull(3_000) {
                                    try {
                                        eregClient.hentOrganisasjonsnavn(orgnummer)
                                    } catch (e: Exception) {
                                        logg.warn("Kall mot Ereg feilet for orgnummer $orgnummer", e)
                                        null
                                    }
                                }
                            }
                        }.mapValues { (_, deferred) ->
                            try {
                                deferred.await()
                            } catch (_: Exception) {
                                null
                            }
                        }
                }

            TidslinjeData(behandlinger, yrkesaktivteter, tilkommen, organisasjonsnavnMap).tilTidslinje()
        }
}

private fun TidslinjeData.tilTidslinje(): List<TidslinjeRad> {
    // Her kan du bruke organisasjonsnavnMap og de øvrige feltene
    val (behandlinger, yrkesaktiviteter, tilkommenInntekt, organisasjonsnavnMap) = this

    val tidslinjeRader = mutableListOf<TidslinjeRad>()
    behandlinger.forEach { behandling ->
        val yrkesaktiviteter = yrkesaktiviteter.filter { it.behandlingId == behandling.id }
        if (yrkesaktiviteter.isEmpty()) {
            tidslinjeRader.add(
                OpprettetBehandling(
                    tidslinjeElementer =
                        listOf(
                            BehandlingTidslinjeElement(
                                fom = behandling.fom,
                                tom = behandling.tom,
                                behandlingId = behandling.id,
                                status = behandling.status,
                                skjæringstidspunkt = behandling.skjæringstidspunkt,
                            ),
                        ),
                ),
            )
        }
        yrkesaktiviteter.filter { it.behandlingId == behandling.id }.forEach { ya ->

            data class IdNavnPair(
                val id: String,
                val navn: String,
            )

            val kat =
                when (ya.kategorisering) {
                    is YrkesaktivitetKategorisering.Arbeidstaker,
                    is YrkesaktivitetKategorisering.Frilanser,
                    -> {
                        val orgnummer = ya.kategorisering.maybeOrgnummer() ?: "TODO FIKSMEG"
                        val navn = organisasjonsnavnMap[orgnummer]?.navn ?: "Ukjent arbeidsgiver"
                        val fulltNavn =
                            if (ya.kategorisering is YrkesaktivitetKategorisering.Frilanser) {
                                "$navn (Frilans)"
                            } else {
                                navn
                            }
                        IdNavnPair(orgnummer, fulltNavn)
                    }

                    is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende ->
                        IdNavnPair(
                            "SELVSTENDIG",
                            "Selvstendig næringsdrivende",
                        )

                    is YrkesaktivitetKategorisering.Inaktiv -> IdNavnPair("INAKTIV", "Inaktiv")
                    is YrkesaktivitetKategorisering.Arbeidsledig -> IdNavnPair("ARBEIDSLEDIG", "Arbeidsledig")
                }

            tidslinjeRader.add(
                SykmeldtYrkesaktivitet(
                    id = kat.id,
                    navn = kat.navn,
                    tidslinjeElementer =
                        listOf(
                            YrkesaktivitetTidslinjeElement(
                                fom = behandling.fom,
                                tom = behandling.tom,
                                behandlingId = behandling.id,
                                yrkesaktivitetId = ya.id,
                                status = behandling.status,
                                ghost = !ya.kategorisering.sykmeldt,
                                skjæringstidspunkt = behandling.skjæringstidspunkt,
                            ),
                        ),
                ),
            )
        }

        if (behandling.status != BehandlingStatus.REVURDERT) {
            tilkommenInntekt.filter { it.behandlingId == behandling.id }.forEach { ti ->
                // TODO sikre at ved overlapp så viser vi bare den som gjelder den man er i?

                tidslinjeRader.add(
                    TilkommenInntekt(
                        id = ti.tilkommenInntekt.ident,
                        navn =
                            if (ti.tilkommenInntekt.yrkesaktivitetType != TilkommenInntektYrkesaktivitetType.PRIVATPERSON) {
                                organisasjonsnavnMap[ti.tilkommenInntekt.ident]?.navn ?: "Ukjent arbeidsgiver"
                            } else {
                                "Privat " + ti.tilkommenInntekt.ident
                            },
                        tidslinjeElementer =
                            listOf(
                                TilkommenInntektTidslinjeElement(
                                    fom = ti.tilkommenInntekt.fom,
                                    tom = ti.tilkommenInntekt.tom,
                                    behandlingId = behandling.id,
                                    tilkommenInntektId = ti.id,
                                    status = behandling.status,
                                    skjæringstidspunkt = behandling.skjæringstidspunkt,
                                ),
                            ),
                    ),
                )
            }
        }
    }

    return tidslinjeRader.gruppertPerTidslinjeRadTypeOgId()
}

private fun List<TidslinjeRad>.gruppertPerTidslinjeRadTypeOgId(): List<TidslinjeRad> =
    this
        .groupBy { rad ->
            when (rad) {
                is OpprettetBehandling -> Pair(OpprettetBehandling::class, rad.id)
                is SykmeldtYrkesaktivitet -> Pair(SykmeldtYrkesaktivitet::class, rad.id)
                is TilkommenInntekt -> Pair(TilkommenInntekt::class, rad.id)
            }
        }.map { (_, rader) ->
            val førsteRad = rader.first()
            when (førsteRad) {
                is OpprettetBehandling -> {
                    OpprettetBehandling(
                        tidslinjeElementer = rader.flatMap { (it as OpprettetBehandling).tidslinjeElementer },
                    )
                }

                is SykmeldtYrkesaktivitet -> {
                    SykmeldtYrkesaktivitet(
                        id = førsteRad.id,
                        navn = førsteRad.navn,
                        tidslinjeElementer = rader.flatMap { (it as SykmeldtYrkesaktivitet).tidslinjeElementer },
                    )
                }

                is TilkommenInntekt -> {
                    TilkommenInntekt(
                        id = førsteRad.id,
                        navn = førsteRad.navn,
                        tidslinjeElementer = rader.flatMap { (it as TilkommenInntekt).tidslinjeElementer },
                    )
                }
            }
        }

abstract class TidslinjeElement {
    abstract val fom: LocalDate
    abstract val tom: LocalDate
}

data class TilkommenInntektTidslinjeElement(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val skjæringstidspunkt: LocalDate,
    override val behandlingId: UUID,
    val tilkommenInntektId: UUID,
    override val status: BehandlingStatus,
) : BehandlingTidslinjeElement(
        fom = fom,
        tom = tom,
        behandlingId = behandlingId,
        status = status,
        skjæringstidspunkt = skjæringstidspunkt,
    )

data class YrkesaktivitetTidslinjeElement(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val skjæringstidspunkt: LocalDate,
    override val behandlingId: UUID,
    override val status: BehandlingStatus,
    val yrkesaktivitetId: UUID,
    val ghost: Boolean,
) : BehandlingTidslinjeElement(
        fom = fom,
        tom = tom,
        behandlingId = behandlingId,
        status = status,
        skjæringstidspunkt = skjæringstidspunkt,
    )

open class BehandlingTidslinjeElement(
    override val fom: LocalDate,
    override val tom: LocalDate,
    open val skjæringstidspunkt: LocalDate,
    open val behandlingId: UUID,
    open val status: BehandlingStatus,
) : TidslinjeElement()

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tidslinjeRadType")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = OpprettetBehandling::class, name = "OpprettetBehandling"),
        JsonSubTypes.Type(value = SykmeldtYrkesaktivitet::class, name = "SykmeldtYrkesaktivitet"),
        JsonSubTypes.Type(value = TilkommenInntekt::class, name = "TilkommenInntekt"),
    ],
)
sealed class TidslinjeRad {
    abstract val tidslinjeElementer: List<TidslinjeElement>
    abstract val id: String
    abstract val navn: String

    data class OpprettetBehandling(
        override val tidslinjeElementer: List<BehandlingTidslinjeElement>,
    ) : TidslinjeRad() {
        override val id: String = "OPPRETTET_BEHANDLING"
        override val navn: String = "Opprettet behandling"
    }

    data class SykmeldtYrkesaktivitet(
        override val tidslinjeElementer: List<YrkesaktivitetTidslinjeElement>,
        override val id: String,
        override val navn: String,
    ) : TidslinjeRad()

    data class TilkommenInntekt(
        override val tidslinjeElementer: List<TilkommenInntektTidslinjeElement>,
        override val id: String,
        override val navn: String,
    ) : TidslinjeRad()
}
