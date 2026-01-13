package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.person.PersonPseudoIdDao
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PersonPseudoIdDaoFake : PersonPseudoIdDao {
    data class Rad(
        val pseudoId: UUID,
        val naturligIdent: NaturligIdent,
        val opprettet: Instant,
    )

    private val rader = ConcurrentHashMap<UUID, Rad>()

    override fun opprettPseudoId(
        pseudoId: UUID,
        naturligIdent: NaturligIdent,
    ) {
        rader[pseudoId] =
            Rad(
                pseudoId = pseudoId,
                naturligIdent = naturligIdent,
                opprettet = Instant.now(),
            )
    }

    override fun finnNaturligIdent(pseudoId: UUID): NaturligIdent? {
        val rad = rader[pseudoId] ?: return null
        return rad.naturligIdent
    }

    override fun finnPseudoID(naturligIdent: NaturligIdent): UUID? {
        val nittiMinutterSiden = Instant.now().minusSeconds(90 * 60)
        val rad =
            rader.values
                .filter { it.naturligIdent == naturligIdent && it.opprettet.isAfter(nittiMinutterSiden) }
                .maxByOrNull { it.opprettet }
                ?: return null
        return rad.pseudoId
    }

    override fun slettPseudoIderEldreEnn(tidspunkt: OffsetDateTime): Int {
        var cnt = 0
        rader.filter { it.value.opprettet < tidspunkt.toInstant() }.forEach {
            rader.remove(it.key)
            cnt++
        }
        return cnt
    }
}
