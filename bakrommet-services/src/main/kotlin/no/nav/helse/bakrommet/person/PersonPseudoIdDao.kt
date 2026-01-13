package no.nav.helse.bakrommet.person

import no.nav.helse.bakrommet.domain.person.NaturligIdent
import java.time.OffsetDateTime
import java.util.UUID

interface PersonPseudoIdDao {
    fun opprettPseudoId(
        pseudoId: UUID,
        naturligIdent: NaturligIdent,
    )

    fun finnNaturligIdent(pseudoId: UUID): NaturligIdent?

    fun finnPseudoID(naturligIdent: NaturligIdent): UUID?

    fun slettPseudoIderEldreEnn(tidspunkt: OffsetDateTime): Int
}
