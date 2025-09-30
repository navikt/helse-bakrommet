package no.nav.helse.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime

data class UtbetalingVurderingDto(
    val godkjent: Boolean,
    val ident: String,
    val epost: String,
    val tidspunkt: LocalDateTime,
    val automatiskBehandling: Boolean,
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UtbetalingtypeDto.UTBETALING::class, name = "UTBETALING"),
    JsonSubTypes.Type(value = UtbetalingtypeDto.ETTERUTBETALING::class, name = "ETTERUTBETALING"),
    JsonSubTypes.Type(value = UtbetalingtypeDto.ANNULLERING::class, name = "ANNULLERING"),
    JsonSubTypes.Type(value = UtbetalingtypeDto.REVURDERING::class, name = "REVURDERING"),
)
sealed class UtbetalingtypeDto {
    data object UTBETALING : UtbetalingtypeDto()

    data object ETTERUTBETALING : UtbetalingtypeDto()

    data object ANNULLERING : UtbetalingtypeDto()

    data object REVURDERING : UtbetalingtypeDto()
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UtbetalingTilstandDto.NY::class, name = "NY"),
    JsonSubTypes.Type(value = UtbetalingTilstandDto.IKKE_UTBETALT::class, name = "IKKE_UTBETALT"),
    JsonSubTypes.Type(value = UtbetalingTilstandDto.IKKE_GODKJENT::class, name = "IKKE_GODKJENT"),
    JsonSubTypes.Type(value = UtbetalingTilstandDto.OVERFØRT::class, name = "OVERFØRT"),
    JsonSubTypes.Type(value = UtbetalingTilstandDto.UTBETALT::class, name = "UTBETALT"),
    JsonSubTypes.Type(value = UtbetalingTilstandDto.GODKJENT::class, name = "GODKJENT"),
    JsonSubTypes.Type(value = UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING::class, name = "GODKJENT_UTEN_UTBETALING"),
    JsonSubTypes.Type(value = UtbetalingTilstandDto.ANNULLERT::class, name = "ANNULLERT"),
    JsonSubTypes.Type(value = UtbetalingTilstandDto.FORKASTET::class, name = "FORKASTET"),
)
sealed class UtbetalingTilstandDto {
    data object NY : UtbetalingTilstandDto()

    data object IKKE_UTBETALT : UtbetalingTilstandDto()

    data object IKKE_GODKJENT : UtbetalingTilstandDto()

    data object OVERFØRT : UtbetalingTilstandDto()

    data object UTBETALT : UtbetalingTilstandDto()

    data object GODKJENT : UtbetalingTilstandDto()

    data object GODKJENT_UTEN_UTBETALING : UtbetalingTilstandDto()

    data object ANNULLERT : UtbetalingTilstandDto()

