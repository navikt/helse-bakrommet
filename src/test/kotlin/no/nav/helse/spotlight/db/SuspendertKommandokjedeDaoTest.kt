package no.nav.helse.spotlight.db

import no.nav.helse.spotlight.AbstractIntegrationTest
import no.nav.helse.spotlight.SuspendertKommandokjede
import no.nav.helse.spotlight.SuspendertKommandokjede.Sti
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoField
import java.util.*
import kotlin.test.assertEquals

class SuspendertKommandokjedeDaoTest : AbstractIntegrationTest() {
    @Test
    fun `Lagrer ny kommandokjede`() {
        // Given:
        val kommandokjede =
            SuspendertKommandokjede(
                commandContextId = UUID.randomUUID(),
                command = "EnCommand",
                førsteTidspunkt = Instant.now(),
                sisteTidspunkt = Instant.now(),
                sisteMeldingId = UUID.randomUUID(),
                sistePartisjonsnøkkel = "${UUID.randomUUID()}",
                totaltAntallGangerPåminnet = 0,
                sistSuspenderteSti =
                    Sti(
                        sti = "[ 0 ]",
                        førsteTidspunkt = Instant.now(),
                        antallGangerPåminnet = 0,
                    ),
            )
        val commandContextId = kommandokjede.commandContextId

        // When:
        dao.insert(kommandokjede)

        // Then:
        assertEquals(kommandokjede.rundetTilMikrosekunder(), dao.finn(commandContextId))
    }

    private fun SuspendertKommandokjede.rundetTilMikrosekunder() =
        copy(
            førsteTidspunkt = førsteTidspunkt.rundetTilMikrosekunder(),
            sisteTidspunkt = sisteTidspunkt.rundetTilMikrosekunder(),
            sistSuspenderteSti =
                sistSuspenderteSti.copy(
                    førsteTidspunkt = sistSuspenderteSti.førsteTidspunkt.rundetTilMikrosekunder(),
                ),
        )

    private fun Instant.rundetTilMikrosekunder(): Instant =
        with(ChronoField.MICRO_OF_SECOND, nano / 1000L + if (nano % 1000 >= 500) 1 else 0)

    @Test
    fun `Henter kommandokjede som er mer enn 30 minutter gammel`() {
        // Given:
        val kommandokjede =
            lagretKommandokjede(
                mottattTidspunkt = Instant.now().minusMinutes(31),
            )

        // When:
        val kommandokjederEldreEnnEnHalvtime = dao.finnAlleEldreEnnEnHalvtime()

        // Then:
        assertEquals(1, kommandokjederEldreEnnEnHalvtime.size)
        assertEquals(kommandokjede, kommandokjederEldreEnnEnHalvtime.first())
    }

    @Test
    fun `Henter ikke kommandokjede som er mindre enn 30 minutter gammel`() {
        // Given:
        lagretKommandokjede(
            mottattTidspunkt = Instant.now().minusMinutes(29),
        )

        // When:
        val kommandokjederEldreEnnEnHalvtime = dao.finnAlleEldreEnnEnHalvtime()

        // Then:
        assertEquals(0, kommandokjederEldreEnnEnHalvtime.size)
    }

    @Test
    fun `Oppdaterer kommandokjede som allerede finnes`() {
        // Given:
        val kommandokjede = lagretKommandokjede()
        val commandContextId = kommandokjede.commandContextId
        val oppdatertKommandokjede = kommandokjede.copy(command = "EnAnnenCommand")

        // When:
        dao.update(oppdatertKommandokjede)

        // Then:
        assertEquals(oppdatertKommandokjede, dao.finn(commandContextId))
    }

    @Test
    fun `Sletter kommandokjede`() {
        // Given:
        val kommandokjede = lagretKommandokjede()
        val commandContextId = kommandokjede.commandContextId

        // When:
        dao.slett(commandContextId)

        // Then:
        assertEquals(null, dao.finn(commandContextId))
    }
}
