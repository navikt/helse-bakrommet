package no.nav.helse.bakrommet.db

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockingTaskExecutor
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.helse.bakrommet.`LåsProvider`
import no.nav.helse.bakrommet.appLogger
import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

class Shedlock(
    private val navnPåLås: String,
    private val dataSource: DataSource,
) : LåsProvider {
    override fun <T : Any> kjørMedLås(
        iMinst: Duration,
        maksimalt: Duration,
        block: () -> T,
    ): T {
        appLogger.debug("Utfører arbeid med lås $navnPåLås")
        val lockProvider = JdbcLockProvider(dataSource, "shedlock")
        val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)
        val task =
            LockingTaskExecutor.TaskWithResult<T> {
                try {
                    block()
                } catch (e: Exception) {
                    appLogger.error("Feil under utførelse av arbeid", e)
                    throw e
                }
            }
        val taskResult =
            lockingExecutor.executeWithLock(
                task,
                LockConfiguration(Instant.now(), navnPåLås, iMinst, maksimalt),
            )
        appLogger.debug("Arbeid m/lås $navnPåLås ble kjørt? : {}", taskResult.wasExecuted())
        return taskResult.result ?: error("Mangler resultat av arbeid")
    }
}
