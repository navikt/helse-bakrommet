package no.nav.helse.bakrommet.db.repository

import kotliquery.sessionOf
import no.nav.helse.bakrommet.db.DBTestFixture
import org.junit.jupiter.api.AfterEach

abstract class RepositoryTest {
    private val dataSource = DBTestFixture.module.dataSource
    protected val session = sessionOf(dataSource)

    @AfterEach
    fun tearDown() {
        session.close()
    }
}
