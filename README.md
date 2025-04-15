# Spotlight
[![Bygg og deploy Spinnvill](https://github.com/navikt/helse-spotlight/actions/workflows/main.yml/badge.svg)](https://github.com/navikt/helse-spotlight/actions/workflows/main.yml)

## Beskrivelse
Backend som lagrer ned kommandokjeder som sitter fast i [Spesialist](https://github.com/navikt/helse-spotlight) og påminner dem.
Varsler på Slack hver morgen om kommandokjeder som er stuck.

Spotlight benytter seg av slack-botten Spy som poster oppdateringer på Slack om hvordan det ligger an med kommandokjedene. Spy poster alle kommandokjeder som er stuck kl 6 hver morgen, til kanalene #speilvendt-alerts og #speilvendt-alerts-dev. For Spotlight er det egne kanaler som benyttes.

Hver halvtime så påminner Spotlight Spesialist om å fortsette suspenderte kommandokjeder. Det skal da skje oftere enn slik det var før Spotlight når Spesialist måtte vente på nytt godkjenningsbehov. 

### Hvordan det var før (ish): 
En periode har stoppet opp i spesialist, den venter f.eks på svar på et behov. Nytt godkjenningsbehov blir sendt 1 gang i døgnet fra Spleis. Spesialist sjekker om det finnes en allerede startet kommandokjede og om den er suspendert, hvis ja blir kommandokjeden avbrutt. 

### Hvordan det er nå (ish):
Spotlight påminner Spesialist om en suspendert kommandokjede. Gitt tilfelle hvor spesialist venter på svar på behov vil den sende ut et nytt behov, forhåpentligvis få svar og kommandokjeden vil fortsette. 

## Format på Commit-meldinger 
I dette repoet skal det commites på dette formatet:
- `[gitmoji] [teksten din]`

  gitmoji finner du her: (https://gitmoji.dev/)
- Eksempel: ✅ Test automatisering uten varsel

NB: Husk å bytte til å bruke unicode characters i stedet for `:<emote>:` notasjon

## Manuell påminnelse
Fyll ut denne og send den fra spout.ansatt.dev.nav.no eller spout.intern.nav.noª:
```json
{
  "@event_name": "kommandokjede_påminnelse",
  "@id": "{{uuidgen}}",
  "commandContextId": "<fyll meg ut>",
  "meldingId": "<fyll meg ut>"
}
```
Dataene kan du finne i logger eller databasen til spesialist (eller spotlight).ª

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen [#team-bømlo-værsågod](https://nav-it.slack.com/archives/C019637N90X).
