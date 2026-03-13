---
name: trainiq-health-sync
description: Specialized knowledge for Samsung Health / Health Connect integration in TrainIQ. Use when implementing data sync, permission flows, or data mapping from health records to domain models.
---

# TrainIQ Health Connect Expert

This skill ensures a robust, privacy-first integration with Health Connect for the TrainIQ app.

## Core Mandates

### 1. Permission Management
- **Always check status:** Use `HealthConnectClient.getSdkStatus()` before every read/write operation.
- **Graceful Failure:** If Health Connect is not supported (`SDK_UNAVAILABLE`), inform the user and disable dependent features.
- **Permission Flow:** Implement a "Permission Manager" UI that explains *why* data is needed (e.g., "We need heart rate to adjust your workout intensity") before showing the system prompt.

### 2. Atomic Synchronization
- **ChangesToken:** Use `ChangesToken` to fetch only new or updated records since the last sync.
- **WorkManager:** Background sync should be handled by `WorkManager` with appropriate constraints (e.g., only when charging, not on low battery).

### 3. Data Mapping
- **Decoupling:** Map `androidx.health.connect.client.records.*` directly to `DomainModels.kt`. Never let Health Connect record types leak into the UI or ViewModel.
- **Unit Conversion:** Standardize all units (e.g., metric) during mapping.

### 4. Metrics to Support
- **Current:** Steps, Calories, Heart Rate.
- **Planned:** Sleep (Duration and Stages), Exercise sessions, Body composition (Weight, Fat %).

## Prohibited Patterns
- ❌ Do NOT read all data every time. Use `TimeRangeFilter` or `ChangesToken`.
- ❌ Do NOT store raw Health Connect records in Room; store mapped domain entities.
- ❌ Do NOT block the main thread for health sync operations.

## Workflow
1. **Check Availability:** Verify SDK status.
2. **Check Permissions:** Verify user has granted access.
3. **Fetch Data:** Use `readRecords` or `getChanges`.
4. **Map to Domain:** Convert raw records to TrainIQ models.
5. **Update Repository:** Save to Room or update StateFlow.
