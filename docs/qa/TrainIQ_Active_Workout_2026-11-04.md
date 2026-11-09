# Actieve training — uitvoering en lokale verificatie, 4 november 2026

**Status:** implementatie, lokale gates en onafhankelijke herbeoordeling gereed. Alle zeven eindbevindingen en de aanvullende lifecyclebevinding N1 zijn opgelost. Volledige JVM-baseline op `914adc0` plus gerichte N1-verificatie op `7302ef6`; Android-bewijs bestaat uit gerichte, waar verantwoord hergebruikte runs.

## Doel, scope en keuzes

Op gebruikersakkoord ‘Ja zo down bro lets go gebruik sub agents’ is gewerkt vanaf `2e0fd9bf3bbfa857815531f5e8f0f00171a174ae` in de bestaande primaire checkout en branch `codex/active-workout-rpe-food`. De combinatie van ontwerpbeelden 1/2/3 staat in de lokale asset `04-gecombineerde-actieve-training.png`. Het werk omvat setkaarten en lay-out, veilig een set opnieuw toevoegen, feitelijke setanalyse en AI-invoer, reactieve afronding en een duurzame rustmelding. Voeding, een nieuwe trainingsmodus, provider, dependency, service of manifestpermissie, release, push en PR vallen buiten scope.

- De bestaande checkout en branch zijn gebruikt om caches en samenhang te behouden; productie-implementatie en Gradle draaiden sequentieel. Dit was geen afzonderlijke worktree; volledige isolatie ontbreekt daardoor, met een schone uitgangsstatus en sequentiële schrijvers als waarborg. Onafhankelijke read-only QA en review liepen waar veilig parallel.
- De optionele RPE-invoer van Task 5 is niet gekozen: de drie velden blijven herhalingen, kg en rust. Herhalen kopieert geen RPE/RIR als nieuwe zelfrapportage; ontbrekende inspanning beperkt de onderbouwing van advies.
- PowerShell is gebruikt als equivalent waar Bash ontbrak; artifactverwerking gebeurde handmatig. De reviewrange begon bij `2e0fd9b`, niet bij `main`: ouder branchwerk is niet opnieuw breed beoordeeld.
- JSON-import wist transiënte timergegevens: een lopende rustperiode wordt stil geïmporteerd, maar sets blijven behouden. Een geldige gewone Room-restore binnen dezelfde boot behoudt de timer.
- Herstel van een verwerkingsfout valt binnen scope omdat dit het directe afrondpad raakt; hiervoor is een beperkte UI-uitbreiding ten opzichte van de oorspronkelijke kaart toegestaan.

## Beoogd gedrag

