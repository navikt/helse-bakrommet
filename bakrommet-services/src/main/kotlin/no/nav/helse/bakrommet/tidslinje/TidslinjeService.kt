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
import no.nav.helse.bakrommet.ereg.Organisasjon
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.person.NaturligIdent
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
    val organisasjonsnavnMap: Map<String, Organisasjon?>,
)

data class Tidslinje(
    val rader: List<TidslinjeRad>,
)

class TidslinjeService(
    private val db: DbDaoer<TidslinjeServiceDaoer>,
    private val eregClient: EregClient,
) {
    suspend fun hentTidslinjeData(
        naturligIdent: NaturligIdent,
    ): TidslinjeData =
        db.nonTransactional {
            val behandlinger = behandlingDao.finnBehandlingerForNaturligIdent(naturligIdent)
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

            TidslinjeData(behandlinger, yrkesaktivteter, tilkommen, organisasjonsnavnMap)
        }

    suspend fun hentTidslinje(naturligIdent: NaturligIdent) = hentTidslinjeData(naturligIdent).tilTidslinje()
}

internal fun TidslinjeData.tilTidslinje(): List<TidslinjeRad> {
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
                            TidslinjeElement(
                                fom = behandling.fom,
                                tom = behandling.tom,
                                behandlingId = behandling.id,
                                status = behandling.status,
                                skjæringstidspunkt = behandling.skjæringstidspunkt,
                                revurdertAv = behandling.revurdertAvBehandlingId,
                                revurdererBehandlingId = behandling.revurdererSaksbehandlingsperiodeId,
                                historiske = emptyList(),
                                historisk = false,
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
                                historiske = emptyList(),
                                historisk = false,
                                revurdertAv = behandling.revurdertAvBehandlingId,
                                revurdererBehandlingId = behandling.revurdererSaksbehandlingsperiodeId,
                            ),
                        ),
                ),
            )
        }

        if (behandling.status != BehandlingStatus.REVURDERT) {
            tilkommenInntekt
                .filter { it.behandlingId == behandling.id }
                .forEach { ti ->
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
                                        revurdertAv = behandling.revurdertAvBehandlingId,
                                        revurdererBehandlingId = behandling.revurdererSaksbehandlingsperiodeId,
                                        historisk = false,
                                        historiske = emptyList(),
                                    ),
                                ),
                        ),
                    )
                }
        }
    }

    val behandlingerMap = behandlinger.associateBy { it.id }
    return tidslinjeRader.gruppertPerTidslinjeRadTypeOgId().grupperInnHistoriske(behandlingerMap)
}

private fun List<TidslinjeRad>.grupperInnHistoriske(behandlingerMap: Map<UUID, Behandling>): List<TidslinjeRad> {
    // Bygg mapping av hvilke behandlinger som er revurdert (har revurdertAvBehandlingId satt)
    val revurderteBehandlinger =
        behandlingerMap.values
            .filter { it.revurdertAvBehandlingId != null }
            .map { it.id }
            .toSet()

    // Bygg mapping av revurderingskjeder: behandlingId -> behandlingId som revurderer den
    val revurdererMap =
        behandlingerMap.values
            .filter { it.revurdererSaksbehandlingsperiodeId != null }
            .associate { it.revurdererSaksbehandlingsperiodeId!! to it.id }

    return this.map { rad ->
        when (rad) {
            is TidslinjeRad.OpprettetBehandling -> {
                val grupperteElementer =
                    grupperElementerIRevurderingskjede(
                        rad.tidslinjeElementer,
                        revurderteBehandlinger,
                        revurdererMap,
                        behandlingerMap,
                    )
                rad.copy(tidslinjeElementer = grupperteElementer)
            }

            is TidslinjeRad.SykmeldtYrkesaktivitet -> {
                val grupperteElementer =
                    grupperYrkesaktivitetElementerIRevurderingskjede(
                        rad.tidslinjeElementer,
                        revurderteBehandlinger,
                        revurdererMap,
                        behandlingerMap,
                    )
                rad.copy(tidslinjeElementer = grupperteElementer)
            }

            is TidslinjeRad.TilkommenInntekt -> {
                val grupperteElementer =
                    grupperTilkommenInntektElementerIRevurderingskjede(
                        rad.tidslinjeElementer,
                        revurderteBehandlinger,
                        revurdererMap,
                        behandlingerMap,
                    )
                rad.copy(tidslinjeElementer = grupperteElementer)
            }
        }
    }
}

