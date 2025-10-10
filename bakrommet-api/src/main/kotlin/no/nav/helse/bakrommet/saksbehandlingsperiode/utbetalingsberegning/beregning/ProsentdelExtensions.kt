package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning

import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.økonomi.Prosentdel

/**
 * Konverterer ProsentdelDto til Prosentdel
 */
fun ProsentdelDto.tilProsentdel(): Prosentdel = Prosentdel.gjenopprett(this)
