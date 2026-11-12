# TrainIQ regressieonderzoek — 12 november 2026

## Doel en grenzen

Onderzoek en herstel van de gemelde mislukte AI-maaltijdscan met foto plus
extra tekst, aangevuld met een gerichte inventarisatie van backend- en
gegevensrisico's. Uitgangspunt: main `1b8333ba9d6a7dc34f530ebaa4dc779c3ef9a956`.
Kleine herstelbare wijzigingen, lokale verificatie en onafhankelijke review
gaan vooraf aan de geautoriseerde integratie en APK-levering.
Geen schema-, provider-, sleutel- of deelrechtenwijzigingen; geen automatische
herimport van bestaande gezaghebbende Room-databases.

## Compacte inventarisatie

| Prioriteit | Bevinding en afbakening | Herstel / bewijs |
|---|---|---|
| P1 | Bibliotheekseeding kon vanuit een nog lege asynchrone snapshot bestaande oefening-ID's overschrijven, inclusief betekenis van historie. Bestaand defect; niet toegeschreven aan de vorige PR. | Eerst initialiseren, vervolgens actuele Room-oefeningen en nieuwe ID's binnen mutex en transactie bepalen. Herhaalde seeding en behoud van custom historie getest. |
| P1 | Ontbrekende `sex` in ondersteund oud JSON-profiel kon de eerste import volledig afbreken. Bestaand defect. | Null-veilige standaardwaarden; echte startup-importtest met bestaande fixture. Seeding blokkeert herstel na een onleesbare eerste bron niet. |
| P2 | Het oorspronkelijke maaltijdformaat met `date` en geaggregeerde macro's kon als moderne lege maaltijd op epoch nul worden gelezen. Bestaand defect. | Alleen herkenbaar oorspronkelijk formaat omzetten; datum en voedingswaarden behouden. Moderne maaltijden zonder items blijven leeg. Geen backfill naar al gemigreerde databases. |
| P2 | Terminale Gemini HTTP-fouten verloren hun veilige categorie en werden door de maaltijdservice verborgen als algemene lokale fallback. | Veilige getypeerde fout doorgeven; HTTP 400/401/403 via echte client/router/service getest. |
| P2 | Na tijdelijke uitval van de eerste provider werd de terminale fout van de tweede provider niet getoond. | Veilige terminale oorzaak heeft voorrang in de gebruikersmelding; private invoer wordt niet getoond. |
| P2 | De gedeelde Health Connect-kaart noemde een controle, ook na een mislukte sync, een geslaagde synchronisatie met ruwe epochwaarde. Regressie door gewijzigde betekenis van de timestamp in de vorige PR. | Zelfde geformatteerde controletijd als Instellingen; ontbreken van tijdstip blijft zonder claim. |
| Advies | De bestaande algemene-gezondheidsdoelregel kan bij een hoger vetpercentage een energietekort kiezen. | Productkeuze vereist verduidelijking; geen wijziging binnen deze reparatie. |

## Gemeld AI-incident: bewijsgrens

De gebruiker ziet alleen een algemene mislukking. Provider, HTTP-status en
concrete invoer zijn niet vastgesteld. De twee foutafhandelingsdefecten zijn
reproduceerbaar, maar bewijzen niet waarom de externe aanvraag van de gebruiker
mislukte. Een Android-contracttest met echte JPEG-voorbereiding, Retrofit/Gson,
productieclient, router, maaltijdparser en portiecontext slaagde voor beide
providers ook vóór de wijzigingen. Alleen het HTTP-transport wordt daarbij
gesimuleerd. Dit is geen live-providerproef.

De gerichte aanvullende inspectie vond geen andere bewezen fout in foto-eigendom,
contextoverdracht, annulering, compressie of resultaatverwerking. Het bestaande
20-secondenbudget omvat bij OpenAI ook modeldetectie. Zonder incidentbewijs
wordt dit budget niet verruimd en worden modellen niet stilzwijgend vervangen.

De actuele [Gemini-afbouwinformatie](https://aistudio.google.com/docs/deprecations)
noemt beperkte bestaande toegang tot Gemini 2.5 Flash en een overgang per
1 december 2026; dit bewijst geen volledige uitschakeling op de onderzoeksdatum.
De [OpenAI-modeldocumentatie voor de huidige voorkeurskandidaat](https://developers.openai.com/api/docs/models/gpt-6-luna)
bevestigt beeldinvoer, Responses, structured outputs en `medium` reasoning.
Ook de [bestaande fallbackkandidaat](https://developers.openai.com/api/docs/models/gpt-5.6-luna)
ondersteunt deze mogelijkheden. Projecttoegang blijft afhankelijk van de gebruiker.

## Verificatie

Vóór productieaanpassingen: routertests 30 uitgevoerd, vijf verwachte failures;
Android-grensproeven 18 uitgevoerd, vier verwachte failures. De overige proeven,
waaronder beide fototransportcontracten en moderne maaltijdgrensgevallen,
slaagden. Logs staan lokaal onder `artifacts/regression-repair-20261112/`.

Definitieve lokale controles op de gezamenlijke wijziging:

| Controle vanuit `TrainIQ-Project/` | Resultaat |
|---|---|
| `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --console=plain --max-workers=2` | BUILD SUCCESSFUL; 1.116 unittests, nul failures/errors/skips. Lint nul fouten, 27 waarschuwingen. |
| `:app:connectedDebugAndroidTest --offline --console=plain --max-workers=2` | BUILD SUCCESSFUL; volledige set van 278 tests, nul failures/skips, API36 agentemulator `emulator-5580`. |
| Onafhankelijke integriteitsreview | GPT-6 Astra (`gpt-6-astra`, `high`): geen actiegerichte bevindingen in volledige bron- en testdiff. |
| Diffcontrole | `git diff --check` geslaagd. |

Risico's zijn op de laagste betrouwbare laag afgedekt: providerclassificatie en
privacy met router/service-unittests; media en wireformat met Android-contracten;
Room-identiteit, import, herstel en migraties met echte databaseproeven;
controletijd met Compose-semantiek. De volledige Android-set controleert daarnaast
bestaande maaltijdroutes, camera/herstel, workouts, navigatie en slaapfuncties.
Logs: `baseline-green.log` en `connected-green.log` in de lokale artifactmap.

Geen regressies gevonden in deze geteste scenario's. Dit is geen garantie voor
alle apparaten, invoer of externe provideromstandigheden. Er zijn geen live
AI-aanvragen, echte gebruikersfoto's of fysieke prestatiemetingen uitgevoerd.
De oorspronkelijke gebruikersstoring blijft onbewezen; de nieuwe foutmelding
maakt een eventuele blijvende providerweigering gericht onderzoekbaar.

De update krijgt versie `1.0.4-B`, code 6, met de bestaande debug-identiteit.
APK-provenance, exacte hashes, schone main-build, daadwerkelijke marker, updateproef,
Drive- en e-mailuitkomst horen bij het lokale leveringsbewijs; deze worden niet
uit bestandsnamen of testresultaten van een andere bron afgeleid.
