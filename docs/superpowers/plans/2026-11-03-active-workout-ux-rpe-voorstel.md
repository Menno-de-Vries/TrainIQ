# Actieve training: UI/UX en betrouwbare inspanningsfeedback — uitvoeringsvoorstel

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Dit is een voorstel voor bespreking; er is nog geen visuele richting gekozen en appcode is niet gewijzigd.

**Goal:** De bestaande actieve training herkenbaar behouden, bediening en witruimte verbeteren en feedback aantoonbaar baseren op werkelijk uitgevoerde sets.

**Architecture:** Compose leest ViewModel-state; één zuivere domeinberekening levert de feiten voor scherm, lokale samenvatting en AI-invoer. Room blijft leidend. Bestaande transacties, generatiebeveiliging en asynchrone debrief blijven behouden.

**Tech Stack:** Bestaande Kotlin, Compose Material 3, StateFlow, Room, WorkManager en Gemini 2.5 Flash; geen nieuwe afhankelijkheden of diensten voorzien.

**Spec:** Het ingebedde ontwerpvoorstel hieronder, de gebruikersvraag van 3 november 2026, de relevante Training/UI/AI-eisen uit `TrainIQ_Target_State_Blueprint.md` en ADR-001/003/004.

**Onderzocht:** `cb13ccd2fd1abff4e19821d36e1c63f95ea6abae`, branch `codex/active-workout-rpe-food`, schone werkboom bij aanvang. Drie subagents: UI en QA met aangevraagde `gpt-6.1-terra` / `high`; RPE-dataketen met `gpt-6.1-sol` / `high`. Model en effort zijn expliciet aan de ondersteunde startinterface doorgegeven. Alleen onderzoek en beeldgeneratie uitgevoerd.

## Doel, scope en grenzen

- Oplevering van deze ronde: onderbouwde bevindingen, drie Image Gen-voorstellen en dit uitvoerbare plan.
- Latere uitvoering: actieve setkaarten, knoppen, tekstuitlijning, ruimte, rust/afrondbediening en de feitelijke set → samenvatting → feedback-keten.
- Bestaande stijl, settypen, supersets, notities, undo, corrigeren/verwijderen/opnieuw loggen, navigatie, herstel en afrondbevestiging behouden.
- Geen voedingswerk, algemene herbouw van `WorkoutScreen.kt`, nieuwe trainingsmodus, providers, permissions, dependencies, release of remote handelingen.
- Optionele RPE-invoer is een voorgestelde productkeuze, geen bestaande functie of al gekozen eis. De drie beeldvoorstellen behouden de huidige drie invoervelden.

## Vastgestelde bevindingen

### Layout: code plus opgeslagen beelden

1. `features/workout/WorkoutScreen.kt:6938`: setinhoud heeft 8 dp horizontale en 4 dp verticale binnenruimte, met 4 dp tussen onderdelen. Dat verklaart weinig visuele ademruimte rond kop, context, velden en logknop.
2. `WorkoutScreen.kt:6685` en `:6793`: herhalingen/kg/rust hebben al gecentreerde waarden en minimaal 56 dp hoogte. Een generiek `TextAlign.Center` toevoegen lost dus niet het hele probleem op; groepsuitlijning, consistente celbreedte en omliggende ruimte zijn de aandachtspunten.
3. `WorkoutScreen.kt:5943`: footer groepeert ruststatus, vier rustacties en een volle afrondknop met slechts 4 dp verticale tussenruimte. Bij smalle breedte/grote tekst stapelt deze. In opgeslagen `food-workout-20261022/dark-final-ime.png` nemen toetsenbord en footer samen een groot deel van het scherm in, vlak onder de setlogknop.
4. `WorkoutScreen.kt:6214`: oefeningkop, rustinstelling, menu en context concurreren om ruimte. Voltooide setacties krijgen op smalle schermen een extra rij. Deze responsieve beveiliging moet blijven werken.

De beelden zijn historische lokale opnamen, geen nieuwe runtime-audit. De code is op bovengenoemde HEAD onderzocht. Live inspectie lukte niet: de aanwezige `adb.exe` kon zijn daemon niet starten. Er is geen toestel bediend, emulator ingericht of app geïnstalleerd.

### RPE: ontbrekende bron en generieke feedback