- De huidige set toont type, vorige prestatie en doel. Gelogde sets hebben een compacte rij met vinkje, correctie en overflow. Twee gelijkvormige logacties staan naast elkaar waar dat past en stapelen bij beperkte breedte of grote tekst.
- ‘Opnieuw loggen’ voegt de laatst voltooide set van dezelfde oefening in dezelfde actieve sessie toe. Correctie, pending of het ontbreken van een vorige set blokkeren de actie vooraf. Bij opslagfout blijft de gewijzigde draft behouden; snelle-tapbeveiliging en undo blijven bestaan. De oudere menuactie ‘Set opnieuw loggen’ behoudt haar bestaande verwijder-en-draftbetekenis.
- Eén domeinsnapshot selecteert de actuele voltooide sets. Totaalvolume telt alle voltooide typen; vergelijkingsvolume gebruikt NORMAL/BACK_OFF en dezelfde fallback voor huidige en historische trainingen. RPE is het ongewogen gemiddelde van geldige gerapporteerde waarden, met dekking; ontbrekend blijft `null`. RPE wordt niet uit kg of herhalingen afgeleid. Spiergroepclaims volgen feitelijke oefenmetadata, met een neutrale fallback; notities gaan niet mee naar AI.
- Voorbeeld: 20 × 10/RPE 3, 60 × 8/RPE 8 en 60 × 8/onbekend geeft 1.160 kg totaalvolume, 960 kg vergelijkingsvolume, 26 herhalingen, gemiddelde set-RPE 5,5 en dekking 2/3. Zonder RPE blijven de volumes gelijk en is inspanning onbekend. De lokale fallback onderbouwt terughoudende progressie zonder onbewezen claims over herstel of verhoging.
- De afronding observeert transactionele Room-gegevens in plaats van maximaal 12 seconden te pollen. Een op seconde 15 opgeslagen debrief verschijnt als het scherm volgens de bestaande ‘blijven’-interactie open is. De bestaande automatische terugkeer blijft; cancellation-, revision- en generation-fences beschermen volgende sessies.
- Room 21 bewaart timeridentiteit, elapsed-deadline, bootidentiteit en handled-status. UI en receiver delen completionclaim/coordinator en elapsed-klok; er zijn geen writes per seconde. Opslag gaat vóór OS-planning. Vertraagde writes worden geweigerd bij vervanging van sessie, start of generatie; misgelopen toekomstige planning wordt bij hervatten hersteld.
- De rusttimer gebruikt een eigen notificatiekanaal, ID 2020, een elapsed-alarm en een private receiver, zonder slaapalarm, fullscreen of loop. Bij toegestane notificaties is het kanaal de enige audiobron. Bij geweigerde appmeldingen mag lokale audio alleen op een daadwerkelijk RESUMED trainingsscherm en met respect voor kanaal, mute en DND. Appgeluid uit maakt de notificatie stil zonder kanaalvoorkeuren te herschrijven. De sheet toont de werkelijke status; het bestaande `POST_NOTIFICATIONS`-recht wordt alleen op expliciete knopdruk via de systeemdialoog gevraagd.
- Een cue die meer dan 30 seconden te laat is, blijft stil. Reboot, legacydata en import wekken geen oude cue. De notificatie-openroute valideert sessie en generatie en gebruikt typed navigatie.

## Commits en beoordelingen

| Onderdeel | Commit | Beoordeling |
|---|---|---|
| Feitelijke setanalyse | `a5508a1` | Terra/high: specificatie en kwaliteit akkoord |
| Fallback en requestfeiten | `ab9bad3` | Terra/high: specificatie en kwaliteit akkoord |
| Reactieve afronding | `fe2e3ff` | Sol/high: specificatie en kwaliteit akkoord |
| Hybride lay-out en veilig herhalen | `ed730c3`, `1ad3440` | Terra/high: vorm- en ruimtefix, scoped herbeoordeling akkoord |
| Duurzame rusttimer | `937756e`, latere fix tot `3a308e2` | Astra/high: scoped beoordeling en herbeoordeling akkoord |

De startinterface accepteerde expliciet aangevraagde parameters GPT-6.1 Sol (`gpt-6.1-sol`)/high voor de samenhangende dataketen en Flow, GPT-6.1 Terra (`gpt-6.1-terra`)/high voor gerichte Composewijzigingen en runtime-QA, en GPT-6 Astra (`gpt-6-astra`)/high voor timerconcurrency, platform en migratie. Een onafhankelijke runtime-attestatie van het model is niet beschikbaar.

De eerste Task 6-reviewclaim dat de echte UI-route de timer voortijdig verloor is ingetrokken na callgraphcontrole: `WorkoutViewModel.finishWorkout` is een helper zonder callers; de normale processingroute behield de timer al bij opslagfout. De helper is wel veilig gemaakt. Een geïntegreerde native proef controleerde behoud bij opslagfout en succesvolle retry/annulering. Een aparte echte geometriefout, de bel 26 px (13 dp) lager dan de tekst, is na een RED-proef opgelost door beide bestaande FlowRows te centreren zonder touch-targets of wrapping te verkleinen.

