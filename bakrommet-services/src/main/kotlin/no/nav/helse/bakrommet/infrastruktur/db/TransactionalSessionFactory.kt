package no.nav.helse.bakrommet.infrastruktur.db

interface TransactionalSessionFactory<out SessionDaosType> {
    suspend fun <RET> transactionalSessionScope(transactionalBlock: suspend (SessionDaosType) -> RET): RET
}

interface DbDaoer<out DaosType> {
    suspend fun <RET> nonTransactional(block: suspend (DaosType.() -> RET)): RET

    suspend fun <RET> transactional(
        eksisterendeTransaksjon: @UnsafeVariance DaosType? = null,
        block: suspend (DaosType.() -> RET),
    ): RET
}
