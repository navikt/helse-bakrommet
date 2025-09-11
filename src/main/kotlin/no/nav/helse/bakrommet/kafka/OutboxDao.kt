package no.nav.helse.bakrommet.kafka

import kotliquery.Session
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import java.time.Instant
import javax.sql.DataSource

data class OutboxEntry(
    val id: Long,
    val kafkaKey: String,
    val kafkaPayload: String,
    val opprettet: Instant,
    val publisert: Instant?,
)

data class KafkaMelding(
    val kafkaKey: String,
    val kafkaPayload: String,
)

class OutboxDao private constructor(private val db: QueryRunner) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun lagreTilOutbox(kafkaMelding: KafkaMelding) {
        db.update(
            """
            insert into kafka_outbox
                (kafka_key, kafka_payload, opprettet)
            values
                (:kafka_key, :kafka_payload, :opprettet)
            """.trimIndent(),
            "kafka_key" to kafkaMelding.kafkaKey,
            "kafka_payload" to kafkaMelding.kafkaPayload,
            "opprettet" to Instant.now(),
        )
    }

    fun markerSomPublisert(id: Long) {
        db.update(
            """
            update kafka_outbox
            set publisert = :publisert
            where id = :id
            """.trimIndent(),
            "publisert" to Instant.now(),
            "id" to id,
        )
    }

    fun hentAlleUpubliserteEntries(): List<OutboxEntry> {
        return db.list(
            """
            select id, kafka_key, kafka_payload, opprettet, publisert
            from kafka_outbox
            where publisert is null
            order by id
            """.trimIndent(),
        ) { row ->
            OutboxEntry(
                id = row.long("id"),
                kafkaKey = row.string("kafka_key"),
                kafkaPayload = row.string("kafka_payload"),
                opprettet = row.instant("opprettet"),
                publisert = row.instantOrNull("publisert"),
            )
        }
    }
}
