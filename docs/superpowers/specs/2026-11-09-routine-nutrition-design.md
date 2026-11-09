# Betrouwbare routines en eenvoudiger voeding

Datum: 2026-11-09. Opdracht: autonoom onderzoeken, verbeteren, onafhankelijk reviewen, bevindingen herstellen, alles gecontroleerd naar main brengen en één update bezorgen via de bestaande TrainIQ Drive-/emailroute.

## Doel en afbakening

Routinegeneratie moet nieuwe én bestaande oefeningen kunnen voorstellen, fouten op de plek van de handeling tonen en invoer behouden bij herstel. Voeding moet met minder tussenstappen bereikbaar zijn en maaltijdconcepten, bewerkingen en hergebruik uit elkaar houden. Drinken moet snel toe te voegen blijven, met vrije invoer en correctie.

De bestaande Material 3-app, Room-opslag, providerkeuze, modellen, toestemming en handmatige/offline routes blijven uitgangspunt. Geen nieuwe diensten, afhankelijkheden, schemawijziging, medische regels of brede herbouw. Geen echte AI-verzoeken met gebruikersgegevens als test. Bestaande lokale werkzaamheden en gegevens behouden.

Aanvulling van de gebruiker tijdens uitvoering: ook AI-voedsel toevoegen werkte onvoldoende. Onderzoek daarom de volledige bestaande foto/import → analyse → portiecorrectie → opslaan-keten en herstel concrete bewezen gebreken, met behoud van fotocontext, bewerkbare schattingen en de expliciete opslagcontrole. Afgebroken of onvolledige analyses mogen geen stilzwijgend onjuiste maaltijd opslaan; fouten moeten herstelbaar zichtbaar zijn. Een aparte onderzoeksagent bepaalt de kleinste gerichte fixbatch, aanvullend op de gedeelde schema-reparatie.

## Onderzoek en gekozen aanpak

- De OpenAI-strictconversie maakt optionele velden verplicht zonder null toe te staan. Daardoor kan een nieuwe oefening geen geldige bibliotheekreferentie krijgen. Herstel de semantiek van optionele velden op de kleinste geschikte grens en bewijs de transitive contracten. Een verzonnen ID negeren zou ongeldige output verhullen; een volledig dubbel routineschema geeft onnodige onderhoudslast.
- Routinefouten verschijnen achter de modal. Voeg een eigen zichtbare foutstatus met herstel toe; gebruik de bestaande adaptieve sheetprimitieven met scrollbare inhoud en bereikbare acties. Behoud opties en preview na relevante fouten.
- Maaltijdhergebruik kan een bestaande bewerkingsidentiteit behouden; doelwisselen kan concepten vermengen. Maak het verlaten/vervangen van een gevuld concept expliciet, met een veilige behoud-/annuleerkeuze. Nieuwe en hergebruikte maaltijden krijgen een nieuwe identiteit; bestaande bewerkingen behouden hun identiteit.
- Maaltijdopslaan forceert middernacht. Behoud een bestaand tijdstip bij dezelfde datum; gebruik nu voor een nieuwe maaltijd vandaag en een expliciete, consistente regel voor een andere datum.
- Toon opgeslagen producten direct doorzoekbaar in het maaltijd-toevoegscherm. Selectie opent hetzelfde controleerbare concept met standaardportie; handmatig, barcode, recept en foto blijven bereikbaar. Dit vermindert tussenstappen zonder direct stilzwijgend op te slaan.
- Voeg snelle, duidelijk benoemde ml-keuzes toe aan vocht. Gebruik dezelfde bestaande write-guard en opslag, met zichtbare fout en herhaalbare poging. Houd vrije invoer, correctie en verwijdering beschikbaar.

Een volledige voedingswizard of brede navigatieherbouw is afgewezen: de bestaande gerichte opslag en conceptcontrole kunnen de opdracht met minder regressierisico vervullen.

## Toetsbare succescriteria

