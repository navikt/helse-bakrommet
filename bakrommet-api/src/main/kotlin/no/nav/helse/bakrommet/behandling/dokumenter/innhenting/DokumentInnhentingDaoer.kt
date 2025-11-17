package no.nav.helse.bakrommet.behandling.dokumenter.innhenting

import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDao
import no.nav.helse.bakrommet.person.PersonDao

interface DokumentInnhentingDaoer {
    val personDao: PersonDao
    val behandlingDao: BehandlingDao
    val dokumentDao: DokumentDao
}
