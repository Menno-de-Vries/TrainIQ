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

## 🗺️ Eindresultaat Visie

TrainIQ moet uiteindelijk functioneren als een AI-native health coach in plaats van een traditionele fitness-app.

De app moet:

* Gezondheidsdata automatisch verzamelen via Health Connect
* Trends herkennen in herstel, slaap, voeding en training
* Proactief coachadvies geven
* Een rustige, snelle en moderne UX bieden
* Volledig offline-safe en schaalbaar blijven
* Structured AI outputs gebruiken in plaats van parsing-hacks
* Eén consistente architecture flow volgen

TrainIQ wordt opgebouwd rond:

```text
Data → Domain → AI Reasoning → UI → Persoonlijke Actie
```

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

### Verplichte architectuurregels

* Business logic staat uitsluitend in `UseCases`
* UI leest alleen state uit ViewModels
* Elke screen gebruikt één `uiState: StateFlow<T>`
* UI-state gebruikt sealed interfaces:

  * Loading
  * Success
  * Error
* Dependency Injection gebeurt met Hilt
* Repositories zijn `@Singleton`
* ViewModel-afhankelijke objecten gebruiken `@ViewModelScoped`
* Geen string-based navigation routes
* Alleen type-safe navigation via `kotlinx.serialization`
* Geen business logic in composables
* Geen database mapping in UI-layer

Voorbeeld:

```kotlin
sealed interface UiState {
    data object Loading : UiState
    data class Success(...) : UiState
    data class Error(val message: String) : UiState
}
```

\---

## 🗄️ Database \& Data Standards

### Room wordt de primaire source of truth

De huidige JSON/local-store architectuur moet uiteindelijk volledig migreren naar Room.

Doelarchitectuur:

```text
Room Database
    ↓
Repositories
    ↓
UseCases
    ↓
ViewModels
    ↓
Compose UI
```

### DataStore mag alleen gebruikt worden voor:

* User preferences
* Theme settings
* AI instellingen
* Sync metadata
* Health Connect changes tokens

### Room Standards

Verplicht:

* AutoMigration waar mogelijk
* Handmatige SQL migraties wanneer nodig
* Geen breaking schema changes zonder migratie
* Repository abstraheert database volledig

Bij nieuwe velden altijd controleren:

* `Entities.kt`
* `DomainModels.kt`
* `Mappers.kt`
* `UseCases.kt`

\---

## ❤️ Health Connect Standards

Health Connect is de centrale databron van TrainIQ.

### Verplicht

* Altijd `HealthConnectClient.getSdkStatus()` controleren
* `PROVIDER\_MISSING` correct afhandelen
* Eerst rationale/permission uitleg tonen
* Daarna pas de system permission prompt
* `ChangesToken` gebruiken voor incrementele sync
* Alleen gewijzigde data ophalen
* Syncs background-safe maken

### Metrics

TrainIQ ondersteunt minimaal:

* Steps
* Heart Rate
* Sleep
* Active Calories
* Weight
* Workout Sessions

### Toekomstige uitbreidingen

* Recovery score
* HRV
* Stress trends
* Readiness analysis
* Adaptive workout intensity

\---

## 🤖 Gemini 2.5 Flash Standards

Gemini vormt de reasoning engine van TrainIQ.

### Fast Mode

Voor:

* Barcode scanning
* Meal scanning
* Food classification
* Simpele inzichten

```text
Thinking disabled
```

### Deep Mode

Voor:

* Coachadvies
* Herstelanalyse
* Weekrapporten
* Trainingsaanbevelingen
* Voedingsanalyse

```text
Thinking Budget: 500–1000 tokens
```

### AI-regels

Verplicht:

* Gemini 2.5 Flash als standaardmodel
* Structured JSON outputs
* `response\_mime\_type = "application/json"`
* Geen regex parsing van AI output
* AI persona blijft consistent:

  * Senior Strength Coach
  * Data-driven
  * Motiverend maar eerlijk

### Toekomstige AI Features

* Meal image recognition
* Supplement label scanning
* Form analysis
* Recovery predictions
* Personalized programming
* Local Gemini Nano assist waar mogelijk

\---

## 🎨 UI/UX Standards

TrainIQ moet modern, rustig en premium aanvoelen.

### Verplicht

* Overal `MaterialTheme.colorScheme`
* Overal `MaterialTheme.typography`
* Dynamic Color op Android 12+
* Geen legacy Material 2 componenten
* Shimmer loading states
* `AnimatedContent` voor subtiele animaties
* Haptic feedback bij belangrijke acties
* Adaptive layouts via `WindowSizeClass`

### UX Doelen

* Zo min mogelijk handmatige input
* AI helpt actief mee
* Geen overload aan informatie
* Home fungeert als cockpit
* Belangrijkste acties binnen 1-2 taps bereikbaar

### Shared Transition Flows

```text
Home → Active Workout
Workout List → Workout Detail
Meal Scan → Result
Progress → Deep Analysis
```

\---

## ⚡ Performance Standards

TrainIQ moet extreem vloeiend aanvoelen.

### Verplicht

* Baseline Profiles gebruiken
* Startup optimalisatie
* Geen zware work op Main Thread
* Vermijd onnodige recompositions
* Lazy loading waar mogelijk
* Stable Compose state gebruiken
* Coroutines correct scopen

### Doelstellingen

* Lage startup latency
* Geen zichtbare UI freezes
* Smooth scrolling
* Geen dubbele network calls
* Efficiënte Health Connect syncs

\---

## 🧪 Testing Standards

### Verplicht testen voor:

* `Mappers.kt`
* `UseCases.kt`
* Repository logic
* Health sync logic
* AI parsing logic
* Navigation routes

### Aanbevolen libraries

* JUnit
* MockK
* Turbine
* Compose UI Testing

### Definition of Done

Nieuwe features zijn pas klaar wanneer:

* Compile check slaagt
* Geen architectuurregels worden gebroken
* Tests aanwezig zijn
* UI state correct werkt
* Error handling aanwezig is

\---

## 🚦 Gemini CLI / Codex Workflow

### 1\. Research

Controleer eerst:

```text
Entities.kt
DomainModels.kt
Repositories
UseCases
Navigation routes
```

Voeg niets dubbel toe.

### 2\. Act

* Kleine precieze wijzigingen
* Geen massale refactors zonder noodzaak
* Respecteer naming conventions
* Respecteer bestaande architecture flow
* Gebruik structured AI configs

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

\---

## 🧠 Einddoel van TrainIQ

TrainIQ moet uiteindelijk voelen als:

```text
Een persoonlijke AI health coach
in plaats van een traditionele fitness tracker
```

De gebruiker hoeft zo min mogelijk handmatig te doen.

TrainIQ:

* Begrijpt trends
* Detecteert patronen
* Geeft hersteladvies
* Analyseert voeding
* Optimaliseert trainingen
* Houdt rekening met slaap en stress
* Werkt snel en rustig
* Voelt modern en intelligent aan

\---

## ✅ Hoofdregel

```text
Long-term code health > short-term speed
```

TrainIQ moet groeien als:

* stabiele app
* schaalbaar platform
* moderne Android app
* AI-native health ecosystem

