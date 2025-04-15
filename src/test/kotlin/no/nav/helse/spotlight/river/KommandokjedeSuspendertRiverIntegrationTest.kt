package no.nav.helse.spotlight.river

import no.nav.helse.spotlight.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertNotNull

class KommandokjedeSuspendertRiverIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `Lagrer ny suspendert kommandokjede basert på melding`() {
        // Given:
        val commandContextId = UUID.randomUUID()
        val meldingId = UUID.randomUUID()
        val partisjonsnøkkel = UUID.randomUUID().toString()

        // When:
        testRapid.sendTestMessage(
            message =
                """
                {
                  "@event_name": "kommandokjede_suspendert",
                  "commandContextId": "$commandContextId",
                  "meldingId": "$meldingId",
                  "command": "EnCommand",
                  "sti": [0],
                  "@opprettet": "2022-02-22T12:34:56.789101987"
                }
                """.trimIndent(),
            key = partisjonsnøkkel,
        )

        // Then:
        dao.finn(commandContextId).let(::assertNotNull).let {
            assertEquals(commandContextId, it.commandContextId)
            assertEquals("EnCommand", it.command)
            assertEquals("2022-02-22T11:34:56.789102Z", it.førsteTidspunkt.toString())
            assertEquals("2022-02-22T11:34:56.789102Z", it.sisteTidspunkt.toString())
            assertEquals(meldingId, it.sisteMeldingId)
            assertEquals(partisjonsnøkkel, it.sistePartisjonsnøkkel)
            assertEquals(0, it.totaltAntallGangerPåminnet)
            assertEquals("[ 0 ]", it.sistSuspenderteSti.sti)
            assertEquals("2022-02-22T11:34:56.789102Z", it.sistSuspenderteSti.førsteTidspunkt.toString())
            assertEquals(0, it.sistSuspenderteSti.antallGangerPåminnet)
        }
    }

    @Test
    fun `Oppdaterer suspendert kommandokjede når ny melding kommer inn for samme kjede og sti`() {
        // Given:
        val eksisterendeKommandokjede = lagretKommandokjede()
        val nyMeldingId = UUID.randomUUID()
        val partisjonsnøkkel = UUID.randomUUID().toString()

        // When:
        testRapid.sendTestMessage(
            message =
                """
                {
                  "@event_name": "kommandokjede_suspendert",
                  "commandContextId": "${eksisterendeKommandokjede.commandContextId}",
                  "meldingId": "$nyMeldingId",
                  "command": "${eksisterendeKommandokjede.command}",
                  "sti": ${eksisterendeKommandokjede.sistSuspenderteSti.sti},
                  "@opprettet": "2022-02-22T13:04:56.789101987"
                }
                """.trimIndent(),
            key = partisjonsnøkkel,
        )

        // Then:
        dao.finn(eksisterendeKommandokjede.commandContextId).let(::assertNotNull).let {
            assertEquals(eksisterendeKommandokjede.command, it.command)
            assertEquals(eksisterendeKommandokjede.førsteTidspunkt, it.førsteTidspunkt)
            assertEquals("2022-02-22T12:04:56.789102Z", it.sisteTidspunkt.toString())
            assertEquals(nyMeldingId, it.sisteMeldingId)
            assertEquals(partisjonsnøkkel, it.sistePartisjonsnøkkel)
            assertEquals(eksisterendeKommandokjede.totaltAntallGangerPåminnet, it.totaltAntallGangerPåminnet)
            assertEquals(eksisterendeKommandokjede.sistSuspenderteSti, it.sistSuspenderteSti)
        }
    }

    @Test
    fun `Oppdaterer suspendert kommandokjede med ny sti når ny melding kommer inn for samme kjede og annen sti`() {
        // Given:
        val eksisterendeKommandokjede = lagretKommandokjede(sti = "[ 0 ]", antallGangerPåminnet = 1)
        val nyMeldingId = UUID.randomUUID()
        val partisjonsnøkkel = UUID.randomUUID().toString()

        // When:
        testRapid.sendTestMessage(
            message =
                """
                {
                  "@event_name": "kommandokjede_suspendert",
                  "commandContextId": "${eksisterendeKommandokjede.commandContextId}",
                  "meldingId": "$nyMeldingId",
                  "command": "${eksisterendeKommandokjede.command}",
                  "sti": [1,2],
                  "@opprettet": "2022-02-22T13:04:56.789101987"
                }
                """.trimIndent(),
            key = partisjonsnøkkel,
        )

        // Then:
        dao.finn(eksisterendeKommandokjede.commandContextId).let(::assertNotNull).let {
            assertEquals(eksisterendeKommandokjede.command, it.command)
            assertEquals(eksisterendeKommandokjede.førsteTidspunkt, it.førsteTidspunkt)
            assertEquals("2022-02-22T12:04:56.789102Z", it.sisteTidspunkt.toString())
            assertEquals(nyMeldingId, it.sisteMeldingId)
            assertEquals(partisjonsnøkkel, it.sistePartisjonsnøkkel)
            assertEquals(eksisterendeKommandokjede.totaltAntallGangerPåminnet, it.totaltAntallGangerPåminnet)
            assertEquals("[ 1, 2 ]", it.sistSuspenderteSti.sti)
            assertEquals("2022-02-22T12:04:56.789102Z", it.sistSuspenderteSti.førsteTidspunkt.toString())
            assertEquals(0, it.sistSuspenderteSti.antallGangerPåminnet)
        }
    }
}
