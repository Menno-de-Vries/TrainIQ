# Health Connect Compact Copy Design

**Datum:** 2026-07-10  
**Status:** Ontwerpoptie 1 goedgekeurd door gebruiker  
**Scope:** Alleen de presentatie van Health Connect-status op Home en in Instellingen. De stappenbron, Samsung raw fallback, synchronisatie, cache, permissies en diagnostische waarden veranderen niet.

## Probleem

De huidige Home-kaart toont bronvensters, bronlijsten en Samsung-pariteitsuitleg alsof dit normale gebruikersinformatie is. In Instellingen worden alle technische diagnosevelden tegelijk getoond. Daardoor nemen succesvolle synchronisaties veel schermruimte in en wordt de belangrijkste informatie moeilijker scanbaar.

## Ontwerpprincipe

Gebruik progressive disclosure:

1. Toon gewone gebruikers alleen status, actuele waarde, bron, tijd en relevante actie.
2. Bewaar alle technische diagnose voor support en probleemonderzoek.
3. Maak die diagnose bereikbaar via één duidelijke, standaard ingeklapte bediening.

## Home

De Health Connect-kaart blijft bestaan, maar toont bij een succesvolle verse meting uitsluitend:

- titel `Health Connect`;
- één primaire regel, bijvoorbeeld `84 stappen · Samsung Health`;
- één secundaire regel `Bijgewerkt om 02:33`;
- actie `Verversen`;
- een kort tijdelijk resultaat na handmatig verversen.

Home toont nooit tegelijk de compacte sync-kaart en de algemene Health Connect-permissiekaart. Bij `CONNECTED`/`NO_DATA` met bruikbare stappentoegang verschijnt alleen de compacte sync-kaart. Bij ontbrekende stappentoegang, providerproblemen, unsupported of error verschijnt alleen de bestaande actiekaart met de passende herstelactie.

Home toont niet langer:

- queryvenster;
- volledige bronlijst;
- raw/aggregate/Data SDK-termen;
- pariteitsverklaring;
- Health Connect-prioriteitsuitleg.

Bij stale, ontbrekende toestemming, providerproblemen of leesfouten toont Home maximaal één korte, actiegerichte zin. Bestaande foutstatus en laatste bekende waarden blijven inhoudelijk correct.

## Instellingen

De Health Connect-sectie toont standaard:

- compacte status, bijvoorbeeld `Verbonden`;
- gebruikerssamenvatting, bijvoorbeeld `84 stappen via Samsung Health`;
- `Laatst gecontroleerd om 02:33`;
- bestaande primaire acties die nodig zijn om toegang te geven, te vernieuwen of Health Connect te openen;
- een bediening `Technische details tonen`.

De lange achtergrondsyncuitleg en alle stapdiagnose staan in een standaard ingeklapte technische sectie:

- bronlijst en queryvenster;
- Samsung vergelijking;
- afzonderlijke getoonde, Health Connect aggregate, Samsung aggregate, raw en directe waarden;
- Samsung-brontiming en directe SDK-status;
- pariteits-, prioriteits- en workout-overlapuitleg;
- syncadvies;
- `Diagnose kopiëren`;
- `Samsung Health openen` en `Prioriteiten openen` waar van toepassing.

De sectie gebruikt `rememberSaveable`, zodat open/dicht tijdens configuratierecreatie behouden blijft. Zij opent niet automatisch: normale foutcopy blijft kort en actiegericht; supportdetails zijn altijd handmatig bereikbaar.

## Copyregels

- Gebruik `Samsung Health` als bronnaam wanneer de directe of officiële Samsung-visible waarde wordt getoond.
- Gebruik `Health Connect` wanneer de algemene Health Connect-aggregate wordt getoond.
- Gebruik geen `raw`, `aggregate`, `SDK`, `pariteit` of foutcode in de standaard Home- of Settings-samenvatting.
- Toon een succesvolle nul als `0 stappen`, niet als ontbrekende data.
- Toon stale data als `Laatst bekend: N stappen`.
- Laat toestemming/provider/error-copy maximaal één korte zin zijn.

## Toegankelijkheid en layout

- De uitklapbediening gebruikt een Material-knop met minimaal 48dp aanraakgebied.
- De bediening heeft een duidelijke semantische status: `Technische details tonen` of `Technische details verbergen`.
- De compacte regels mogen afbreken bij grote lettertypes; essentiële acties blijven bereikbaar op 360x640 en font scale 1.3+.
- Gebruik uitsluitend bestaande `MaterialTheme.typography`, kleuren, spacing en kaartcomponenten.
- Voeg geen dependency, navigatieroute of apart diagnosescherm toe.

## Teststrategie

- TDD voor de compacte Home-regels bij verse Samsung, verse Health Connect, stale, ontbrekende toestemming en error.
- TDD voor de compacte Settings-samenvatting, inclusief verse nul en stale data.
- Bron-/UI-regressietest dat technische diagnose achter de uitklapper blijft en bestaande diagnose-/actiecontracten behouden zijn.
- Gerichte Home- en Settings-unit-tests.
- Volledige debug-unit-suite, assemble, lint en AndroidTest-compilatie.
- APK-installatie en fysieke Samsung-smoke door de gebruiker; controleer Home en Instellingen ook met grotere lettergrootte indien praktisch.

## Acceptatiecriteria

- Een succesvolle Home-kaart past inhoudelijk in titel plus twee korte regels en een refreshactie.
- Home bevat geen pariteitsparagraaf of technische bronvelden.
- Instellingen toont standaard hoogstens status, samenvatting, tijd, noodzakelijke acties en de technische-detailsknop.
- Alle bestaande technische diagnose is na uitklappen nog leesbaar en kopieerbaar.
- De Samsung raw fallback blijft exact dezelfde 84 stappen selecteren als vóór deze UX-wijziging.
- Geen bestaande permission-, sync-, cache- of diagnoseactie verdwijnt.