| Onderdeel | Huidig gedrag | Betekenis voor dit voorstel |
|---|---|---|
| Nieuwe set, `WorkoutScreen.kt:884` | `rpe = correctionSet?.rpe ?: 0.0`; nieuwe RIR is onbekend | Nieuwe prestaties leveren geen gerapporteerde RPE op. Dit is bestaand productgedrag, geen bewezen opslagfout. |
| Draft, `WorkoutScreen.kt:7924` | Commit maakt `rpe` leeg | Eventuele nieuwe invoer moet de hele keten aanpassen; alleen een veld tekenen is onvoldoende. |
| `domain/model/WorkoutEffortSummary.kt:4` | Ongewogen gemiddelde van geldige RPE 1–10 op voltooide sets; onbekend wordt `null` | Deze berekening werkt al. Niet vervangen door een verzonnen RPE uit kg/reps. |
| Werkelijke AI-call, `data/repository/TrainIqRepository.kt:636` en `:712` | Opgeslagen sets, setdetails en nullable RPE per oefening gaan mee | AI mist de uitgevoerde sets niet volledig; oorzaak nauwkeurig houden. |
| Lokale debrief, `ai/services/AiServices.kt:1171` | Vooral vaste tekst en `MAINTAIN`; actuele RPE is geen invoer | Dit is een concrete verbeterplek: feiten en datadekking moeten lokale feedback sturen. |
| Progressie, `WorkoutProgressionSuggestionCalculator.kt:109` | Zonder RPE/RIR conservatief `MAINTAIN` | Toon waarom het advies behouden blijft; onbekend is geen bewijs van hoge inspanning. |
| Selectie, `TrainIqRepository.kt:649` | Vergelijkingsvolume kiest NORMAL/BACK_OFF als aanwezig; UI telt alle voltooide typen | Duidelijke labels en één gedeelde selectie nodig; niet stilzwijgend betekenissen wijzigen. |
| Verdeling, `TrainIqRepository.kt:681` | Spiergroepverdeling komt uit het plan | Baseer claims over uitgevoerde training op werkelijk uitgevoerde oefeningen. |
| Late debrief, `WorkoutCompletionLoader.kt:38` | Slechts zes herlezingen met twee seconden interval | Een later opgeslagen AI-resultaat wordt niet vanzelf zichtbaar op een open scherm. |

Geen bewezen fout gevonden waarbij een oude AI-response een vervangende sessie overschrijft; generation-ID-controles bestaan en blijven behouden. Normaal loggen maakt sets voltooid; onvoltooide historische/import-rows zijn een apart randgeval voor de refreshberekening.

## Ontwerpvoorstel en open keuzes

### Drie visuele richtingen

Beelden staan in de volgorde waarin ze in de chat zijn getoond. Alle drie zijn afzonderlijk met de ingebouwde Image Gen gemaakt, met twee werkelijk bekeken appopnamen als beeldreferentie. Promptdoel: een mobiel viewport van 390 × 844; geen exacte dp-meting of runtimebewijs.

| Beeld | Richting | Afweging |
|---|---|---|
| 1 | Vertrouwde verfijning | Bestaande samenvatting en kaarten zeer herkenbaar; minder ruimtewinst boven de actieve set. |
| 2 | Duidelijke setfocus — aanbeveling | Compactere sessiekop maakt ruimte voor huidige set; rustigste hiërarchie. Terug en weggooien blijven bereikbaar; verplaatsen van weggooien naar menu is een expliciet voorstel. |
| 3 | Ruime bediening | Bestaande kaartopbouw met afzonderlijke rustknoppen; duidelijke groepering, iets meer verticale ruimte nodig. |

Lokale bestanden, buiten Git, in `C:/Users/menno/.codex/visualizations/2026/11/03/01a1a335-0618-7ac1-a457-8ddd926e73ec/`: `01-vertrouwde-verfijning.png`, `02-duidelijke-setfocus.png`, `03-ruime-bediening.png`. De originelen blijven ook op de door Image Gen gemelde locatie staan.