De daaropvolgende whole-change-review vond nog vier Important/P2-punten: bij de bestaande `processingError` blijven drie shimmers zichtbaar en staan knoppen zonder scroll buiten beeld; een append met rust 0 wordt leeg en valt terug op planrust; notificatie-identiteit wordt dag-ID met een TOCTOU-risico rond `GetOrStart`; en een correctierij kan twee settypepopups tonen. Drie Minor-punten betreffen de enkelvoudige header, duidelijkere voortgang tijdens correctie en drie `UseKtx`-meldingen. Alle zeven punten zijn in commit `914adc0` gecorrigeerd en onafhankelijk herbeoordeeld. Eén nieuw lifecycle-randgeval N1 is daarna binnen dezelfde fixwave gericht opgelost in `7302ef6`; alle verdicts zijn akkoord. Er is geen tweede brede review uitgevoerd.

## Lokale gates en herkomst

Omgeving: JBR `C:/Program Files/Android/Android Studio/jbr`; SDK `C:/Users/menno/AppData/Local/Android/Sdk`; bestaande agent-AVD `TrainIQ_Agent_FoodWorkout_20261022`, API 36, `emulator-5584`. Commands vanuit `TrainIQ-Project/` met `JAVA_HOME`, `ANDROID_HOME` en `ANDROID_SERIAL` ingesteld. Er zijn geen downloads, `clean`, cloudrunner, remote cache of live AI gebruikt.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:generateDebugRoomMigrationChainVerificationMarker '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest,com.trainiq.data.repository.WorkoutRestTimerRoomTest,com.trainiq.core.workout.WorkoutRestTimerNotificationInstrumentedTest,com.trainiq.core.database.WorkoutCompletionObservationInstrumentedTest,com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest,com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest' --offline --console=plain --no-parallel --max-workers=2
.\gradlew.bat :app:assembleDebug --offline --console=plain --no-parallel --max-workers=2
```

De markeropdracht hangt af van `:app:connectedDebugAndroidTest`. De volledige migratieklasse draaide vóór markergeneratie; daarna volgde assemble en is de marker in de APK gecontroleerd.

| Bewijs | Resultaat en begrenzing |
|---|---|
| JVM, eerdere volledige bronset | 1.049 tests, 145 suites, 0 failures/errors/skips; de rootagent telde bewaarde XML onafhankelijk. |
| Native run 3 | 88/88: migratie 16, observation 2, adapter 4, TargetedRoom 48, Room-timer 3, setacties 15. XML onafhankelijk geteld. |
| Hergebruik uit run 2 | 17 geselecteerde ongewijzigde regressies: slaap 3, debriefidentity 2 en completionRecovery 12; 0 failures/errors/skips. Run 2 als geheel was 104/105 en wordt niet als geheel groen gerapporteerd. Deze 17 en de 88 zijn geen enkele actuele run van 105. MainActivity recreate/CREATED → RESUMED en nieuwe PendingIntent `onNewIntent`/sheetroute zijn in de actuele setactieklasse opnieuw bewezen. |
| Marker | `trainiq-room-migration-chain-v2-to-v21`, current/end 21, migrationCount 16, payload-SHA-256 `d80c35ab1fa36e2d6fb04ba6fbacef63150dc8f31d7687194b89195865166763`. |
| Timerfix op bron `3a308e2` | 110 gerichte JVM-tests groen; vier native cases groen, verdeeld over drie in een eerste run en geometrie 1/1 na alleen een correctie van de testnodeselector. Lint: 0 errors, 30 warnings. AssembleDebug daarna groen; schema en marker ongewijzigd en opnieuw in APK gecontroleerd. |
| Lint, eerdere volledige bronset | 0 errors, 30 warnings: MissingKeepAnnotation 2, ObsoleteSdkInt 2, UnsupportedChromeOsCameraSystemFeature 1, UnusedResources 7, UseKtx 18. |

De eerdere debug-APK had SHA-256 `8FDC01152DB9F38BD37994D767FF57AE21778B840AF6894BF532268EFA3EB146`; de APK na timerfix had SHA-256 `3522804D8894BAE369E91E2D80609204533BB5D45D1301FD68BD10506D9B60BD`, vóór installatie onafhankelijk gecontroleerd. De hoofdfixwave bouwde APK `655102EAA0824C2528BBBCF94C71836061AA47C7D122D5649AE8B6FD08D3295F`; definitieve N1-APK: **`B7D9D5F304F465286791A8CACD1D673C979EC4375F898F24522ED30B6D7A2E43`**.

Snapshot-, request-, fallback-, Flow-, appendpolicy- en timerpolicy/coordinator/importregressies zijn eerst rood en daarna groen waargenomen. Niet ieder aanvullend Androidcontract heeft een afzonderlijke opgeslagen rode run. Tests die uitsluitend oude bronvorm, pill of shortcutrij voorschreven zijn vervangen door gedrag en native geometrie; het JVM-aantal daalde tijdelijk van 1.043 naar 1.041, niet door verwijderde productdekking. Eerdere native iteraties waren 99/102 en 104/105 voordat de geselecteerde run van 88 groen werd; onder meer harness/fixture-, scrollanker- en lifecycleproblemen zijn daarin gecorrigeerd. Bestaande 48dp-, glyph- en overlapasserties bleven behouden.

## Runtimecontrole en beperkingen

De [Task 4-layoutcontrole](C:/Users/menno/.codex/visualizations/2026/11/03/01a1a335-0618-7ac1-a457-8ddd926e73ec/active-workout-implementation-2026-11-04/runtime-layout-qa.md) gebruikte bron `1ad3440` en APK-SHA-256 `11C1398531F9EC3BA5F9D194CD5B8AD35196ECCF3B834EB136A38C03A6D4D28C`: 390 × 844 dp/font 1.0/licht en 360 × 640 dp/font 1.5/donker. Fictieve 80 kg × 12 plus herhalen gaf twee sets en 1.920 kg. Notitie opslaan/inklappen, correctiestand, menu en IME zijn bekeken; acties waren na scrollen en sluiten van het toetsenbord bereikbaar. Dit bewijst niet dat alle acties tegelijk met open IME zichtbaar zijn. Een mogelijke afwijking in de voortgang tijdens correctie is daar beschreven zonder vastgestelde regressie. Per toestand zijn screenshots en verse UI-trees bewaard; de rootagent bekeek relevante beelden.

De [rusttimercontrole](C:/Users/menno/.codex/visualizations/2026/11/03/01a1a335-0618-7ac1-a457-8ddd926e73ec/active-workout-implementation-2026-11-04/runtime-rest-final-qa.md) gebruikte de eerdere APK `8FDC…` op 360 × 640 dp/font 1.3/licht en font 1.5/donker en op 640 × 360 dp/font 1.3. De echte notificatiedialoog is geweigerd en daarna toegestaan; de sheet toonde respectievelijk ‘Meldingen niet toegestaan’ en ‘Geluid en melding aan’. Bij 15 seconden rust, Home en `am kill com.trainiq` verdween PID 19239. Android registreerde een inexact `ELAPSED_WAKEUP`-alarm op elapsed 6657.837 s met venster tot 6669.076 s; receiver-PID 20335 plaatste één private, automatisch verwijderbare notificatie 2020 op `trainiq_workout_rest`, zonder fullscreen intent. Een echte tik op ‘Verder trainen’ heropende dezelfde actieve sessie met 1/3 set en 960 kg.

Na een tweede set met 90 seconden rust, Home en `am kill` verdween PID 20335; heropening onder PID 20698 toonde 2/3 sets, 1.920 kg en 0:53 resterend. Een te vroege UI-dump met null root is vervangen door een verse geslaagde dump; force-stop is niet gebruikt als bewijs voor procesdood. In compact liggend formaat waren footer en bel zichtbaar en overige setacties en sheetbeheer na scrollen bereikbaar; niet alles was tegelijk zichtbaar. Een native test bewijst contentIntent → `onNewIntent` → typed route → sheet → voorkeurherstel → skip.

De [brede controle](C:/Users/menno/.codex/visualizations/2026/11/03/01a1a335-0618-7ac1-a457-8ddd926e73ec/active-workout-implementation-2026-11-04/runtime-wide-root-qa.md) gebruikte bron `3a308e2`, APK `3522804D8894BAE369E91E2D80609204533BB5D45D1301FD68BD10506D9B60BD` en één formaat van 900 × 640 dp/font 1.3/licht. Met fictieve WideQA/Bench Press-gegevens waren huidige set, velden, beide logacties naast elkaar, timerstatus, bel en sheetbeheer leesbaar en na scrollen bereikbaar. De voltooide rij stond deels boven de viewport; volledige wide-targetgeometrie daarvan is niet bewezen. Tijdelijke undo- en herstelsnackbars zijn onderscheiden van permanente afkapping. De rootagent bekeek de echte beelden en verse UI-trees. Een eerdere bredere aanvraag was vóór uitvoering door automatische goedkeuringscontrole afgewezen; na controle dat breed/resizable binnen de afgesproken scope viel, is de begrensde directe controle opnieuw goedgekeurd en uitgevoerd.

Na QA is het apparaat hersteld naar 720 × 1280 px, density 320, font 1.3 en licht. Ringer, volume, DND en kanaal zijn niet gewijzigd. Niet uitgevoerd: afzonderlijk 360 × 800, lock/unlock, geforceerde Doze, echte intrekking van exacte-alarmtoegang of reboot, menselijke TalkBack/Switch Access, fysiek luisteren en hardwareperformance. Eén breed formaat is geen algemene tablet- of foldablecertificatie. Clock-, boot-, stale-, mute-, failed-scheduling- en generationbeleid is op de laagste betrouwbare laag getest; dat is geen volledige hardware- of OS-certificatie. De volledige Androidsuite, Health Connect en live AI zijn niet uitgevoerd.

Room-claim en externe notificatie zijn geen enkele transactie: een crash na claim en vóór plaatsen kan een cue verliezen; niet herhalen voorkomt dubbele audio. Android kan korte opeenvolgende idle-alarmen vertragen, ook met exacte-alarmtoegang. De sheet benoemt dit; secondeprecisie op een vergrendeld toestel is niet beloofd.

## Eindverificatie

Hoofdfixwave: `914adc02020a5cbc83c8908456a4c72bf14b4f5b` op `3a308e2`; 13 exacte bron-/testpaden. Finale bron na het gerichte N1-addendum: `7302ef60bc0f23615b1b4dcee473f29db76099d4`. Root heeft de volledige JVM-baseline, de aanvullende N1-selectie en de native deelruns onafhankelijk geteld en de uiteindelijke APK-hash gecontroleerd.

| Hoofdfixgate op `914adc0` | Uitslag |
|---|---|
| Volledige `:app:testDebugUnitTest` | **1.052 tests / 146 suites, 0 failures/errors/skips**. Eenmalig verbreed na concrete verwijzingen vanuit gedeelde route/usecase/load naar drie architectuur-/repositorytestklassen buiten de eerste 144-selectie. |
| `:app:lintDebug` | **0 errors, 27 bestaande warnings**; precies de drie nieuwe UseKtx-waarschuwingen zijn weg. |
| ActiveWorkoutSetActions + notificatieadapter | **24 unieke groene cases**: 23 uit `portrait-run1`, gecorrigeerde testfixture `room-resume` 1/1. Geen claim van één volledig groene 24-test-run. |
| Foutpagina portret en compact landschap | De aangepaste echte fout/retrycase is daarnaast 1/1 groen in elk formaat, met werkelijke knopbediening, dezelfde timer na mislukte opslag en één opgeslagen training/alarmannulering na retry. |
| `:app:assembleDebug` na de finale JVM-/Android-gates | Geslaagd. APK-hash hierboven gecontroleerd; de ongewijzigde echte v2→21/count16-marker is opnieuw in de ZIP gecontroleerd. Geen migratie-/schemabron gewijzigd. |

De volledige JVM-run op `914adc0` vervangt de eerdere 1.049-testbaseline. Voor de kleine N1-uitbreiding zijn de geraakte start/resume/navigationcontracten op `7302ef6` opnieuw gericht getest; de overige ongewijzigde JVM-contracten zijn hergebruikt. De ongewijzigde migratie-, Room-, slaap-, identity- en completionRecovery-resultaten blijven per beschreven bronlaag geldig. De vier Android-adaptercases draaiden opnieuw; echte toestemming/achtergrondprocesdood uit de eerdere APK blijft uitsluitend bewijs voor de ongewijzigde adapter-/persistencewerking. Het nieuwe routepad is actueel bewezen met een echte PendingIntent vanaf Home en Activity.recreate, plus de Room-barrièreproef.

- **Foutherstel:** Error toont een fouttitel zonder shimmers, spinner of statuschips; de inhoud kan scrollen. Op 360 × 640 en 640 × 360 dp/font 1,3 zijn beide herstelknoppen volledig zichtbaar en echt aangeraakt. De SQL-trigger wordt altijd opgeruimd.
- **Notificatie-identiteit:** een volledig verzoek blijft behouden tot compare-and-set-consumptie. De bestaande typed route heeft optionele nullable identiteitsargs met null-defaults; normale day-only start blijft compatibel. Een gedeeltelijke identiteit blijft fail-closed. De aparte resume-only usecase leest de huidige sessie na andere lookup-reads opnieuw en doet geen getOrStart of draft-write. Gecontroleerde onderbrekingen met echte Room-finish/discard/vervanging maken geen nieuwe draft en wijzigen de vervangende sessie niet. Dit is geen absolute UI/DB-atomiciteitsclaim.
- **Nulrust:** alleen append bewaart de gekopieerde rust expliciet als numerieke string, ook 0. De algemene lege-draftfallback blijft gelijk. Echte opslag bewaart dezelfde kg/reps/type, onbekende nieuwe RPE/RIR, precies één extra set en geen nieuwe timerplanning.
- **Eén typekiezer:** tijdens correctie is één popup-eigenaar actief. Beide toegankelijke ingangen tonen één optiereeks; selectie sluit het menu en wordt pas bij correctie naar dezelfde set opgeslagen.
- **Copy en semantiek:** `1 oefening` is correct; voortgang benoemt de lopende correctie en de footer blijft opgeslagen sets tellen. Selectie, volume en finishpolicy blijven gelijk.

De native REDs zijn echt vastgelegd voor 0→90 seconden, 1→2 popups en loadingpresentatie in Error. Een eerste alarmfixture-interferentie, een ontbrekende testimport, een test-only Hilt-entrypoint en een onjuiste lazy-headerwacht zijn afzonderlijk bewaard en niet als productfout of geslaagde run gepresenteerd. Het definitieve barrièrefixture gebruikt de bestaande debugdatabase-entrypoint en een echte RoomWorkoutRepository; geen productie-testhook.

Belangrijkste laatste commands, vanuit dezelfde JBR/SDK/serial en met de reeds vermelde offlineflags:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests 'com.trainiq.domain.usecase.StartWorkoutSessionUseCaseTest' --tests 'com.trainiq.features.workout.WorkoutInputValidationTest' --tests 'com.trainiq.features.workout.RestTimerTest' --tests 'com.trainiq.navigation.*' :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest,com.trainiq.core.workout.WorkoutRestTimerNotificationInstrumentedTest' --offline --console=plain --no-parallel --max-workers=2
# Zie het fixrapport voor de afzonderlijke Room-/landschap-/portretreruns na testfixturecorrecties.
.\gradlew.bat :app:testDebugUnitTest --offline --console=plain --no-parallel --max-workers=2
.\gradlew.bat :app:assembleDebug --offline --console=plain --no-parallel --max-workers=2
```

