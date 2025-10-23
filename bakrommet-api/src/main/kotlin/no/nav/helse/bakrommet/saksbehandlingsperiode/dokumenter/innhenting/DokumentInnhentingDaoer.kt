package no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting

import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDao

interface DokumentInnhentingDaoer {
    val personDao: PersonDao
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val dokumentDao: DokumentDao
}
