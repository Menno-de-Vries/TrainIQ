# AGENTS.md

## Projectdoel
Deze Android app moet continu verbeterd worden op:
- gebruiksgemak
- design en visuele consistentie
- performance
- stabiliteit
- toegankelijkheid
- foutafhandeling
- verwijderen van glitches, design flaws en onbedoelde werking
- betere flow voor normale gebruikers

## Belangrijkste regel
Werk altijd in kleine, veilige iteraties. Verander niet te veel tegelijk.

## Werkwijze per taak
Volg altijd deze volgorde:

1. Onderzoek de bestaande app en code.
2. Zoek naar concrete problemen in UX, UI, bugs, glitches, onbedoelde functies of slechte flows.
3. Maak eerst een kort plan.
4. Voer maximaal 1 tot 3 gerichte verbeteringen tegelijk uit.
5. Draai relevante tests.
6. Bouw de Android app als dat mogelijk is.
7. Controleer of bestaande functionaliteit niet kapot is gegaan.
8. Geef aan:
   - wat aangepast is
   - waarom dit beter is
   - welke bestanden gewijzigd zijn
   - welke tests/build checks zijn uitgevoerd
   - welke risico’s of vervolgstappen er nog zijn

## Niet doen
- Geen grote redesigns zonder duidelijke reden.
- Geen bestaande functies verwijderen zonder onderbouwing.
- Geen onnodige dependencies toevoegen.
- Geen code herschrijven puur om het herschrijven.
- Geen UI mooier maken ten koste van bruikbaarheid.
- Geen breaking changes zonder uitleg.

## Android kwaliteitseisen
Controleer waar mogelijk:
- app start correct op
- schermen laden zonder crash
- navigatie werkt logisch
- knoppen hebben duidelijke labels
- foutmeldingen zijn begrijpelijk
- loading states zijn aanwezig waar nodig
- lege states zijn netjes afgehandeld
- inputvalidatie werkt
- donkere/lichtmodus breekt de UI niet
- layout werkt op meerdere schermgroottes
- geen rare overlap, clipping of onleesbare tekst

## Testregels
Gebruik waar mogelijk:
- bestaande unit tests
- bestaande UI tests
- Android build/test commands
- lint checks
- handmatige testscenario’s als automatische tests ontbreken

## Outputstijl
Geef altijd een duidelijk eindrapport in het Nederlands.