De gecombineerde opdracht bevatte de 144-testselectie; een eerste poging stopte bij de Android-testcompilatie. Na de testcorrecties draaiden de native opdrachten afzonderlijk. De volledige JVM-command is vervolgens afzonderlijk uitgevoerd. Het fixrapport bewaart deze exacte volgorde en de tussenresultaten.

**Scoped herbeoordeling:** alle zeven oorspronkelijke punten en N1 zijn ADDRESSED; geen open Critical/Important-bevindingen. Dezelfde onafhankelijke Astra-review blijft begrensd tot de fixdiff en het N1-addendum; geen nieuwe volledige review of productvereisten. De source-, test- en beeldartefacts worden lokaal bewaard in het bewijsarchief; geen gegenereerde logs, captures of APK in Git. De oorspronkelijke emulatorinstellingen zijn opnieuw gelezen en correct bevonden; alleen de door root gestarte emulator-5584 is gestopt en daarna als disconnected bevestigd. De bestaande AVD is behouden. Geen release, push of PR.

## N1 en echte procesherstelproef

Een nieuwe guard mag de oorspronkelijke rustgeneratie niet permanent aan een al geaccepteerde sessie blijven koppelen. Het echte RED was geldig openen → overslaan → Activity.recreate → oefening verdwijnt. Een ViewModel-owned SavedStateHandle bewaart nu de geaccepteerde requestidentiteit en sessiestarttijd. Eerste/onbekende requests blijven generatie-strict; herladen van exact dezelfde geaccepteerde bestemming blijft read-only en controleert day/session/startedAt, ook na een normale timerwijziging. Een vervangen of verdwenen sessie blijft geweigerd. Geen nieuwe routeargs, schema of dependency in dit addendum.

