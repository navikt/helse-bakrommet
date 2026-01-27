package no.nav.helse.bakrommet.db.repository

import no.nav.helse.bakrommet.assertInstantEquals
import no.nav.helse.bakrommet.domain.enBehandling
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.enNavIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderKombinasjonerSykepengegrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.sykepengegrunnlag.FrihåndSykepengegrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.sykepengegrunnlag.Næringsdel
import no.nav.helse.bakrommet.domain.sykepenger.sykepengegrunnlag.Sammenlikningsgrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Sykefraværstilfelle
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.SykefraværstilfelleId
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.test.*

class PgSykefraværstilfelleRepositoryTest : RepositoryTest() {
    private val repository = PgSykefraværstilfelleRepository(session)
    private val behandlingRepository = PgBehandlingRepository(session)

    @Test
    fun `lagre og hente sykefraværstilfelle med vanlig sykepengegrunnlag uten næringsdel`() {
        // given
        val naturligIdent = enNaturligIdent()
        val skjæringstidspunkt = LocalDate.of(2024, 1, 15)
        val behandling = enBehandling().ogLagre()
        val opprettetAvNavIdent = enNavIdent()
        val sykefraværstilfelle =
            Sykefraværstilfelle.nytt(
                naturligIdent = naturligIdent,
                skjæringstidspunkt = skjæringstidspunkt,
                sykepengegrunnlag =
                    Sykepengegrunnlag(
                        grunnbeløp = 118620.årlig,
                        sykepengegrunnlag = 500000.årlig,
                        seksG = 711720.årlig,
                        begrensetTil6G = false,
                        grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                        beregningsgrunnlag = 500000.årlig,
                        næringsdel = null,
                        kombinertBeregningskode = BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SELVSTENDIG_SYKEPENGEGRUNNLAG,
                    ),
                sammenlikningsgrunnlag =
                    Sammenlikningsgrunnlag(
                        totaltSammenlikningsgrunnlag = 480000.årlig,
                        avvikProsent = 4.17,
                        avvikMotInntektsgrunnlag = 20000.årlig,
                        basertPåDokumentId = UUID.randomUUID(),
                    ),
                opprettetAv = opprettetAvNavIdent,
                opprettetForBehandling = behandling.id,
            )

        // when
        repository.lagre(sykefraværstilfelle)

        // then
        val funnet = repository.finn(sykefraværstilfelle.id)
        assertNotNull(funnet)
        assertEquals(naturligIdent, funnet.id.naturligIdent)
        assertEquals(skjæringstidspunkt, funnet.id.skjæringstidspunkt)
        assertEquals(opprettetAvNavIdent, funnet.opprettetAv)
        assertEquals(behandling.id, funnet.opprettetForBehandling)
        assertFalse(funnet.låst)
        assertInstantEquals(sykefraværstilfelle.opprettet, funnet.opprettet)
        assertInstantEquals(sykefraværstilfelle.oppdatert, funnet.oppdatert)

        // Sjekk sykepengegrunnlag
        assertTrue(funnet.sykepengegrunnlag is Sykepengegrunnlag)
        val grunnlag = funnet.sykepengegrunnlag as Sykepengegrunnlag
        assertEquals(118620.årlig, grunnlag.grunnbeløp)
        assertEquals(500000.årlig, grunnlag.sykepengegrunnlag)
        assertEquals(711720.årlig, grunnlag.seksG)
        assertFalse(grunnlag.begrensetTil6G)
        assertEquals(LocalDate.of(2024, 5, 1), grunnlag.grunnbeløpVirkningstidspunkt)
        assertEquals(500000.årlig, grunnlag.beregningsgrunnlag)
        assertNull(grunnlag.næringsdel)
        assertEquals(BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SELVSTENDIG_SYKEPENGEGRUNNLAG, grunnlag.kombinertBeregningskode)

        // Sjekk sammenlikningsgrunnlag
        assertEquals(480000.årlig, funnet.sammenlikningsgrunnlag.totaltSammenlikningsgrunnlag)
        assertEquals(4.17, funnet.sammenlikningsgrunnlag.avvikProsent)
        assertEquals(20000.årlig, funnet.sammenlikningsgrunnlag.avvikMotInntektsgrunnlag)
        assertEquals(sykefraværstilfelle.sammenlikningsgrunnlag.basertPåDokumentId, funnet.sammenlikningsgrunnlag.basertPåDokumentId)
    }

