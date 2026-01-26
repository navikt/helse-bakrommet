package no.nav.helse.bakrommet.pdl

import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.fakerConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Random

val pdlReplyGenerator: (String) -> String? = { ident ->
    // Hvis fnr ikke starter med 404404
    if (!ident.startsWith("404404")) {
        // Seed faker fra fnr for konsistent generering
        val seed = ident.hashCode().toLong()
        val config =
            fakerConfig {
                locale = "nb_NO"
                random = Random(seed)
            }
        val faker = Faker(config)

        val fornavn = faker.name.firstName()
        val etternavn = faker.name.lastName()
        val mellomnavn = if (faker.random.nextBoolean()) faker.name.firstName() else null

        // Generer fødselsdato (mellom 18 og 80 år gammel)
        val alder = faker.random.nextInt(18, 80)
        val fødselsdato =
            LocalDate
                .now()
                .minusYears(alder.toLong())
                .minusDays(faker.random.nextInt(0, 365).toLong())

        // Generer aktørId basert på fnr
        val aktorId = "${ident}00"

        PdlMock.pdlReply(
            fnr = ident,
            aktorId = aktorId,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            foedselsdato = fødselsdato.format(DateTimeFormatter.ISO_DATE),
        )
    } else {
        null
    }
}
