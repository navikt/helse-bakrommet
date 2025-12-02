package no.nav.helse.bakrommet.api.dto.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse

data class YrkesaktivitetDto(
    val id: String, // UUID som String
    val kategorisering: YrkesaktivitetKategoriseringDto,
    val dagoversikt: List<DagDto>?,
    val generertFraDokumenter: List<String>, // List<UUID> som List<String>
    val perioder: PerioderDto?,
    val inntektRequest: InntektRequestDto?,
    val inntektData: InntektDataDto?,
    val refusjon: List<RefusjonsperiodeDto>? = null,
) : ApiResponse
