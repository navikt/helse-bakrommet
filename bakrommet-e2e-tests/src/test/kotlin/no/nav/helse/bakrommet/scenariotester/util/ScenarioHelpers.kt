package no.nav.helse.bakrommet.scenariotester.util

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.FrilanserInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.PensjonsgivendeInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.BeregningResponseUtDto
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDTO
import no.nav.helse.bakrommet.serde.objectMapperCustomSerde
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.util.*

internal suspend fun ApplicationTestBuilder.opprettSaksbehandlingsperiode(
    personId: String,
    fom: LocalDate,
    tom: LocalDate,
): Saksbehandlingsperiode {
    val response =
        client.post("/v1/$personId/saksbehandlingsperioder") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody("""{ "fom": "$fom", "tom": "$tom" }""")
        }

    assertEquals(201, response.status.value, "Saksbehandlingsperiode skal opprettes med status 201")

    val responseBody = response.body<JsonNode>()
    assertTrue(responseBody.has("id"), "Response skal inneholde periode ID")
    val periodeId = UUID.fromString(responseBody["id"].asText())
    assertTrue(periodeId != null, "Periode ID skal være gyldig UUID")

    val getResponse =
        client.get("/v1/$personId/saksbehandlingsperioder") {
            bearerAuth(TestOppsett.userToken)
        }

    assertEquals(200, getResponse.status.value, "Henting av saksbehandlingsperioder skal returnere status 200")

    val json = getResponse.body<String>()
    val perioder = objectMapperCustomSerde.readValue<List<Saksbehandlingsperiode>>(json, objectMapperCustomSerde.typeFactory.constructCollectionType(List::class.java, Saksbehandlingsperiode::class.java))

    assertTrue(perioder.isNotEmpty(), "Det skal finnes minst én saksbehandlingsperiode")
    val periode = perioder.first()
    assertEquals(periodeId, periode.id, "Periode ID skal matche det som ble opprettet")

    return periode
}

internal suspend fun ApplicationTestBuilder.opprettArbeidstakerYrkesaktivitet(
    periodeId: UUID,
    personId: String,
    orgnr: String,
): UUID {
    val kategorisering =
        """
        {
            "kategorisering": {
                "INNTEKTSKATEGORI": "ARBEIDSTAKER",
                "ORGNUMMER": "$orgnr",
                "ER_SYKMELDT": "ER_SYKMELDT_JA",
                "TYPE_ARBEIDSTAKER": "ORDINÆRT_ARBEIDSFORHOLD"
            }
        }
        """.trimIndent()

    val response =
        client.post("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(kategorisering)
        }

    assertEquals(201, response.status.value, "Arbeidstaker yrkesaktivitet skal opprettes med status 201")
    val body = response.body<JsonNode>()

    assertTrue(body.has("id"), "Response skal inneholde yrkesaktivitet ID")
    assertTrue(body.has("kategorisering"), "Response skal inneholde kategorisering")

    val yrkesaktivitetId = UUID.fromString(body["id"].asText())
    assertTrue(yrkesaktivitetId != null, "Yrkesaktivitet ID skal være gyldig UUID")

    assertEquals("ARBEIDSTAKER", body["kategorisering"]["INNTEKTSKATEGORI"].asText(), "Inntektskategori skal være ARBEIDSTAKER")
    assertEquals(orgnr, body["kategorisering"]["ORGNUMMER"].asText(), "Organisasjonsnummer skal være satt")
    assertEquals("ER_SYKMELDT_JA", body["kategorisering"]["ER_SYKMELDT"].asText(), "Er sykmeldt skal være JA")
    assertEquals("ORDINÆRT_ARBEIDSFORHOLD", body["kategorisering"]["TYPE_ARBEIDSTAKER"].asText(), "Type arbeidstaker skal være ORDINÆRT")

    return yrkesaktivitetId
}

internal suspend fun ApplicationTestBuilder.opprettNaeringsdrivendeYrkesaktivitet(
    periodeId: UUID,
    personId: String,
): UUID {
    val kategorisering =
        """
        {
            "kategorisering": {
                "INNTEKTSKATEGORI": "SELVSTENDIG_NÆRINGSDRIVENDE",
                "ER_SYKMELDT": "ER_SYKMELDT_JA",
                "TYPE_SELVSTENDIG_NÆRINGSDRIVENDE": "FISKER"
            }
        }
        """.trimIndent()

    val response =
        client.post("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(kategorisering)
        }

    assertEquals(201, response.status.value, "Næringsdrivende yrkesaktivitet skal opprettes med status 201")
    val body = response.body<JsonNode>()

    assertTrue(body.has("id"), "Response skal inneholde yrkesaktivitet ID")
    assertTrue(body.has("kategorisering"), "Response skal inneholde kategorisering")

    val yrkesaktivitetId = UUID.fromString(body["id"].asText())
    assertTrue(yrkesaktivitetId != null, "Yrkesaktivitet ID skal være gyldig UUID")

    assertEquals("SELVSTENDIG_NÆRINGSDRIVENDE", body["kategorisering"]["INNTEKTSKATEGORI"].asText(), "Inntektskategori skal være SELVSTENDIG_NÆRINGSDRIVENDE")
    assertEquals("ER_SYKMELDT_JA", body["kategorisering"]["ER_SYKMELDT"].asText(), "Er sykmeldt skal være JA")
    assertEquals("FISKER", body["kategorisering"]["TYPE_SELVSTENDIG_NÆRINGSDRIVENDE"].asText(), "Type næringsdrivende skal være FISKER")

    return yrkesaktivitetId
}

