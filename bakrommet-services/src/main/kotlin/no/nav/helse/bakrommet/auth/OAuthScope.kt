package no.nav.helse.bakrommet.auth

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
