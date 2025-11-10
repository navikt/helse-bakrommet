package no.nav.helse.bakrommet.kafka

import kotliquery.Session
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import java.time.Instant
import javax.sql.DataSource

data class OutboxDbRecord(
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

interface OutboxDao {
    fun lagreTilOutbox(kafkaMelding: KafkaMelding)

    fun markerSomPublisert(id: Long)

    fun hentAlleUpubliserteEntries(): List<OutboxDbRecord>
}

class OutboxDaoPg private constructor(
    private val db: QueryRunner,
) : OutboxDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun lagreTilOutbox(kafkaMelding: KafkaMelding) {
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

    override fun markerSomPublisert(id: Long) {
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

    override fun hentAlleUpubliserteEntries(): List<OutboxDbRecord> =
        db.list(
            """
            select id, kafka_key, kafka_payload, opprettet, publisert
            from kafka_outbox
            where publisert is null
            order by id
            """.trimIndent(),
        ) { row ->
            OutboxDbRecord(
                id = row.long("id"),
                kafkaKey = row.string("kafka_key"),
                kafkaPayload = row.string("kafka_payload"),
                opprettet = row.instant("opprettet"),
                publisert = row.instantOrNull("publisert"),
            )
        }
}