internal suspend fun ApplicationTestBuilder.opprettFrilanserYrkesaktivitet(
    periodeId: UUID,
    personId: String,
): UUID {
    val kategorisering =
        """
        {
            "kategorisering": {
                "INNTEKTSKATEGORI": "FRILANSER",
                "ORGNUMMER": "987654321",
                "ER_SYKMELDT": "ER_SYKMELDT_JA",
                "FRILANSER_FORSIKRING": "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG"
            }
        }
        """.trimIndent()

    val response =
        client.post("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(kategorisering)
        }

    assertEquals(201, response.status.value, "Frilanser yrkesaktivitet skal opprettes med status 201")
    val body = response.body<JsonNode>()

    assertTrue(body.has("id"), "Response skal inneholde yrkesaktivitet ID")
    assertTrue(body.has("kategorisering"), "Response skal inneholde kategorisering")

    val yrkesaktivitetId = UUID.fromString(body["id"].asText())
    assertTrue(yrkesaktivitetId != null, "Yrkesaktivitet ID skal være gyldig UUID")

    assertEquals("FRILANSER", body["kategorisering"]["INNTEKTSKATEGORI"].asText(), "Inntektskategori skal være FRILANSER")
    assertEquals("987654321", body["kategorisering"]["ORGNUMMER"].asText(), "Organisasjonsnummer skal være satt")
    assertEquals("ER_SYKMELDT_JA", body["kategorisering"]["ER_SYKMELDT"].asText(), "Er sykmeldt skal være JA")
    assertEquals("FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG", body["kategorisering"]["FRILANSER_FORSIKRING"].asText(), "Frilanser forsikring skal være 100% fra første sykedag")

    return yrkesaktivitetId
}

internal suspend fun ApplicationTestBuilder.oppdaterArbeidstakerInntektMedInntektsmelding(
    periodeId: UUID,
    yrkesaktivitetId: UUID,
    inntektsmeldingId: String,
    personId: String,
) {
    val inntektRequest =
        InntektRequest.Arbeidstaker(
            data =
                ArbeidstakerInntektRequest.Inntektsmelding(
                    inntektsmeldingId = inntektsmeldingId,
                    begrunnelse = "Velger inntektsmelding for arbeidstaker",
                    refusjon = emptyList(),
                ),
        )

    val response =
        client.put("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(objectMapperCustomSerde.writeValueAsString(inntektRequest))
        }

    assertEquals(204, response.status.value, "Inntektsoppdatering for arbeidstaker skal returnere status 204")
}

internal suspend fun ApplicationTestBuilder.oppdaterFrilanserInntektMedAInntekt(
    periodeId: UUID,
    yrkesaktivitetId: UUID,
    personId: String,
) {
    val inntektRequest =
        InntektRequest.Frilanser(
            data =
                FrilanserInntektRequest.Ainntekt(
                    begrunnelse = "Bruker a-inntekt for frilanser",
                ),
        )

    val response =
        client.put("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(objectMapperCustomSerde.writeValueAsString(inntektRequest))
        }

    assertEquals(204, response.status.value, "Inntektsoppdatering for frilanser skal returnere status 204")
}

internal suspend fun ApplicationTestBuilder.oppdaterNaeringsdrivendeInntektMedPensjonsgivendeInntekt(
    periodeId: UUID,
    yrkesaktivitetId: UUID,
    personId: String,
) {
    val inntektRequest =
        InntektRequest.SelvstendigNæringsdrivende(
            data =
                PensjonsgivendeInntektRequest.PensjonsgivendeInntekt(
                    begrunnelse = "Bruker pensjonsgivende inntekt for næringsdrivende",
                ),
        )

    val response =
        client.put("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(objectMapperCustomSerde.writeValueAsString(inntektRequest))
        }

    assertEquals(204, response.status.value, "Inntektsoppdatering for næringsdrivende skal returnere status 204")
}

internal suspend fun ApplicationTestBuilder.hentYrkesaktiviteter(
    periodeId: UUID,
    personId: String,
): List<YrkesaktivitetDTO> {
    val response =
        client
            .get("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
                bearerAuth(TestOppsett.userToken)
            }
    val json = response.body<String>()
    return objectMapperCustomSerde.readValue(json, objectMapperCustomSerde.typeFactory.constructCollectionType(List::class.java, YrkesaktivitetDTO::class.java))
}

internal suspend fun ApplicationTestBuilder.settDagoversikt(
    periodeId: UUID,
    yrkesaktivitetId: UUID,
    personId: String,
    dager: List<Dag>,
) {
    val dagoversikt = dager.serialisertTilString()
    val response =
        client.put("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId/dagoversikt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(dagoversikt)
        }
    assertEquals(204, response.status.value)
}

internal suspend fun ApplicationTestBuilder.hentUtbetalingsberegning(
    periodeId: UUID,
    personId: String,
): BeregningResponseUtDto? {
    val response =
        client.get("/v1/$personId/saksbehandlingsperioder/$periodeId/utbetalingsberegning") {
            bearerAuth(TestOppsett.userToken)
        }
    assertEquals(200, response.status.value)
    val responseText = response.bodyAsText()
    if (responseText == "null") return null

    // API-et sender BeregningResponseUtDto (med InntektDto som har alle felt)
    return objectMapper.readValue(responseText, BeregningResponseUtDto::class.java)
}
