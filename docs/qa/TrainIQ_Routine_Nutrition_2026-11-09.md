# Routinegeneratie en voeding — lokale kwaliteitscontrole, 9 november 2026

Status: implementatie, lokale integratiecontrole, volledige onafhankelijke eindreview en gezamenlijke herstelronde gereed. Scoped Astra-herreview **Approved op `db3c27a`**, nul open Critical/Important/Minor uit de gecombineerde lijst. Dit bronverslag wordt vóór main-integratie vastgelegd. De uiteindelijke main-SHA, bestandsdigests, updateproef en eenmalige bezorgstatus horen daarna in de ignored lokale afleverledger en het afleververslag; bronverificatie is geen reeds verstuurde build.

## Opdracht en afbakening

De gebruiker vroeg routinegeneratie te herstellen, eten en drinken eenvoudiger toe te voegen, samenhangende bestaande flows te verbeteren en aanvullend AI-voedseltoevoeging te repareren. Hij autoriseerde autonome uitvoering met subagents, onafhankelijke review en herstel, alle verbeteringen naar main brengen en daarna één update via de bestaande Drive-/emailprocedure. De geïnstalleerde versie is de debug-APK uit de email van 22 oktober. Daarom wordt uitsluitend voor deze bezorging de bestaande debughandtekening behouden; de APK wordt duidelijk als debug benoemd. Een anders ondertekende release zou geen gegevensbehoudende update zijn.

Uitgangspunt: primaire checkout `C:/My-PC-Files/GitHub/TrainIQ`, bestaande branch `codex/active-workout-rpe-food`, bron `927c1eb056c920b5c8ae3b7de1157684bed5217c`, remote-main bij aanvang `c69e24e5f4a660430cc93a29eae1843dc01f4460`. De eerdere actieve-training/RPE/timerverbeteringen op deze branch gaan eveneens mee. Hun afzonderlijke bewijs staat in [het eerdere QA-rapport](TrainIQ_Active_Workout_2026-11-04.md); de volledige eindreview heeft het hele te mergen bereik beoordeeld.

Geen nieuwe dienst, dependency, AI-model, toestemming, signingmateriaal of Room-schema toegevoegd in deze nieuwe routine-/voedingsbatch. Geen live AI-aanvraag met persoonlijke gegevens. Handmatige en offline invoer blijven beschikbaar. Het eerdere trainingswerk bevat de reeds gedocumenteerde Room-migratie 20→21; de uiteindelijke main-build krijgt vers migratiebewijs via de bestaande taken.

## Resultaat en ontwerpkeuzes

- Routinegeneratie bewaart de betekenis van verplichte en optionele JSON-velden bij de OpenAI-strictconversie. Een nieuwe oefening gebruikt een null-bibliotheekreferentie; een onjuiste bestaande verwijzing wordt afgewezen. Geblokkeerde, afgebroken en onbruikbare routineoutput krijgt een zichtbare herstelbare fout. De adaptieve generator houdt opties en acties bereikbaar; ontbrekend profiel heeft een concrete vervolgstap.
- Een maaltijdconcept heeft een expliciete bestemming en identiteit. Bewerken, een andere maaltijd kiezen en geschiedenis hergebruiken vragen bij gevulde invoer om behouden of vervangen. Hergebruik maakt een nieuwe registratie; retry behoudt dezelfde identiteit. Opslagafloop kan een nieuwer concept niet wissen.
- Een nieuwe maaltijd van vandaag gebruikt het werkelijke moment van opslaan, ook als het scherm rond middernacht openbleef. Een andere gekozen datum gebruikt 12:00 lokaal. Een bewerking op dezelfde datum bewaart het oorspronkelijke tijdstip; een andere datum bewaart de lokale kloktijd met normale tijdzoneafhandeling.
- Het toevoegscherm zoekt direct in opgeslagen producten. De standaardportie komt in een controleerbaar concept; selectie slaat geen maaltijd stilzwijgend op. Handmatig, barcode, recept en foto blijven bereikbaar. Snelle +250/+500 ml gebruiken de bestaande opslagguard en retry-identiteit. Vrije hoeveelheid en registraties zijn uitklapbaar, zodat eten op een compacte telefoon zichtbaar blijft.
- AI-voedselanalyse neemt de gekozen bron ook bij camera-invoer mee. Een ViewModel-coordinator bezit de batch met stabiele rij-ID's en onveranderlijke gevalideerde invoer. Gedeeltelijk succes verwijdert uitsluitend geslaagde rijen; retry verwerkt resterende correcties. Verouderde analyses en bewerkingen tijdens opslag zijn geblokkeerd met echte disabled-semantics.
- Maaltijd- en receptoverdracht gebruiken doel-/concepttokens en ontvangstbevestiging. De bestaande afzonderlijke navigatie-eigenaren blijven behouden. Tijdens een lokale batch of nog niet verwerkte afloop voorkomt een tijdelijke vertrekguard verlies van die eigenaar. Overblijvende AI-review verlaten vraagt een expliciete keuze. Dit is geen duurzame queue of algemene exactly-oncegarantie bij procesdood.

