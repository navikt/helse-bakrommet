package no.nav.helse.bakrommet.ereg

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

val malermesternAS =
    Organisasjon(
        navn = "Malermestern AS",
        orgnummer = "834567890",
    )

val plankeFabrikken =
    Organisasjon(
        navn = "Fredrikstad Plankefabrikk AS",
        orgnummer = "834567000",
    )

val veihjelpenAS =
    Organisasjon(
        navn = "Veihjelpen AS",
        orgnummer = "963743254",
    )
val skogenSFO =
    Organisasjon(
        navn = "Skogen SFO",
        orgnummer = "999999993",
    )

val organisasjonsnavnMap: Map<String, Organisasjon> =
    mapOf(
        kranførerkompaniet.orgnummer to kranførerkompaniet,
        plankeFabrikken.orgnummer to plankeFabrikken,
        krankompisen.orgnummer to krankompisen,
        danskebåten.orgnummer to danskebåten,
        pengeløsSparebank.orgnummer to pengeløsSparebank,
        ruterNesoddbåten.orgnummer to ruterNesoddbåten,
        veganskSlakteri.orgnummer to veganskSlakteri,
        sauefabrikk.orgnummer to sauefabrikk,
        sjokkerendeElektriker.orgnummer to sjokkerendeElektriker,
        snillTorpedo.orgnummer to snillTorpedo,
        hårreisendeFrisør.orgnummer to hårreisendeFrisør,
        klonelabben.orgnummer to klonelabben,
        mursteinAS.orgnummer to mursteinAS,
        betongbyggAS.orgnummer to betongbyggAS,
        veihjelpenAS.orgnummer to veihjelpenAS,
        skogenSFO.orgnummer to skogenSFO,
        malermesternAS.orgnummer to malermesternAS,
    ).also {
        val seenOrgnr = mutableSetOf<String>()
        it.forEach { (orgnr, organisasjon) ->
            require(seenOrgnr.add(orgnr)) { "Duplikat orgnr $orgnr for organisasjonene ${it[orgnr]} og ${organisasjon.navn}" }
        }
    }
