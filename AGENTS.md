# TrainIQ — Engineering Foundation \& Standards

Dit document is de leidende standaard voor technische keuzes binnen TrainIQ. Elke implementatie moet schaalbaar, onderhoudbaar, performant en AI-native blijven.

\---

## 🚀 Projectvisie

**TrainIQ** zet passieve gezondheidsdata om in actieve coaching.

```text
Health Connect → Gemini 2.5 Flash → Material 3 UI → Persoonlijke actie
```

**Doel:** een bijna onzichtbare gebruikerservaring waarbij data automatisch wordt verzameld en inzichten proactief worden aangeboden.

\---

## 🗺️ Roadmap 2026

### Phase 1 — Foundation

* MVVM-structuur met Hilt DI
* Room entities en repositories
* Eerste Health Connect DataSource
* Gemini 1.5/2.0-integratie aanwezig, maar update nodig

### Phase 2 — Modernization \& Precision

* Type-safe navigation met `kotlinx.serialization`
* Volledige Material 3 UI met Dynamic Color
* Health Connect-sync met `ChangesToken`
* Multi-metric support: steps, heart rate, sleep, active calories en workouts
* Upgrade naar Gemini 2.5 Flash
* Thinking Budget voor coach-, advies- en rapportagefeatures

### Phase 3 — Invisible Coach

* Proactieve inzichten op basis van slaap, herstel en training
* Multimodal scanning voor maaltijden, supplementlabels en fysieke houding
* Gemini Nano voor snelle lokale feedback waar mogelijk

\---

## 🛠️ Architectuur

TrainIQ gebruikt:

```text
MVVM + Clean Architecture + Unidirectional Data Flow
```

Structuur:

```text
Data → Domain → UI
```

Regels:

* Business logic staat in `UseCases`
* UI gebruikt alleen state uit ViewModels
* Elke screen heeft één `uiState: StateFlow<T>`
* UI-state gebruikt een sealed interface: `Loading`, `Success`, `Error`
* Dependency Injection gebeurt met Hilt
* Repositories zijn `@Singleton`
* ViewModel-afhankelijke objecten zijn `@ViewModelScoped`
* Navigatie gebruikt Navigation 2.8.0+ type-safe routes
* Geen string-based routes gebruiken

Voorbeeld:

```kotlin
sealed interface UiState {
    data object Loading : UiState
    data class Success(...) : UiState
    data class Error(val message: String) : UiState
}
```

\---

## 🎨 UI/UX Standards

TrainIQ moet modern, vloeiend en rustig aanvoelen.

Verplicht:

* Gebruik overal `MaterialTheme.colorScheme`
* Gebruik overal `MaterialTheme.typography`
* Ondersteun Dynamic Color op Android 12+
* Gebruik shimmer states in plaats van standaard spinners
* Gebruik subtiele animaties met `AnimatedContent`
* Voeg haptic feedback toe bij belangrijke acties
* Ondersteun tablets en foldables met `WindowSizeClass`

Aanbevolen flows voor shared transitions:

```text
Home → Active Workout
Workout List → Workout Detail
Meal Scan → Result
```

\---

## ❤️ Health Connect Standards

Health Connect is de databron van TrainIQ en moet veilig, duidelijk en betrouwbaar werken.

Verplicht:

* Controleer altijd `HealthConnectClient.getSdkStatus()`
* Handel `PROVIDER\\\_MISSING` netjes af
* Toon eerst een Permission Manager-screen met uitleg
* Toon pas daarna de systeem-permission prompt
* Gebruik `ChangesToken` voor incrementele sync
* Haal alleen gewijzigde data op sinds de laatste sync

Belangrijke metrics:

* Steps
* Heart Rate
* Sleep
* Active Calories
* Workout Sessions

\---

## 🤖 Gemini 2.5 Flash Standards

Gemini is de reasoning engine van TrainIQ.

### Fast Mode

Voor snelle taken zoals meal scanning, barcodeherkenning en simpele classificatie.

```text
Thinking disabled
```

### Deep Mode

Voor coachadvies, weekrapporten, herstelanalyse en trainingsaanbevelingen.

```text
Thinking Budget: 500–1000 tokens
```

AI-regels:

* Gebruik standaard Gemini 2.5 Flash
* Behoud de persona van een Senior Strength Coach
* Output moet altijd JSON zijn
* Gebruik `response\\\_mime\\\_type: "application/json"`
* Gebruik nooit regex om JSON uit vrije tekst te halen

\---

## 🗄️ Database \& Quality

### Room

* Gebruik `AutoMigration` waar mogelijk
* Gebruik handmatige SQL-migraties wanneer nodig
* Controleer vóór nieuwe velden altijd:

  * `Entities.kt`
  * `DomainModels.kt`
  * `Mappers.kt`

### Performance

* Gebruik Baseline Profiles tegen startup-lag en JIT-vertraging
* Vermijd onnodige recompositions in Compose
* Houd mapping en business logic buiten de UI-laag

### Testing

Verplicht testen voor:

* `Mappers.kt`
* `UseCases.kt`
* Belangrijke repositorylogica

Aanbevolen:

* JUnit
* Turbine
* MockK

\---

## 🚦 Gemini CLI / Codex Workflow

Gebruik bij elke wijziging deze volgorde:

### 1\. Research

Controleer eerst:

```text
Entities.kt
DomainModels.kt
Repositories
UseCases
Navigation routes
```

Voeg niets dubbel of inconsistent toe.

### 2\. Act

* Gebruik `replace` voor kleine, precieze wijzigingen
* Houd wijzigingen klein en controleerbaar
* Voeg bij nieuwe AI-calls Gemini 2.5 Flash-parameters toe
* Respecteer bestaande architectuur en naming conventions

### 3\. Validate

Minimaal:

```text
Compile check
```

Waar relevant:

* Unit tests
* Mapper tests
* UseCase tests
* UI state checks

### 4\. Prompt-regel

Wanneer een verzoek het woord **prompt** bevat, moet er een **Codex-prompt** worden gegenereerd in plaats van dat de wijziging direct wordt uitgevoerd.

\---

## ✅ Hoofdregel

```text
Long-term code health > short-term speed
```

TrainIQ moet groeien als een stabiele, moderne en AI-native health-app.



