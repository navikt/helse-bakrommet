package no.nav.helse.bakrommet.person

import no.nav.helse.bakrommet.util.insert
import no.nav.helse.bakrommet.util.single
import no.nav.helse.bakrommet.util.somDbArray
import javax.sql.DataSource

internal class PersonDao(private val dataSource: DataSource) {
    fun finnPersonId(vararg ident: String): String? {
        return dataSource.single(
            "select spillerom_id from ident where naturlig_ident = ANY (:ident::varchar[])",
            "ident" to ident.toSet().somDbArray(),
        ) { it.string(1) }
    }

    fun opprettPerson(
        naturligIdent: String,
        spilleromId: String,
    ) {
        dataSource.insert(
            """
            insert into ident (spillerom_id, naturlig_ident)
            values (:spillerom_id, :naturlig_ident)
            """.trimIndent(),
            "naturlig_ident" to naturligIdent,
            "spillerom_id" to spilleromId,
        )
    }
}