    @Test
    fun `lagre og hente sykefraværstilfelle med næringsdel`() {
        // given
        val naturligIdent = enNaturligIdent()
        val behandling = enBehandling().ogLagre()
        val skjæringstidspunkt = LocalDate.of(2024, 2, 1)
        val sykefraværstilfelle =
            Sykefraværstilfelle.nytt(
                naturligIdent = naturligIdent,
                skjæringstidspunkt = skjæringstidspunkt,
                sykepengegrunnlag =
                    Sykepengegrunnlag(
                        grunnbeløp = 118620.årlig,
                        sykepengegrunnlag = 711720.årlig,
                        seksG = 711720.årlig,
                        begrensetTil6G = true,
                        grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                        beregningsgrunnlag = 800000.årlig,
                        næringsdel =
                            Næringsdel(
                                pensjonsgivendeÅrsinntekt = 300000.årlig,
                                pensjonsgivendeÅrsinntekt6GBegrenset = 300000.årlig,
                                pensjonsgivendeÅrsinntektBegrensetTil6G = false,
                                næringsdel = 300000.årlig,
                                sumAvArbeidsinntekt = 500000.årlig,
                            ),
                        kombinertBeregningskode = null,
                    ),
                sammenlikningsgrunnlag =
                    Sammenlikningsgrunnlag(
                        totaltSammenlikningsgrunnlag = 750000.årlig,
                        avvikProsent = 6.67,
                        avvikMotInntektsgrunnlag = 50000.årlig,
                        basertPåDokumentId = UUID.randomUUID(),
                    ),
                opprettetAv = enNavIdent(),
                opprettetForBehandling = behandling.id,
            )

        // when
        repository.lagre(sykefraværstilfelle)

        // then
        val funnet = repository.finn(sykefraværstilfelle.id)
        assertNotNull(funnet)

        assertTrue(funnet.sykepengegrunnlag is Sykepengegrunnlag)
        val grunnlag = funnet.sykepengegrunnlag as Sykepengegrunnlag
        assertTrue(grunnlag.begrensetTil6G)
        assertEquals(800000.årlig, grunnlag.beregningsgrunnlag)
        assertEquals(null, grunnlag.kombinertBeregningskode)

        // Sjekk næringsdel
        assertNotNull(grunnlag.næringsdel)
        assertEquals(300000.årlig, grunnlag.næringsdel!!.pensjonsgivendeÅrsinntekt)
        assertEquals(300000.årlig, grunnlag.næringsdel!!.pensjonsgivendeÅrsinntekt6GBegrenset)
        assertFalse(grunnlag.næringsdel!!.pensjonsgivendeÅrsinntektBegrensetTil6G)
        assertEquals(300000.årlig, grunnlag.næringsdel!!.næringsdel)
        assertEquals(500000.årlig, grunnlag.næringsdel!!.sumAvArbeidsinntekt)
    }

    @Test
    fun `lagre og hente frihånd sykepengegrunnlag med beregningskoder`() {
        // given
        val naturligIdent = enNaturligIdent()
        val behandling = enBehandling().ogLagre()
        val skjæringstidspunkt = LocalDate.of(2024, 3, 1)
        val sykefraværstilfelle =
            Sykefraværstilfelle.nytt(
                naturligIdent = naturligIdent,
                skjæringstidspunkt = skjæringstidspunkt,
                sykepengegrunnlag =
                    FrihåndSykepengegrunnlag(
                        grunnbeløp = 118620.årlig,
                        sykepengegrunnlag = 450000.årlig,
                        seksG = 711720.årlig,
                        begrensetTil6G = false,
                        grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                        beregningsgrunnlag = 450000.årlig,
                        begrunnelse = "Skjønnsmessig fastsatt på grunn av uriktig inntektsmelding",
                        beregningskoder =
                            listOf(
                                BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG,
                            ),
                    ),
                sammenlikningsgrunnlag =
                    Sammenlikningsgrunnlag(
                        totaltSammenlikningsgrunnlag = 420000.årlig,
                        avvikProsent = 7.14,
                        avvikMotInntektsgrunnlag = 30000.årlig,
                        basertPåDokumentId = UUID.randomUUID(),
                    ),
                opprettetAv = enNavIdent(),
                opprettetForBehandling = behandling.id,
            )

        // when
        repository.lagre(sykefraværstilfelle)

        // then
        val funnet = repository.finn(sykefraværstilfelle.id)
        assertNotNull(funnet)

        // Sjekk at det er frihånd sykepengegrunnlag
        assertTrue(funnet.sykepengegrunnlag is FrihåndSykepengegrunnlag)
        val grunnlag = funnet.sykepengegrunnlag as FrihåndSykepengegrunnlag
        assertEquals(450000.årlig, grunnlag.sykepengegrunnlag)
        assertEquals("Skjønnsmessig fastsatt på grunn av uriktig inntektsmelding", grunnlag.begrunnelse)
        assertEquals(1, grunnlag.beregningskoder.size)
        assertEquals(BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG, grunnlag.beregningskoder.first())
    }

