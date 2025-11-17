package no.nav.helse.bakrommet.behandling.dokumenter.innhenting

import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDao
import no.nav.helse.bakrommet.person.PersonDao

interface DokumentInnhentingDaoer {
    val personDao: PersonDao
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val dokumentDao: DokumentDao
}
