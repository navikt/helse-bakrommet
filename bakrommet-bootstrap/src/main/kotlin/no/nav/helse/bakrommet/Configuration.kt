package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.aareg.AAregModule
import no.nav.helse.bakrommet.ainntekt.AInntektModule
import no.nav.helse.bakrommet.api.ApiModule
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.db.DBModule
import no.nav.helse.bakrommet.ereg.EregClientModule
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClientModule
import no.nav.helse.bakrommet.obo.OboModule
import no.nav.helse.bakrommet.pdl.PdlClientModule
import no.nav.helse.bakrommet.sigrun.SigrunClientModule
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesøknadBackendClientModule

data class Configuration(
    val aareg: AAregModule.Configuration,
    val obo: OboModule.Configuration,
    val pdl: PdlClientModule.Configuration,
    val sykepengesoknadBackend: SykepengesøknadBackendClientModule.Configuration,
    val ainntekt: AInntektModule.Configuration,
    val ereg: EregClientModule.Configuration,
    val inntektsmelding: InntektsmeldingClientModule.Configuration,
    val sigrun: SigrunClientModule.Configuration,
    val db: DBModule.Configuration,
    val api: ApiModule.Configuration,
    val naisClusterName: String,
) {
    companion object {
        private fun String.asSet() = this.split(",").map { it.trim() }.toSet()

        fun fromEnv(env: Map<String, String> = System.getenv()): Configuration =
            Configuration(
                api =
                    ApiModule.Configuration(
                        auth =
                            ApiModule.Configuration.Auth(
                                clientId = env.getValue("AZURE_APP_CLIENT_ID"),
                                issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
                                jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
                                tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
                            ),
                        roller =
                            ApiModule.Configuration.Roller(
                                les = env.getValue("ROLLE_GRUPPER_LES").asSet(),
                                saksbehandler = env.getValue("ROLLE_GRUPPER_SAKSBEHANDLER").asSet(),
                                beslutter = env.getValue("ROLLE_GRUPPER_BESLUTTER").asSet(),
                            ),
                    ),
                db = DBModule.Configuration(jdbcUrl = env.getValue("DATABASE_JDBC_URL")),
                obo = OboModule.Configuration(url = env.getValue("NAIS_TOKEN_EXCHANGE_ENDPOINT")),
                pdl =
                    PdlClientModule.Configuration(
                        scope = OAuthScope(env.getValue("PDL_SCOPE")),
                        hostname = env.getValue("PDL_HOSTNAME"),
                    ),
                sykepengesoknadBackend =
                    SykepengesøknadBackendClientModule.Configuration(
                        scope = OAuthScope(env.getValue("SYKEPENGESOKNAD_BACKEND_SCOPE")),
                        hostname = env.getValue("SYKEPENGESOKNAD_BACKEND_HOSTNAME"),
                    ),
                aareg =
                    AAregModule.Configuration(
                        scope = OAuthScope(env.getValue("AAREG_SCOPE")),
                        hostname = env.getValue("AAREG_HOSTNAME"),
                    ),
                ainntekt =
                    AInntektModule.Configuration(
                        scope = OAuthScope(env.getValue("INNTEKTSKOMPONENTEN_SCOPE")),
                        hostname = env.getValue("INNTEKTSKOMPONENTEN_HOSTNAME"),
                    ),
                ereg =
                    EregClientModule.Configuration(
                        baseUrl = env.getValue("EREG_SERVICES_BASE_URL"),
                    ),
                inntektsmelding =
                    InntektsmeldingClientModule.Configuration(
                        scope = OAuthScope(env.getValue("INNTEKTSMELDING_SCOPE")),
                        baseUrl = env.getValue("INNTEKTSMELDING_BASE_URL"),
                    ),
                sigrun =
                    SigrunClientModule.Configuration(
                        scope = OAuthScope(env.getValue("SIGRUN_SCOPE")),
                        baseUrl = env.getValue("SIGRUN_URL"),
                    ),
                naisClusterName = env.getValue("NAIS_CLUSTER_NAME"),
            )
    }
}
