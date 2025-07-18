package no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode

fun String.erGyldigSomKode(): Boolean {
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
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
}

class VilkårService(
    daoer: VilkårServiceDaoer,
    sessionFactory: TransactionalSessionFactory<VilkårServiceDaoer>,
) {
    private val db = DbDaoer(daoer, sessionFactory)

    fun hentVilkårsvurderingerFor(ref: SaksbehandlingsperiodeReferanse): List<VurdertVilkår> =
        db.nonTransactional {
            val periode = saksbehandlingsperiodeDao.hentPeriode(ref, krav = null)
            vurdertVilkårDao.hentVilkårsvurderinger(periode.id)
        }

    fun leggTilEllerOpprettVurdertVilkår(
        ref: SaksbehandlingsperiodeReferanse,
        vilkårsKode: Kode,
        vurdertVilkår: JsonNode,
        saksbehandler: Bruker,
    ): Pair<VurdertVilkår, OpprettetEllerEndret> =
        db.transactional {
            val periode = saksbehandlingsperiodeDao.hentPeriode(ref, krav = saksbehandler.erSaksbehandlerPåSaken())
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

    fun slettVilkårsvurdering(
        ref: SaksbehandlingsperiodeReferanse,
        vilkårsKode: Kode,
        saksbehandler: Bruker,
    ): Boolean =
        db.transactional {
            val periode = saksbehandlingsperiodeDao.hentPeriode(ref, krav = saksbehandler.erSaksbehandlerPåSaken())
            val numAffectedRows = vurdertVilkårDao.slettVilkårsvurdering(periode.id, vilkårsKode.kode)
            (numAffectedRows > 0)
        }
}