De afbeeldingen zijn sfeer- en indelingsvoorstellen. Beeld 1 heeft ongevraagd een decoratief stapeltje halterschijven; dat wordt niet overgenomen. In beeld 2/3 staan enkele bestaande acties achter menu's; hun uiteindelijke vindbaarheid moet expliciet worden gecontroleerd. Geen functie vervalt omdat ze niet in dit ene frame staat. Een afbeelding kan geen exacte touch-targets, focus of grootlettergedrag bewijzen.

### Maatvoering en uitlijning

- Uitgangspunt: 16 dp buitenmarge en 16 dp binnenruimte voor de oefening; 12 dp tussen verwante onderdelen en 20–24 dp tussen hoofdgroepen. Gebruik bestaande spacingtokens; geen globale themaverandering.
- `Set loggen`: minimaal 56 dp hoog, volle beschikbare breedte, pictogram en label samen gecentreerd; genoeg verticale ruimte boven en onder.
- Invoercellen: gelijke breedte, minimaal 56 dp hoog, eventueel 64 dp waar dat past; label en waarde vormen één gecentreerde groep. Hoofdstukken en lopende tekst blijven links uitgelijnd.
- Iconen mogen visueel 20–24 dp blijven; elk aanraakvlak minimaal 48 × 48 dp. Tussen afzonderlijke bedieningselementen bij voorkeur 8 dp vrije ruimte. Geen overlappende onzichtbare touch targets.
- Ruststatus en bediening logisch groeperen; hoofdactie en afronden niet even zwaar tonen. Voorgesteld: gevulde logknop en omlijnde afrondknop, beide duidelijk bereikbaar en met bestaande bevestiging.
- Bij grote tekst/smalle beschikbare breedte naar meerdere regels of een 2+1 invoerindeling; geen vaste hoogte die tekst afsnijdt. Footer en inhoud reageren op IME en systeeminsets. Minder bovenruimte winnen vóór touch targets verkleinen.
- Foutmelding staat bij het relevante veld, krijgt ruimte en duwt inhoud door; een snackbar mag geen noodzakelijke vervolghandeling permanent blokkeren.

48 dp is de Android-richtlijn; 56/64 dp en het voorgestelde witruimteritme zijn ontwerpkeuzes voor TrainIQ, geen universele platformeis.

### Berekening en bron van inspanning

**Aanbevolen kern:** bereken objectieve prestaties direct uit actuele voltooide sets, maak onbekende RPE zichtbaar en gebruik dezelfde feiten lokaal en in de AI-call. Exacte ervaren inspanning volgt niet betrouwbaar uit uitsluitend kg en herhalingen.

**Optionele uitbreiding:** een ingeklapte actie `Inspanning toevoegen` per set met vrijwillige RPE 1–10, ook achteraf corrigeerbaar. Geen verplichte extra stap en geen vooraf ingevulde plan-RPE. Leeg blijft onbekend. Opnieuw loggen/kopiëren neemt gewicht/reps/rust over, maar presenteert inspanning van een vorige set niet als nieuwe zelfrapportage. Correctie van dezelfde historische set bewaart de waarde tot die expliciet wordt gewijzigd of gewist.

**Alternatief zonder extra invoer:** toon volume, herhalingen en vergelijkbare prestaties met `RPE niet ingevuld`. Geef alleen onderbouwde conservatieve feedback. Geen automatische numerieke RPE toevoegen onder het label gerapporteerde inspanning. Een apart geschat belastingsmodel vergt eigen validatie en valt buiten deze eerste verbetering.

Eén voorgestelde domeininterface:

`buildWorkoutAnalysisSnapshot(sets: List<LoggedSet>): WorkoutAnalysisSnapshot`

De immutable snapshot bevat `completedSets`, `completedSetCount`, `totalReps`, `totalVolumeKg`, `comparisonVolumeKg`, `comparisonSetCount`, `reportedMeanRpe: Double?` en `reportedRpeCount`; `completedSetCount` is tevens de dekkingnoemer. De per-oefening-details hergebruiken `WorkoutEffortSummary.exercises`. Deze namen zijn voorgestelde nieuwe interfaces, geen bestaande API.

Dit is uitsluitend een snapshot van setfeiten. De bestaande repository-adapter levert daarnaast de exercise-ID → naam/spiergroep-metadata en historische sessies voor vergelijking. Ontbrekende of verwijderde metadata krijgt een neutraal oefeninglabel en een onbekende spiergroep; geen groep afleiden uit de naam. Historische vergelijkbaarheid blijft volgens de bestaande criteria bepaald.

