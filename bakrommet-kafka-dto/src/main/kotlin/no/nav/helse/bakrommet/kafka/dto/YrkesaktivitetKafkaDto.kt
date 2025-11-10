package no.nav.helse.bakrommet.kafka.dto

import java.util.*

// TODO typ strengt når landet
data class YrkesaktivitetKafkaDto(
    val id: UUID,
    val kategorisering: Map<String, String>,
    val dagoversikt: List<DagKafkaDto>?,
)
