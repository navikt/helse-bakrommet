package no.nav.helse.bakrommet.behandling.vilkaar

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.behandling.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import java.util.UUID

fun String.erGyldigSomKode(): Boolean {
    // Først sjekk om det er en gyldig UUID
    val erGyldigUuid =
        try {
            UUID.fromString(this)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    if (erGyldigUuid) return true

    // Hvis ikke UUID, bruk opprinnelig regex
    val regex = "^[A-ZÆØÅ0-9_]*$".toRegex()
    return regex.matches(this)
}

class Kode(
    @JsonValue val kode: String,
) {
    init {
        if (!kode.erGyldigSomKode()) {
            throw InputValideringException("Ugyldig format på Kode")
        }
    }

    override fun toString(): String = kode
}

enum class OpprettetEllerEndret {
    OPPRETTET,
    ENDRET,
}

interface VilkårServiceDaoer {
    val vurdertVilkårDao: VurdertVilkårDao
    val behandlingDao: BehandlingDao
}

class VilkårService(
    private val db: DbDaoer<VilkårServiceDaoer>,
) {
    suspend fun hentVilkårsvurderingerFor(ref: SaksbehandlingsperiodeReferanse): List<VurdertVilkår> =
        db.nonTransactional {
            val periode = behandlingDao.hentPeriode(ref, krav = null, måVæreUnderBehandling = false)
            vurdertVilkårDao.hentVilkårsvurderinger(periode.id)
        }

    suspend fun leggTilEllerOpprettVurdertVilkår(
        ref: SaksbehandlingsperiodeReferanse,
        vilkårsKode: Kode,
        vurdertVilkår: JsonNode,
        saksbehandler: Bruker,
    ): Pair<VurdertVilkår, OpprettetEllerEndret> =
        db.transactional {
            val periode = behandlingDao.hentPeriode(ref, krav = saksbehandler.erSaksbehandlerPåSaken())
            val finnesFraFør = vurdertVilkårDao.eksisterer(periode, vilkårsKode)
            val opprettetEllerEndret =
                if (finnesFraFør) {
                    vurdertVilkårDao.oppdater(periode, vilkårsKode, vurdertVilkår)
                    OpprettetEllerEndret.ENDRET
                } else {
                    vurdertVilkårDao.leggTil(periode, vilkårsKode, vurdertVilkår)
                    OpprettetEllerEndret.OPPRETTET
                }
            Pair(
                vurdertVilkårDao.hentVilkårsvurdering(periode.id, vilkårsKode.kode)!!,
                opprettetEllerEndret,
            )
        }

    suspend fun slettVilkårsvurdering(
        ref: SaksbehandlingsperiodeReferanse,
        vilkårsKode: Kode,
        saksbehandler: Bruker,
    ): Boolean =
        db.transactional {
            val periode = behandlingDao.hentPeriode(ref, krav = saksbehandler.erSaksbehandlerPåSaken())
            val numAffectedRows = vurdertVilkårDao.slettVilkårsvurdering(periode.id, vilkårsKode.kode)
            (numAffectedRows > 0)
        }
}