- Voltooide sets eerst selecteren; een verwijderde of onvoltooide set komt niet in tellingen, volume, topsets of inspanningsclaims.
- `totalVolumeKg = Σ(gewicht × uitgevoerde reps)` voor alle voltooide sets. Bodyweight-registraties blijven volgens huidig gewichtmodel; nul externe kg betekent niet nul inspanning.
- `comparisonVolumeKg` behoudt de huidige NORMAL/BACK_OFF-selectie en bestaande fallback als die typen ontbreken; het scherm noemt dit zo nodig `Vergelijkingsvolume`, niet ongemerkt al het werkvolume. Dropsets/failure blijven in de feitelijke setlijst en het totaalvolume staan.
- Pas dezelfde voltooid-/settype-/fallbackselectie toe op zowel de huidige als de historische vergelijkingsbasis, met behoud van de bestaande vergelijkbaarheidscriteria. Alleen typen in daadwerkelijk voltooide sets mogen de fallbackselectie bepalen.
- `reportedMeanRpe` behoudt het bestaande ongewogen gemiddelde over alle voltooide settypen met geldige RPE. Geen nul of planwaarde als vervanging; afronding uitsluitend voor presentatie. Label `Gemiddelde set-RPE`, niet een gemeten sessie-RPE.
- Toon datadekking, bijvoorbeeld `RPE 7,5 · 2 van 3 sets ingevuld`. Geen stellige intensiteitsclaim over de onbekende derde set.
- Eventuele afzonderlijke werkset-RPE is een latere expliciete definitiekeuze; niet nodig voor deze kernfix.

Rekenvoorbeeld voor acceptatie: warming-up 20 × 10/RPE 3, normale set 60 × 8/RPE 8, normale set 60 × 8/onbekend → totaalvolume 1.160 kg, vergelijkingsvolume 960 kg, gemiddelde set-RPE 5,5, dekking 2/3. Worden dezelfde sets zonder inspanning ingevoerd, blijven volumes gelijk en is RPE onbekend. Alleen geldige RPE 6 en 9 plus een onbekende en een onvoltooide RPE 10 → gemiddelde 7,5.

## Global Constraints

- Architectuur, Room-authoriteit, gerichte transacties, nullable AI-invoer en generation-fences behouden.
- Schema-backed JSON, huidige Gemini 2.5 Flash en bestaande opt-in/timeout/retry/privacygrenzen behouden; geen live betaalde AI-call nodig voor testen.
- Bestaande nulwaarde voor onbekende RPE in opslag handhaven; in analyse en AI `null`. Geen schemawijziging voorzien; indien alsnog nodig eerst migratie-impact opnieuw bepalen.
- Alleen lokale tests/evidence; geen CI, upload, push, PR, release, signing, nieuwe emulator-download of toestelpermission-wijziging.
- Ontwerpbeelden en testuitvoer blijven ongetrackt. Alleen gerichte bron-/test-/documentpaden committen bij latere uitvoering.

## Review Focus

- Correctie, verwijderen, undo en opnieuw loggen beïnvloeden zowel de zichtbare feiten als het uiteindelijke fake AI-request; oude waarden mogen niet terugkeren.
- Grote tekst plus IME en actieve rust mag de logknop of afrondactie niet onbereikbaar maken; semantiek en geometrie controleren.
- Ontbrekende of deels ingevulde RPE, warm-up, drop/failure en nul externe kg mogen geen gefabriceerde inspanning of misleidend volume geven.
- Een AI-resultaat op seconde 15 moet zichtbaar worden als het scherm nog open is; oude sessiegeneraties mogen nooit overschrijven.
- Herstel na echte processtop is iets anders dan Activity.recreate; voer alleen de relevante herstelproef uit op een geïsoleerde veilige emulator.

## Taak 1 — Eén feitelijke analyse van de uitgevoerde sets

**Paden:** nieuw `TrainIQ-Project/app/src/main/java/com/trainiq/domain/model/WorkoutAnalysisSnapshot.kt`; bestaande `domain/model/WorkoutEffortSummary.kt`, `data/repository/TrainIqRepository.kt`; nieuwe spiegelende `WorkoutAnalysisSnapshotTest.kt` en bestaande `WorkoutEffortSummaryTest.kt`, `WorkoutProgressComparisonTest.kt`, `WorkoutEffortRequestTest.kt`.

