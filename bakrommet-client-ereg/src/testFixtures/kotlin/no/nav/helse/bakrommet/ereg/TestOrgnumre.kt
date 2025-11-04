package no.nav.helse.bakrommet.ereg

data class Organisasjon(
    val navn: String,
    val orgnummer: String,
) {
    fun toPair() = orgnummer to navn
}

val kranførerkompaniet =
    Organisasjon(
        navn = "Kranførerkompaniet",
        orgnummer = "987654321",
    )
val krankompisen =
    Organisasjon(
        navn = "Krankompisen",
        orgnummer = "123456789",
    )
val danskebåten =
    Organisasjon(
        navn = "Danskebåten",
        orgnummer = "889955555",
    )
val pengeløsSparebank =
    Organisasjon(
        navn = "Pengeløs Sparebank",
        orgnummer = "972674818",
    )
val ruterNesoddbåten =
    Organisasjon(
        navn = "Ruter, avd Nesoddbåten",
        orgnummer = "222222222",
    )
val veganskSlakteri =
    Organisasjon(
        navn = "Vegansk slakteri",
        orgnummer = "805824352",
    )
val sauefabrikk =
    Organisasjon(
        navn = "Sauefabrikk",
        orgnummer = "896929119",
    )
val sjokkerendeElektriker =
    Organisasjon(
        navn = "Sjokkerende elektriker",
        orgnummer = "947064649",
    )
val snillTorpedo =
    Organisasjon(
        navn = "Snill torpedo",
        orgnummer = "967170232",
    )
val hårreisendeFrisør =
    Organisasjon(
        navn = "Hårreisende frisør",
        orgnummer = "839942907",
    )
val klonelabben =
    Organisasjon(
        navn = "Klonelabben",
        orgnummer = "907670201",
    )
val mursteinAS =
    Organisasjon(
        navn = "Murstein AS",
        orgnummer = "999999991",
    )
val betongbyggAS =
    Organisasjon(
        navn = "Betongbygg AS",
        orgnummer = "999999992",
    )
val veihjelpenAS =
    Organisasjon(
        navn = "Veihjelpen AS",
        orgnummer = "963743254",
    )

val organisasjonsnavnMap: Map<String, String> =
    mapOf(
        kranførerkompaniet.toPair(),
        krankompisen.toPair(),
        danskebåten.toPair(),
        pengeløsSparebank.toPair(),
        ruterNesoddbåten.toPair(),
        veganskSlakteri.toPair(),
        sauefabrikk.toPair(),
        sjokkerendeElektriker.toPair(),
        snillTorpedo.toPair(),
        hårreisendeFrisør.toPair(),
        klonelabben.toPair(),
        mursteinAS.toPair(),
        betongbyggAS.toPair(),
        veihjelpenAS.toPair(),
    )
