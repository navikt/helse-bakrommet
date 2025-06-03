package no.nav.helse.bakrommet.testutils

import no.nav.helse.bakrommet.util.objectMapper

inline fun <reified T> String.somListe(): List<T> {
    return objectMapper.readValue(
        this,
        objectMapper.typeFactory.constructCollectionType(
            List::class.java,
            T::class.java,
        ),
    )
}