**Produceert:** bovenstaande pure snapshotinterface voor taak 2/3; bestaande per-oefening-effort blijft herbruikbaar.

- [ ] Leg eerst fixtures vast voor het rekenvoorbeeld, uitsluitend onbekende RPE, incomplete/gemengde settypen, nul externe kg en geen sets. Test ook historische onvoltooide rows, historie zonder NORMAL/BACK_OFF en ontbrekende oefenmetadata. Assert de exacte waarden, dezelfde selectiebasis aan beide kanten en behoud van setdetails.
- [ ] Voer de nieuwe gerichte JVM-tests uit en bevestig de bedoelde ontbrekende/onjuiste contracten voordat de implementatie wijzigt.
- [ ] Bouw de snapshot en gebruik die in afronden, refresh en relevante schermtotalen. Bereken spiergroepclaims uit de uitgevoerde exercise-ID's; geen niet-uitgevoerde planonderdelen als prestatie presenteren.
- [ ] Breid fake-requestdekking uit: corrigeer een set, verwijder een set, rond af en controleer de daadwerkelijk geserialiseerde request op de overblijvende waarden en expliciete nulls. Gebruik bestaande repository-fixtures.
- [ ] Controleer unit- en repositorycontracten; maak een gericht lokaal commit na review van de staged diff.

## Taak 2 — Lokale feedback die de gegevens verklaart

**Paden:** `ai/services/AiServices.kt`, `ai/prompts/AiPrompts.kt`, `data/repository/TrainIqRepository.kt`, bestaande completion-/requesttests; nieuw spiegelend `ai/services/WorkoutLocalDebriefTest.kt` indien bestaande tests dit contract niet dragen.

**Consumeert:** `WorkoutAnalysisSnapshot`; bestaande vergelijkingsbasis/historie en foutcontext. **Produceert:** lokale `WorkoutDebrief` plus AI-request met dezelfde feiten en gelabelde selectiebasis.

- [ ] Leg tests vast voor onbekende RPE, gedeeltelijke dekking, bekende hoge/lage RPE en geen vergelijkbare historie. Assert feiten en reden voor terughoudend advies, geen onbewezen verhoog-/herstelclaims.
- [ ] Laat de lokale samenvatting set-/reptellingen, werkelijk volume en RPE-dekking gebruiken. Verwijder de vaste RPE<8-tekst als automatische verklaring van deze training; onbekende inspanning duidelijk benoemen.
- [ ] Houd de bestaande conservatieve progressieregel intact waar gegevens ontbreken, maar maak de reden zichtbaar. Geen nieuwe universele trainingsdrempels uitvinden.
- [ ] Houd notities buiten de AI-invoer; behoud schema, budget, opt-in, cancellation en fallback. Test aan fake transportgrens, niet met live accountgegevens.
- [ ] Draai gerichte fallback/request-/progressietests en commit uitsluitend deze afgeronde taak.

## Taak 3 — Betrouwbaar verversen van de debrief

**Paden:** `features/workout/WorkoutCompletionLoader.kt`, completion-aanroepen in `WorkoutScreen.kt`, `domain/repository/Repositories.kt`, `data/repository/TrainIqRepository.kt`; alleen indien nodig een gerichte DAO-observatie in `core/database/TrainIqDao.kt` en runtime-store. Tests: `WorkoutCompletionLoaderTest.kt`, `WorkoutCompletionRecoveryInstrumentedTest.kt`, bestaande generatie-/identiteitstests.

**Interfacevoorstel:** `observeWorkoutCompletionSummary(sessionId: Long): Flow<WorkoutCompletionSummary?>`; collectie alleen tijdens relevante schermlifecycle. Geen onbegrensd pollwerk of hele-app-state-mirror op de hot path.