- JVM N1: **34 tests / 7 suites, 0 failures/errors/skips**, onafhankelijk uit bewaarde XML geteld. Een nieuwe stateholder uit opgeslagen primitive values bewijst het saved-statecontract; dat is op zichzelf geen OS-procesdoodclaim.
- Native N1: twee unieke groene cases uit twee runs: echte Room-finish/discard/vervanging en de uitgebreide PendingIntent-reeks. Die reeks gaat vanaf Home via skip/recreate, nieuwe set/timergeneratie/recreate en historie/terug, en weigert daarna een opnieuw geleverde oude intent. Een onjuiste historietitelverwachting is apart bewaard; de echte Terug-knop wordt gebruikt. Geen productieaanpassing na groene JVM/lint.
- Lint **0 errors/27 bestaande warnings**, assemble na deze gates geslaagd. De definitieve APK-hash hierboven is door root vóór installatie gecontroleerd; de echte ongewijzigde Room 21/count16-marker is opnieuw in de APK gecontroleerd.
- Root voerde aanvullend op deze definitieve APK een echte notificationtap vanaf routinedetails uit, voegde een tweede fictieve set met nieuwe timergeneratie toe en ging naar Home. De eerste te vroege amkill deed niets en is niet als procesdood geteld. Na read-only vaststelling van cached-status15/oom900 zonder services verwijderde de gewone herhaalde amkill PID31515. Launcherheropening was COLD met PID32662 en toonde Actieve training met dezelfde twee sets. De timer was vóór de bevestigde kill verstreken: geen extra lopende-countdownclaim. De gerichte crashbuffer voor het herstelde proces bevatte geen regels. Root heeft verse UI-trees en beelden werkelijk bekeken.