    @Test
    fun `lagre og hente frihånd sykepengegrunnlag med flere beregningskoder`() {
        // given
        val naturligIdent = enNaturligIdent()
        val behandling = enBehandling().ogLagre()
        val skjæringstidspunkt = LocalDate.of(2024, 4, 1)
        val sykefraværstilfelle =
            Sykefraværstilfelle.nytt(
                naturligIdent = naturligIdent,
                skjæringstidspunkt = skjæringstidspunkt,
                sykepengegrunnlag =
                    FrihåndSykepengegrunnlag(
                        grunnbeløp = 118620.årlig,
                        sykepengegrunnlag = 600000.årlig,
                        seksG = 711720.årlig,
                        begrensetTil6G = false,
                        grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                        beregningsgrunnlag = 600000.årlig,
                        begrunnelse = "Kombinert arbeidstaker og selvstendig",
                        beregningskoder =
                            listOf(
                                BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL,
                                BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL,
                            ),
                    ),
                sammenlikningsgrunnlag =
                    Sammenlikningsgrunnlag(
                        totaltSammenlikningsgrunnlag = 580000.årlig,
                        avvikProsent = 3.45,
                        avvikMotInntektsgrunnlag = 20000.årlig,
                        basertPåDokumentId = UUID.randomUUID(),
                    ),
                opprettetAv = enNavIdent(),
                opprettetForBehandling = behandling.id,
            )

        // when
        repository.lagre(sykefraværstilfelle)

        // then
        val funnet = repository.finn(sykefraværstilfelle.id)
        assertNotNull(funnet)

        assertTrue(funnet.sykepengegrunnlag is FrihåndSykepengegrunnlag)
        val grunnlag = funnet.sykepengegrunnlag as FrihåndSykepengegrunnlag
        assertEquals(2, grunnlag.beregningskoder.size)
        assertTrue(grunnlag.beregningskoder.contains(BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL))
        assertTrue(grunnlag.beregningskoder.contains(BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL))
    }

    @Test
    fun `oppdatere eksisterende sykefraværstilfelle`() {
        // given
        val naturligIdent = enNaturligIdent()
        val skjæringstidspunkt = LocalDate.of(2024, 5, 1)
        val behandling = enBehandling().ogLagre()
        val opprettetTidspunkt = Instant.now().minusSeconds(3600)
        val oppdatertTidspunkt = Instant.now()

        val opprinnelig =
            Sykefraværstilfelle.fraLagring(
                id = SykefraværstilfelleId(naturligIdent, skjæringstidspunkt),
                sykepengegrunnlag =
                    Sykepengegrunnlag(
                        grunnbeløp = 118620.årlig,
                        sykepengegrunnlag = 400000.årlig,
                        seksG = 711720.årlig,
                        begrensetTil6G = false,
                        grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                        beregningsgrunnlag = 400000.årlig,
                        næringsdel = null,
                        kombinertBeregningskode = null,
                    ),
                sammenlikningsgrunnlag =
                    Sammenlikningsgrunnlag(
                        totaltSammenlikningsgrunnlag = 380000.årlig,
                        avvikProsent = 5.26,
                        avvikMotInntektsgrunnlag = 20000.årlig,
                        basertPåDokumentId = UUID.randomUUID(),
                    ),
                opprettetAv = enNavIdent(),
                opprettet = opprettetTidspunkt,
                oppdatert = opprettetTidspunkt,
                opprettetForBehandling = behandling.id,
                låst = false,
            )
        repository.lagre(opprinnelig)

        val oppdatertGrunnlag =
            Sykefraværstilfelle.fraLagring(
                id = SykefraværstilfelleId(naturligIdent, skjæringstidspunkt),
                sykepengegrunnlag =
                    Sykepengegrunnlag(
                        grunnbeløp = 118620.årlig,
                        sykepengegrunnlag = 450000.årlig,
                        seksG = 711720.årlig,
                        begrensetTil6G = false,
                        grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                        beregningsgrunnlag = 450000.årlig,
                        næringsdel = null,
                        kombinertBeregningskode = null,
                    ),
                sammenlikningsgrunnlag =
                    Sammenlikningsgrunnlag(
                        totaltSammenlikningsgrunnlag = 430000.årlig,
                        avvikProsent = 4.65,
                        avvikMotInntektsgrunnlag = 20000.årlig,
                        basertPåDokumentId = UUID.randomUUID(),
                    ),
                opprettetAv = enNavIdent(),
                opprettet = opprettetTidspunkt,
                oppdatert = oppdatertTidspunkt,
                opprettetForBehandling = behandling.id,
                låst = false,
            )

        // when
        repository.lagre(oppdatertGrunnlag)

        // then
        val funnet = repository.finn(oppdatertGrunnlag.id)
        assertNotNull(funnet)
        assertEquals(450000.årlig, funnet.sykepengegrunnlag.sykepengegrunnlag)
        assertEquals(430000.årlig, funnet.sammenlikningsgrunnlag.totaltSammenlikningsgrunnlag)
        assertInstantEquals(opprettetTidspunkt, funnet.opprettet)
        assertInstantEquals(oppdatertTidspunkt, funnet.oppdatert)
    }

