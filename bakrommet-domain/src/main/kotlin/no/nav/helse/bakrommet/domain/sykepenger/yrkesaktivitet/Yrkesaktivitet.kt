package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.sykepenger.Periode
import java.util.UUID

@JvmInline
value class YrkesaktivitetId(
    val value: UUID,
)

sealed interface Yrkesaktivitet {
    val id: YrkesaktivitetId
    val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId

    sealed interface Arbeidstaker : Yrkesaktivitet {
        val arbeidsgiverperioder: Arbeidsgiverperioder

        class Ordinær(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            val organisasjonsnummer: String,
            override val arbeidsgiverperioder: Arbeidsgiverperioder,
        ) : Arbeidstaker

        class Maritim(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            val organisasjonsnummer: String,
            override val arbeidsgiverperioder: Arbeidsgiverperioder,
        ) : Arbeidstaker

        class Fisker(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            val organisasjonsnummer: String,
            override val arbeidsgiverperioder: Arbeidsgiverperioder,
        ) : Arbeidstaker

        class DimmitertVernepliktig(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            override val arbeidsgiverperioder: Arbeidsgiverperioder,
        ) : Arbeidstaker

        class PrivatArbeidsgiver(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            val arbeidsgiversFødselsnummer: String,
            override val arbeidsgiverperioder: Arbeidsgiverperioder,
        ) : Arbeidstaker
    }

    sealed interface SelvstendigNæringsdrivende : Yrkesaktivitet {
        val perioder: Ventetidsperioder
        val forsikring: SelvstendigForsikring

        class Ordinær(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            override val forsikring: SelvstendigForsikring,
            override val perioder: Ventetidsperioder,
        ) : SelvstendigNæringsdrivende

        class BarnepasserEgetHjem(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            override val forsikring: SelvstendigForsikring,
            override val perioder: Ventetidsperioder,
        ) : SelvstendigNæringsdrivende

        // Fisker har alltid 100% fra første sykedag implisitt
        class Fisker(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            override val perioder: Ventetidsperioder,
        ) : SelvstendigNæringsdrivende {
            override val forsikring: SelvstendigForsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG
        }

        class Jordbruker(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            override val forsikring: SelvstendigForsikring,
            override val perioder: Ventetidsperioder,
        ) : SelvstendigNæringsdrivende

        class Reindrift(
            override val id: YrkesaktivitetId,
            override val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            override val forsikring: SelvstendigForsikring,
            override val perioder: Ventetidsperioder,
        ) : SelvstendigNæringsdrivende
    }

    class Frilanser(
        override val id: YrkesaktivitetId,
        override val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
        val organisasjonsnummer: String,
        val forsikring: FrilanserForsikring,
        val ventetidsperioder: Ventetidsperioder,
    ) : Yrkesaktivitet

    class Arbeidsledig(
        override val id: YrkesaktivitetId,
        override val sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
    ) : Yrkesaktivitet

    companion object {
        fun ordinærArbeidstaker(
            sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            organisasjonsnummer: String,
            arbeidsgiverperioder: Arbeidsgiverperioder,
        ) = Arbeidstaker.Ordinær(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleVersjonId = sykefraværstilfelleVersjonId,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsgiverperioder = arbeidsgiverperioder,
        )

        fun maritimArbeidstaker(
            sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            organisasjonsnummer: String,
            arbeidsgiverperioder: Arbeidsgiverperioder,
        ) = Arbeidstaker.Maritim(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleVersjonId = sykefraværstilfelleVersjonId,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsgiverperioder = arbeidsgiverperioder,
        )

        fun fisker(
            sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            organisasjonsnummer: String,
            arbeidsgiverperioder: Arbeidsgiverperioder,
        ) = Arbeidstaker.Fisker(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleVersjonId = sykefraværstilfelleVersjonId,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsgiverperioder = arbeidsgiverperioder,
        )

        fun dimmitertVernepliktig(
            sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            arbeidsgiverperioder: Arbeidsgiverperioder,
        ) = Arbeidstaker.DimmitertVernepliktig(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleVersjonId = sykefraværstilfelleVersjonId,
            arbeidsgiverperioder = arbeidsgiverperioder,
        )

        fun medPrivatArbeidsgiver(
            sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            arbeidsgiversFødselsnummer: String,
            arbeidsgiverperioder: Arbeidsgiverperioder,
        ) = Arbeidstaker.PrivatArbeidsgiver(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleVersjonId = sykefraværstilfelleVersjonId,
            arbeidsgiversFødselsnummer = arbeidsgiversFødselsnummer,
            arbeidsgiverperioder = arbeidsgiverperioder,
        )

        fun ordinærSelvstendigNæringsdrivende(
            sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            forsikring: SelvstendigForsikring,
            perioder: Ventetidsperioder,
        ) = SelvstendigNæringsdrivende.Ordinær(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleVersjonId = sykefraværstilfelleVersjonId,
            forsikring = forsikring,
            perioder = perioder,
        )

        fun barnepasser(
            sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            forsikring: SelvstendigForsikring,
            perioder: Ventetidsperioder,
        ) = SelvstendigNæringsdrivende.BarnepasserEgetHjem(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleVersjonId = sykefraværstilfelleVersjonId,
            forsikring = forsikring,
            perioder = perioder,
        )

        fun selvstendigFisker(
            sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            perioder: Ventetidsperioder,
        ) = SelvstendigNæringsdrivende.Fisker(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleVersjonId = sykefraværstilfelleVersjonId,
            perioder = perioder,
        )

        fun jordbruker(
            sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            perioder: Ventetidsperioder,
            forsikring: SelvstendigForsikring,
        ) = SelvstendigNæringsdrivende.Jordbruker(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleVersjonId = sykefraværstilfelleVersjonId,
            perioder = perioder,
            forsikring = forsikring,
        )

        fun reindrift(
            sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            perioder: Ventetidsperioder,
            forsikring: SelvstendigForsikring,
        ) = SelvstendigNæringsdrivende.Reindrift(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleVersjonId = sykefraværstilfelleVersjonId,
            perioder = perioder,
            forsikring = forsikring,
        )

        fun frilanser(
            sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
            organisasjonsnummer: String,
            forsikring: FrilanserForsikring,
            ventetidsperioder: Ventetidsperioder,
        ) = Frilanser(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleVersjonId = sykefraværstilfelleVersjonId,
            organisasjonsnummer = organisasjonsnummer,
            forsikring = forsikring,
            ventetidsperioder = ventetidsperioder,
        )

        fun arbeidsledig(
            sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId,
        ) = Arbeidsledig(
            id = YrkesaktivitetId(UUID.randomUUID()),
            sykefraværstilfelleVersjonId = sykefraværstilfelleVersjonId,
        )
    }
}

class Ventetidsperioder(
    val perioder: List<Periode>,
)

class Arbeidsgiverperioder(
    val perioder: List<Periode>,
)