- [ ] Fake klok/Flow-test: eerst lokale samenvatting, AI pas op seconde 15, daarna tweede sessie. Assert zichtbaar nieuw resultaat en geen oud resultaat op nieuwe sessie.
- [ ] Vervang de beperkte polling door gerichte observatie en behoud cancellation/revision-/generation-beveiliging. Een geforceerde reload mag opgeslagen training niet dupliceren.
- [ ] Maak lokaal resultaat en eventuele bestaande fout-/retrycontext begrijpelijk. Claim `wacht op verbinding` of `bezig` alleen met echte workerstatus; nieuwe persistente jobstatus valt buiten dit voorstel tenzij implementatie die noodzakelijk maakt.
- [ ] Behoud de bestaande automatische terugkeerpolicy; de seconde-15-proef houdt het scherm volgens de bestaande interactiepolicy open. Veranderen van de terugkeerpolicy is een afzonderlijke ontwerpkeuze.
- [ ] Verifieer offline/timeout, retry, delete en late oude generatie; commit de afgeronde wijziging.

## Taak 4 — Gekozen visuele verfijning uitvoeren

**Paden:** gerichte functies `ActiveWorkoutSessionSummary`, `ActiveWorkoutPlanCard`, `SetRow`, `ActiveSetInputMetrics` en `ActiveWorkoutBottomBar` in `features/workout/WorkoutScreen.kt`; bestaande previews en `ActiveWorkoutSetActionsInstrumentedTest.kt`.

**Consumeert:** gekozen beeld plus bovenstaande maatvoering. **Produceert:** dezelfde werkende trainingsflow met rustiger hiërarchie en passende groottes.

- [ ] Maak representatieve previews/fixtures voor current, completed, fout, rust actief/inactief en lange naam. Eerst actuele nulmeting op veilige emulator zodra ADB werkt.
- [ ] Pas gericht de binnenruimte, knop-/veldgeometrie en groepsuitlijning aan. Verplaats bediening alleen volgens de gekozen richting; behoud discoverability en bestaande bevestigingen.
- [ ] Leg op semantiek/geometrie vast: minimaal 48 dp targets, geen overlap, gelijke invoerveldhoogtes waar op één rij, bereikbare log-/afrondactie en correcte IME-focus. Test geen broncodevorm of exacte decoratieve pixels.
- [ ] Inspecteer 360 × 640 en 360 × 800, font 1,3/1,5, light/dark met actuele set, rust en IME. Kies alleen combinaties met uniek risico; voeg tablet/landscape toe als gewijzigde layout daarop doorwerkt. Bekijk scherm én UI-tree; TalkBack/Switch Access apart vastleggen.
- [ ] Verifieer notes, settype, supersets, correctie/verwijderen/relog, undo, achtergrond/terugkeer, herstel en eenmaal afronden voor de werkelijk geraakte grenzen. Commit alleen de complete visuele wijziging.

## Taak 5 — Optionele RPE-invoer, uitsluitend als gekozen

**Paden:** `SetInputDraft`/`commitActiveSetDraft` en set-log-/correctieroute in `WorkoutScreen.kt`; bestaande opgeslagen draft/RPE-velden en mappers controleren; `WorkoutInputValidationTest.kt`, `ActiveWorkoutSessionMutationsTest.kt`, `ActiveWorkoutSetActionsInstrumentedTest.kt`, `WorkoutEffortRequestTest.kt`.

**Consumeert:** gebruikerskeuze voor optionele invoer en snapshot uit taak 1. **Produceert:** vrijwillige, herleidbare gerapporteerde RPE per werkelijk uitgevoerde set.

- [ ] Tests eerst: leeg → onbekend, 1/10 geldig, buiten bereik/NaN/oneindig ongeldig, Nederlandse decimale invoer, wissen, correctie, kopiëren/relog zonder automatische nieuwe zelfrapportage en herstel van een vrijwillige draft.
- [ ] Voeg ingeklapte toegankelijke invoer toe zonder logging verplicht te onderbreken. De gekozen waarde door commit/validatie/logging/opslag naar analyse laten lopen; plan-RPE nooit als invoer bevestigen.
- [ ] Vervang bewust de bestaande test die afwezige RPE-invoer als productcontract eist; behoud de geldige assertions voor historische correcties, snelle registratie en onbekende RPE.
- [ ] Bewijs log → correctie/verwijderen → finish → snapshot/fake AI-request en echte herstelgrens. Geen databasewijziging uitsluitend om dezelfde aanwezige RPE opnieuw op te slaan.
- [ ] Draai de geraakte tests en UI-controles; commit deze productuitbreiding apart. Als deze keuze niet wordt gemaakt, blijven taken 1–4 volledig bruikbaar.

