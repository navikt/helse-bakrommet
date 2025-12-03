package no.nav.helse.bakrommet.api.dto.sykepengegrunnlag

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse
import java.time.LocalDate
import java.util.UUID

data class SykepengegrunnlagResponseDto(
    val sykepengegrunnlag: SykepengegrunnlagBaseDto?,
    val sammenlikningsgrunnlag: SammenlikningsgrunnlagDto?,
    val opprettetForBehandling: UUID,
) : ApiResponse

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SykepengegrunnlagDto::class, name = "SYKEPENGEGRUNNLAG"),
    JsonSubTypes.Type(value = FrihåndSykepengegrunnlagDto::class, name = "FRIHÅND_SYKEPENGEGRUNNLAG"),
)
sealed class SykepengegrunnlagBaseDto {
    abstract val grunnbeløp: Double // InntektbeløpDto.Årlig som Double
    abstract val sykepengegrunnlag: Double // InntektbeløpDto.Årlig som Double
    abstract val seksG: Double // InntektbeløpDto.Årlig som Double
    abstract val begrensetTil6G: Boolean
    abstract val grunnbeløpVirkningstidspunkt: LocalDate
    abstract val beregningsgrunnlag: Double // InntektbeløpDto.Årlig som Double
}

data class SykepengegrunnlagDto(
    override val grunnbeløp: Double,
    override val sykepengegrunnlag: Double,
    override val seksG: Double,
    override val begrensetTil6G: Boolean,
    override val grunnbeløpVirkningstidspunkt: LocalDate,
    override val beregningsgrunnlag: Double,
    val næringsdel: NæringsdelDto?,
    val kombinertBeregningskode: BeregningskoderKombinasjonerSykepengegrunnlagDto? = null,
) : SykepengegrunnlagBaseDto()

data class FrihåndSykepengegrunnlagDto(
    override val grunnbeløp: Double,
    override val sykepengegrunnlag: Double,
    override val seksG: Double,
    override val begrensetTil6G: Boolean,
    override val grunnbeløpVirkningstidspunkt: LocalDate,
    override val beregningsgrunnlag: Double,
    val begrunnelse: String,
    val beregningskoder: List<BeregningskoderSykepengegrunnlagDto>,
) : SykepengegrunnlagBaseDto()

data class NæringsdelDto(
    val pensjonsgivendeÅrsinntekt: Double, // InntektbeløpDto.Årlig som Double
    val pensjonsgivendeÅrsinntekt6GBegrenset: Double, // InntektbeløpDto.Årlig som Double
    val pensjonsgivendeÅrsinntektBegrensetTil6G: Boolean,
    val næringsdel: Double, // InntektbeløpDto.Årlig som Double
    val sumAvArbeidsinntekt: Double, // InntektbeløpDto.Årlig som Double
)

data class SammenlikningsgrunnlagDto(
    val totaltSammenlikningsgrunnlag: Double, // InntektbeløpDto.Årlig som Double
    val avvikProsent: Double,
    val avvikMotInntektsgrunnlag: Double, // InntektbeløpDto.Årlig som Double
    val basertPåDokumentId: UUID,
)