De begrensde aanpak vermijdt een volledige voedingswizard of appbrede navigatieherbouw. Kosten: één extra tik voor vrije vochtinvoer/geschiedenis, een korte vertrekpauze tijdens opslag en een bevestiging bij bewust verlaten van onvoltooide AI-review. Deze keuzes ondersteunen de gevraagde eenvoud en beschermen bestaande invoer.

## Uitvoering en onafhankelijke taakreviews

Eén bronwriter en één Gradle-/devicegebruiker tegelijk; read-only onderzoek en review liepen waar onafhankelijk parallel. Primaire checkout en caches zijn hergebruikt. PowerShell-equivalenten en handmatige reviewpakketten vervangen shellhelpers; dit verandert geen productgedrag.

| Onderdeel | Commits | Bewijs en review |
|---|---|---|
| Routinecontract en herstel | `44b00af`, `3da956a` | 313 betrokken JVM-tests groen; oorspronkelijke native selectie 7/7 plus gerichte profielherstelcase 1/1. Onafhankelijke Sol/high-review akkoord na R1: actiegerichte profielmelding en opties behouden bij profielbezoek. |
| Maaltijdconcepten en tijdstippen | `47ca16b`, `c6b2273` | 118 betrokken voedings-JVM-tests en native selectie 10/10. Review I1 vond gecachte standaarddatum rond middernacht; na fix 12 JVM-tests en 3 native cases groen, scoped herbeoordeling akkoord. |
| Eenvoudig toevoegen en drinken | `1a950ac`, `32a13d0` | 118 voedings-JVM-tests; 16 barcodecases groen binnen een 19-caserun, daarna 3 FirstAdd-cases groen na gerichte harnesscorrectie. Vochtselecties 6/6, compacte fix 7/7 plus afzonderlijke geschiedeniscase 1/1. Review F1 vond dat de waterkaart alle voeding onder de vouw duwde; compactere disclosure en daadwerkelijke Main-beelden akkoord. |
| AI-voedselbatch en eigenaar | `2407472`, `9018b27` | 157 JVM-tests/23 suites en 17 native cases groen; laatste navigatiewijziging apart 33 JVM-tests/7 suites en 2 native cases groen. Review vond I1 (bewerkte context genegeerd) en I2 (eerder opgeslagen grammen ontbraken bij deelretry). Na gezamenlijke fix 66 gerichte, 131 betrokken JVM-tests en 4 native cases groen; scoped herreview Approved, 0 open Critical/Important. |

Rode gedragstests zijn bewaard vóór de relevante implementatiefixes. Niet iedere aanvullende integratiecase heeft een afzonderlijke rode run. Tests zijn niet verzwakt of verwijderd om groen te verkrijgen.

De uitvoerinterface accepteerde expliciete configuraties GPT-6.1 Sol (`gpt-6.1-sol`), high voor samenhangende AI-/state-/lifecyclecode en onafhankelijke taakreviews, en GPT-6.1 Terra (`gpt-6.1-terra`), high voor gerichte voedings-/vochtverbeteringen en QA. Terra is volgens projectvoorkeur overwogen en ingezet. Een vroege Astra/high-aanvraag voor de complexe AI-voedingstaak kon door de actieve agentlimiet niet starten; bestaande Sol/high-capaciteit is toen expliciet gebruikt. Na de Task5-overdracht slaagde een nieuwe expliciete start van GPT-6 Astra (`gpt-6-astra`), high voor de ene volledige eindreview van het gehele bereik `c69e24e..e10aac8`, vanwege het gezamenlijke data-/concurrencyrisico. Er is geen onafhankelijke runtime-attestatie van modelinstellingen.

## Omgeving en betekenis van het bewijs