private fun grupperElementerIRevurderingskjede(
    elementer: List<TidslinjeElement>,
    revurderteBehandlinger: Set<UUID>,
    revurdererMap: Map<UUID, UUID>,
    behandlingerMap: Map<UUID, Behandling>,
): List<TidslinjeElement> {
    if (elementer.isEmpty()) return elementer

    // Sjekk om noen elementer revurderer andre elementer i listen
    // Hvis ja, må vi gruppere dem sammen uavhengig av kjede-ID
    val behandlingIdSet = elementer.map { it.behandlingId }.toSet()
    val elementerSomRevurdererAndre =
        elementer.filter { element ->
            val revurdererBehandlingId = behandlingerMap[element.behandlingId]?.revurdererSaksbehandlingsperiodeId
            revurdererBehandlingId != null && revurdererBehandlingId in behandlingIdSet
        }

    // Hvis noen elementer revurderer andre i listen, behandle alle sammen som én gruppe
    if (elementerSomRevurdererAndre.isNotEmpty()) {
        return grupperKjedeElementer(elementer, revurderteBehandlinger, behandlingerMap)
    }

    // Ellers, grupper elementer i revurderingskjeder som før
    val kjeder = mutableMapOf<UUID, MutableList<TidslinjeElement>>()
    val elementerUtenKjede = mutableListOf<TidslinjeElement>()

    elementer.forEach { element ->
        val kjedeId = finnKjedeId(element.behandlingId, revurdererMap, behandlingerMap)
        if (kjedeId != null) {
            kjeder.getOrPut(kjedeId) { mutableListOf() }.add(element)
        } else {
            elementerUtenKjede.add(element)
        }
    }

    val resultat = mutableListOf<TidslinjeElement>()

    // Behandle hver kjede
    kjeder.values.forEach { kjedeElementer ->
        val gruppertKjede =
            grupperKjedeElementer(kjedeElementer, revurderteBehandlinger, behandlingerMap)
        resultat.addAll(gruppertKjede)
    }

    // Legg til elementer uten kjede
    resultat.addAll(elementerUtenKjede)

    return resultat
}

private fun grupperYrkesaktivitetElementerIRevurderingskjede(
    elementer: List<YrkesaktivitetTidslinjeElement>,
    revurderteBehandlinger: Set<UUID>,
    revurdererMap: Map<UUID, UUID>,
    behandlingerMap: Map<UUID, Behandling>,
): List<YrkesaktivitetTidslinjeElement> {
    if (elementer.isEmpty()) return elementer

    // Sjekk om noen elementer revurderer andre elementer i listen
    // Hvis ja, må vi gruppere dem sammen uavhengig av kjede-ID
    val behandlingIdSet = elementer.map { it.behandlingId }.toSet()
    val elementerSomRevurdererAndre =
        elementer.filter { element ->
            val revurdererBehandlingId = behandlingerMap[element.behandlingId]?.revurdererSaksbehandlingsperiodeId
            revurdererBehandlingId != null && revurdererBehandlingId in behandlingIdSet
        }

    // Hvis noen elementer revurderer andre i listen, behandle alle sammen som én gruppe
    if (elementerSomRevurdererAndre.isNotEmpty()) {
        return grupperYrkesaktivitetKjedeElementer(elementer, revurderteBehandlinger, behandlingerMap)
    }

    // Ellers, grupper elementer i revurderingskjeder som før
    val kjeder = mutableMapOf<UUID, MutableList<YrkesaktivitetTidslinjeElement>>()
    val elementerUtenKjede = mutableListOf<YrkesaktivitetTidslinjeElement>()

    elementer.forEach { element ->
        val kjedeId = finnKjedeId(element.behandlingId, revurdererMap, behandlingerMap)
        if (kjedeId != null) {
            kjeder.getOrPut(kjedeId) { mutableListOf() }.add(element)
        } else {
            elementerUtenKjede.add(element)
        }
    }

    val resultat = mutableListOf<YrkesaktivitetTidslinjeElement>()

    // Behandle hver kjede
    kjeder.values.forEach { kjedeElementer ->
        val gruppertKjede =
            grupperYrkesaktivitetKjedeElementer(kjedeElementer, revurderteBehandlinger, behandlingerMap)
        resultat.addAll(gruppertKjede)
    }

    // Legg til elementer uten kjede
    resultat.addAll(elementerUtenKjede)

    return resultat
}

