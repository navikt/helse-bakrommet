package no.nav.helse.bakrommet.auth

@JvmInline
value class AccessToken(
    val value: String,
)

class OAuthScope(
    val baseValue: String,
) {
    init {
        require(!baseValue.contains("api://") && !baseValue.contains("/.default")) {
            "Vennligst oppgi scope uten 'api://' og '/.default'"
        }
    }

    fun asDefaultScope(): String = "api://$baseValue/.default"
}