Lokaal: Android Studio JBR `C:/Program Files/Android/Android Studio/jbr`, SDK `C:/Users/menno/AppData/Local/Android/Sdk`, bestaande caches, `--offline --console=plain --no-parallel --max-workers=2`. Geen `clean`, downloads, CI of cloudtests. Alle devicecommando's zijn gericht op de ene nieuw aangemaakte agent-AVD `TrainIQ_Agent_RoutineFood_20261109`, API 36, `emulator-5586`. Geen onbekend fysiek toestel is aangepast.

De openingsbaseline op `927c1eb` slaagde met `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:checkReleaseSigningReadiness`: 1.053 JVM-tests/146 suites, 0 failures/errors/skips; lint 0 errors en 27 bestaande warnings.

De gezamenlijke baseline op `9018b27` plus uitsluitend versie 4/1.0.2-B (daarna commit `e10aac83236492b565a3d67bf33f39738cb55bf1`) slaagde met `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`: **1.095 tests/151 suites, 0 failures/errors/skips**. Root telde de bewaarde XML onafhankelijk. Lint heeft 0 errors en 28 warnings: één nieuwe `MissingKeepAnnotation` bij `FoodProviderMode` als typed navigatieargument, hieronder als M1 afzonderlijk hersteld en herbeoordeeld. APK-metadata bevestigt package `com.trainiq`, versiecode 4 en de bestaande debughandtekening. Deze baseline is voorafgaand aan de hieronder gereproduceerde aanvullende raceproeven, niet de finale fixbaseline.

## Aanvullende integratiebevindingen

- **R1, afronden versus pending set:** de snapshotread bestond al op `c69e24e`; dit wordt niet als nieuw geïntroduceerde regressie gepresenteerd. Een gecontroleerde barrière in de echte Hilt-coordinator/Room-timermutex laat de tweede logwrite vóór finish committen. Het logresultaat bevestigt twee actieve sets; de finish bewaart slechts één completed set en ruimt alle actieve sets op. De gerichte native proef is werkelijk rood (1 test/1 failure, door root XML-geteld). Een UI-guard alleen herstelt de transactionele snapshotrace niet.
- **R2, timerverversing:** een queued +30-wijziging met een nieuwe fixturegeneratie wordt door een exact ongewijzigde bevestigde timerrefresh gesupersedet. De writerlaag voert nul persistwrites uit waar één verwacht wordt. De gerichte JVM-proef is rood (1/1, root XML-geteld). Dit bewijst het mechanisme; een volledige collapse-UI-runtimeclaim wordt hieruit niet afgeleid. Werkelijk nieuwe sets/sessies en timer-/handled-vervanging moeten oude writes blijven kunnen vervangen.

De ene volledige onafhankelijke Astra/high-review beoordeelde het hele vaste bereik `c69e24e5f4a660430cc93a29eae1843dc01f4460..e10aac83236492b565a3d67bf33f39738cb55bf1`: 24 commits en 115 paden, inclusief alle eerdere trainings-/voedingswijzigingen. Verdict: 0 Critical, precies R1/R2 als Important en de enum-Keepwaarschuwing als Minor. De volledige productiediff en gerichte test-/XML-evidence zijn beoordeeld; dit is geen claim dat iedere testregel even diep is geaudit. De gezamenlijke fixwave heeft deze drie bevindingen hersteld, gevolgd door uitsluitend een scoped herreview van de fix en gevolgen.

De geldige rode regressies zijn behouden en worden hieronder in de fixbaseline groen bewezen. De oude baseline hierboven wordt niet als een geheel groene actuele bron met de toen nieuwe rode tests gepresenteerd. De aanvullende zes integratietests op `e10aac8` (vier XML-suites, door root onafhankelijk geteld) zijn groen: twee actieve-trainingroutes, twee MealDetail/Roomcases, één eerdere fragiele eerste-save/editcase en één werkelijke handmatige eerste maaltijd.

Een compact IME-tussenscreenshot toonde de sluitknop gedeeltelijk. De begrensde vervolgproef bewees verder scrollen, een volledig bereikbare 48dp-klikparent, echt sluiten en behouden zoekterm. Verse 176 ml bleef bij gewone plus/sluiten-, scroll- en inklap/heropenacties behouden; 177 ml bleef behouden tijdens één offscreen kaart-/schermformaatproef. Een eerdere eenmalige 175ml-afwijking uit een langere reeks is niet causaal geïsoleerd en wordt niet als bevestigd sluit-/vochtdefect gepresenteerd. Bij de formaatproef is Activity-recreatie niet onafhankelijk vastgesteld. Er is geen nieuwe algemene lifecycleclaim of aanvullende fixscope.