private fun grupperTilkommenInntektElementerIRevurderingskjede(
    elementer: List<TilkommenInntektTidslinjeElement>,
    revurderteBehandlinger: Set<UUID>,
    revurdererMap: Map<UUID, UUID>,
    behandlingerMap: Map<UUID, Behandling>,
): List<TilkommenInntektTidslinjeElement> {
    if (elementer.isEmpty()) return elementer

    // Sjekk om noen elementer revurderer andre elementer i listen
    // Hvis ja, må vi gruppere dem sammen uavhengig av kjede-ID
    val behandlingIdSet = elementer.map { it.behandlingId }.toSet()
    val elementerSomRevurdererAndre =
        elementer.filter { element ->
            val revurdererBehandlingId = behandlingerMap[element.behandlingId]?.revurdererSaksbehandlingsperiodeId
            revurdererBehandlingId != null && revurdererBehandlingId in behandlingIdSet
        }

    // Hvis noen elementer revurderer andre i listen, behandle alle sammen som én gruppe
    if (elementerSomRevurdererAndre.isNotEmpty()) {
        return grupperTilkommenInntektKjedeElementer(
            elementer,
            revurderteBehandlinger,
            revurdererMap,
            behandlingerMap,
        )
    }

    // Ellers, grupper elementer i revurderingskjeder som før
    val kjeder = mutableMapOf<UUID, MutableList<TilkommenInntektTidslinjeElement>>()
    val elementerUtenKjede = mutableListOf<TilkommenInntektTidslinjeElement>()

    elementer.forEach { element ->
        val kjedeId = finnKjedeId(element.behandlingId, revurdererMap, behandlingerMap)
        if (kjedeId != null) {
            kjeder.getOrPut(kjedeId) { mutableListOf() }.add(element)
        } else {
            elementerUtenKjede.add(element)
        }
    }

    val resultat = mutableListOf<TilkommenInntektTidslinjeElement>()

    // Behandle hver kjede
    kjeder.values.forEach { kjedeElementer ->
        val gruppertKjede =
            grupperTilkommenInntektKjedeElementer(
                kjedeElementer,
                revurderteBehandlinger,
                revurdererMap,
                behandlingerMap,
            )
        resultat.addAll(gruppertKjede)
    }

    // Legg til elementer uten kjede
    resultat.addAll(elementerUtenKjede)

    return resultat
}

/**
 * Finner kjede-ID for en behandling. Kjede-ID er den nyeste behandlingen i kjeden som ikke er revurdert av noen annen.
 * En behandling er del av en kjede hvis den revurderer en annen behandling eller er revurdert av en annen.
 */
private fun finnKjedeId(
    behandlingId: UUID,
    revurdererMap: Map<UUID, UUID>,
    behandlingerMap: Map<UUID, Behandling>,
): UUID? {
    val behandling = behandlingerMap[behandlingId] ?: return null

    // Sjekk om behandlingen er del av en kjede
    val erDelAvKjede =
        behandling.revurdererSaksbehandlingsperiodeId != null || behandling.revurdertAvBehandlingId != null
    if (!erDelAvKjede) {
        return null
    }

    // Finn toppen av kjeden ved å følge revurderer-kjeden oppover
    var current = behandlingId
    var visited = mutableSetOf<UUID>()

    while (true) {
        if (current in visited) {
            // Sirkulær referanse, returner current
            return current
        }
        visited.add(current)

        val currentBehandling = behandlingerMap[current] ?: return current

        // Finn behandlingen som revurderer current (hvis noen)
        val revurderer =
            revurdererMap.values.find { revurdererId ->
                behandlingerMap[revurdererId]?.revurdererSaksbehandlingsperiodeId == current
            }

        if (revurderer == null) {
            // Ingen revurderer denne behandlingen, vi er på toppen
            return current
        }

        current = revurderer
    }
}

