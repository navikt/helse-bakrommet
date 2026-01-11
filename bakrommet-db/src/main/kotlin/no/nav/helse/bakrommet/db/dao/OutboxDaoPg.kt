package no.nav.helse.bakrommet.db.dao

import kotliquery.Session
import no.nav.helse.bakrommet.db.MedDataSource
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.db.tilPgJson
import no.nav.helse.bakrommet.kafka.KafkaMelding
import no.nav.helse.bakrommet.kafka.OutboxDao
import no.nav.helse.bakrommet.kafka.OutboxDbRecord
import java.time.Instant
import javax.sql.DataSource

class OutboxDaoPg private constructor(
    private val db: QueryRunner,
) : OutboxDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun lagreTilOutbox(kafkaMelding: KafkaMelding) {
        db.update(
            """
            insert into kafka_outbox
                (key, payload, opprettet, topic)
            values
                (:key, :payload, :opprettet, :topic)
            """.trimIndent(),
            "key" to kafkaMelding.key,
            "payload" to kafkaMelding.payload.tilPgJson(),
            "opprettet" to Instant.now(),
            "topic" to kafkaMelding.topic,
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
            select id, key, payload, opprettet, publisert, topic
            from kafka_outbox
            where publisert is null
            order by id
            """.trimIndent(),
        ) { row ->
            OutboxDbRecord(
                id = row.long("id"),
                kafkaKey = row.string("key"),
                kafkaPayload = row.string("payload"),
                opprettet = row.instant("opprettet"),
                publisert = row.instantOrNull("publisert"),
                topic = row.string("topic"),
            )
        }
}