    data object FORKASTET : UtbetalingTilstandDto()
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = BegrunnelseDto.SykepengedagerOppbrukt::class, name = "SykepengedagerOppbrukt"),
    JsonSubTypes.Type(value = BegrunnelseDto.SykepengedagerOppbruktOver67::class, name = "SykepengedagerOppbruktOver67"),
    JsonSubTypes.Type(value = BegrunnelseDto.MinimumInntekt::class, name = "MinimumInntekt"),
    JsonSubTypes.Type(value = BegrunnelseDto.MinimumInntektOver67::class, name = "MinimumInntektOver67"),
    JsonSubTypes.Type(value = BegrunnelseDto.EgenmeldingUtenforArbeidsgiverperiode::class, name = "EgenmeldingUtenforArbeidsgiverperiode"),
    JsonSubTypes.Type(value = BegrunnelseDto.AndreYtelserForeldrepenger::class, name = "AndreYtelserForeldrepenger"),
    JsonSubTypes.Type(value = BegrunnelseDto.AndreYtelserAap::class, name = "AndreYtelserAap"),
    JsonSubTypes.Type(value = BegrunnelseDto.AndreYtelserOmsorgspenger::class, name = "AndreYtelserOmsorgspenger"),
    JsonSubTypes.Type(value = BegrunnelseDto.AndreYtelserPleiepenger::class, name = "AndreYtelserPleiepenger"),
    JsonSubTypes.Type(value = BegrunnelseDto.AndreYtelserSvangerskapspenger::class, name = "AndreYtelserSvangerskapspenger"),
    JsonSubTypes.Type(value = BegrunnelseDto.AndreYtelserOpplaringspenger::class, name = "AndreYtelserOpplaringspenger"),
    JsonSubTypes.Type(value = BegrunnelseDto.AndreYtelserDagpenger::class, name = "AndreYtelserDagpenger"),
    JsonSubTypes.Type(value = BegrunnelseDto.MinimumSykdomsgrad::class, name = "MinimumSykdomsgrad"),
    JsonSubTypes.Type(value = BegrunnelseDto.EtterDødsdato::class, name = "EtterDødsdato"),
    JsonSubTypes.Type(value = BegrunnelseDto.Over70::class, name = "Over70"),
    JsonSubTypes.Type(value = BegrunnelseDto.ManglerOpptjening::class, name = "ManglerOpptjening"),
    JsonSubTypes.Type(value = BegrunnelseDto.ManglerMedlemskap::class, name = "ManglerMedlemskap"),
    JsonSubTypes.Type(value = BegrunnelseDto.NyVilkårsprøvingNødvendig::class, name = "NyVilkårsprøvingNødvendig"),
)
sealed class BegrunnelseDto {
    data object SykepengedagerOppbrukt : BegrunnelseDto()

    data object SykepengedagerOppbruktOver67 : BegrunnelseDto()

    data object MinimumInntekt : BegrunnelseDto()

    data object MinimumInntektOver67 : BegrunnelseDto()

    data object EgenmeldingUtenforArbeidsgiverperiode : BegrunnelseDto()

    data object AndreYtelserForeldrepenger : BegrunnelseDto()

    data object AndreYtelserAap : BegrunnelseDto()

    data object AndreYtelserOmsorgspenger : BegrunnelseDto()

    data object AndreYtelserPleiepenger : BegrunnelseDto()

    data object AndreYtelserSvangerskapspenger : BegrunnelseDto()

    data object AndreYtelserOpplaringspenger : BegrunnelseDto()

    data object AndreYtelserDagpenger : BegrunnelseDto()

    data object MinimumSykdomsgrad : BegrunnelseDto()

    data object EtterDødsdato : BegrunnelseDto()

    data object Over70 : BegrunnelseDto()

    data object ManglerOpptjening : BegrunnelseDto()

    data object ManglerMedlemskap : BegrunnelseDto()

    data object NyVilkårsprøvingNødvendig : BegrunnelseDto()
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = FagområdeDto.SPREF::class, name = "SPREF"),
    JsonSubTypes.Type(value = FagområdeDto.SP::class, name = "SP"),
)
sealed class FagområdeDto {
    data object SPREF : FagområdeDto()

    data object SP : FagområdeDto()
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = EndringskodeDto.NY::class, name = "NY"),
    JsonSubTypes.Type(value = EndringskodeDto.UEND::class, name = "UEND"),
    JsonSubTypes.Type(value = EndringskodeDto.ENDR::class, name = "ENDR"),
)
sealed class EndringskodeDto {
    data object NY : EndringskodeDto()

    data object UEND : EndringskodeDto()

    data object ENDR : EndringskodeDto()
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = OppdragstatusDto.OVERFØRT::class, name = "OVERFØRT"),
    JsonSubTypes.Type(value = OppdragstatusDto.AKSEPTERT::class, name = "AKSEPTERT"),
    JsonSubTypes.Type(value = OppdragstatusDto.AKSEPTERT_MED_FEIL::class, name = "AKSEPTERT_MED_FEIL"),
    JsonSubTypes.Type(value = OppdragstatusDto.AVVIST::class, name = "AVVIST"),
    JsonSubTypes.Type(value = OppdragstatusDto.FEIL::class, name = "FEIL"),
)
sealed class OppdragstatusDto {
    data object OVERFØRT : OppdragstatusDto()