/**
 * Grupperer elementer i en kjede. Finner det nyeste elementet som ikke er revurdert av noen annen,
 * og legger de andre i historisk listen.
 */
private fun grupperKjedeElementer(
    elementer: List<TidslinjeElement>,
    revurderteBehandlinger: Set<UUID>,
    behandlingerMap: Map<UUID, Behandling>,
): List<TidslinjeElement> {
    if (elementer.isEmpty()) return elementer
    if (elementer.size == 1) {
        val element = elementer.first()
        val erHistorisk = element.behandlingId in revurderteBehandlinger
        return listOf(
            TidslinjeElement(
                fom = element.fom,
                tom = element.tom,
                skjæringstidspunkt = element.skjæringstidspunkt,
                behandlingId = element.behandlingId,
                status = element.status,
                historisk = erHistorisk,
                revurdererBehandlingId = element.revurdererBehandlingId,
                revurdertAv = element.revurdertAv,
                historiske = emptyList(),
            ),
        )
    }

    // Finn det nyeste elementet som ikke er revurdert av noen annen
    val elementerSortert = elementer.sortedByDescending { behandlingerMap[it.behandlingId]?.opprettet }

    val hovedElement =
        elementerSortert.firstOrNull { element ->
            val behandling = behandlingerMap[element.behandlingId]
            behandling?.revurdertAvBehandlingId == null
        } ?: elementerSortert.first()

    // Hvis hovedelementet revurderer en annen behandling, så skal den revurderte behandlingen legges i historiske
    val revurdertBehandlingId = behandlingerMap[hovedElement.behandlingId]?.revurdererSaksbehandlingsperiodeId
    val revurdertElement =
        revurdertBehandlingId?.let { revurdertId ->
            elementer.find { it.behandlingId == revurdertId }
        }

    val historiskeElementer =
        elementer
            .filter { it.behandlingId != hovedElement.behandlingId }
            .sortedBy { behandlingerMap[it.behandlingId]?.opprettet }

    // Legg til det revurderte elementet i historiske hvis det ikke allerede er der
    val alleHistoriske =
        if (revurdertElement != null && revurdertElement.behandlingId != hovedElement.behandlingId) {
            val eksistererAllerede = historiskeElementer.any { it.behandlingId == revurdertElement.behandlingId }
            if (!eksistererAllerede) {
                historiskeElementer + revurdertElement
            } else {
                historiskeElementer
            }
        } else {
            historiskeElementer
        }

    // Sorter historiske elementer slik at den eldste som ikke revurderer noen ligger sist
    // Først: elementer som revurderer noen (sortert etter opprettet, nyeste først)
    // Så: elementer som ikke revurderer noen (sortert etter opprettet, eldste sist)
    val elementerSomRevurderer =
        alleHistoriske
            .filter {
                behandlingerMap[it.behandlingId]?.revurdererSaksbehandlingsperiodeId != null
            }.sortedByDescending { behandlingerMap[it.behandlingId]?.opprettet }

    val elementerSomIkkeRevurderer =
        alleHistoriske
            .filter {
                behandlingerMap[it.behandlingId]?.revurdererSaksbehandlingsperiodeId == null
            }.sortedBy { behandlingerMap[it.behandlingId]?.opprettet } // Eldste først, så reverserer vi

    val sorterteHistoriske = elementerSomRevurderer + elementerSomIkkeRevurderer.reversed()

    val erHistorisk = hovedElement.behandlingId in revurderteBehandlinger

    return listOf(
        TidslinjeElement(
            fom = hovedElement.fom,
            tom = hovedElement.tom,
            skjæringstidspunkt = hovedElement.skjæringstidspunkt,
            behandlingId = hovedElement.behandlingId,
            status = hovedElement.status,
            historisk = erHistorisk,
            revurdererBehandlingId = hovedElement.revurdererBehandlingId,
            revurdertAv = hovedElement.revurdertAv,
            historiske =
                sorterteHistoriske.map {
                    TidslinjeElement(
                        fom = it.fom,
                        tom = it.tom,
                        skjæringstidspunkt = it.skjæringstidspunkt,
                        behandlingId = it.behandlingId,
                        status = it.status,
                        historisk = true,
                        revurdererBehandlingId = it.revurdererBehandlingId,
                        revurdertAv = it.revurdertAv,
                        historiske = emptyList(),
                    )
                },
        ),
    )
}

