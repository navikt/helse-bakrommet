package no.nav.helse.bakrommet.kodeverk

enum class VilkårskodeBegrunnelse {
    UTE_AV_ARBEID_INNEKTSTAP,
    UTE_AV_ARBEID_IKKE_INNTEKTSTAP,
    UTE_AV_ARBEID_MINST_1G,
    UTE_AV_ARBEID_MINDRE_ENN_1G,

    MINSTEINNTEKT, // 8-3, krever minst en halv G
    IKKE_MINSTEINNTEKT, // 8-3, krever minst en halv G
}
