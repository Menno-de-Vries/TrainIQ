# TrainIQ - Gemini CLI Foundation & Standards

This document is the **authoritative source of truth** for all engineering decisions in the TrainIQ project. All implementations must align with these standards to ensure a high-performance, maintainable, and AI-native health application.

---

## 🚀 Project Vision: The "AI Health OS"
**TrainIQ** transforms passive health data into active coaching. 
- **Core Loop:** Health Connect (Ingestion) ➔ Gemini 2.5 Flash (Reasoning) ➔ Material 3 UI (Action).
- **Target:** A seamless, "invisible" user experience where data entry is automated and insights are proactive.

---

## 🗺️ Evolutionary Roadmap (2026 Vision)

### Phase 1: Foundation (Current)
- [x] Basic MVVM Structure & Hilt DI.
- [x] Core Room Entities & Repositories.
- [x] Initial Health Connect DataSource.
- [x] Gemini 1.5/2.0 integration (Needs update to 2.5).

### Phase 2: Modernization & Precision (Immediate Focus)
- [ ] **Type-Safe Navigation:** Migration from string-based routes to `kotlinx.serialization` routes.
- [ ] **Dynamic UI:** Full Material 3 implementation with Dynamic Color support and predictive animations.
- [ ] **Robust Health Connect:** Advanced sync logic with `ChangesToken` and multi-metric reading (Heart Rate, Sleep, Active Calories).
- [ ] **Gemini 2.5 Flash Upgrade:** Implement **Thinking Budget** for the "Coach" feature to provide deeper reasoning.

### Phase 3: The "Invisible" Coach (Future)
- [ ] **Proactive Insights:** Background analysis of sleep vs. training performance to suggest rest days *before* the user feels fatigued.
- [ ] **Multimodal Scanning:** Analyze not just meals, but supplement labels and physical form via video.
- [ ] **Edge AI:** Offload simple reasoning to on-device Gemini Nano where possible for instant feedback.

---

## 🛠️ High-Level Engineering Standards

### 1. Architecture: MVVM + Clean + UDF
- **Layers:** `Data` (Infrastructure), `Domain` (Business logic/UseCases), `UI` (State/Compose).
- **State Management:** Every screen must have a single `val uiState: StateFlow<T>` using a sealed interface (`Loading`, `Success`, `Error`).
- **Navigation:** Use **Type-Safe Navigation** (Navigation 2.8.0+). No strings.
- **Dependency Injection:** Hilt is mandatory. Scope instances correctly (e.g., `@Singleton` for Repositories, `@ViewModelScoped` for ViewModels).

### 2. UI/UX: The "Alive" Interface (Future-Proofing)
- **Material 3 Ecosystem:**
    - Use `MaterialTheme.colorScheme` and `MaterialTheme.typography` throughout.
    - **Dynamic Color:** Must support `dynamicLightColorScheme` for Android 12+.
- **Predictive UX:**
    - Use **Shimmer effects** (not spinners) to maintain layout structure during loads.
    - **Shared Element Transitions:** Use for seamless flow between lists and details (e.g., Home ➔ Active Workout).
- **Micro-interactions:**
    - Haptic feedback on critical actions (Button clicks, data sync completion).
    - Subtle `AnimatedContent` for state transitions.
- **Responsiveness:** Support for Foldables and Tablets using `WindowSizeClass`.

### 3. Health Connect: The Data Backbone
- **Safety First:** Always check `HealthConnectClient.getSdkStatus()` and handle `PROVIDER_MISSING` gracefully.
- **Privacy:** Implement a "Permission Manager" screen that explains data usage before showing the system prompt.
- **Atomic Sync:** Implement `ChangesToken` logic to fetch only what has changed since the last app open.

### 4. Gemini 2.5 Flash: The Reasoning Engine
- **Hybrid Reasoning:** 
    - **Fast Mode:** Disabled thinking for Meal Scanning (Speed is king).
    - **Deep Mode:** Enabled **Thinking Budget** (500-1000 tokens) for Goal Advice and Weekly Reports.
- **Persona Persistence:** System instructions must reinforce the "Senior Strength Coach" persona.
- **JSON Security:** Always use `response_mime_type: "application/json"`. Never rely on regex to find JSON in text.

### 5. Database & Quality
- **Room Migrations:** Use `AutoMigration` where possible, otherwise manual SQL migrations.
- **Optimization:** Use **Baseline Profiles** to eliminate JIT lag during startup.
- **Testing:** mandatory unit tests for `Mappers.kt` and `UseCases.kt` using `Turbine`.

---

## 🚦 Operational Workflow for Gemini CLI

1.  **Research:** Always check `Entities.kt` and `DomainModels.kt` before adding fields.
2.  **Act:** Use `replace` for surgical precision. Propose 2.5 Flash parameters in all new AI calls.
3.  **Validate:** Compile check is minimum. Propose unit tests for business logic.

---

*This mandate is active as of March 2026. Prioritize long-term code health over short-term speed.*
