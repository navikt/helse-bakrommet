package no.nav.helse.bakrommet.behandling.vilkaar

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetServiceDaoer
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import java.util.*

fun String.erGyldigSomKode(): Boolean {
    // Først sjekk om det er en gyldig UUID
    val erGyldigUuid =
        try {
            UUID.fromString(this)
            true
        } catch (_: IllegalArgumentException) {
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

interface VilkårServiceDaoer :
    YrkesaktivitetServiceDaoer,
    Beregningsdaoer {
    override val behandlingDao: BehandlingDao
}

class VilkårServiceOld(
    private val db: DbDaoer<VilkårServiceDaoer>,
) {
    suspend fun hentVilkårsvurderingerFor(ref: BehandlingReferanse): List<LegacyVurdertVilkår> =
        db.nonTransactional {
            val periode = behandlingDao.hentPeriode(ref, krav = null, måVæreUnderBehandling = false)
            vurdertVilkårDao.hentVilkårsvurderinger(periode.id)
        }
}