## Verificatie en oplevercriteria bij latere uitvoering

Alle opdrachten vanuit `TrainIQ-Project/`, met lokaal ontdekte JBR/SDK. Geen `clean`, geen remote runners/cache. Selecteer één geïsoleerde veilige emulator en scope connected opdrachten met `ANDROID_SERIAL`.

```powershell
# Na aanmaken van de nieuwe snapshot-test; voeg andere nieuwe testklassen toe.
.\gradlew.bat :app:testDebugUnitTest --tests '*WorkoutAnalysisSnapshotTest' --tests '*WorkoutEffortSummaryTest' --tests '*WorkoutEffortRequestTest' --tests '*WorkoutProgressComparisonTest' --tests '*WorkoutProgressionSuggestionCalculatorTest' --tests '*WorkoutCompletionLoaderTest' --tests '*WorkoutDebriefGenerationTest' --console=plain

# Eenmaal de lokale baseline voor alle geraakte bronlagen.
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain

# Gericht Android-bewijs; uitbreiden alleen waar unieke risico's dat vereisen.
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest,com.trainiq.features.workout.ActiveWorkoutRestoreInstrumentedTest,com.trainiq.features.workout.WorkoutCompletionRecoveryInstrumentedTest,com.trainiq.core.database.WorkoutDebriefIdentityInstrumentedTest' --console=plain
```

Acceptatie: herkenbare bestaande opzet; consistente ruimte/uitlijning; bereikbare acties bij grote tekst/IME; werkelijk uitgevoerde actuele sets als bron; onbekende inspanning blijft onbekend; dezelfde feiten in UI/lokaal/AI; late feedback ververst; geen dataverlies of dubbele save. Migratieketen alleen uitbreiden wanneer opslag/schema verandert; geen release- of fysieke prestatieclaim onderdeel van dit werk.

## Bewijsstatus van deze planronde

- Uitgevoerd: repository/code-/testbrononderzoek, onafhankelijke bevindingen van drie agents, relevante richtlijnen gelezen, historische screenshots werkelijk bekeken, drie afzonderlijke Image Gen-calls met echte beeldreferenties, visuele beoordeling van de voorstellen.
- Onafhankelijke planreview: QA-agent controleerde scope, bewijsstatus en uitvoerbaarheid. Aanvullingen voor symmetrische historische vergelijking en de afzonderlijke metadata-adapter zijn verwerkt.
- Niet uitgevoerd: nieuwe Gradle-builds, JVM-/connected tests, live AI-debrief, nieuwe runtime-opnamen, TalkBack/Switch Access of prestatiemeting. Historische groene tests gelden niet als bewijs voor nog te bouwen wijzigingen.
- Beperking: ADB kon zijn daemon niet starten. Product Design-contextpreflight kon niet als Python-script draaien omdat Python niet op PATH staat; de verwachte opgeslagen context bestaat niet. Repo en bekeken lokale beelden zijn de gebruikte ontwerpbron.
- Open keuze: visuele richting 1/2/3 en wel/geen optionele RPE-invoer. De kernverbeteringen baseren feedback in beide gevallen op de werkelijk beschikbare feiten.

## Bronnen en toepassing

- [Android Developers: Compose accessibility API defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), gecontroleerd 3 november 2026: minimaal 48 dp en voldoende fysieke layoutruimte om overlappende touch targets te voorkomen; toegepast in maatvoering en geometriechecks.
- [Zourdos et al., oorspronkelijke studie naar RIR-gebaseerde RPE](https://pubmed.ncbi.nlm.nih.gov/26049792/): RPE betreft ervaren inspanning/proximity to failure. Hier alleen als context voor brononderscheid; er wordt geen nieuw rekenmodel of universele coachingsdrempel uit afgeleid.
- Repository: `docs/agent-guides/local-testing.md`, `docs/TrainIQ_Architecture_Decisions.md`, `docs/agent-guides/active-workout-rpe-food-verification.md` en `docs/qa/TrainIQ_Active_Workout_Polish_2026-09-30.md`. Historische uitslagen onderscheiden van de huidige bewijsstatus.
