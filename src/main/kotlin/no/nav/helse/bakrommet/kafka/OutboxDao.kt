package no.nav.helse.bakrommet.kafka

import kotliquery.Session
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import java.time.Instant
import javax.sql.DataSource

class OutboxDao private constructor(private val db: QueryRunner) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun lagreTilOutbox(
        kafkaKey: String,
        kafkaPayload: String,
    ) {
        db.update(
            """
            insert into kafka_outbox
                (kafka_key, kafka_payload, opprettet)
            values
                (:kafka_key, :kafka_payload, :opprettet)
            """.trimIndent(),
            "kafka_key" to kafkaKey,
            "kafka_payload" to kafkaPayload,
            "opprettet" to Instant.now(),
        )
    }
}
