package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.sykepenger.Periode
import java.util.UUID

@JvmInline
value class YrkesaktivitetId(
    val value: UUID,
)

sealed interface Yrkesaktivitet {
    val id: YrkesaktivitetId
    val sykefraværstilfelleId: SykefraværstilfelleId

    sealed interface Arbeidstaker : Yrkesaktivitet {
        val arbeidsgiverperioder: Arbeidsgiverperioder

        class Ordinær(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleId: SykefraværstilfelleId,
            val organisasjonsnummer: String,
            override val arbeidsgiverperioder: Arbeidsgiverperioder,
        ) : Arbeidstaker

        class Maritim(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleId: SykefraværstilfelleId,
            val organisasjonsnummer: String,
            override val arbeidsgiverperioder: Arbeidsgiverperioder,
        ) : Arbeidstaker

        class Fisker(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleId: SykefraværstilfelleId,
            val organisasjonsnummer: String,
            override val arbeidsgiverperioder: Arbeidsgiverperioder,
        ) : Arbeidstaker

        class DimmitertVernepliktig(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleId: SykefraværstilfelleId,
            override val arbeidsgiverperioder: Arbeidsgiverperioder,
        ) : Arbeidstaker

        class PrivatArbeidsgiver(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleId: SykefraværstilfelleId,
            val arbeidsgiversFødselsnummer: String,
            override val arbeidsgiverperioder: Arbeidsgiverperioder,
        ) : Arbeidstaker
    }

    sealed interface SelvstendigNæringsdrivende : Yrkesaktivitet {
        val perioder: Ventetidsperioder
        val forsikring: SelvstendigForsikring

        class Ordinær(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleId: SykefraværstilfelleId,
            override val forsikring: SelvstendigForsikring,
            override val perioder: Ventetidsperioder,
        ) : SelvstendigNæringsdrivende

        class BarnepasserEgetHjem(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleId: SykefraværstilfelleId,
            override val forsikring: SelvstendigForsikring,
            override val perioder: Ventetidsperioder,
        ) : SelvstendigNæringsdrivende

        // Fisker har alltid 100% fra første sykedag implisitt
        class Fisker(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleId: SykefraværstilfelleId,
            override val perioder: Ventetidsperioder,
        ) : SelvstendigNæringsdrivende {
            override val forsikring: SelvstendigForsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG
        }

        class Jordbruker(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleId: SykefraværstilfelleId,
            override val forsikring: SelvstendigForsikring,
            override val perioder: Ventetidsperioder,
        ) : SelvstendigNæringsdrivende

        class Reindrift(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleId: SykefraværstilfelleId,
            override val forsikring: SelvstendigForsikring,
            override val perioder: Ventetidsperioder,
        ) : SelvstendigNæringsdrivende
    }

    class Frilanser(
        override val id: YrkesaktivitetId,
        override val sykefraværstilfelleId: SykefraværstilfelleId,
        val organisasjonsnummer: String,
        val forsikring: FrilanserForsikring,
        val ventetidsperioder: Ventetidsperioder,
    ) : Yrkesaktivitet

    class Arbeidsledig(
        override val id: YrkesaktivitetId,
        override val sykefraværstilfelleId: SykefraværstilfelleId,
    ) : Yrkesaktivitet

    companion object {
        fun ordinærArbeidstaker(
            sykefraværstilfelleId: SykefraværstilfelleId,
            organisasjonsnummer: String,
            arbeidsgiverperioder: Arbeidsgiverperioder,
        ) = Arbeidstaker.Ordinær(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleId = sykefraværstilfelleId,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsgiverperioder = arbeidsgiverperioder,
        )

        fun maritimArbeidstaker(
            sykefraværstilfelleId: SykefraværstilfelleId,
            organisasjonsnummer: String,
            arbeidsgiverperioder: Arbeidsgiverperioder,
        ) = Arbeidstaker.Maritim(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleId = sykefraværstilfelleId,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsgiverperioder = arbeidsgiverperioder,
        )

        fun fisker(
            sykefraværstilfelleId: SykefraværstilfelleId,
            organisasjonsnummer: String,
            arbeidsgiverperioder: Arbeidsgiverperioder,
        ) = Arbeidstaker.Fisker(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleId = sykefraværstilfelleId,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsgiverperioder = arbeidsgiverperioder,
        )

        fun dimmitertVernepliktig(
            sykefraværstilfelleId: SykefraværstilfelleId,
            arbeidsgiverperioder: Arbeidsgiverperioder,
        ) = Arbeidstaker.DimmitertVernepliktig(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleId = sykefraværstilfelleId,
            arbeidsgiverperioder = arbeidsgiverperioder,
        )

        fun medPrivatArbeidsgiver(
            sykefraværstilfelleId: SykefraværstilfelleId,
            arbeidsgiversFødselsnummer: String,
            arbeidsgiverperioder: Arbeidsgiverperioder,
        ) = Arbeidstaker.PrivatArbeidsgiver(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleId = sykefraværstilfelleId,
            arbeidsgiversFødselsnummer = arbeidsgiversFødselsnummer,
            arbeidsgiverperioder = arbeidsgiverperioder,
        )

        fun ordinærSelvstendigNæringsdrivende(
            sykefraværstilfelleId: SykefraværstilfelleId,
            forsikring: SelvstendigForsikring,
            perioder: Ventetidsperioder,
        ) = SelvstendigNæringsdrivende.Ordinær(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleId = sykefraværstilfelleId,
            forsikring = forsikring,
            perioder = perioder,
        )

        fun barnepasser(
            sykefraværstilfelleId: SykefraværstilfelleId,
            forsikring: SelvstendigForsikring,
            perioder: Ventetidsperioder,
        ) = SelvstendigNæringsdrivende.BarnepasserEgetHjem(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleId = sykefraværstilfelleId,
            forsikring = forsikring,
            perioder = perioder,
        )

        fun selvstendigFisker(
            sykefraværstilfelleId: SykefraværstilfelleId,
            perioder: Ventetidsperioder,
        ) = SelvstendigNæringsdrivende.Fisker(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleId = sykefraværstilfelleId,
            perioder = perioder,
        )

        fun jordbruker(
            sykefraværstilfelleId: SykefraværstilfelleId,
            perioder: Ventetidsperioder,
            forsikring: SelvstendigForsikring,
        ) = SelvstendigNæringsdrivende.Jordbruker(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleId = sykefraværstilfelleId,
            perioder = perioder,
            forsikring = forsikring,
        )

        fun reindrift(
            sykefraværstilfelleId: SykefraværstilfelleId,
            perioder: Ventetidsperioder,
            forsikring: SelvstendigForsikring,
        ) = SelvstendigNæringsdrivende.Reindrift(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleId = sykefraværstilfelleId,
            perioder = perioder,
            forsikring = forsikring,
        )

        fun frilanser(
            sykefraværstilfelleId: SykefraværstilfelleId,
            organisasjonsnummer: String,
            forsikring: FrilanserForsikring,
            ventetidsperioder: Ventetidsperioder,
        ) = Frilanser(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleId = sykefraværstilfelleId,
            organisasjonsnummer = organisasjonsnummer,
            forsikring = forsikring,
            ventetidsperioder = ventetidsperioder,
        )

        fun arbeidsledig(
            sykefraværstilfelleId: SykefraværstilfelleId,
        ) = Arbeidsledig(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleId = sykefraværstilfelleId,
        )
    }
}

class Ventetidsperioder(
    val perioder: List<Periode>,
)

class Arbeidsgiverperioder(
    val perioder: List<Periode>,
)