## Finale herstelronde en baseline

Commit `db3c27a19f53ad0ad00aa03bb2af9f67665b4100` op `e10aac8` herstelt exact R1/R2/M1 in tien bron-/testpaden. Vooraf werden twee gerichte timertests en twee native cases werkelijk rood op de oude productcode: zowel het setverlies als de echte Hilt WorkoutViewModel-merge die de optimistische timer kwijtraakte. Een ontbrekende fixturealias en een bronanker dat de verplaatste builder niet vond zijn daarna betekenisbehoudend gecorrigeerd; die compile-/testharnessproblemen zijn niet als gedrags-RED of groene run gepresenteerd.

- Actieve finish legt eerst alleen de bedoelde identiteit vast. De bestaande timerlock → storemutex → Roomtransactie leest vervolgens verse sessie, sets en gerichte metadata, bouwt inhoud én analyse/debrief uit dezelfde bron en schrijft atomisch. Vervangen of verdwenen sessies falen zonder cleanup van een andere training. Geen netwerk/Flow-await of food-/health-export binnen de transactie. Legacy finish behoudt expliciete sets/duur en verwijdert geen actieve sessie.
- Timerbronobservaties staan los van de optimistische weergave. Ongewijzigde refresh en eigen bevestigingen behouden geldige queued tikken; echte set-/sessie-/rawtimervervanging blijft een fence, ook bij nieuwe nulrustset met twee nulltimers. De werkelijke `applyActiveSession`-merge gebruikt de effectieve timer. Correlatie leeft uitsluitend zolang de queue actief is.
- `FoodProviderMode` heeft `@Keep`; uitsluitend de nieuwe waarschuwing is verholpen.

| Gate op de finale bron | Resultaat |
|---|---|
| Volledige `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin` | **1.102 tests / 153 suites, 0 failures/errors/skips**; alle taken geslaagd. |
| Lint | **0 errors, 27 bestaande warnings**; geen FoodProviderMode-issue meer. |
| Laatste canonical connected R1-klasse | **6/6 groen**: actuele setinhoud/940 kg en dubbele finish, vervanging/starttijd, verwijderen, echte SQLite-rollback met behoud actieve sets/timer, legacy en incomplete input. FK-check leeg. |
| Betrokken matrix vóór uitsluitend R1-fixtureaanscherping | 283 JVM-tests/37 suites en 14 native tests/4 suites groen. De overige acht native cases betreffen echte Hilt-VM-merge (1), Room-timer (3) en notificatiecontracten (4); hun inputs veranderden daarna niet. De laatste zes R1-cases zijn afzonderlijk opnieuw bewezen. Dit is geen som tot twintig unieke of één grotere native run. |

Root en Astra telden bovenstaande finale XML en lint onafhankelijk. De pure finishbuilder bewijst overeenkomstige setinhoud, RPE, volume, notities en resultaat; de echte Room-proeven bewijzen schrijfvolgorde, identityfence en rollback. De Hilt-VM-mergeproef is een echte ViewModel-wiringtest via testreflectie, geen volledige visuele UI-/Roomtimerwriteclaim. De bestaande notificatietests wijzigen geen rechten, DND, ringer of kanaalkeuzes.

