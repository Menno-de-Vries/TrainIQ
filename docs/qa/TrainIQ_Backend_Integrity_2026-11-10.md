# TrainIQ backend — gegevensbehoud en correctheid

Datum: 10 november 2026. Basis: `87d74c1a07f3f38888dc9a2d759ab5a83829b732`.

## Scope en acceptatie

Herstel van de acht onderbouwde diagnosebevindingen, zonder nieuwe diensten,
afhankelijkheden, permissies of Room-schema. De gebruiker heeft correcties,
subagents, integratie naar main en één APK-levering via de bestaande private
Drive-map en e-mail expliciet toegestaan.

Geen bekende regressie mag worden opgeleverd. Per fout is eerst een falende
regressietest uitgevoerd; daarna volgen de betrokken lokale tests en een
onafhankelijke review. Dit is controleerbaar bewijs voor de geteste situaties,
geen absolute garantie voor alle apparaten of mogelijke invoer.

## Correcties

| Gebied | Gedrag en bescherming |
|---|---|
| Opstartimport | De legacy JSON-loader schrijft niet meer naar de echte Room-database. De eenmalige seed controleert autoriteit en bestaande rijen transactioneel; wissen en opnieuw starten mogen oude gegevens niet terugbrengen. Export wacht op initialisatie. Het legacybestand blijft intact. |
| Upgrade v11 | Verweesde ouders worden vóór afhankelijke rijen verwijderd; nullable historieverbindingen worden daarna gerepareerd. Geldige historie blijft behouden; de FK-controle blijft verplicht. |
| Weegfoto | Gelabelde spiermassa wordt uitgesloten van de losse-kg-gewichtfallback. |
| Nederlandse doelen | Vetverlies/afvallen en spieropbouw gebruiken dezelfde berekening als hun Engelse equivalenten, ook in de lokale adviesrichting. |
| Macroconsistentie | Bestaande verdeling blijft behouden als deze past. Als eiwit en vet samen het gekozen caloriebudget overschrijden, worden ze samen evenredig begrensd; resterende calorieën gaan naar koolhydraten. De test bewijst rekensamenhang, geen medische geschiktheid van een doel. |
| AI-uitwijking | Tijdelijke Gemini-transportfouten mogen de tweede provider gebruiken, uitsluitend met bestaande toestemming. Authenticatie en annulering blijven terminaal. |
| Voedingsbasis | Massa- en volumebasis blijven onderscheiden in het tijdelijke providerresultaat. Herkende volumewaarden worden niet als gramwaarden geïmporteerd; verrijking behoudt de bewerkbare schatting. Geen dichtheid wordt verzonnen en bestaande hydratatieregels blijven behouden. |
| Health Connect | Het controletijdstip en het succesvolle tijdstip van de geselecteerde stappendata zijn gescheiden. Cache, legacy zonder betrouwbare tijd, dagwisseling en ingetrokken toegang krijgen geen verzonnen verse updatetijd. |

## Onderzoek en verificatie

Alle Android-controles worden lokaal uitgevoerd vanuit `TrainIQ-Project`, met
Android Studio JBR, de bestaande Android SDK en `--offline --console=plain
--max-workers=2`. Android-instrumentatie gebruikt uitsluitend de voor deze taak
gestarte bestaande agent-AVD `TrainIQ_Agent_Migration_20260911`,
`emulator-5580`, API 36. Er zijn geen gebruikersapparaten gewist of aangepast.

- Diagnosebasis: 1.102 unittests en zeven lokale voedingsgatewaytests geslaagd.
- Opslag-RED: 19 connected tests, drie verwachte fouten: actuele profielnaam
  werd door legacy vervangen en beide nieuwe transitieve migratietests faalden
  op een foreign-key-verwijzing. De bestaande migratiescenario's slaagden.
- Overige RED: 224 gerichte unittests, tien verwachte fouten bij de concrete
  doel-, macro-, tijdstempel-, transport-, context- en vloeistofscenario's.
- Een tussentijdse compilatie overlapte een DAO-wijziging en leverde een
  verouderde gegenereerde implementatie op. Dit was geen gedragsbewijs;
  bronwijzigingen zijn daarna bevroren tijdens gezamenlijke controles.
- Brede integratie: PASS, debug build, alle 1.112 unittests, 118 connected
  tests zonder fouten of skips, lint zonder fouten (27 waarschuwingen).
  Dit omvat 18 migratietests en echte generatie van het v2–v21-migratiebewijs.
- Onafhankelijke volledige review: GPT-6 Astra, `high`; één nieuwe P2 in de
  permanente migratiemarkering na een mislukte eerste bronlezing. Twee nieuwe
  tests reproduceerden dit; de derde grenscontrole met bestaande Room-data
  bleef groen. De loader bewaart nu of de lezing werkelijk slaagde. Een
  mislukte eerste lezing blokkeert een volgende migratiepoging niet.
- Hercontrole na die correctie: PASS, opnieuw alle 1.112 unittests, build,
  lint en 80 betrokken connected datatests, waaronder alle tien opstarttests.
  De eerdere bredere UI-resultaten blijven geldig voor ongewijzigde UI-code.
- Onafhankelijke gerichte herreview: P2 **ADDRESSED**, specificatie en
  codekwaliteit akkoord; geen resterende materiële reviewbevindingen.

Exacte integratieopdracht (dezelfde taken voor beide runs):

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:generateDebugRoomMigrationChainVerificationMarker :app:lintDebug "-Pandroid.testInstrumentationRunnerArguments.class=$selectedClasses" --offline --console=plain --max-workers=2
```

`selectedClasses` voor de laatste 80-test-run, kommagescheiden:
`com.trainiq.data.repository.LegacyRoomStartupInstrumentedTest`,
`com.trainiq.core.database.TrainIqDatabaseMigrationTest`,
`com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest`,
`com.trainiq.data.migration.RoomJsonImportSinkTest`.
De bredere 118-test-run voegde hieraan toe:
`com.trainiq.data.migration.RoomImportDryRunInstrumentedTest`,
`com.trainiq.features.nutrition.BarcodeMealFlowInstrumentedTest`,
`com.trainiq.features.coach.CoachProfileStateRestorationInstrumentedTest`,
`com.trainiq.features.workout.ActiveWorkoutRestoreInstrumentedTest`,
`com.trainiq.features.workout.WorkoutCompletionRecoveryInstrumentedTest`,
`com.trainiq.features.nutrition.MealDetailPersistenceTest`.

Lokale logs en resultaten blijven ongetrackt onder
`artifacts/backend-integrity/` en `TrainIQ-Project/app/build/`.

## APK en grenzen

De vorige aantoonbare levering is `com.trainiq`, debug, versiecode 4. Deze
correctielevering krijgt `1.0.3-B`, versiecode 5, met dezelfde bestaande
debugondertekening voor een compatibele update. De uiteindelijke APK wordt
uitsluitend gebouwd uit een schone, vastgepinde remote-mainversie; de
leveringsadministratie bevat de volledige bron-SHA, hashes, updateproef,
private Drive-metadata en bevestigde e-mail-ID.

Geen live AI-verzoeken met gebruikerssleutels, echte gezondheidsgegevens of
FatSecret-productiegegevens. Geen fysieke prestatieclaims of menselijke
TalkBack/Switch Access-certificering. Onvolledige of foutieve voedingsmetadata
bij een externe bron blijft een datakwaliteitsgrens.
