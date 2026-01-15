package no.nav.helse.bakrommet.infrastruktur.db

import no.nav.helse.bakrommet.repository.BehandlingRepository
import no.nav.helse.bakrommet.repository.VilkårsvurderingRepository

interface TransactionalSessionFactory<out SessionDaosType> {
    suspend fun <RET> transactionalSessionScope(transactionalBlock: suspend (SessionDaosType) -> RET): RET
}

interface Repositories {
    val behandlingRepository: BehandlingRepository
    val vilkårsvurderingRepository: VilkårsvurderingRepository
}

interface DbDaoer<out DaosType> {
    suspend fun <RET> nonTransactional(block: suspend (DaosType.() -> RET)): RET

    suspend fun <RET> transactional(
        eksisterendeTransaksjon: @UnsafeVariance DaosType? = null,
        block: suspend (DaosType.() -> RET),
    ): RET
}
