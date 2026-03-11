# TrainIQ

Current runtime status:

- The app builds and runs with Compose, Hilt, Navigation, CameraX, Health Connect guards, and Gemini service fallbacks.
- Room entities, DAO, and database contracts remain present under `app/src/main/java/com/trainiq/core/database`.
- Room is not the active runtime store yet.

Room note:

- This repository previously failed on Windows during Room annotation processing because the SQLite verifier/native extraction path was not reliable.
- To keep the project buildable and runnable, runtime storage currently uses an in-memory sample-backed repository.
- The Room model layer is still intact, so persistent storage can be wired back in later without redesigning the schema.

Navigation note:

- Bottom navigation uses one root `NavController`.
- Top-level tabs use `launchSingleTop`, `restoreState`, and `findStartDestination()` to avoid duplicate entries and tab-switch crashes.
