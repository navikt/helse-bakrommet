package no.nav.helse.bakrommet.saksbehandlingsperiode

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.bakrommet.Daoer
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ainntekt.etInntektSvar
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.sykepengesoknad.Arbeidsgiverinfo
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.bakrommet.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import java.util.*

object SaksbehandlingsperiodeTestFixtures {
    const val TEST_FNR = "01019012349"
    const val TEST_PERSON_ID = "65hth"
    const val TEST_PERIODE_FOM = "2023-01-01"
    const val TEST_PERIODE_TOM = "2023-01-31"

    val testArbeidsgiver1 =
        Arbeidsgiverinfo(
            identifikator = "123321123",
            navn = "Bedrift AS",
        )

    val testArbeidsgiver2 =
        Arbeidsgiverinfo(
            identifikator = "654321123",
            navn = "Annen Bedrift AS",
        )

    fun lagTestSøknad(
        fnr: String = TEST_FNR,
        arbeidsgiver: Arbeidsgiverinfo? = testArbeidsgiver1,
        type: SoknadstypeDTO = SoknadstypeDTO.ARBEIDSTAKERE,
        arbeidssituasjon: ArbeidssituasjonDTO = ArbeidssituasjonDTO.ARBEIDSTAKER,
    ): JsonNode =
        enSøknad(
            fnr = fnr,
            id = UUID.randomUUID().toString(),
            type = type,
            arbeidssituasjon = arbeidssituasjon,
            arbeidsgiverinfo = arbeidsgiver,
        ).asJsonNode()

    fun lagSelvstendigSøknad(fnr: String = TEST_FNR): JsonNode =
        enSøknad(
            fnr = fnr,
            id = UUID.randomUUID().toString(),
            type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
            arbeidsgiverinfo = null,
        ).asJsonNode()

    suspend fun opprettTestPeriode(
        client: io.ktor.client.HttpClient,
        personId: String = TEST_PERSON_ID,
        fom: String = TEST_PERIODE_FOM,
        tom: String = TEST_PERIODE_TOM,
        søknader: List<String> = emptyList(),
    ): Saksbehandlingsperiode {
        val søknadIds =
            if (søknader.isNotEmpty()) {
                søknader.joinToString("\", \"", "\"", "\"")
            } else {
                ""
            }

        val søknadPart = if (søknadIds.isNotEmpty()) ", \"søknader\": [$søknadIds]" else ""

        client.post("/v1/$personId/saksbehandlingsperioder") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody("""{ "fom": "$fom", "tom": "$tom"$søknadPart }""")
        }

        return client.get("/v1/$personId/saksbehandlingsperioder") {
            bearerAuth(TestOppsett.userToken)
        }.body<List<Saksbehandlingsperiode>>().first()
    }

    suspend fun opprettTestYrkesaktivitet(
        client: io.ktor.client.HttpClient,
        personId: String = TEST_PERSON_ID,
        periodeId: UUID,
        kategorisering: String,
    ): UUID {
        val response =
            client.post("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(kategorisering)
            }
        return UUID.fromString(response.body<JsonNode>()["id"].asText())
    }

    fun lagKategoriseringArbeidstaker(
        orgnummer: String? = null,
        erSykmeldt: String? = null,
    ): String {
        val orgnummerPart = orgnummer?.let { ", \"ORGNUMMER\": \"$it\"" } ?: ""
        val sykmeldtPart = erSykmeldt?.let { ", \"ER_SYKMELDT\": \"$it\"" } ?: ""

        return """
            {
                "kategorisering": {
                    "INNTEKTSKATEGORI": "ARBEIDSTAKER"$orgnummerPart$sykmeldtPart
                }
            }
            """.trimIndent()
    }

    fun lagKategoriseringSelvstendig(
        type: String = "FISKER",
        erSykmeldt: String? = null,
    ): String {
        val sykmeldtPart = erSykmeldt?.let { ", \"ER_SYKMELDT\": \"$it\"" } ?: ""

        return """
            {
                "kategorisering": {
                    "INNTEKTSKATEGORI": "SELVSTENDIG_NÆRINGSDRIVENDE",
                    "TYPE_SELVSTENDIG_NÆRINGSDRIVENDE": "$type"$sykmeldtPart
                }
            }
            """.trimIndent()
    }

    fun testDagoversikt(): String =
        """
        [
            {
                "id": "dag-1",
                "type": "SYKEDAG",
                "dato": "2023-01-01"
            },
            {
                "id": "dag-2",
                "type": "HELGEDAG",
                "dato": "2023-01-02"
            },
            {
                "id": "dag-3",
                "type": "SYKEDAG",
                "dato": "2023-01-03"
            }
        ]
        """.trimIndent()

    fun runTestMedMocks(
        søknader: List<JsonNode> = emptyList(),
        testBlock: suspend (daoer: Daoer) -> Unit,
    ) {
        val fakeInntektSvar = etInntektSvar(fnr = TEST_FNR)
        val fakeAARegSvar = AARegMock.Person1.respV2

        val sykepengesoknadClient =
            if (søknader.isNotEmpty()) {
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    søknadIdTilSvar = søknader.associateBy { it.søknadId },
                )
            } else {
                SykepengesoknadMock.sykepengersoknadBackendClientMock(emptyMap())
            }

        runApplicationTest(
            sykepengesoknadBackendClient = sykepengesoknadClient,
            aaRegClient = AARegMock.aaRegClientMock(mapOf(TEST_FNR to fakeAARegSvar)),
            aInntektClient = AInntektMock.aInntektClientMock(mapOf(TEST_FNR to fakeInntektSvar)),
        ) { daoer ->
            daoer.personDao.opprettPerson(TEST_FNR, TEST_PERSON_ID)
            testBlock(daoer)
        }
    }

    private val JsonNode.søknadId: String get() = this["id"].asText()
}