private fun grupperYrkesaktivitetKjedeElementer(
    elementer: List<YrkesaktivitetTidslinjeElement>,
    revurderteBehandlinger: Set<UUID>,
    behandlingerMap: Map<UUID, Behandling>,
): List<YrkesaktivitetTidslinjeElement> {
    if (elementer.isEmpty()) return elementer
    if (elementer.size == 1) {
        val element = elementer.first()
        val erHistorisk = element.behandlingId in revurderteBehandlinger
        return listOf(
            element.copy(
                historisk = erHistorisk,
                historiske = emptyList(),
            ),
        )
    }

    // Finn det nyeste elementet som ikke er revurdert av noen annen
    val elementerSortert = elementer.sortedByDescending { behandlingerMap[it.behandlingId]?.opprettet }

    val hovedElement =
        elementerSortert.firstOrNull { element ->
            val behandling = behandlingerMap[element.behandlingId]
            behandling?.revurdertAvBehandlingId == null
        } ?: elementerSortert.first()

    // Hvis hovedelementet revurderer en annen behandling, så skal den revurderte behandlingen legges i historiske
    val revurdertBehandlingId = behandlingerMap[hovedElement.behandlingId]?.revurdererSaksbehandlingsperiodeId
    val revurdertElement =
        revurdertBehandlingId?.let { revurdertId ->
            elementer.find { it.behandlingId == revurdertId }
        }

    val historiskeElementer = elementer.filter { it.behandlingId != hovedElement.behandlingId }

    // Legg til det revurderte elementet i historiske hvis det ikke allerede er der
    val alleHistoriske =
        if (revurdertElement != null && revurdertElement.behandlingId != hovedElement.behandlingId) {
            val eksistererAllerede = historiskeElementer.any { it.behandlingId == revurdertElement.behandlingId }
            if (!eksistererAllerede) {
                historiskeElementer + revurdertElement
            } else {
                historiskeElementer
            }
        } else {
            historiskeElementer
        }

    // Sorter historiske elementer slik at den eldste som ikke revurderer noen ligger sist
    val elementerSomRevurderer =
        alleHistoriske
            .filter {
                behandlingerMap[it.behandlingId]?.revurdererSaksbehandlingsperiodeId != null
            }.sortedByDescending { behandlingerMap[it.behandlingId]?.opprettet }

    val elementerSomIkkeRevurderer =
        alleHistoriske
            .filter {
                behandlingerMap[it.behandlingId]?.revurdererSaksbehandlingsperiodeId == null
            }.sortedBy { behandlingerMap[it.behandlingId]?.opprettet }

    val sorterteHistoriske = elementerSomRevurderer + elementerSomIkkeRevurderer.reversed()

    val erHistorisk = hovedElement.behandlingId in revurderteBehandlinger

    return listOf(
        hovedElement.copy(
            historisk = erHistorisk,
            historiske = sorterteHistoriske.map { it.copy(historisk = true, historiske = emptyList()) },
        ),
    )
}

