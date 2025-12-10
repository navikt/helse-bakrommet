package no.nav.helse.bakrommet.behandling.dokumenter.innhenting

import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDao
import no.nav.helse.bakrommet.person.PersonPseudoIdDao

interface DokumentInnhentingDaoer {
    val personPseudoIdDao: PersonPseudoIdDao
    val behandlingDao: BehandlingDao
    val dokumentDao: DokumentDao
}
