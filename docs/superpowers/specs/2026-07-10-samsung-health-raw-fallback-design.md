# Samsung Health Official Raw Steps Fallback Design

**Datum:** 2026-07-10  
**Status:** Goedgekeurd door gebruiker  
**Aanleiding:** Op hetzelfde fysieke Samsung-toestel en hetzelfde tijdstip toonde Samsung Health 84 stappen, terwijl TrainIQ 5 liet zien. TrainIQ diagnosticeerde daarbij Health Connect aggregate 5, Samsung-origin aggregate 5 en Samsung raw 84. De directe Samsung Health Data SDK-read was niet beschikbaar door autorisatiefout 2003.

## Doel

TrainIQ toont voor vandaag dezelfde bruikbare Samsung-stappenwaarde als Samsung Health wanneer Health Connect de officiële Samsung raw `StepsRecord`-waarde wel levert, maar zijn aggregates aantoonbaar lager blijven.

## Bronregel

De geselecteerde dagwaarde volgt deze volgorde:

1. Een verse directe Samsung Health Data SDK `TOTAL`, inclusief een geldige waarde `0`.
2. Anders de hoogste positieve waarde van:
   - de som van raw `StepsRecord.count` uit exact `com.sec.android.app.shealth` binnen het bestaande lokale dagvenster;
   - de Health Connect aggregate gefilterd op de officiële Samsung Health `DataOrigin`.
3. Anders de algemene Health Connect-stappenaggregate.

Voor het gemelde bewijsgeval wordt dat: direct niet beschikbaar, officiële raw 84, Samsung aggregate 5 en algemene aggregate 5, dus TrainIQ toont 84.

## Veiligheidsgrenzen

- Alleen `metadata.dataOrigin.packageName == com.sec.android.app.shealth` (case-insensitive) mag aan de raw fallback bijdragen.
- Een package dat alleen het woord `samsung` bevat blijft hoogstens een diagnostisch bronlabel.
- De app blijft het bestaande lokale dagvenster `00:00` tot het huidige tijdstip gebruiken.
- Individuele raw records, IDs en timestamps worden niet nieuw gepersist; alleen de afgeleide scalaire dagwaarde, datum en bestaande bronmetadata blijven cachebaar.
- Een verse directe Data SDK-waarde blijft altijd leidend, ook als raw of aggregate hoger is.
- Bestaande cache-, datumwissel-, permission-, provenance- en foutafhandeling blijven ongewijzigd.

## Diagnose en gebruikersuitleg

Wanneer officiële raw Samsung-stappen de aggregate overtreffen en daadwerkelijk worden getoond, meldt de diagnose expliciet dat de officiële Samsung raw fallback is gebruikt en houdt zij raw, Samsung aggregate, algemene aggregate en directe SDK-status afzonderlijk zichtbaar.

## Acceptatiecriteria

- `aggregate=5`, `Samsung aggregate=5`, `official raw=84`, `direct=null` resulteert in 84.
- `direct=0` of een positieve directe waarde blijft autoritatief.
- Een raw package-lookalike kan de geselecteerde waarde niet beïnvloeden.
- Een hogere Samsung aggregate blijft bruikbaar wanneer raw lager of afwezig is.
- Geen individuele raw staprecords worden aan de cache toegevoegd.
- Gerichte tests, alle debug-unit-tests, assemble, lint en AndroidTest-compilatie slagen.

