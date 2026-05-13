# TrainIQ

TrainIQ is een native Android health coaching app die passieve gezondheidsdata omzet in concrete acties voor training, voeding, herstel en voortgang.

De app combineert lokale workout- en voedingsregistratie met Health Connect en expliciete AI-coachingflows via Gemini 2.5 Flash of OpenAI. Het doel is een rustige, bijna onzichtbare coachervaring: data wordt veilig verzameld, verwerkt in de app en vertaald naar persoonlijke inzichten.

```text
Health Connect -> Room -> Domain UseCases -> Compose Material 3 UI
                         -> Gemini/OpenAI coaching
```

## Wat zit er in de app

- Home-dashboard met training, herstel, voeding en voortgangssamenvatting.
- Trainingstab met routines, actieve workout logging, setregistratie en workout-samenvattingen.
- Voedingstab met producten, barcode/camera flows en AI-ondersteunde analyse.
- Coach-tab met AI-advies, weekrapporten en routinegeneratie.
- Voortgangstab met metingen, grafieken en progressie-inzichten.
- Instellingen voor Health Connect, Gemini/OpenAI API-keys, thema en appbeheer.
- Health Connect-integratie met permission rationale, provider checks en incrementele sync.
- Room als runtime source of truth, met migratieschema's en import/export-compatibiliteit.
- Performance, diagnostics, accessibility en release readiness documentatie.

## Tech stack

- Kotlin, Jetpack Compose en Material 3
- MVVM + Clean Architecture + unidirectional data flow
- Hilt dependency injection
- Room, DataStore en WorkManager
- Navigation Compose met type-safe routepatronen
- Health Connect
- CameraX, ML Kit Barcode Scanning en Coil
- Retrofit, OkHttp en Gson voor Gemini/OpenAI API-calls
- JUnit, Turbine, AndroidX Test en Macrobenchmark

## Repository structuur

```text
.
|-- TrainIQ-Project/                 # Android app, Gradle project en Android README
|   |-- app/
|   |   |-- src/main/java/com/trainiq/
|   |   |   |-- ai/                 # Gemini services, prompts en JSON schemas
|   |   |   |-- analytics/          # Analytics engine
|   |   |   |-- core/               # Database, DI, UI, theme, health, diagnostics
|   |   |   |-- data/               # Data sources, repositories, mappers, migration
|   |   |   |-- domain/             # Domain models, repository contracts, use cases
|   |   |   |-- features/           # Compose screens per appgebied
|   |   |   |-- navigation/         # App navigation
|   |   |   |-- MainActivity.kt
|   |   |   `-- MainViewModel.kt
|   |   |-- schemas/               # Room schema history
|   |   `-- src/test, src/androidTest
|   |-- macrobenchmark/             # Startup/performance benchmark module
|   |-- docs/                       # Android QA, release, security en architecture docs
|   `-- gradle/
|-- docs/                            # Product, QA en target-state voortgang
|-- promo/                           # Promo/video/screenshot assets
|-- trainiq_design_assets/           # Design mockups en visual assets
|-- runtime-gemini-test/             # Runtime QA evidence voor Gemini flows
|-- runtime-fix-validation/          # Runtime fix validation evidence
|-- TrainIQ_Target_State_Blueprint.md
|-- AGENTS.md
`-- README.md
```

## Snel starten

Vereisten:

- JDK 17 of nieuwer
- Android Studio of Android SDK command-line tools
- Android SDK platform voor de `compileSdk` uit `TrainIQ-Project/app/build.gradle.kts`
- Emulator of fysiek device voor connected tests en Health Connect QA

Build en unit tests:

```powershell
cd .\TrainIQ-Project
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
```

Aanvullende checks:

```powershell
.\gradlew.bat :app:lintDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
.\gradlew.bat :app:checkReleaseSigningReadiness --console=plain
```

Performance validatie:

```powershell
.\gradlew.bat :macrobenchmark:assembleProfileable --console=plain
.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain
```

Gebruik voor betrouwbare macrobenchmark-cijfers bij voorkeur een fysiek device.

## AI setup

AI is standaard uitgeschakeld totdat de gebruiker AI inschakelt en lokaal minimaal een Gemini- of OpenAI API-key opslaat via Instellingen.

- Gemini model: `gemini-2.5-flash`
- OpenAI model: `gpt-4.1-mini`
- Output: JSON
- MIME type: `application/json`
- Fast scan/classification flows: thinking budget `0`
- Coaching, rapportage en routinegeneratie: thinking budget rond `1000`
- API-keys worden lokaal via Android Keystore beheerd
- Provider-routing probeert de gekozen voorkeursprovider eerst, daarna de andere ingestelde provider bij tijdelijke fouten, en eindigt met lokale fallback.

Commit nooit API-keys, keystores of production secrets.

## Health Connect QA

Test Health Connect flows minimaal met:

- geen Health Connect provider geinstalleerd;
- provider aanwezig zonder permissies;
- gedeeltelijke permissies;
- ingetrokken permissies terwijl de app open is;
- grotere datasets met paged reads en changes tokens.

De app hoort eerst een duidelijke rationale te tonen en pas daarna de systeem-permission prompt te openen.

## Ontwikkelregels

- Business logic hoort in `domain/usecase`.
- UI leest state uit ViewModels en werkt met een enkele `StateFlow` per screen.
- UI-state gebruikt sealed loading/success/error modellen.
- Repositories blijven de brug tussen data en domain.
- Mappers houden Room entities en domain models gescheiden.
- Compose gebruikt `MaterialTheme.colorScheme` en `MaterialTheme.typography`.
- Nieuwe Room-velden moeten worden afgestemd met `Entities.kt`, `DomainModels.kt` en `Mappers.kt`.
- Nieuwe AI-calls moeten JSON-contracten gebruiken en geen regex parsing van vrije tekst.

## Belangrijke documentatie

- [Android project README](TrainIQ-Project/README.md)
- [Target State Blueprint](TrainIQ_Target_State_Blueprint.md)
- [Architecture Decisions](docs/TrainIQ_Architecture_Decisions.md)
- [Target State Progress](docs/TrainIQ_Target_State_Progress.md)
- [Target State Backlog](docs/TrainIQ_Target_State_Backlog.md)
- [Release evidence](TrainIQ-Project/docs/play-privacy-release-evidence.md)

## Release notes

Release signing is bewust zonder secrets in de repository ingericht. Configureer signing via environment variables of Gradle properties zoals beschreven in `TrainIQ-Project/README.md`.

Voor release readiness zijn build, unit tests, lint, connected migration tests, Health Connect runtime checks, accessibility QA en performance evidence leidend.