    @Test
    fun `finn returnerer null når sykefraværstilfelle ikke finnes`() {
        // given
        val ikkeFinnbarId = SykefraværstilfelleId(enNaturligIdent(), LocalDate.of(2024, 7, 1))

        // when
        val funnet = repository.finn(ikkeFinnbarId)

        // then
        assertNull(funnet)
    }

    @Test
    fun `lagre sykefraværstilfelle med månedlig inntekt konverteres riktig`() {
        // given
        val naturligIdent = enNaturligIdent()
        val skjæringstidspunkt = LocalDate.of(2024, 8, 1)
        val behandling = enBehandling().ogLagre()
        val sykefraværstilfelle =
            Sykefraværstilfelle.nytt(
                naturligIdent = naturligIdent,
                skjæringstidspunkt = skjæringstidspunkt,
                sykepengegrunnlag =
                    Sykepengegrunnlag(
                        grunnbeløp = 9885.månedlig,
                        sykepengegrunnlag = 41667.månedlig,
                        seksG = 59310.månedlig,
                        begrensetTil6G = false,
                        grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                        beregningsgrunnlag = 41667.månedlig,
                        næringsdel = null,
                        kombinertBeregningskode = null,
                    ),
                sammenlikningsgrunnlag =
                    Sammenlikningsgrunnlag(
                        totaltSammenlikningsgrunnlag = 40000.månedlig,
                        avvikProsent = 4.17,
                        avvikMotInntektsgrunnlag = 1667.månedlig,
                        basertPåDokumentId = UUID.randomUUID(),
                    ),
                opprettetAv = enNavIdent(),
                opprettetForBehandling = behandling.id,
            )

        // when
        repository.lagre(sykefraværstilfelle)

        // then
        val funnet = repository.finn(sykefraværstilfelle.id)
        assertNotNull(funnet)
        // Sjekk at inntektene ble konvertert korrekt (månedlig * 12 = årlig)
        assertEquals(500004.årlig, funnet.sykepengegrunnlag.sykepengegrunnlag)
        assertEquals(480000.årlig, funnet.sammenlikningsgrunnlag.totaltSammenlikningsgrunnlag)
    }

    @Test
    fun `lagre sykefraværstilfelle med alle beregningskoder for frilanser`() {
        // given
        val naturligIdent = enNaturligIdent()
        val skjæringstidspunkt = LocalDate.of(2024, 9, 1)
        val behandling = enBehandling().ogLagre()
        val sykefraværstilfelle =
            Sykefraværstilfelle.nytt(
                naturligIdent = naturligIdent,
                skjæringstidspunkt = skjæringstidspunkt,
                sykepengegrunnlag =
                    FrihåndSykepengegrunnlag(
                        grunnbeløp = 118620.årlig,
                        sykepengegrunnlag = 400000.årlig,
                        seksG = 711720.årlig,
                        begrensetTil6G = false,
                        grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
                        beregningsgrunnlag = 400000.årlig,
                        begrunnelse = "Frilanser med avvik",
                        beregningskoder =
                            listOf(
                                BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL,
                                BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK,
                            ),
                    ),
                sammenlikningsgrunnlag =
                    Sammenlikningsgrunnlag(
                        totaltSammenlikningsgrunnlag = 360000.årlig,
                        avvikProsent = 11.11,
                        avvikMotInntektsgrunnlag = 40000.årlig,
                        basertPåDokumentId = UUID.randomUUID(),
                    ),
                opprettetAv = enNavIdent(),
                opprettetForBehandling = behandling.id,
            )

        // when
        repository.lagre(sykefraværstilfelle)

        // then
        val funnet = repository.finn(sykefraværstilfelle.id)
        assertNotNull(funnet)
        assertTrue(funnet.sykepengegrunnlag is FrihåndSykepengegrunnlag)
        val grunnlag = funnet.sykepengegrunnlag as FrihåndSykepengegrunnlag
        assertEquals(2, grunnlag.beregningskoder.size)
        assertTrue(grunnlag.beregningskoder.contains(BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL))
        assertTrue(grunnlag.beregningskoder.contains(BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK))
    }

    private fun Behandling.ogLagre(): Behandling {
        behandlingRepository.lagre(this)
        return this
    }
}