1. OpenAI kan met een lege bibliotheek een nieuwe oefening met null-referentie opleveren; juiste bestaande IDs blijven geldig en onjuiste referenties worden afgewezen.
2. Beide AI-providers behouden hun ondersteunde JSON-contract; afgebroken, geblokkeerde en ongeldige routineoutput krijgt een passende veilige status. Cancellation blijft doorgegeven.
3. Generatiefout en opnieuw proberen staan in het open generatievenster; invoer blijft behouden, dubbele acties schrijven/genereren niet dubbel.
4. Maaltijd bewerken, een ander doel kiezen en geschiedenis hergebruiken kunnen geen andere maaltijd overschrijven of onzichtbaar concepten vermengen. Afbreken van de wissel behoudt het oorspronkelijke concept.
5. Opslagfouten behouden invoer en identiteit; geslaagde opslag reset uitsluitend het afgehandelde concept. Tijdstippen blijven correct bij bewerken en nieuwe invoer.
6. Een opgeslagen product is direct te zoeken en te selecteren vanuit maaltijd toevoegen, gevolgd door controle van hoeveelheid en opslaan. Lege bibliotheek en geen zoekresultaat bieden een bruikbare vervolgstap.
7. Snelle vochtinvoer en vrije invoer registreren de juiste hoeveelheid éénmaal; fouten kunnen worden hersteld en calorieën veranderen niet.
8. Gewijzigde flows blijven leesbaar/bedienbaar op compacte telefoon met groot lettertype, toetsenbord en donker thema, en op een bredere viewport. Semantics en runtimebeelden vullen elkaar aan.
9. Lokale build, JVM, lint en betrokken Android-/Room-grenzen zijn bewezen. Een onafhankelijke volledige review vindt geen open blokkerende taakdefecten vóór main/bezorging.
10. De bezorgde APK komt uit een schone vastgepinde remote-main-revisie, heeft package com.trainiq, een hogere versiecode en exact de bestaande debughandtekening van de APK die de gebruiker op 22 oktober ontving. Een update-installatie behoudt synthetische gegevens. ZIP/Drive-link/email worden eenmaal gecontroleerd bezorgd.
11. AI-voeding heeft contractbewijs voor geldige/ongeldige analyse, gram-/vochteenheden, gedeeltelijke of afgebroken resultaten en fout/herstel; een gerichte UI-/opslagflow bewijst dat de gecontroleerde hoeveelheden en correcties juist worden opgeslagen, zonder dubbele invoer.

## Bevoegdheid en bezorgvariant

De gebruiker bevestigde tijdens dit werk dat de debug-APK van 22 oktober geïnstalleerd is en vroeg expliciet alles goed naar main te brengen en daarna de bezorgprocedure uit te voeren. Dit autoriseert de benodigde push/PR/merge, lokale bouw en één Drive-/emailbezorging. De release-only standaardvariant van de bezorgskill wordt voor deze run vervangen door de bestaande debugvariant om een update zonder verwijderen mogelijk te maken; noem deze nooit een release-APK. Geen nieuwe sleutel, sleutelrotatie of uninstall. De overige regels voor bronvastlegging, schone detached build, ledger, privétoegang en duplicaatpreventie blijven gelden.

## Bronnen

- Bestaande repositorycode, AGENTS.md, lokale testgids, ADR-001/003/004, blueprint en itch.io-beleid.
- OpenAI, Structured Outputs: optionele waarden als verplichte nullable velden: https://developers.openai.com/api/docs/guides/structured-outputs
- Google, structured output en finish reasons: https://ai.google.dev/gemini-api/docs/structured-output en https://ai.google.dev/api/generate-content
- Android, updatevoorwaarden: https://developer.android.com/google/play/app-updates

## Beperkingen

De exacte fout van de eerdere gebruikerspoging is onbekend. De gevonden contractfout is structureel bewezen, maar niet als oorzaak van die specifieke poging vastgesteld. Emulatorbewijs is geen fysieke prestatie- of menselijke toegankelijkheidscertificering. Live AI en fysieke telefooncontrole worden alleen geclaimd wanneer daadwerkelijk uitgevoerd.