De laatste gecombineerde opdracht vanuit `TrainIQ-Project/`:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.WorkoutFinishPendingSetRaceInstrumentedTest' --offline --console=plain --no-parallel --max-workers=2
```

Ruwe evidence: `final-fix/baseline/{xml,native,lint.xml,gradle.log}` en `final-fix/affected-green/` onder de artifactroot. De scoped Astra-herreview las alle tien fixdiffpaden, nieuwe tests en directe transitive grenzen en is Approved op de exacte commit: R1/R2/M1 opgelost, geen nieuwe concrete fixregressie. Geen tweede volledige audit of algemene appcertificering. Main-integratie en de verse migratiemarker/compatibele updatebuild volgen als afzonderlijke geautoriseerde afleverstappen.

De AI-voedselbewijzen hebben verschillende grenzen:

- De safetytest gebruikt de productie-UI en coordinator met een fakewriter voor enabled-semantics en verouderde analyse.
- De ownertest gebruikt echte typed Nutrition→MealDetail-backstackentries en `ActivityScenario.recreate`, met een behouden fixture-VM en productiecoordinator/gate. Hij bewijst behoud van entry/VM, geblokkeerd Back/topnavigeren, gedeeltelijke fout en uitsluitend resterende retry. Dit is geen Hilt- of Roomtest.
- De aparte wiringtest gebruikt de echte NutritionRoute, Hilt NutritionViewModel met Activity-owner, SaveFoodUseCase en Room. Correctie naar 200 g/300 kcal geeft één product met 150 kcal/100 g en standaardportie 200 g, geen maaltijd en geen replay na recreatie. Deze bewijst de productiewiring, niet de NavBackStackEntry-lifetime.

Eén eerste 17-caserun gaf 16 pass/1 fail bij de bestaande maaltijd-editfixture: de eerste callback bleef 0. Er was geen wijziging aan de maaltijdsavebody. De harness controleert nu expliciet zichtbaar/enabled en de eerste callback direct; alle identiteit-/datum-/retryasserties blijven behouden. Gerichte diagnose en daarop de gehele 17-selectie slaagden. De oorspronkelijke oorzaak is niet als bewezen productbug geclaimd. Een eerdere FirstAdd-timeout is gericht verholpen door werkelijk naar het bedoelde schermanker te scrollen; de aanvankelijke batch wordt niet als volledig groen gerapporteerd.

Visueel zijn echte Main-schermen en directe routefixtures afzonderlijk benoemd. Compacte basis: 720×1280 px, density 320 = 360×640 dp, font 1,3, donker. Routineformulier met IME, profielherstel, voedseloverzicht en vochtactie zijn met PNG én verse UI-tree bekeken. Echte Main-routine- en productmodals zijn ook op 900×640 dp/font 1,3/licht bekeken; tekst, zoekveld en acties passen of blijven via scroll bereikbaar. Daarna is het oorspronkelijke compacte donkere profiel teruggelezen. De aanvankelijke weergaveaanvraag zonder ingebouwd herstel was vóór mutatie afgewezen; de gewijzigde `try/finally`-opdracht is goedgekeurd en afgerond, zonder resterende blokkade. Het AI-reviewbeeld is een synthetische routefixture met open toetsenbord, geen live fotoanalyse of volledige Main-scaffold. Root bekeek ook het fixbeeld: de opslagactie is zichtbaar, terwijl de fixture geen productieclaim over systeembarinsets ondersteunt. De laatste fix veranderde geen layout; passende visuele evidence is hergebruikt.

## Grenzen en actuele bronnen

De specifieke eerdere mislukte gebruikersaanvraag is niet gereproduceerd met zijn sleutel/account. Structurele fouten zijn met lokale contracten en synthetische opslag-/lifecycleflows bewezen. Live providerbeschikbaarheid, quota en beeldschattingskwaliteit zijn niet vastgesteld. Er is geen menselijke TalkBack-/Switch Access-test, fysiek beluisterde timeraudio of hardwareprestatieclaim.

De app gebruikt direct de Gemini Developer API met `gemini-2.5-flash`. Google's [releasebericht van 2 november](https://firebase.google.com/support/releases) en [modeloverzicht](https://firebase.google.com/docs/ai-logic/models) kondigen voor Developer API-verzoeken vanaf 1 december 2026 automatische routering aan; dit is geen bewijs dat de app op 9 november algemeen is uitgeschakeld. De pin is behouden. Een toekomstige modelmigratie met gewijzigde tarieven/gedrag valt buiten deze reparatie; het specifieke account is niet live getest.

Overige primaire bronnen voor de gerichte keuzes: [OpenAI Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs), [Gemini structured output](https://ai.google.dev/gemini-api/docs/structured-output), [Gemini finish reasons](https://ai.google.dev/api/generate-content), [Android updatevoorwaarden](https://developer.android.com/google/play/app-updates). GitHub [skip-instructies](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/skip-workflow-runs) worden gebruikt om de bestaande PR-workflow bij deze uitsluitend lokaal geteste levering over te slaan; een GitHub-status geldt niet als testbewijs.

Ruwe logs/XML/PNG en operationele helpers blijven ignored onder `artifacts/qa/2026-11-09-routine-nutrition/`; taakbriefs/reviewrapporten staan tijdens uitvoering onder `.superpowers/sdd/2026-11-09-routine-nutrition/`. De blijvende specificatie en het plan staan onder `docs/superpowers/`. De latere afleverledger staat uitsluitend ignored onder `.codex/device-qa/apk-deliveries/`.