private fun grupperTilkommenInntektKjedeElementer(
    elementer: List<TilkommenInntektTidslinjeElement>,
    revurderteBehandlinger: Set<UUID>,
    revurdererMap: Map<UUID, UUID>,
    behandlingerMap: Map<UUID, Behandling>,
): List<TilkommenInntektTidslinjeElement> {
    if (elementer.isEmpty()) return elementer
    if (elementer.size == 1) {
        val element = elementer.first()
        return listOf(element.copy(historiske = emptyList()))
    }

    // Finn det nyeste elementet som ikke er revurdert av noen annen
    val elementerSortert = elementer.sortedByDescending { behandlingerMap[it.behandlingId]?.opprettet }

    val hovedElement =
        elementerSortert.firstOrNull { element ->
            val behandling = behandlingerMap[element.behandlingId]
            behandling?.revurdertAvBehandlingId == null
        } ?: elementerSortert.first()

    // Hvis hovedelementet revurderer en annen behandling, så skal den revurderte behandlingen legges i historiske
    val revurdertBehandlingId = behandlingerMap[hovedElement.behandlingId]?.revurdererSaksbehandlingsperiodeId
    val revurdertElement =
        revurdertBehandlingId?.let { revurdertId ->
            elementer.find { it.behandlingId == revurdertId }
        }

    val historiskeElementer = elementer.filter { it.behandlingId != hovedElement.behandlingId }

    // Legg til det revurderte elementet i historiske hvis det ikke allerede er der
    val alleHistoriske =
        if (revurdertElement != null && revurdertElement.behandlingId != hovedElement.behandlingId) {
            val eksistererAllerede = historiskeElementer.any { it.behandlingId == revurdertElement.behandlingId }
            if (!eksistererAllerede) {
                historiskeElementer + revurdertElement
            } else {
                historiskeElementer
            }
        } else {
            historiskeElementer
        }

    // Sorter historiske elementer slik at den eldste som ikke revurderer noen ligger sist
    val elementerSomRevurderer =
        alleHistoriske
            .filter {
                behandlingerMap[it.behandlingId]?.revurdererSaksbehandlingsperiodeId != null
            }.sortedByDescending { behandlingerMap[it.behandlingId]?.opprettet }

    val elementerSomIkkeRevurderer =
        alleHistoriske
            .filter {
                behandlingerMap[it.behandlingId]?.revurdererSaksbehandlingsperiodeId == null
            }.sortedBy { behandlingerMap[it.behandlingId]?.opprettet }

    val sorterteHistoriske = elementerSomRevurderer + elementerSomIkkeRevurderer.reversed()

    return listOf(
        hovedElement.copy(
            historiske = sorterteHistoriske.map { it.copy(historiske = emptyList()) },
        ),
    )
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
                        tidslinjeElementer = rader.flatMap { it.tidslinjeElementer },
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

data class TilkommenInntektTidslinjeElement(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val skjæringstidspunkt: LocalDate,
    override val behandlingId: UUID,
    val tilkommenInntektId: UUID,
    override val status: BehandlingStatus,
    override val historisk: Boolean,
    override val revurdererBehandlingId: UUID?,
    override val revurdertAv: UUID?,
    override val historiske: List<TilkommenInntektTidslinjeElement>,
) : TidslinjeElement(
        fom = fom,
        tom = tom,
        behandlingId = behandlingId,
        status = status,
        skjæringstidspunkt = skjæringstidspunkt,
        historiske = historiske,
        historisk = historisk,
        revurdertAv = revurdertAv,
        revurdererBehandlingId = revurdererBehandlingId,
    )

data class YrkesaktivitetTidslinjeElement(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val skjæringstidspunkt: LocalDate,
    override val behandlingId: UUID,
    override val status: BehandlingStatus,
    override val historisk: Boolean,
    override val revurdererBehandlingId: UUID?,
    override val revurdertAv: UUID?,
    val yrkesaktivitetId: UUID,
    val ghost: Boolean,
    override val historiske: List<YrkesaktivitetTidslinjeElement>,
) : TidslinjeElement(
        fom = fom,
        tom = tom,
        behandlingId = behandlingId,
        historisk = historisk,
        status = status,
        skjæringstidspunkt = skjæringstidspunkt,
        historiske = historiske,
        revurdertAv = revurdertAv,
        revurdererBehandlingId = revurdererBehandlingId,
    )

open class TidslinjeElement(
    open val fom: LocalDate,
    open val tom: LocalDate,
    open val skjæringstidspunkt: LocalDate,
    open val behandlingId: UUID,
    open val status: BehandlingStatus,
    open val historisk: Boolean,
    open val revurdererBehandlingId: UUID?,
    open val revurdertAv: UUID?,
    open val historiske: List<TidslinjeElement>,
)

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
        override val tidslinjeElementer: List<TidslinjeElement>,
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