    data object AKSEPTERT : OppdragstatusDto()

    data object AKSEPTERT_MED_FEIL : OppdragstatusDto()

    data object AVVIST : OppdragstatusDto()

    data object FEIL : OppdragstatusDto()
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = KlassekodeDto.RefusjonIkkeOpplysningspliktig::class, name = "RefusjonIkkeOpplysningspliktig"),
    JsonSubTypes.Type(value = KlassekodeDto.SykepengerArbeidstakerOrdinær::class, name = "SykepengerArbeidstakerOrdinær"),
    JsonSubTypes.Type(value = KlassekodeDto.SelvstendigNæringsdrivendeOppgavepliktig::class, name = "SelvstendigNæringsdrivendeOppgavepliktig"),
    JsonSubTypes.Type(value = KlassekodeDto.SelvstendigNæringsdrivendeFisker::class, name = "SelvstendigNæringsdrivendeFisker"),
    JsonSubTypes.Type(value = KlassekodeDto.SelvstendigNæringsdrivendeJordbrukOgSkogbruk::class, name = "SelvstendigNæringsdrivendeJordbrukOgSkogbruk"),
    JsonSubTypes.Type(value = KlassekodeDto.SelvstendigNæringsdrivendeBarnepasserOppgavepliktig::class, name = "SelvstendigNæringsdrivendeBarnepasserOppgavepliktig"),
)
sealed class KlassekodeDto(val verdi: String) {
    data object RefusjonIkkeOpplysningspliktig : KlassekodeDto("SPREFAG-IOP")

    data object SykepengerArbeidstakerOrdinær : KlassekodeDto("SPATORD")

    data object SelvstendigNæringsdrivendeOppgavepliktig : KlassekodeDto("SPSND-OP")

    data object SelvstendigNæringsdrivendeFisker : KlassekodeDto("SPSNDFISK")

    data object SelvstendigNæringsdrivendeJordbrukOgSkogbruk : KlassekodeDto("SPSNDJORD")

    data object SelvstendigNæringsdrivendeBarnepasserOppgavepliktig : KlassekodeDto("SPSNDDM-OP")
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = FeriepengerfagområdeDto.SPREF::class, name = "SPREF"),
    JsonSubTypes.Type(value = FeriepengerfagområdeDto.SP::class, name = "SP"),
)
sealed class FeriepengerfagområdeDto {
    data object SPREF : FeriepengerfagområdeDto()

    data object SP : FeriepengerfagområdeDto()
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = FeriepengerendringskodeDto.NY::class, name = "NY"),
    JsonSubTypes.Type(value = FeriepengerendringskodeDto.UEND::class, name = "UEND"),
    JsonSubTypes.Type(value = FeriepengerendringskodeDto.ENDR::class, name = "ENDR"),
)
sealed class FeriepengerendringskodeDto {
    data object NY : FeriepengerendringskodeDto()

    data object UEND : FeriepengerendringskodeDto()

    data object ENDR : FeriepengerendringskodeDto()
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = FeriepengerklassekodeDto.RefusjonFeriepengerIkkeOpplysningspliktig::class, name = "RefusjonFeriepengerIkkeOpplysningspliktig"),
    JsonSubTypes.Type(value = FeriepengerklassekodeDto.SykepengerArbeidstakerFeriepenger::class, name = "SykepengerArbeidstakerFeriepenger"),
)
sealed class FeriepengerklassekodeDto(val verdi: String) {
    data object RefusjonFeriepengerIkkeOpplysningspliktig : FeriepengerklassekodeDto("SPREFAGFER-IOP")

    data object SykepengerArbeidstakerFeriepenger : FeriepengerklassekodeDto("SPATFER")
}
