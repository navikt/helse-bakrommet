package no.nav.helse.bakrommet.api.dto.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse
import java.util.*

data class YrkesaktivitetDto(
    val id: UUID,
    val kategorisering: YrkesaktivitetKategoriseringDto,
    val orgnavn: String? = null,
    val dagoversikt: List<DagDto>?,
    val generertFraDokumenter: List<UUID>,
    val perioder: PerioderDto?,
    val inntektRequest: InntektRequestDto?,
    val inntektData: InntektDataDto?,
    val refusjon: List<RefusjonsperiodeDto>? = null,
) : ApiResponse
