package no.nav.helse.bakrommet.pdl

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.bakrommet.Configuration

class PdlClient(
    private val configuration: Configuration.PDL,
) {
    private val hentIdenterMedHistorikkQuery =
        """
        query(${"$"}ident: ID!){
          hentIdenter(ident: ${"$"}ident, historikk: true) {
            identer {
              ident,
              gruppe
            }
          }
        }
        """.trimIndent()

    private fun hentIdenterRequest(ident: String): String {
        val m = jacksonObjectMapper()
        return m.createObjectNode().apply {
            put("query", hentIdenterMedHistorikkQuery)
            set<ObjectNode>(
                "variables",
                m.createObjectNode().apply {
                    put("ident", ident)
                },
            )
        }.toString()
    }

    fun hentIdenterFor(
        pdlToken: String,
        ident: String,
    ): List<String> {
        return emptyList()
    }
}