De nieuwe code en gerichte tests zijn onafhankelijk Approved. Ook dit procesbewijs is als door root uitgevoerde aanvulling gelezen, niet door de reviewer nogmaals uitgevoerd. Ruwe commands, XML, JSON en beelden staan in het lokale bewijsarchief; geen fysieke audio-, TalkBack-, Doze- of performanceclaim.

## Keuzes en gevolgen

| Keuze | Begrenzing of gevolg |
|---|---|
| Bestaande schone checkout/branch, sequentiële implementatie | Geen extra worktree-isolatie; geen overlap met userwerk en exact gestagede taakpaden. |
| Optionele RPE-invoer niet geselecteerd | Geen nieuwe zelfrapportage-interface; onbekende inspanning beperkt advies en blijft expliciet onbekend. |
| PowerShell-equivalent waar Bash ontbrak | Handmatige verwerking van dezelfde plan-/reviewartefacts; geen scriptinstallatie, mogelijke afwijkingen expliciet gecontroleerd. |
| Reviewrange vanaf uitvoeringsbasis `2e0fd9b` | Ouder branchwerk niet breed opnieuw beoordeeld; geraakte interacties en regressies wel gecontroleerd. |
| Transiënte timeridentiteit wissen bij import | Een geïmporteerde lopende rust wordt stil; training, sets en notities blijven behouden; een nieuwe timer werkt normaal. |
| Beperkte processing-errorreparatie in scope | Iets meer UI dan de oorspronkelijke setkaart; uitsluitend het direct gebruikte afrond-/herstelpad, zonder beleidswijziging. |

