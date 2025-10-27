package no.nav.helse.bakrommet.saksbehandlingsperiode

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ainntekt.etInntektSvar
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock.enInntektsmelding
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock.inntektsmeldingMockHttpClient
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.*
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDTO
import no.nav.helse.bakrommet.serde.objectMapperCustomSerde
import no.nav.helse.bakrommet.sigrun.SigrunClientTest
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettSaksbehandlingsperiode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class YrkesaktivitetInntektOppdateringTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
    }

    @Test
    fun `oppretter saksbehandlingsperiode med tre yrkesaktiviteter og oppdaterer inntekt for hver`() {
        val im1Id = UUID.randomUUID().toString()
        val im1 = enInntektsmelding(arbeidstakerFnr = FNR, inntektsmeldingId = im1Id)
        val antallKallTilInntektsmeldingAPI = AtomicInteger(0)

        runApplicationTest(
            inntektsmeldingClient =
                InntektsmeldingApiMock.inntektsmeldingClientMock(
                    mockClient =
                        inntektsmeldingMockHttpClient(
                            fnrTilSvar = mapOf(FNR to "[$im1]"),
                            inntektsmeldingIdTilSvar = mapOf(im1Id to im1),
                            callCounter = antallKallTilInntektsmeldingAPI,
                        ),
                ),
            aInntektClient =
                AInntektMock.aInntektClientMock(
                    fnrTilSvar = mapOf(FNR to etInntektSvar(fnr = FNR)),
                ),
            sigrunClient = SigrunClientTest.client2010to2050(FNR),
        ) { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            // Opprett saksbehandlingsperiode
            val periode = opprettSaksbehandlingsperiode()

            // Sett skjæringstidspunkt for perioden
            val skjaeringstidspunktResponse =
                client.put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/skjaeringstidspunkt") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody("""{ "skjaeringstidspunkt": "2023-01-15" }""")
                }
            assertEquals(200, skjaeringstidspunktResponse.status.value, "Skjæringstidspunkt skal settes med status 200")

            // Opprett tre yrkesaktiviteter
            val arbeidstakerId = opprettArbeidstakerYrkesaktivitet(periode.id)
            val naeringsdrivendeId = opprettNaeringsdrivendeYrkesaktivitet(periode.id)
            val frilanserId = opprettFrilanserYrkesaktivitet(periode.id)

            // Verifiser at alle yrkesaktiviteter ble opprettet
            val yrkesaktiviteter = hentYrkesaktiviteter(periode.id)
            assertEquals(3, yrkesaktiviteter.size)

            // Oppdater inntekt for arbeidstaker med inntektsmelding
            oppdaterArbeidstakerInntektMedInntektsmelding(periode.id, arbeidstakerId, im1Id)

            // Oppdater inntekt for frilanser med a-inntekt
            oppdaterFrilanserInntektMedAInntekt(periode.id, frilanserId)

            // Oppdater inntekt for næringsdrivende med pensjonsgivende inntekt
            oppdaterNaeringsdrivendeInntektMedPensjonsgivendeInntekt(periode.id, naeringsdrivendeId)

            // Verifiser at alle inntekter ble oppdatert korrekt
            verifiserInntektOppdateringer(periode.id, yrkesaktiviteter)

            // Verifiser at inntektsmelding API ble kalt
            assertTrue(antallKallTilInntektsmeldingAPI.get() > 0, "Inntektsmelding API skal være kalt minst én gang")
        }
    }

    private suspend fun ApplicationTestBuilder.opprettSaksbehandlingsperiode(): Saksbehandlingsperiode =
        opprettSaksbehandlingsperiode(
            PERSON_ID,
            LocalDate.parse("2023-01-01"),
            LocalDate.parse("2023-01-31"),
        )

    private suspend fun ApplicationTestBuilder.opprettArbeidstakerYrkesaktivitet(periodeId: UUID): UUID {
        val kategorisering =
            """
            {
                "kategorisering": {
                    "INNTEKTSKATEGORI": "ARBEIDSTAKER",
                    "ORGNUMMER": "123456789",
                    "ER_SYKMELDT": "ER_SYKMELDT_JA",
                    "TYPE_ARBEIDSTAKER": "ORDINÆRT_ARBEIDSFORHOLD"
                }
            }
            """.trimIndent()

        val response =
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
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
        assertEquals("123456789", body["kategorisering"]["ORGNUMMER"].asText(), "Organisasjonsnummer skal være satt")
        assertEquals("ER_SYKMELDT_JA", body["kategorisering"]["ER_SYKMELDT"].asText(), "Er sykmeldt skal være JA")
        assertEquals("ORDINÆRT_ARBEIDSFORHOLD", body["kategorisering"]["TYPE_ARBEIDSTAKER"].asText(), "Type arbeidstaker skal være ORDINÆRT")

        return yrkesaktivitetId
    }

    private suspend fun ApplicationTestBuilder.opprettNaeringsdrivendeYrkesaktivitet(periodeId: UUID): UUID {
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
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
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

    private suspend fun ApplicationTestBuilder.opprettFrilanserYrkesaktivitet(periodeId: UUID): UUID {
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
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
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

    private suspend fun ApplicationTestBuilder.oppdaterArbeidstakerInntektMedInntektsmelding(
        periodeId: UUID,
        yrkesaktivitetId: UUID,
        inntektsmeldingId: String,
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
            client.put("/v1/$PERSON_ID/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(objectMapperCustomSerde.writeValueAsString(inntektRequest))
            }

        assertEquals(204, response.status.value, "Inntektsoppdatering for arbeidstaker skal returnere status 204")
    }

    private suspend fun ApplicationTestBuilder.oppdaterFrilanserInntektMedAInntekt(
        periodeId: UUID,
        yrkesaktivitetId: UUID,
    ) {
        val inntektRequest =
            InntektRequest.Frilanser(
                data =
                    FrilanserInntektRequest.Ainntekt(
                        begrunnelse = "Bruker a-inntekt for frilanser",
                    ),
            )

        val response =
            client.put("/v1/$PERSON_ID/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(objectMapperCustomSerde.writeValueAsString(inntektRequest))
            }

        assertEquals(204, response.status.value, "Inntektsoppdatering for frilanser skal returnere status 204")
    }

    private suspend fun ApplicationTestBuilder.oppdaterNaeringsdrivendeInntektMedPensjonsgivendeInntekt(
        periodeId: UUID,
        yrkesaktivitetId: UUID,
    ) {
        val inntektRequest =
            InntektRequest.SelvstendigNæringsdrivende(
                data =
                    PensjonsgivendeInntektRequest.PensjonsgivendeInntekt(
                        begrunnelse = "Bruker pensjonsgivende inntekt for næringsdrivende",
                    ),
            )

        val response =
            client.put("/v1/$PERSON_ID/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(objectMapperCustomSerde.writeValueAsString(inntektRequest))
            }

        assertEquals(204, response.status.value, "Inntektsoppdatering for næringsdrivende skal returnere status 204")
    }

    private suspend fun ApplicationTestBuilder.hentYrkesaktiviteter(periodeId: UUID): List<YrkesaktivitetDTO> {
        val response =
            client
                .get("/v1/$PERSON_ID/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
                    bearerAuth(TestOppsett.userToken)
                }
        val json = response.body<String>()
        return objectMapperCustomSerde.readValue(json, objectMapperCustomSerde.typeFactory.constructCollectionType(List::class.java, YrkesaktivitetDTO::class.java))
    }

    private suspend fun ApplicationTestBuilder.verifiserInntektOppdateringer(
        periodeId: UUID,
        yrkesaktiviteter: List<YrkesaktivitetDTO>,
    ) {
        val oppdaterteYrkesaktiviteter = hentYrkesaktiviteter(periodeId)

        // Verifiser at antall yrkesaktiviteter er uendret
        assertEquals(yrkesaktiviteter.size, oppdaterteYrkesaktiviteter.size, "Antall yrkesaktiviteter skal være uendret etter inntektsoppdatering")

        // Verifiser at alle yrkesaktiviteter har fått inntektRequest satt
        oppdaterteYrkesaktiviteter.forEach { yrkesaktivitet ->
            assertTrue(yrkesaktivitet.inntektRequest != null, "Yrkesaktivitet ${yrkesaktivitet.id} skal ha inntektRequest")
            assertTrue(yrkesaktivitet.kategorisering.isNotEmpty(), "Yrkesaktivitet skal ha kategorisering")
        }

        // Verifiser at alle yrkesaktiviteter har unike IDer
        val unikeId = oppdaterteYrkesaktiviteter.map { it.id }.toSet()
        assertEquals(oppdaterteYrkesaktiviteter.size, unikeId.size, "Alle yrkesaktiviteter skal ha unike IDer")

        // Verifiser spesifikke inntektstyper
        val arbeidstaker =
            oppdaterteYrkesaktiviteter.find {
                it.kategorisering["INNTEKTSKATEGORI"] == "ARBEIDSTAKER"
            }!!
        val naeringsdrivende =
            oppdaterteYrkesaktiviteter.find {
                it.kategorisering["INNTEKTSKATEGORI"] == "SELVSTENDIG_NÆRINGSDRIVENDE"
            }!!
        val frilanser =
            oppdaterteYrkesaktiviteter.find {
                it.kategorisering["INNTEKTSKATEGORI"] == "FRILANSER"
            }!!

        // Verifiser at alle tre yrkesaktiviteter ble funnet (!! operator sikrer at de ikke er null)

        // Verifiser arbeidstaker inntektsmelding
        val arbeidstakerInntekt = arbeidstaker.inntektRequest as InntektRequest.Arbeidstaker
        assertTrue(arbeidstakerInntekt.data is ArbeidstakerInntektRequest.Inntektsmelding, "Arbeidstaker inntekt skal være Inntektsmelding type")
        val inntektsmelding = arbeidstakerInntekt.data as ArbeidstakerInntektRequest.Inntektsmelding
        assertEquals("INNTEKTSMELDING", inntektsmelding::class.simpleName?.uppercase(), "Inntektsmelding class navn skal være korrekt")
        assertEquals("Velger inntektsmelding for arbeidstaker", inntektsmelding.begrunnelse, "Inntektsmelding begrunnelse skal være korrekt")
        assertTrue(inntektsmelding.inntektsmeldingId.isNotEmpty(), "Inntektsmelding ID skal være satt")
        assertTrue(inntektsmelding.refusjon != null, "Refusjon skal være satt (kan være tom liste)")

        // Verifiser frilanser a-inntekt
        val frilanserInntekt = frilanser.inntektRequest as InntektRequest.Frilanser
        assertTrue(frilanserInntekt.data is FrilanserInntektRequest.Ainntekt, "Frilanser inntekt skal være Ainntekt type")
        val ainntekt = frilanserInntekt.data as FrilanserInntektRequest.Ainntekt
        assertEquals("AINNTEKT", ainntekt::class.simpleName?.uppercase(), "Ainntekt class navn skal være korrekt")
        assertEquals("Bruker a-inntekt for frilanser", ainntekt.begrunnelse, "Ainntekt begrunnelse skal være korrekt")

        // Verifiser næringsdrivende pensjonsgivende inntekt
        val naeringsdrivendeInntekt = naeringsdrivende.inntektRequest as InntektRequest.SelvstendigNæringsdrivende
        assertTrue(naeringsdrivendeInntekt.data is PensjonsgivendeInntektRequest.PensjonsgivendeInntekt, "Næringsdrivende inntekt skal være PensjonsgivendeInntekt type")
        val pensjonsgivendeInntekt = naeringsdrivendeInntekt.data as PensjonsgivendeInntektRequest.PensjonsgivendeInntekt
        assertEquals("PENSJONSGIVENDEINNTEKT", pensjonsgivendeInntekt::class.simpleName?.uppercase(), "PensjonsgivendeInntekt class navn skal være korrekt")
        assertEquals("Bruker pensjonsgivende inntekt for næringsdrivende", pensjonsgivendeInntekt.begrunnelse, "PensjonsgivendeInntekt begrunnelse skal være korrekt")

        // Verifiser at alle inntektRequest objekter har korrekt type
        oppdaterteYrkesaktiviteter.forEach { yrkesaktivitet ->
            val inntektRequest = yrkesaktivitet.inntektRequest!!
            val inntektskategori = yrkesaktivitet.kategorisering["INNTEKTSKATEGORI"]!!

            when (inntektskategori) {
                "ARBEIDSTAKER" -> assertTrue(inntektRequest is InntektRequest.Arbeidstaker, "Arbeidstaker skal ha Arbeidstaker inntektRequest type")
                "SELVSTENDIG_NÆRINGSDRIVENDE" -> assertTrue(inntektRequest is InntektRequest.SelvstendigNæringsdrivende, "Næringsdrivende skal ha SelvstendigNæringsdrivende inntektRequest type")
                "FRILANSER" -> assertTrue(inntektRequest is InntektRequest.Frilanser, "Frilanser skal ha Frilanser inntektRequest type")
                else -> throw AssertionError("Ukjent inntektskategori: $inntektskategori")
            }
        }
    }
}
