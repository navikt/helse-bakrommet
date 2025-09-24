package no.nav.helse.bakrommet.auth

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class OAuthScopeTest {
    @Test
    fun `OAuthScope genererer riktig format på default scope`() {
        val scope = "dev-gcp.et_namespace:en_app"
        val oAuthScope = OAuthScope(scope)
        assertEquals("api://$scope/.default", oAuthScope.asDefaultScope())
    }

    @Test
    fun `OAuthScope nekter instansiering ved uventet format på input`() {
        val scope = "dev-gcp.et_namespace:en_app"

        assertThrows<IllegalArgumentException> { OAuthScope("api://$scope") }
        assertThrows<IllegalArgumentException> { OAuthScope("$scope/.default") }
    }
}