Lokaal bewijsarchief: [index](C:/Users/menno/.codex/visualizations/2026/11/03/01a1a335-0618-7ac1-a457-8ddd926e73ec/active-workout-implementation-2026-11-04/README.md), [exacte fixcommands en bronkeuzes](C:/Users/menno/.codex/visualizations/2026/11/03/01a1a335-0618-7ac1-a457-8ddd926e73ec/active-workout-implementation-2026-11-04/final-fix-report.md), [laatste onafhankelijke beoordeling](C:/Users/menno/.codex/visualizations/2026/11/03/01a1a335-0618-7ac1-a457-8ddd926e73ec/active-workout-implementation-2026-11-04/n1-review.md), [gewijzigde paden](C:/Users/menno/.codex/visualizations/2026/11/03/01a1a335-0618-7ac1-a457-8ddd926e73ec/active-workout-implementation-2026-11-04/changed-paths.txt) en [uiteindelijke UI met testdata](C:/Users/menno/.codex/visualizations/2026/11/03/01a1a335-0618-7ac1-a457-8ddd926e73ec/active-workout-implementation-2026-11-04/runtime/n1-process/final-active-training.png).

## Toegepaste primaire bronnen

- [Compose-toegankelijkheid](https://developer.android.com/develop/ui/compose/accessibility/api-defaults): minimaal 48 dp; 56/64 dp en tussenruimte zijn eigen ontwerpkeuzes.
- [AlarmManager](https://developer.android.com/reference/android/app/AlarmManager) en [Doze](https://developer.android.com/training/monitoring-device-state/doze-standby): elapsed wakeup, exacte/inexacte planning en idlequota, toegepast in scheduler, status en bewijsgrenzen.
- [NotificationCompat.Builder](https://developer.android.com/reference/androidx/core/app/NotificationCompat.Builder): per notificatie stil, private en verwijderbaar, zonder fullscreen of insistent gedrag.
- [Notificatierecht](https://developer.android.com/develop/ui/compose/notifications/notification-permission) en [notificatiekanalen](https://developer.android.com/develop/ui/compose/notifications/channels): expliciete aanvraag, werkelijke status en behoud van kanaalvoorkeur.
