package no.nav.helse.bakrommet.ereg

typealias Organisasjon = Pair<String, String>

val kranførerkompaniet =
    Organisasjon(
        "987654321",
        "Kranførerkompaniet",
    )
val krankompisen =
    Organisasjon(
        "123456789",
        "Krankompisen",
    )
val danskebåten =
    Organisasjon(
        "889955555",
        "Danskebåten",
    )
val pengeløsSparebank =
    Organisasjon(
        "972674818",
        "Pengeløs Sparebank",
    )
val ruterNesoddbåten =
    Organisasjon(
        "222222222",
        "Ruter, avd Nesoddbåten",
    )
val veganskSlakteri =
    Organisasjon(
        "805824352",
        "Vegansk slakteri",
    )
val sauefabrikk =
    Organisasjon(
        "896929119",
        "Sauefabrikk",
    )
val sjokkerendeElektriker =
    Organisasjon(
        "947064649",
        "Sjokkerende elektriker",
    )
val snillTorpedo =
    Organisasjon(
        "967170232",
        "Snill torpedo",
    )
val hårreisendeFrisør =
    Organisasjon(
        "839942907",
        "Hårreisende frisør",
    )
val klonelabben =
    Organisasjon(
        "907670201",
        "Klonelabben",
    )
val mursteinAS =
    Organisasjon(
        "999999991",
        "Murstein AS",
    )
val betongbyggAS =
    Organisasjon(
        "999999992",
        "Betongbygg AS",
    )

val malermesternAS =
    Organisasjon(
        "834567890",
        "Malermestern AS",
    )

val veihjelpenAS =
    Organisasjon(
        "963743254",
        "Veihjelpen AS",
    )
val skogenSFO =
    Organisasjon(
        "999999993",
        "Skogen SFO",
    )

val organisasjonsnavnMap: Map<String, String> =
    mapOf(
        kranførerkompaniet,
        krankompisen,
        danskebåten,
        pengeløsSparebank,
        ruterNesoddbåten,
        veganskSlakteri,
        sauefabrikk,
        sjokkerendeElektriker,
        snillTorpedo,
        hårreisendeFrisør,
        klonelabben,
        mursteinAS,
        betongbyggAS,
        veihjelpenAS,
        skogenSFO,
        malermesternAS,
    ).also {
        it.forEach { (navn, orgnr) ->
            require(!it.containsKey(orgnr)) { "Duplikat orgnr $orgnr for organisasjonene ${it[orgnr]} og $navn" }
        }
    }
